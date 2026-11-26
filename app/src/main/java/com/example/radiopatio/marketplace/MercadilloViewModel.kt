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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Estados posibles de la sincronización con Firestore (interfaz sellada).
 */
sealed interface EstadoActualizacion {
    data object Inactivo : EstadoActualizacion
    data object Actualizando : EstadoActualizacion
    data class Error(val mensaje: String) : EstadoActualizacion
}

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
    val currentUid: String = "",
    val productosTotales: List<Producto> = emptyList(),
    val actualizacion: EstadoActualizacion = EstadoActualizacion.Inactivo
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
            val activoEnFeed = producto.estado != EstadoProducto.VENDIDO &&
                    producto.estado != EstadoProducto.DEVUELTO
            val coincideComunidad = comunidadActualId.isBlank() ||
                    producto.comunidadId.isBlank() ||
                    producto.comunidadId == comunidadActualId
            val coincideTexto = producto.titulo.contains(textoBusqueda, ignoreCase = true)
            val coincideModo = producto.esParaComprar == esModoComprar
            val coincideCategoria = categoriaSeleccionada == CategoriaProducto.TODOS ||
                    producto.categoriaProducto == categoriaSeleccionada

            activoEnFeed && coincideComunidad && coincideTexto && coincideModo && coincideCategoria
        }
}

/**
 * Factory flexible para instanciar MercadilloViewModel tanto con Room (DAO)
 * como con repositorios de Firestore y Comunidad.
 */
class MercadilloViewModelFactory(
    private val mercadilloRepository: MercadilloRepository,
    private val userRepository: UserRepository,
    private val comunidadRepository: ComunidadRepository
) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MercadilloViewModel::class.java)) {
            return MercadilloViewModel(
                mercadilloRepository = mercadilloRepository,
                userRepository = userRepository,
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
    private val mercadilloRepository: MercadilloRepository,
    private val userRepository: UserRepository,
    private val comunidadRepository: ComunidadRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    var uiState by mutableStateOf(
        MercadilloUiState(currentUid = auth.currentUser?.uid.orEmpty())
    )
        private set

    init {
        observarProductosDesdeRoom()
        observarUsuarioYComunidad()
    }

    private fun observarProductosDesdeRoom(){
        viewModelScope.launch {
            mercadilloRepository.observarProductos().collect { productos ->
                uiState = uiState.copy(productosTotales = productos)
            }
        }
    }

    private fun observarUsuarioYComunidad() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(Pair(usuario, ""))
                    } else {
                        comunidadRepository.observarComunidadPorId(comId).map { comunidad ->
                            val nombreCom = comunidad?.nombre?.takeIf { it.isNotBlank() }
                                ?: usuario?.comunidadNombre
                                ?: ""
                            Pair(usuario, nombreCom)
                        }
                    }
                }
                .collect { (usuario, nombreCom) ->
                    val comId = usuario?.comunidadId.orEmpty()
                    val cambioComunidad = comId != uiState.comunidadActualId

                    uiState = uiState.copy(
                        usuarioActual = usuario,
                        comunidadActualId = comId,
                        comunidadNombre = nombreCom,
                        currentUid = auth.currentUser?.uid.orEmpty()
                    )

                    if (cambioComunidad || uiState.productosTotales.isEmpty()) {
                        actualizarProductos()
                    }
                }
        }
    }

    fun actualizarProductos() {
        val comId = uiState.comunidadActualId
        viewModelScope.launch {
            uiState = uiState.copy(actualizacion = EstadoActualizacion.Actualizando)
            try {
                mercadilloRepository.actualizarProductos(comId)
                uiState = uiState.copy(actualizacion = EstadoActualizacion.Inactivo)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    actualizacion = EstadoActualizacion.Error(
                        e.localizedMessage ?: "Error al actualizar el mercadillo"
                    )
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

    fun onLimpiarError() {
        if(uiState.actualizacion is EstadoActualizacion.Error){
            uiState = uiState.copy(actualizacion = EstadoActualizacion.Inactivo)
        }
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
            val usuario = userRepository.getCurrentUserData().getOrNull() ?: uiState.usuarioActual
            val comId = usuario?.comunidadId.orEmpty().ifBlank { uiState.comunidadActualId }

            if (comId.isBlank()) {
                uiState = uiState.copy(
                    actualizacion = EstadoActualizacion.Error(
                        "Debes pertenecer a una comunidad para publicar anuncios."
                    )
                )
                return@launch
            }

            val ubicacionReal = usuario?.vivienda?.ifBlank { "Mi portal" } ?: "Mi portal"
            val uid = auth.currentUser?.uid.orEmpty()
            val nombreVendedor = usuario?.nombre.orEmpty()

            val nuevoProducto = Producto(
                titulo = titulo,
                descripcion = descripcion,
                precio = precio,
                ubicacion = ubicacionReal,
                categoriaProducto = categoria,
                esParaComprar = esParaComprar,
                comunidadId = comId,
                vendedorUid = uid,
                vendedorNombre = nombreVendedor,
                marca = marca,
                color = color,
                estadoUso = estadoUso,
                fotos = fotosUris,
                estado = EstadoProducto.DISPONIBLE,
                plazoDiasEntrega = 7,
                diasPrestamo = plazoDias,
                likes = emptyList()
            )

            mercadilloRepository.crearProducto(
                producto = nuevoProducto,
                comunidadId = comId,
                vendedorUid = uid,
                vendedorNombre = nombreVendedor
            ).onSuccess {
                onSuccess()
            }.onFailure { e ->
                uiState = uiState.copy(
                    actualizacion = EstadoActualizacion.Error(
                        e.localizedMessage ?: "Error al publicar el producto"
                    )
                )
            }
        }
    }

    /**
     * Elimina un producto delegando en el repositorio y limpiando la caché de Room si existe.
     */
    fun onEliminarProducto(producto: Producto) {
        val comId = producto.comunidadId.ifBlank { uiState.comunidadActualId }

        if (comId.isBlank() || producto.id.isBlank()) return
        viewModelScope.launch {
            mercadilloRepository.eliminarProducto(producto, comId)
                .onFailure { e ->
                    uiState = uiState.copy(
                        actualizacion = EstadoActualizacion.Error(
                            e.localizedMessage ?: "Error al eliminar el producto"
                        )
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
        if (comId.isBlank() || producto.id.isBlank() || uid.isBlank()) return
        viewModelScope.launch {
            mercadilloRepository.toggleMeGustaProducto(
                comunidadId = comId,
                productoId = producto.id,
                usuarioUid = uid
            )
        }
    }
}
