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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DetalleProductoUiState(
    val cargando: Boolean = true,
    val producto: Producto? = null,
    val esPropietario: Boolean = false,
    val esComprador: Boolean = false,
    val diasRestantes: Long = 0L,
    val conversacionesProducto: List<Conversacion> = emptyList(),
    val mensaje: String? = null
)

class DetalleProductoViewModelFactory(
    private val firestoreId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetalleProductoViewModel(firestoreId) as T
    }
}

class DetalleProductoViewModel(
    private val firestoreId: String
) : ViewModel() {

    var uiState by mutableStateOf(DetalleProductoUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid = auth.currentUser?.uid ?: ""

    private var targetDocRef: DocumentReference? = null
    private var docListener: ListenerRegistration? = null
    private var conversacionesListener: ListenerRegistration? = null

    init {
        cargarYEscucharProducto()
    }

    private fun cargarYEscucharProducto() {
        if (firestoreId.isBlank()) {
            uiState = uiState.copy(cargando = false)
            return
        }

        viewModelScope.launch {
            var userComId = ""
            try {
                val userDoc = db.collection("users").document(currentUid).get().await()
                userComId = userDoc.getString("comunidadId").orEmpty()
            } catch (_: Exception) {
            }

            var ref: DocumentReference? = null

            // 1. Probar en la subcolección de la comunidad del usuario
            if (userComId.isNotBlank()) {
                val candidate = db.subcoleccionComunidad(userComId, "productos").document(firestoreId)
                try {
                    val candidateSnap = candidate.get().await()
                    if (candidateSnap.exists()) {
                        ref = candidate
                    }
                } catch (_: Exception) {
                }
            }

            // 2. Probar en colección raíz por si existía de antes
            if (ref == null) {
                val candidateRoot = db.collection("productos").document(firestoreId)
                try {
                    val rootSnap = candidateRoot.get().await()
                    if (rootSnap.exists()) {
                        ref = candidateRoot
                    }
                } catch (_: Exception) {
                }
            }

            // 3. Probar por collectionGroup("productos")
            if (ref == null) {
                try {
                    val groupSnap = db.collectionGroup("productos")
                        .whereEqualTo("firestoreId", firestoreId)
                        .limit(1)
                        .get()
                        .await()
                    ref = groupSnap.documents.firstOrNull()?.reference
                } catch (_: Exception) {
                }
            }

            // 4. Si aún no se encontró, asignar por defecto la subcolección de la comunidad o raíz
            if (ref == null) {
                ref = if (userComId.isNotBlank()) {
                    db.subcoleccionComunidad(userComId, "productos").document(firestoreId)
                } else {
                    db.collection("productos").document(firestoreId)
                }
            }

            targetDocRef = ref

            docListener?.remove()
            docListener = ref.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    uiState = uiState.copy(cargando = false)
                    return@addSnapshotListener
                }

                val autorUid = snapshot.getString("autorUid") ?: snapshot.getString("vendedorUid") ?: ""
                val compradorUid = snapshot.getString("compradorUid") ?: ""

                var estado = try {
                    EstadoProducto.valueOf(snapshot.getString("estado") ?: EstadoProducto.DISPONIBLE.name)
                } catch (_: Exception) {
                    EstadoProducto.DISPONIBLE
                }

                val plazoDias = snapshot.getLong("plazoDiasEntrega")?.toInt() ?: 7
                val diasPrestamo = snapshot.getLong("diasPrestamo")?.toInt() ?: 7
                val fechaCompra = snapshot.getLong("fechaCompra") ?: 0L
                val fechaLimite = snapshot.getLong("fechaLimiteEntrega") ?: (fechaCompra + plazoDias * 24 * 60 * 60 * 1000L)
                val vendedorOk = snapshot.getBoolean("vendedorConfirmoEntrega") ?: false
                val compradorOk = snapshot.getBoolean("compradorConfirmoRecepcion") ?: false
                val motivoConflicto = snapshot.getString("motivoConflicto") ?: ""

                // Si está en entrega, ha pasado la fecha límite y no hay conflicto abierto, se autocompleta con éxito
                val ahora = System.currentTimeMillis()
                val esCompra = snapshot.getBoolean("esParaComprar") ?: true
                if (estado == EstadoProducto.EN_ENTREGA && fechaLimite > 0 && ahora > fechaLimite && motivoConflicto.isBlank()) {
                    val nuevoEstado = if (esCompra) EstadoProducto.VENDIDO else EstadoProducto.PRESTADO
                    estado = nuevoEstado
                    snapshot.reference.update("estado", nuevoEstado.name)
                }

                val compradorNombre = snapshot.getString("compradorNombre") ?: ""
                val compradorPiso = snapshot.getString("compradorPiso") ?: ""
                val comIdProducto = snapshot.getString("comunidadId") ?: userComId

                @Suppress("UNCHECKED_CAST")
                val fotos = snapshot.get("fotos") as? List<String> ?: emptyList()
                val prod = Producto(
                    firestoreId = snapshot.id,
                    comunidadId = comIdProducto,
                    autorUid = autorUid,
                    vendedorUid = snapshot.getString("vendedorUid") ?: autorUid,
                    compradorUid = compradorUid,
                    compradorNombre = compradorNombre,
                    compradorPiso = compradorPiso,
                    titulo = snapshot.getString("titulo") ?: "",
                    descripcion = snapshot.getString("descripcion") ?: "",
                    precio = snapshot.getString("precio") ?: "",
                    ubicacionYTiempo = snapshot.getString("ubicacion") ?: snapshot.getString("ubicacionYTiempo") ?: "",
                    categoriaProducto = try {
                        CategoriaProducto.valueOf(snapshot.getString("categoria") ?: CategoriaProducto.TODOS.name)
                    } catch (_: Exception) {
                        CategoriaProducto.TODOS
                    },
                    esParaComprar = snapshot.getBoolean("esParaComprar") ?: true,
                    marca = snapshot.getString("marca") ?: "",
                    color = snapshot.getString("color") ?: "",
                    estadoUso = snapshot.getString("estadoUso") ?: "",
                    fotos = fotos,
                    imagenUrl = fotos.firstOrNull() ?: snapshot.getString("imagenUrl"),
                    estado = estado,
                    fechaCompra = fechaCompra,
                    plazoDiasEntrega = plazoDias,
                    diasPrestamo = diasPrestamo,
                    fechaLimiteEntrega = fechaLimite,
                    vendedorConfirmoEntrega = vendedorOk,
                    compradorConfirmoRecepcion = compradorOk,
                    motivoConflicto = motivoConflicto,
                    fechaDevolucion = snapshot.getString("fechaDevolucion") ?: ""
                )

                val diffMilis = fechaLimite - ahora
                val diasRestantes = if (diffMilis > 0) (diffMilis / (24 * 60 * 60 * 1000L)) + 1 else 0L
                val esProp = (autorUid == currentUid)

                uiState = uiState.copy(
                    cargando = false,
                    producto = prod,
                    esPropietario = esProp,
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

    // Acción de comprar / tomar prestado
    fun onComprarOPrestar() {
        val prod = uiState.producto ?: return

        viewModelScope.launch {
            var nombreComprador = "Un vecino"
            var pisoComprador = ""
            try {
                val userDoc = db.collection("users").document(currentUid).get().await()
                if (userDoc.exists()) {
                    val nom = userDoc.getString("nombre") ?: ""
                    val ape = userDoc.getString("apellidos") ?: ""
                    nombreComprador = "$nom $ape".trim().ifBlank {
                        auth.currentUser?.email?.substringBefore("@") ?: "Un vecino"
                    }
                    pisoComprador = userDoc.getString("piso")
                        ?: userDoc.getString("vivienda") ?: ""
                }
            } catch (_: Exception) {
                nombreComprador = auth.currentUser?.email?.substringBefore("@") ?: "Un vecino"
            }

            val plazoDias = prod.plazoDiasEntrega
            val fechaActual = System.currentTimeMillis()
            val fechaLimite = fechaActual + (plazoDias * 24 * 60 * 60 * 1000L)

            val updates = hashMapOf<String, Any>(
                "estado" to EstadoProducto.EN_ENTREGA.name,
                "compradorUid" to currentUid,
                "compradorNombre" to nombreComprador,
                "compradorPiso" to pisoComprador,
                "fechaCompra" to fechaActual,
                "fechaLimiteEntrega" to fechaLimite,
                "vendedorConfirmoEntrega" to false,
                "compradorConfirmoRecepcion" to false
            )

            targetDocRef?.update(updates)

            val datosVecino = if (pisoComprador.isNotBlank()) "$nombreComprador ($pisoComprador)" else nombreComprador
            val titulo = if (prod.esParaComprar) "¡Artículo vendido!" else "¡Préstamo solicitado!"
            val mensaje = if (prod.esParaComprar) {
                "El vecino $datosVecino ha comprado \"${prod.titulo}\". Tienes $plazoDias días para llevárselo."
            } else {
                "El vecino $datosVecino ha solicitado el préstamo de \"${prod.titulo}\"."
            }

            NotificacionesHelper.enviarNotificacion(
                destinatarioUid = prod.autorUid,
                titulo = titulo,
                mensaje = mensaje,
                productoId = prod.firestoreId
            )
        }
    }

    // Vendedor confirma que ya lo entregó + notificacion
    fun onConfirmarEntregaVendedor() {
        val prod = uiState.producto ?: return
        val estadoFinal = if (prod.esParaComprar) EstadoProducto.VENDIDO.name else EstadoProducto.PRESTADO.name
        val nuevoEstado = if (prod.compradorConfirmoRecepcion) estadoFinal else EstadoProducto.EN_ENTREGA.name

        val updates = mapOf(
            "vendedorConfirmoEntrega" to true,
            "estado" to nuevoEstado
        )
        targetDocRef?.update(updates)

        NotificacionesHelper.enviarNotificacion(
            destinatarioUid = prod.compradorUid,
            titulo = "Producto entregado por el vendedor",
            mensaje = "El vendedor ha marcado \"${prod.titulo}\" como entregado. Por favor, confirma su recepción cuando lo tengas en mano.",
            productoId = prod.firestoreId
        )
    }

    // Comprador confirma que ya lo recibió + notificacion
    fun onConfirmarRecepcionComprador() {
        val prod = uiState.producto ?: return
        val estadoFinal = if (prod.esParaComprar) EstadoProducto.VENDIDO.name else EstadoProducto.PRESTADO.name

        val updates = hashMapOf<String, Any>(
            "compradorConfirmoRecepcion" to true,
            "estado" to estadoFinal
        )

        if (!prod.esParaComprar) {
            val fechaLimiteDevolucionMilis = System.currentTimeMillis() + (prod.diasPrestamo * 24 * 60 * 60 * 1000L)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            updates["fechaDevolucion"] = sdf.format(java.util.Date(fechaLimiteDevolucionMilis))
        }

        targetDocRef?.update(updates)

        NotificacionesHelper.enviarNotificacion(
            destinatarioUid = prod.autorUid,
            titulo = "Entrega completada",
            mensaje = "El comprador ha confirmado la recepción de \"${prod.titulo}\". ¡Trato finalizado con éxito!",
            productoId = prod.firestoreId
        )
    }

    // Reportar conflicto al Administrador
    fun onReportarConflicto(motivo: String) {
        val prod = uiState.producto ?: return
        viewModelScope.launch {
            val updates = mapOf(
                "estado" to EstadoProducto.EN_CONFLICTO.name,
                "motivoConflicto" to motivo
            )
            targetDocRef?.update(updates)
        }
    }

    override fun onCleared() {
        super.onCleared()
        docListener?.remove()
        conversacionesListener?.remove()
    }
}