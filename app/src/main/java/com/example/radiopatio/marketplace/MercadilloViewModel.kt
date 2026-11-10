package com.example.radiopatio.marketplace

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Factory necesario para poder inyectar el DAO en el ViewModel
 */
class MercadilloViewModelFactory(
    private val productoDao: ProductoDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        if(modelClass.isAssignableFrom(MercadilloViewModel::class.java)){
            return MercadilloViewModel(productoDao) as T
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
    val productosTotales: List<Producto> = emptyList(),
    val mostrarDialogoNuevoProducto: Boolean = false
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
    private val productoDao: ProductoDao
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

    private fun descargarProductosDeFirebase(){
        db.collection("productos").get().addOnSuccessListener { documentos ->
            val listaNuevosProductos = mutableListOf<ProductoEntidad>()

            for(documento in documentos){
                val titulo = documento.getString("titulo") ?: ""
                val precio = documento.getString("precio") ?: ""
                val ubicacionYTiempo = documento.getString("ubicacionYTiempo") ?: ""
                val categoria = documento.getString("categoria") ?: CategoriaProducto.TODOS.name
                val esParaComprar = documento.getBoolean("esParaComprar") ?: true

                val entidad = ProductoEntidad(
                    // Al ser autogenerado en Room, le pasamos 0 o dejamos que Room le asigne el ID
                    titulo = titulo,
                    precio = precio,
                    ubicacionYTiempo = ubicacionYTiempo,
                    categoria = categoria,
                    esParaComprar = esParaComprar,
                    imagenRes = null // De momento null hasta que integremos Coil para URLs
                )
                listaNuevosProductos.add(entidad)
            }

            viewModelScope.launch {
                withContext(Dispatchers.IO){
                    listaNuevosProductos.forEach { productoDao.insertar(it) }
                }
                cargarProductosDesdeRoom()
            }
        }
    }

    private fun actualizarUiStateConEntidades(listaEntidades: List<ProductoEntidad>) {
        val listaProductos = listaEntidades.map { entidad ->
            Producto(
                id = entidad.id,
                titulo = entidad.titulo,
                precio = entidad.precio,
                ubicacionYTiempo = entidad.ubicacionYTiempo,
                categoriaProducto = CategoriaProducto.valueOf(entidad.categoria),
                esParaComprar = entidad.esParaComprar,
                imagenRes = entidad.imagenRes
            )
        }
        uiState = uiState.copy(productosTotales = listaProductos)
    }

    fun onCrearProducto(nuevoProducto: Producto){
        val productoMap = hashMapOf(
            "titulo" to nuevoProducto.titulo,
            "precio" to nuevoProducto.precio,
            "ubicacionYTiempo" to nuevoProducto.ubicacionYTiempo,
            "categoria" to nuevoProducto.categoriaProducto.name,
            "esParaComprar" to nuevoProducto.esParaComprar
        )

        db.collection("productos").document().set(productoMap)
            .addOnSuccessListener {
                val entidad = ProductoEntidad(
                    titulo = nuevoProducto.titulo,
                    precio = nuevoProducto.precio,
                    ubicacionYTiempo = nuevoProducto.ubicacionYTiempo,
                    categoria = nuevoProducto.categoriaProducto.name,
                    esParaComprar = nuevoProducto.esParaComprar,
                    imagenRes = nuevoProducto.imagenRes
                )

                viewModelScope.launch {
                    withContext(Dispatchers.IO){
                        productoDao.insertar(entidad)
                    }
                    cargarProductosDesdeRoom()
                    uiState = uiState.copy(mostrarDialogoNuevoProducto = false)
                }
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

    fun onAbrirDialogoNuevoProducto(){
        uiState = uiState.copy(mostrarDialogoNuevoProducto = true)
    }

    fun onCerrarDialogoNuevoProducto(){
        uiState = uiState.copy(mostrarDialogoNuevoProducto = false)
    }


}