package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.example.radiopatio.mensajeria.Conversacion
import com.example.radiopatio.notificacion.NotificacionesHelper
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la pantalla de detalle de un producto del mercadillo.
 */
data class DetalleProductoUiState(
    val cargando: Boolean = true,
    val producto: Producto? = null,
    val esPropietario: Boolean = false,
    val esComprador: Boolean = false,
    val esFavorito: Boolean = false,
    val diasRestantes: Long = 0L,
    val conversacionesProducto: List<Conversacion> = emptyList(),
    val mensaje: String? = null
)

/**
 * Factoría para inyectar dependencias en DetalleProductoViewModel.
 */
class DetalleProductoViewModelFactory(
    private val productoId: String,
    private val mercadilloRepository: MercadilloRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetalleProductoViewModel(productoId, mercadilloRepository, userRepository) as T
    }
}

/**
 * ViewModel del detalle de un producto:
 * - Observa el producto reactivamente desde Room (Caché local como Single Source of Truth).
 * - Delega todas las acciones (compra, confirmaciones, conflictos, favoritos) en MercadilloRepository.
 */
class DetalleProductoViewModel(
    private val productoId: String,
    private val mercadilloRepository: MercadilloRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf(DetalleProductoUiState())
        private set

    val currentUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var conversacionesListener: ListenerRegistration? = null

    init {
        observarProducto()
        cargarProductoSiFalta()
    }


    /**
     * Observa el producto en Room mediante Flow.
     * Si la entrega está vencida y no hay conflicto, solicita su autocompletado en segundo plano.
     */
    private fun observarProducto() {
        if (productoId.isBlank()) {
            uiState = uiState.copy(cargando = false)
            return
        }

        viewModelScope.launch {

            viewModelScope.launch {
                mercadilloRepository.observarProductoPorId(productoId).collect{ producto ->
                    if(producto == null) {
                        uiState = uiState.copy(cargando = false, producto = null)
                        return@collect
                    }
                    // Autocompletar automaticamente si ha vencido el plazo sin disputa
                    mercadilloRepository.autocompletarEntregaSiVencida(producto)

                    val ahora = System.currentTimeMillis()
                    val diffMilis = producto.fechaLimiteEntrega - ahora
                    val diasRestantes = if (diffMilis > 0) (diffMilis / (24 * 60 * 60 * 1000L)) + 1 else 0L

                uiState = uiState.copy(
                    cargando = false,
                    producto = prod,
                    esPropietario = (autorUid == currentUid),
                    esComprador = (compradorUid == currentUid),
                    diasRestantes = diasRestantes
                )

                    observarConversacionesProducto(
                        comunidadId = comIdProducto,
                        productoId = snapshot.id,
                        esPropietario = esProp
                    )
            }
        }
    }

    private fun observarConversacionesProducto(
        comunidadId: String,
        productoId: String,
        esPropietario: Boolean
    ) {
        conversacionesListener?.remove()
        conversacionesListener = null

        if (!esPropietario || currentUid.isBlank() || productoId.isBlank()) {
            uiState = uiState.copy(conversacionesProducto = emptyList())
            return
        }

        val comIdBusqueda = comunidadId.ifBlank { "general" }
        conversacionesListener = db.collection("comunidades")
            .document(comIdBusqueda)
            .collection("conversaciones")
            .whereEqualTo("productoId", productoId)
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversacion::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.ultimoMensajeTimestamp }.orEmpty()

                val compradorActualVacio = uiState.producto?.compradorUid.isNullOrBlank()
                val primeraConv = lista.firstOrNull()
                val prodActualizado = if (compradorActualVacio && primeraConv != null && primeraConv.compradorUid.isNotBlank()) {
                    uiState.producto?.copy(
                        compradorUid = primeraConv.compradorUid,
                        compradorNombre = uiState.producto?.compradorNombre?.ifBlank { primeraConv.compradorNombre }
                            ?: primeraConv.compradorNombre
                    )
                } else {
                    uiState.producto
                }

                uiState = uiState.copy(
                    producto = prodActualizado,
                    conversacionesProducto = lista
                )
            }
    }

    /**
     * Si el producto no estaba en Room (por venir directo de una notificación o ser previo),
     * lo busca en Firestore y lo mete en Room para que observarProducto() lo recoja.
     */
    private fun cargarProductoSiFalta() {
        if (productoId.isBlank()) return
        viewModelScope.launch {
            val comId = userRepository.getCurrentUserData().getOrNull()?.comunidadId.orEmpty()
            mercadilloRepository.refrescarProductoPorId(productoId, comId)
        }
    }

    /**
     * El vecino actual solicita comprar o tomar prestado el producto.
     */
    fun onComprarOPrestar() {
        val prod = uiState.producto ?: return

        viewModelScope.launch {
            val usuario = userRepository.getCurrentUserData().getOrNull()
            val nombreComprador = usuario?.nombre?.ifBlank { "Un vecino" } ?: "Un vecino"
            val pisoComprador = usuario?.vivienda?.takeIf { it!= "Sin vivienda asignada" }.orEmpty()

            mercadilloRepository.comprarOPrestar(
                producto = prod,
                compradorUid = currentUid,
                compradorNombre = nombreComprador,
                compradorPiso = pisoComprador
            )
        }
    }

    /**
     * El vendedor confirma que ha entregado el artículo al comprador.
     */
    fun onConfirmarEntregaVendedor() {
        val prod = uiState.producto ?: return
        viewModelScope.launch {
            mercadilloRepository.confirmarEntregaVendedor(prod)
        }
    }

    /**
     * El comprador confirma que ya ha recibido el artículo en mano.
     */
    fun onConfirmarRecepcionComprador() {
        val prod = uiState.producto ?: return
        viewModelScope.launch {
            mercadilloRepository.confirmarRecepcionComprador(producto =prod )
        }
    }

    /**
     * Se reporta una incidencia o desacuerdo al administrador.
     */
    fun onReportarConflicto(motivo: String) {
        val producto = uiState.producto ?: return
        viewModelScope.launch {
            mercadilloRepository.reportarConflicto(producto, motivo)
        }
    }

    /**
     * Añade o quita el producto de favoritos.
     */
    fun toggleFavorito() {
        val prod = uiState.producto ?: return
        val uid = currentUid.ifBlank { return }
        val comId = prod.comunidadId.ifBlank { return }

        viewModelScope.launch {
            mercadilloRepository.toggleMeGustaProducto(
                comunidadId = comId,
                productoId = prod.id,
                usuarioUid = uid
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        docListener?.remove()
        conversacionesListener?.remove()
    }
}
    }