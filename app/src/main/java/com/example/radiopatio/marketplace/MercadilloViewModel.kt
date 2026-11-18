package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.comunidad.migrarDocumentosRaizAComunidad
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * UiState inmutable para la pantalla del Mercadillo.
 * Agrupa todos los datos que definen el estado de la UI con aislamiento por comunidad.
 */
data class MercadilloUiState(
    val textoBusqueda: String = "",
    val esModoComprar: Boolean = true,
    val categoriaSeleccionada: CategoriaProducto = CategoriaProducto.TODOS,
    val comunidadActualId: String = "",
    val comunidadNombre: String = "",
    val usuarioActual: Usuario? = null,
    val productosTotales: List<Producto> = emptyList(),
    val mostrarDialogoNuevoProducto: Boolean = false,
    val cargando: Boolean = true,
    val errorMensaje: String? = null
) {
    /**
     * Se recalcula automáticamente y aplica:
     * 1. Filtro obligatorio por `comunidadActualId`
     * 2. Búsqueda por texto
     * 3. Modo (Comprar / Prestar)
     * 4. Categoría
     * 5. Exclusión de los productos publicados por el usuario activo (no ve sus propios artículos en el feed)
     */
    val productosFiltrados: List<Producto>
        get() = productosTotales.filter { producto ->
            val coincideComunidad = comunidadActualId.isBlank() ||
                producto.comunidadId.isBlank() ||
                producto.comunidadId == comunidadActualId
            val coincideTexto = producto.titulo.contains(textoBusqueda, ignoreCase = true)
            val coincideModo = producto.esParaComprar == esModoComprar
            val coincideCategoria = categoriaSeleccionada == CategoriaProducto.TODOS ||
                producto.categoriaProducto == categoriaSeleccionada

            val miUid = usuarioActual?.uid.orEmpty().ifBlank {
                FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            }
            val noEsMio = miUid.isBlank() || (producto.autorUid != miUid && producto.vendedorUid != miUid)

            coincideComunidad && coincideTexto && coincideModo && coincideCategoria && noEsMio
        }
}

/**
 * Factory flexible para instanciar MercadilloViewModel tanto con Room (DAO)
 * como con repositorios de Firestore y Comunidad.
 */
