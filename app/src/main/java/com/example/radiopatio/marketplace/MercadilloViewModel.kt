package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
     * Se recalcula automáticamente y aplica filtro obligatorio por `comunidadActualId`
     * a nivel de lógica de negocio.
     */
    val productosFiltrados: List<Producto>
        get() = productosTotales.filter { producto ->
            val coincideComunidad = comunidadActualId.isNotBlank() &&
                producto.comunidadId == comunidadActualId
            val coincideTexto = producto.titulo.contains(textoBusqueda, ignoreCase = true)
            val coincideModo = producto.esParaComprar == esModoComprar
            val coincideCategoria = categoriaSeleccionada == CategoriaProducto.TODOS ||
                producto.categoriaProducto == categoriaSeleccionada

            coincideComunidad && coincideTexto && coincideModo && coincideCategoria
        }
}

class MercadilloViewModelFactory(
    private val userRepository: UserRepository = UserRepository(),
    private val mercadilloRepository: MercadilloRepository = MercadilloRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MercadilloViewModel::class.java)) {
            return MercadilloViewModel(userRepository, mercadilloRepository, comunidadRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

/**
 * Gestiona el estado y la lógica de la pantalla del Mercadillo en tiempo real con Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MercadilloViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val mercadilloRepository: MercadilloRepository = MercadilloRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(MercadilloUiState())
        private set

    init {
        observarMercadilloEnTiempoReal()
    }

    private fun observarMercadilloEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(Triple<Usuario?, String, List<Producto>>(usuario, "", emptyList()))
                    } else {
                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            mercadilloRepository.observarProductosDeComunidad(comId) { mensaje ->
                                uiState = uiState.copy(errorMensaje = mensaje)
                            }
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
                        productosTotales = productos.filter { comId.isNotBlank() && it.comunidadId == comId },
                        cargando = false
                    )
                }
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

    fun onCrearProducto(nuevoProducto: Producto) {
        val comId = uiState.comunidadActualId
        if (comId.isBlank()) {
            uiState = uiState.copy(
                errorMensaje = "Debes unirte a una comunidad antes de subir artículos."
            )
            return
        }

        val uid = uiState.usuarioActual?.uid.orEmpty()
        val nombreVendedor = uiState.usuarioActual?.nombre.orEmpty()
        val vivienda = uiState.usuarioActual?.vivienda?.takeIf { it.isNotBlank() } ?: "Mi portal"

        val productoPreparado = nuevoProducto.copy(
            comunidadId = comId,
            ubicacionYTiempo = "$vivienda · Ahora mismo"
        )

        viewModelScope.launch {
            val resultado = mercadilloRepository.crearProducto(
                producto = productoPreparado,
                comunidadId = comId,
                vendedorUid = uid,
                vendedorNombre = nombreVendedor
            )
            uiState = if (resultado.isSuccess) {
                uiState.copy(mostrarDialogoNuevoProducto = false, errorMensaje = null)
            } else {
                uiState.copy(
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "Error al publicar el artículo."
                )
            }
        }
    }

    fun onEliminarProducto(producto: Producto) {
        val comId = uiState.comunidadActualId
        if (comId.isBlank() || producto.comunidadId != comId) return

        viewModelScope.launch {
            mercadilloRepository.eliminarProducto(producto, comId)
        }
    }
}
