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

    private fun descargarProductosDeFirebase(){
        val currentUserUid = auth.currentUser?.uid

        db.collection("productos").get().addOnSuccessListener { documentos ->
            val listaNuevosProductos = mutableListOf<ProductoEntidad>()

            for(documento in documentos){
                // Filtramos para NO descargar los productos del propio usuario al mercadillo
                val autorUid = documento.getString("autorUid")
                if(autorUid != null && autorUid == currentUserUid){
                    continue // Si el producto es del usuario no lo muestro
                }

                val titulo = documento.getString("titulo") ?: ""
                val precio = documento.getString("precio") ?: ""
                val ubicacion = documento.getString("ubicacion") ?: ""
                val fechaCreacion = documento.getLong("fechaCreacion") ?: System.currentTimeMillis()
                val categoria = documento.getString("categoria") ?: CategoriaProducto.TODOS.name
                val esParaComprar = documento.getBoolean("esParaComprar") ?: true

                val entidad = ProductoEntidad(
                    // Al ser autogenerado en Room, le pasamos 0 o dejamos que Room le asigne el ID
                    titulo = titulo,
                    precio = precio,
                    ubicacion = ubicacion,
                    fechaCreacion = fechaCreacion,
                    categoria = categoria,
                    esParaComprar = esParaComprar,
                    imagenRes = null // De momento null hasta que integremos Coil para URLs
                )
                listaNuevosProductos.add(entidad)
            }

            viewModelScope.launch {
                withContext(Dispatchers.IO){
                    productoDao.obtenerProductos().forEach{ productoDao.eliminar(it) }
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
                categoriaProducto = CategoriaProducto.valueOf(entidad.categoria),
                esParaComprar = entidad.esParaComprar,
                imagenRes = entidad.imagenRes
            )
        }
        uiState = uiState.copy(productosTotales = listaProductos)
    }

    fun onCrearProducto(nuevoProducto: Producto, onSuccess: () -> Unit){
        viewModelScope.launch {
            val resultadoUsuario = userRepository.getCurrentUserData()
            val usuarioActual = resultadoUsuario.getOrNull()

            val ubicacionReal = if(usuarioActual != null && usuarioActual.vivienda.isNotBlank()){
                usuarioActual.vivienda
            } else {
                "Mi portal"
            }

            val fechaActual = System.currentTimeMillis()

            val productoMap = hashMapOf(
                "titulo" to nuevoProducto.titulo,
                "precio" to nuevoProducto.precio,
                "ubicacion" to ubicacionReal,
                "fechaCreacion" to fechaActual,
                "categoria" to nuevoProducto.categoriaProducto.name,
                "esParaComprar" to nuevoProducto.esParaComprar
            )

            db.collection("productos").document().set(productoMap)
                .addOnSuccessListener {
                    // Este producto no lo guardamos en la bd local de Room del mercadillo
                    // porque no lo queremos en esa ventana.
                    uiState = uiState.copy(mostrarDialogoNuevoProducto = false)
                    onSuccess()
                }
        }
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

    fun onAbrirDialogoNuevoProducto(){
        uiState = uiState.copy(mostrarDialogoNuevoProducto = true)
    }

    fun onCerrarDialogoNuevoProducto(){
        uiState = uiState.copy(mostrarDialogoNuevoProducto = false)
    }


}