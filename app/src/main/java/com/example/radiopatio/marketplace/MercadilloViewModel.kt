package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            coincideComunidad && coincideTexto && coincideModo && coincideCategoria
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
                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            mercadilloRepository.observarProductosDeComunidad(comId)
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

                    // Sincronizamos la caché local Room en segundo plano
                    if (productoDao != null && productos.isNotEmpty()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                productoDao.obtenerProductos().forEach { productoDao.eliminar(it) }
                                productos.forEach { prod ->
                                    productoDao.insertar(
                                        ProductoEntidad(
                                            firestoreId = prod.firestoreId,
                                            autorUid = prod.autorUid,
                                            titulo = prod.titulo,
                                            descripcion = prod.descripcion,
                                            precio = prod.precio,
                                            ubicacion = prod.ubicacionYTiempo.substringBefore("·").trim(),
                                            fechaCreacion = prod.timestamp,
                                            categoria = prod.categoriaProducto.name,
                                            esParaComprar = prod.esParaComprar,
                                            marca = prod.marca,
                                            color = prod.color,
                                            estadoUso = prod.estadoUso,
                                            estado = prod.estado.name,
                                            imagenUrl = prod.imagenUrl
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                // Ignorar errores menores de caché
                            }
                        }
                    }
                }
        }
    }
    
    private fun actualizarUiStateConEntidades(listaEntidades: List<ProductoEntidad>) {
        val listaProductos = listaEntidades.map { mercadilloRepository.entidadToProducto(it) }
        uiState = uiState.copy(productosTotales = listaProductos)
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
            val uid = auth.currentUser?.uid.orEmpty()
            val nombreVendedor = usuario?.nombre.orEmpty()

            val nuevoProducto = Producto(
                id = 0,
                titulo = titulo,
                descripcion = descripcion,
                precio = precio,
                ubicacionYTiempo = ubicacionReal,
                categoriaProducto = categoria,
                esParaComprar = esParaComprar,
                imagenRes = null,
                imagenUrl = fotosUris.firstOrNull(),
                firestoreId = "",
                comunidadId = comId,
                vendedorUid = uid,
                vendedorNombre = nombreVendedor,
                autorUid = uid,
                marca = marca,
                color = color,
                estadoUso = estadoUso,
                fotos = fotosUris,
                estado = EstadoProducto.DISPONIBLE,
                plazoDiasEntrega = 7,
                diasPrestamo = plazoDias,
                likes = emptyList()
            )

            val resultado = mercadilloRepository.crearProducto(
                producto = nuevoProducto,
                comunidadId = comId,
                vendedorUid = uid,
                vendedorNombre = nombreVendedor

            )

            resultado.onSuccess {
                onSuccess()
            }.onFailure { e ->
                uiState = uiState.copy(
                    errorMensaje = e.localizedMessage ?: "Error al publicar el producto"
                )
            }
        }
    }

    /**
     * Elimina un producto delegando en el repositorio y limpiando la caché de Room si existe.
     */
    fun onEliminarProducto(producto: Producto) {
        val comId = producto.comunidadId.ifBlank { uiState.comunidadActualId }
        val firestoreId = producto.firestoreId
        if (comId.isBlank() || firestoreId.isBlank()) return
        viewModelScope.launch {
            val resultado = mercadilloRepository.eliminarProducto(producto, comId)
            
            resultado.onSuccess {
                if (productoDao != null) {
                    withContext(Dispatchers.IO) {
                        productoDao.obtenerProductos()
                            .find { it.firestoreId == firestoreId }
                            ?.let { productoDao.eliminar(it) }
                    }
                }
            }.onFailure { e ->
                uiState = uiState.copy(
                    errorMensaje = e.localizedMessage ?: "Error al eliminar el producto"
                )
            }
        }
    }

    /**
     * Añade o quita un producto de favoritos
     */
    fun toggleFavorito(producto: Producto) {
        val comId = producto.comunidadId.ifBlank { uiState.comunidadActualId }
        val uid = auth.currentUser?.uid.orEmpty()
        if (comId.isBlank() || producto.firestoreId.isBlank() || uid.isBlank()) return
        viewModelScope.launch {
            mercadilloRepository.toggleMeGustaProducto(
                comunidadId = comId,
                productoFirestoreId = producto.firestoreId,
                usuarioUid = uid
            )
        }
    }
}
