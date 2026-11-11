package com.example.radiopatio.marketplace

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Factory necesario para poder inyectar el DAO en el ViewModel
 */
class MercadilloViewModelFactory(
    private val productoDao: ProductoDao,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        if(modelClass.isAssignableFrom(MercadilloViewModel::class.java)){
            return MercadilloViewModel(productoDao, userRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}

/**
 * UiState inmutable para la pantalla del Mercadillo
 * Agrupa todos los datos que definen el estado de la UI
 */
data class MercadilloUiState(
    val textoBusqueda: String = "",
    val esModoComprar: Boolean = true,
    val categoriaSeleccionada: CategoriaProducto = CategoriaProducto.TODOS,
    val productosTotales: List<Producto> = emptyList()
){
    // Se recalcula automaticamente cuando cambia la busqueda o el modo
    val productosFiltrados: List<Producto>
        get() = productosTotales.filter { producto ->
            // 1. Filtrar por texto
            val coincideTexto = producto.titulo.contains(textoBusqueda, ignoreCase = true)
            // 2. Filtrar por modo
            val coincideModo = producto.esParaComprar == esModoComprar
            // 3. Filtra por categoria
            val coincideCategoria = categoriaSeleccionada == CategoriaProducto.TODOS || producto.categoriaProducto == categoriaSeleccionada

            coincideTexto && coincideModo && coincideCategoria
        }
}

/**
 * Gestiona el estaod y la lógica de la pantalla
 */
class MercadilloViewModel(
    private val productoDao: ProductoDao,
    private val userRepository: UserRepository
) : ViewModel(){

//    private val listaInicial = listOf(
//        Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h", CategoriaProducto.MUEBLES, true),
//        Producto(2, "Altavoz BT", "30€", "2ºC - Hace 2h", CategoriaProducto.ELECTRONICA, true),
//        Producto(3, "Chaqueta", "45€", "6ºA - Hace 3h", CategoriaProducto.ROPA, true),
//        Producto(4, "Osito de peluche", "3€", "3ºD - Hace 3h", CategoriaProducto.JUGUETES, false), // Para prestar
//        Producto(5, "Portátil", "220€", "4ºB - Hace 5h", CategoriaProducto.ELECTRONICA, true),
//        Producto(6, "Taladro Bosch", "25€", "Bajo A - Ayer", CategoriaProducto.HERRAMIENTAS, false) // Para prestar
//    )

    var uiState by mutableStateOf(MercadilloUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init{
        cargarProductosDesdeRoom()
        descargarProductosDeFirebase()
    }

    private fun cargarProductosDesdeRoom(){
        viewModelScope.launch {
            val listaEntidades = withContext(Dispatchers.IO){
                productoDao.obtenerProductos()
            }

            actualizarUiStateConEntidades(listaEntidades)
        }
    }

    private fun descargarProductosDeFirebase() {
        val currentUserUid = auth.currentUser?.uid

        db.collection("productos").get().addOnSuccessListener { documentos ->
            val listaNuevosProductos = mutableListOf<ProductoEntidad>()

            for (documento in documentos) {
                val autorUid = documento.getString("autorUid") ?: ""
                // No mostramos en el mercadillo los productos subidos por el propio usuario
                if (autorUid.isNotBlank() && autorUid == currentUserUid) {
                    continue
                }

                // Tampoco mostramos los que ya estén vendidos
                val estado = documento.getString("estado") ?: "DISPONIBLE"
                if (estado == "VENDIDO") {
                    continue
                }

                val titulo = documento.getString("titulo") ?: ""
                val descripcion = documento.getString("descripcion") ?: ""
                val precio = documento.getString("precio") ?: ""
                val ubicacion = documento.getString("ubicacion") ?: ""
                val fechaCreacion = documento.getLong("fechaCreacion") ?: System.currentTimeMillis()
                val categoria = documento.getString("categoria") ?: CategoriaProducto.TODOS.name
                val esParaComprar = documento.getBoolean("esParaComprar") ?: true
                val marca = documento.getString("marca") ?: ""
                val color = documento.getString("color") ?: ""
                val estadoUso = documento.getString("estadoUso") ?: ""

                @Suppress("UNCHECKED_CAST")
                val fotos = documento.get("fotos") as? List<String> ?: emptyList()
                val fotoPortada = fotos.firstOrNull()

                val entidad = ProductoEntidad(
                    firestoreId = documento.id, // Guardamos el ID real de Firestore
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
                    estado = estado,
                    imagenUrl = fotoPortada
                )
                listaNuevosProductos.add(entidad)
            }

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    productoDao.obtenerProductos().forEach { productoDao.eliminar(it) }
                    listaNuevosProductos.forEach { productoDao.insertar(it) }
                }
                cargarProductosDesdeRoom()
            }
        }
    }

    private fun actualizarUiStateConEntidades(listaEntidades: List<ProductoEntidad>) {
        val listaProductos = listaEntidades.map { entidad ->
            val tiempoString = calcularTiempoTranscurrido(entidad.fechaCreacion)

            Producto(
                id = entidad.id,
                titulo = entidad.titulo,
                precio = entidad.precio,
                ubicacionYTiempo = "${entidad.ubicacion} - $tiempoString",
                categoriaProducto = try {
                    CategoriaProducto.valueOf(entidad.categoria)
                } catch (e: Exception) {
                    CategoriaProducto.TODOS
                },
                esParaComprar = entidad.esParaComprar,
                imagenUrl = entidad.imagenUrl,
                firestoreId = entidad.firestoreId,
                autorUid = entidad.autorUid,
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

        return when{
            segundos < 60 -> "Ahora mismo"
            minutos < 60 -> "Hace $minutos min"
            horas < 24 -> "Hace $horas h"
            dias == 1L -> "Ayer"
            else -> "Hace $dias"
        }
    }

    fun onTextoBusquedaChange(nuevoTexto: String){
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onCambiarModo(esComprar: Boolean){
        uiState = uiState.copy(esModoComprar = esComprar)
    }

    fun onCategoriaSeleccionada(categoria: CategoriaProducto){
        uiState = uiState.copy(categoriaSeleccionada = categoria)
    }

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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val resultadoUsuario = userRepository.getCurrentUserData()
            val usuarioActual = resultadoUsuario.getOrNull()
            val ubicacionReal = usuarioActual?.vivienda?.ifBlank { "Mi portal" } ?: "Mi portal"
            val fechaActual = System.currentTimeMillis()

            val productoMap = hashMapOf(
                "autorUid" to (auth.currentUser?.uid ?: ""),
                "titulo" to titulo,
                "descripcion" to descripcion,
                "precio" to precio,
                "ubicacion" to ubicacionReal,
                "fechaCreacion" to fechaActual,
                "categoria" to categoria.name,
                "esParaComprar" to esParaComprar,
                "marca" to marca,
                "color" to color,
                "estadoUso" to estadoUso,
                "fotos" to fotosUris,
                "estado" to EstadoProducto.DISPONIBLE.name
            )

            db.collection("productos").document().set(productoMap)
                .addOnSuccessListener {
                    onSuccess()
                }
        }
    }
}