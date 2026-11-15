package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.notificacion.NotificacionesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DetalleProductoUiState(
    val cargando: Boolean = true,
    val producto: Producto? = null,
    val esPropietario: Boolean = false,
    val esComprador: Boolean = false,
    val diasRestantes: Long = 0L,
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

    init {
        cargarYEscucharProducto()
    }

    private fun cargarYEscucharProducto() {
        if (firestoreId.isBlank()) {
            uiState = uiState.copy(cargando = false)
            return
        }

        db.collection("productos").document(firestoreId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    uiState = uiState.copy(cargando = false)
                    return@addSnapshotListener
                }

                val autorUid = snapshot.getString("autorUid") ?: ""
                val compradorUid = snapshot.getString("compradorUid") ?: ""

                var estado = try {
                    EstadoProducto.valueOf(snapshot.getString("estado") ?: EstadoProducto.DISPONIBLE.name)
                } catch (e: Exception) {
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
                    db.collection("productos").document(firestoreId).update("estado", nuevoEstado.name)
                }

                val compradorNombre = snapshot.getString("compradorNombre") ?: ""
                val compradorPiso = snapshot.getString("compradorPiso") ?: ""

                @Suppress("UNCHECKED_CAST")
                val fotos = snapshot.get("fotos") as? List<String> ?: emptyList()
                val prod = Producto(
                    firestoreId = snapshot.id,
                    autorUid = autorUid,
                    compradorUid = compradorUid,
                    compradorNombre = compradorNombre,
                    compradorPiso = compradorPiso,
                    titulo = snapshot.getString("titulo") ?: "",
                    descripcion = snapshot.getString("descripcion") ?: "",
                    precio = snapshot.getString("precio") ?: "",
                    ubicacionYTiempo = snapshot.getString("ubicacion") ?: "",
                    categoriaProducto = try {
                        CategoriaProducto.valueOf(snapshot.getString("categoria") ?: CategoriaProducto.TODOS.name)
                    } catch (e: Exception) {
                        CategoriaProducto.TODOS
                    },
                    esParaComprar = snapshot.getBoolean("esParaComprar") ?: true,
                    marca = snapshot.getString("marca") ?: "",
                    color = snapshot.getString("color") ?: "",
                    estadoUso = snapshot.getString("estadoUso") ?: "",
                    fotos = fotos,
                    imagenUrl = fotos.firstOrNull(),
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

                uiState = uiState.copy(
                    cargando = false,
                    producto = prod,
                    esPropietario = (autorUid == currentUid),
                    esComprador = (compradorUid == currentUid),
                    diasRestantes = diasRestantes
                )
            }
    }

    // Acción de comprar / tomar prestado
    fun onComprarOPrestar() {
        val prod = uiState.producto ?: return

        viewModelScope.launch {
            // 1. Buscamos el nombre y vivienda del usuario actual (el que compra/pide)
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
            } catch (e: Exception) {
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
            db.collection("productos").document(prod.firestoreId).update(updates)

            // 2. Notificación con los datos concretos del vecino para el vendedor
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
        db.collection("productos").document(prod.firestoreId).update(
            "vendedorConfirmoEntrega", true,
            "estado", nuevoEstado
        )

        // Enviar aviso al comprador
        NotificacionesHelper.enviarNotificacion(
            destinatarioUid = prod.compradorUid,
            titulo = "Producto entregado por el vendedor",
            mensaje = "El vendedor ha marcado \"${prod.titulo}\" como entregado. Por favor, confirma su recepción cuando lo tengas en mano.",
            productoId = prod.firestoreId
        )
    }

    // Comprador confirma que ya lo recibió + notitifacion
    fun onConfirmarRecepcionComprador() {
        val prod = uiState.producto ?: return
        val estadoFinal = if (prod.esParaComprar) EstadoProducto.VENDIDO.name else EstadoProducto.PRESTADO.name

        val updates = hashMapOf<String, Any>(
            "compradorConfirmoRecepcion" to true,
            "estado" to estadoFinal
        )

        // Si es un préstamo, fijamos la fecha de devolución sumando los días acordados
        if (!prod.esParaComprar) {
            val fechaLimiteDevolucionMilis = System.currentTimeMillis() + (prod.diasPrestamo * 24 * 60 * 60 * 1000L)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            updates["fechaDevolucion"] = sdf.format(java.util.Date(fechaLimiteDevolucionMilis))
        }

        db.collection("productos").document(prod.firestoreId).update(updates)

        // Enviar aviso al vendedor
        NotificacionesHelper.enviarNotificacion(
            destinatarioUid = prod.autorUid,
            titulo = "Entrega completada",
            mensaje = "El comprador ha confirmado la recepción de \"${prod.titulo}\". ¡Trato finalizado con éxito!",
            productoId = prod.firestoreId
        )
    }

    // Reportar conflicto al Administrador
    fun onReportarConflicto(motivo: String) {
//        val prod = uiState.producto ?: return
//        viewModelScope.launch {
//            // 1. Marcar el producto en conflicto
//            db.collection("productos").document(prod.firestoreId).update(
//                "estado", EstadoProducto.EN_CONFLICTO.name,
//                "motivoConflicto", motivo
//            )
//            // 2. Crear una incidencia para el Administrador de la comunidad
//            val incidencia = hashMapOf(
//                "tipo" to "CONFLICTO_ENTREGA",
//                "productoId" to prod.firestoreId,
//                "tituloProducto" to prod.titulo,
//                "reportadoPorUid" to currentUid,
//                "vendedorUid" to prod.autorUid,
//                "compradorUid" to prod.compradorUid,
//                "motivo" to motivo,
//                "fecha" to System.currentTimeMillis(),
//                "resuelto" to false
//            )
//            db.collection("incidencias").add(incidencia)
//        }
    }
}