class MercadilloViewModelFactory(
    private val productoDao: ProductoDao? = null,
    private val userRepository: UserRepository = UserRepository(),
    private val mercadilloRepository: MercadilloRepository = MercadilloRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {

    constructor(productoDao: ProductoDao, userRepository: UserRepository) : this(
        productoDao = productoDao,
        userRepository = userRepository,
        mercadilloRepository = MercadilloRepository(),
        comunidadRepository = ComunidadRepository()
    )

    constructor() : this(
        productoDao = null,
        userRepository = UserRepository(),
        mercadilloRepository = MercadilloRepository(),
        comunidadRepository = ComunidadRepository()
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MercadilloViewModel::class.java)) {
            return MercadilloViewModel(
                productoDao = productoDao,
                userRepository = userRepository,
                mercadilloRepository = mercadilloRepository,
                comunidadRepository = comunidadRepository
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

/**
 * Gestiona el estado y la lógica de la pantalla del Mercadillo en tiempo real dentro de
 * la subcolección `comunidades/{comunidadId}/productos`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MercadilloViewModel(
    private val productoDao: ProductoDao? = null,
    private val userRepository: UserRepository = UserRepository(),
    private val mercadilloRepository: MercadilloRepository = MercadilloRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(MercadilloUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        if (productoDao != null) {
            cargarProductosDesdeRoom()
        }
        observarMercadilloEnTiempoReal()
    }

    private fun cargarProductosDesdeRoom() {
        if (productoDao == null) return
        viewModelScope.launch {
            val listaEntidades = withContext(Dispatchers.IO) {
                productoDao.obtenerProductos()
            }
            if (uiState.productosTotales.isEmpty() && listaEntidades.isNotEmpty()) {
                actualizarUiStateConEntidades(listaEntidades)
            }
        }
    }

    private fun observarMercadilloEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(Triple(usuario, "", emptyList<Producto>()))
                    } else {
                        // Migra de forma transparente cualquier producto raíz previo a la subcolección de la comunidad
                        launch {
                            db.migrarDocumentosRaizAComunidad(comId, "productos")
                        }

                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            observarProductosFirestore(comId)
                        ) { comunidad, productos ->
                            val nombreCom = comunidad?.nombre?.takeIf { it.isNotBlank() }
                                ?: usuario?.comunidadNombre
                                ?: ""
                            Triple(usuario, nombreCom, productos)
                        }
                    }
                }
                .collect { (usuario, nombreCom, productos) ->
                    val comId = usuario?.comunidadId.orEmpty()
                    uiState = uiState.copy(
                        comunidadActualId = comId,
                        comunidadNombre = nombreCom,
                        usuarioActual = usuario,
                        productosTotales = productos,
                        cargando = false
                    )
                }
        }
    }

    private fun observarProductosFirestore(comunidadId: String): Flow<List<Producto>> = callbackFlow {
        // Escucha en tiempo real la subcolección comunidades/{comunidadId}/productos
        val subcoleccionRef = db.subcoleccionComunidad(comunidadId, "productos")
        val listener = subcoleccionRef
            .addSnapshotListener { documentos, error ->
                if (error != null || documentos == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val listaNuevosProductos = mutableListOf<Producto>()
                val listaEntidadesParaRoom = mutableListOf<ProductoEntidad>()

                for (documento in documentos) {
                    val autorUid = documento.getString("autorUid") ?: documento.getString("vendedorUid") ?: ""
                    val vendedorUid = documento.getString("vendedorUid") ?: autorUid

                    val estadoStr = documento.getString("estado") ?: EstadoProducto.DISPONIBLE.name
                    val estado = try {
                        EstadoProducto.valueOf(estadoStr)
                    } catch (e: Exception) {
                        EstadoProducto.DISPONIBLE
                    }

                    // Omitimos productos ya completados en el mercadillo público
                    if (estado == EstadoProducto.VENDIDO || estado == EstadoProducto.DEVUELTO) {
                        continue
                    }

                    val titulo = documento.getString("titulo") ?: ""
                    val descripcion = documento.getString("descripcion") ?: ""
                    val precio = documento.getString("precio") ?: ""
                    val ubicacion = documento.getString("ubicacion") ?: documento.getString("ubicacionYTiempo") ?: "Comunidad"
                    val fechaCreacion = documento.getLong("fechaCreacion") ?: documento.getLong("timestamp") ?: System.currentTimeMillis()
                    val categoria = documento.getString("categoria") ?: CategoriaProducto.TODOS.name
                    val esParaComprar = documento.getBoolean("esParaComprar") ?: true
                    val marca = documento.getString("marca") ?: ""
                    val color = documento.getString("color") ?: ""
                    val estadoUso = documento.getString("estadoUso") ?: ""
                    val compradorUid = documento.getString("compradorUid") ?: ""
                    val plazoDias = documento.getLong("plazoDiasEntrega")?.toInt() ?: 7
                    val diasPrestamo = documento.getLong("diasPrestamo")?.toInt() ?: 7
                    val vendedorNombre = documento.getString("vendedorNombre") ?: ""

                    @Suppress("UNCHECKED_CAST")
                    val fotos = documento.get("fotos") as? List<String> ?: emptyList()
                    val fotoPortada = fotos.firstOrNull() ?: documento.getString("imagenUrl")

                    val tiempoString = calcularTiempoTranscurrido(fechaCreacion)
                    val ubicacionYTiempo = if (ubicacion.contains("·") || ubicacion.contains("-")) ubicacion else "$ubicacion · $tiempoString"
                    val numericId = documento.getLong("id")?.toInt() ?: abs(documento.id.hashCode())

                    val prod = Producto(
                        id = numericId,
                        titulo = titulo,
                        precio = precio,
                        ubicacionYTiempo = ubicacionYTiempo,
                        categoriaProducto = try {
                            CategoriaProducto.valueOf(categoria)
                        } catch (e: Exception) {
                            CategoriaProducto.TODOS
                        },
                        esParaComprar = esParaComprar,
                        imagenRes = null,
                        imagenUrl = fotoPortada,
                        firestoreId = documento.id,
                        comunidadId = comunidadId,
                        vendedorUid = vendedorUid,
                        vendedorNombre = vendedorNombre,
                        timestamp = fechaCreacion,
                        autorUid = autorUid,
                        descripcion = descripcion,
                        marca = marca,
                        color = color,
                        estadoUso = estadoUso,
                        fotos = if (fotos.isNotEmpty()) fotos else if (!fotoPortada.isNullOrBlank()) listOf(fotoPortada) else emptyList(),
                        estado = estado,
                        plazoDiasEntrega = plazoDias,
                        diasPrestamo = diasPrestamo,
                        compradorUid = compradorUid
                    )

                    listaNuevosProductos.add(prod)

                    if (productoDao != null) {
                        listaEntidadesParaRoom.add(
                            ProductoEntidad(
                                firestoreId = documento.id,
                                autorUid = autorUid,
                                titulo = titulo,
                                descripcion = descripcion,
                                precio = precio,
                                ubicacion = ubicacion,
                                fechaCreacion = fechaCreacion,
                                categoria = categoria,
                                esParaComprar = esParaComprar,
                                marca = marca,
                                color = color,
                                estadoUso = estadoUso,
                                estado = estadoStr,
                                imagenUrl = fotoPortada
                            )
                        )
                    }
                }

                trySend(listaNuevosProductos)

                if (productoDao != null && listaEntidadesParaRoom.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            productoDao.obtenerProductos().forEach { productoDao.eliminar(it) }
                            listaEntidadesParaRoom.forEach { productoDao.insertar(it) }
                        } catch (e: Exception) {
                            // Ignorar errores menores de caché Room
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    private fun actualizarUiStateConEntidades(listaEntidades: List<ProductoEntidad>) {
        val listaProductos = listaEntidades.map { entidad ->
            val tiempoString = calcularTiempoTranscurrido(entidad.fechaCreacion)

            Producto(
                id = entidad.id,
                titulo = entidad.titulo,
                precio = entidad.precio,
                ubicacionYTiempo = "${entidad.ubicacion} · $tiempoString",
                categoriaProducto = try {
                    CategoriaProducto.valueOf(entidad.categoria)
                } catch (e: Exception) {
                    CategoriaProducto.TODOS
                },
                esParaComprar = entidad.esParaComprar,
                imagenUrl = entidad.imagenUrl,
                firestoreId = entidad.firestoreId,
                autorUid = entidad.autorUid,
                vendedorUid = entidad.autorUid,
                descripcion = entidad.descripcion,
                marca = entidad.marca,
                color = entidad.color,
                estadoUso = entidad.estadoUso,
                fotos = if (!entidad.imagenUrl.isNullOrBlank()) listOf(entidad.imagenUrl) else emptyList(),
                estado = try {
                    EstadoProducto.valueOf(entidad.estado)
                } catch (e: Exception) {
                    EstadoProducto.DISPONIBLE
                }
            )
        }
        uiState = uiState.copy(productosTotales = listaProductos)
    }

    private fun calcularTiempoTranscurrido(tiempoMilisegundos: Long): String {
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - tiempoMilisegundos

        val segundos = diferencia / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            segundos < 60 -> "Ahora mismo"
            minutos < 60 -> "Hace $minutos min"
            horas < 24 -> "Hace $horas h"
            dias == 1L -> "Ayer"
            else -> "Hace $dias d"
        }
    }

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onCambiarModo(esComprar: Boolean) {
        uiState = uiState.copy(esModoComprar = esComprar)
    }

    fun onCategoriaSeleccionada(categoria: CategoriaProducto) {
        uiState = uiState.copy(categoriaSeleccionada = categoria)
    }

    fun onAbrirDialogoNuevoProducto() {
        uiState = uiState.copy(mostrarDialogoNuevoProducto = true, errorMensaje = null)
    }

    fun onCerrarDialogoNuevoProducto() {
        uiState = uiState.copy(mostrarDialogoNuevoProducto = false, errorMensaje = null)
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    /**
     * Publicación completa utilizada por PantallaCrearProducto (soporte de fotos y datos avanzados).
     * Se guarda directamente en comunidades/{comunidadId}/productos.
     */
    fun publicarNuevoProducto(
        titulo: String,
        descripcion: String,
        precio: String,
        esParaComprar: Boolean,
        categoria: CategoriaProducto,
        marca: String,
        color: String,
        estadoUso: String,
        fotosUris: List<String>,
        plazoDias: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val resultadoUsuario = userRepository.getCurrentUserData()
            val usuario = resultadoUsuario.getOrNull()
            val comId = usuario?.comunidadId.orEmpty().ifBlank { uiState.comunidadActualId }
            if (comId.isBlank()) {
                uiState = uiState.copy(errorMensaje = "Debes pertenecer a una comunidad para publicar anuncios.")
                return@launch
            }

            val ubicacionReal = usuario?.vivienda?.ifBlank { "Mi portal" } ?: "Mi portal"
            val fechaActual = System.currentTimeMillis()
            val uid = auth.currentUser?.uid.orEmpty()
            val nombreVendedor = usuario?.nombre.orEmpty()

            val docRef = db.subcoleccionComunidad(comId, "productos").document()
            val numericId = abs(docRef.id.hashCode())

            val productoMap = hashMapOf(
                "id" to numericId,
                "firestoreId" to docRef.id,
                "comunidadId" to comId,
                "autorUid" to uid,
                "vendedorUid" to uid,
                "vendedorNombre" to nombreVendedor,
                "titulo" to titulo,
                "descripcion" to descripcion,
                "precio" to precio,
                "ubicacion" to ubicacionReal,
                "ubicacionYTiempo" to "$ubicacionReal · Ahora mismo",
                "fechaCreacion" to fechaActual,
                "timestamp" to fechaActual,
                "categoria" to categoria.name,
                "esParaComprar" to esParaComprar,
                "marca" to marca,
                "color" to color,
                "estadoUso" to estadoUso,
                "fotos" to fotosUris,
                "plazoDiasEntrega" to 7,        // 7 días para entrega presencial
                "diasPrestamo" to plazoDias,    // Duración acordada del préstamo
                "estado" to EstadoProducto.DISPONIBLE.name
            )

            docRef.set(productoMap)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    uiState = uiState.copy(
                        errorMensaje = e.localizedMessage ?: "Error al publicar el producto"
                    )
                }
        }
    }

    /**
     * Publicación directa a partir de un objeto Producto (usada por diálogos rápidos).
     */
    fun onCrearProducto(nuevoProducto: Producto) {
        val comId = uiState.comunidadActualId
        if (comId.isBlank()) {
            uiState = uiState.copy(
                errorMensaje = "Debes unirte a una comunidad antes de subir artículos."
            )
            return
        }

        viewModelScope.launch {
            val uid = uiState.usuarioActual?.uid.orEmpty().ifBlank { auth.currentUser?.uid.orEmpty() }
            val nombreVendedor = uiState.usuarioActual?.nombre.orEmpty()
            val vivienda = uiState.usuarioActual?.vivienda?.takeIf { it.isNotBlank() } ?: "Mi portal"
            val fechaActual = System.currentTimeMillis()

            val docRef = db.subcoleccionComunidad(comId, "productos").document()
            val numericId = abs(docRef.id.hashCode())

            val productoMap = hashMapOf(
                "id" to numericId,
                "firestoreId" to docRef.id,
                "comunidadId" to comId,
                "autorUid" to uid,
                "vendedorUid" to uid,
                "vendedorNombre" to nombreVendedor,
                "titulo" to nuevoProducto.titulo,
                "descripcion" to nuevoProducto.descripcion,
                "precio" to nuevoProducto.precio,
                "ubicacion" to vivienda,
                "ubicacionYTiempo" to "$vivienda · Ahora mismo",
                "fechaCreacion" to fechaActual,
                "timestamp" to fechaActual,
                "categoria" to nuevoProducto.categoriaProducto.name,
                "esParaComprar" to nuevoProducto.esParaComprar,
                "marca" to nuevoProducto.marca,
                "color" to nuevoProducto.color,
                "estadoUso" to nuevoProducto.estadoUso,
                "fotos" to nuevoProducto.fotos,
                "plazoDiasEntrega" to nuevoProducto.plazoDiasEntrega,
                "diasPrestamo" to nuevoProducto.diasPrestamo,
                "estado" to nuevoProducto.estado.name
            )

            docRef.set(productoMap)
                .addOnSuccessListener {
                    uiState = uiState.copy(mostrarDialogoNuevoProducto = false, errorMensaje = null)
                }
                .addOnFailureListener { e ->
                    uiState = uiState.copy(
                        errorMensaje = e.localizedMessage ?: "Error al publicar el artículo."
                    )
                }
        }
    }

    fun onEliminarProducto(producto: Producto) {
        val comId = producto.comunidadId.ifBlank { uiState.comunidadActualId }
        val firestoreId = producto.firestoreId
        if (comId.isNotBlank() && firestoreId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    db.subcoleccionComunidad(comId, "productos").document(firestoreId).delete()
                } catch (e: Exception) {
                    uiState = uiState.copy(
                        errorMensaje = e.localizedMessage ?: "Error al eliminar el artículo."
                    )
                }
            }
        }
    }
}
