package com.example.radiopatio.notificacion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class NotificacionesUiState(
    val cargando: Boolean = true,
    val notificaciones: List<Notificacion> = emptyList()
)

class NotificacionesViewModel : ViewModel() {
    var uiState by mutableStateOf(NotificacionesUiState())
        private set
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    init {
        escucharNotificaciones()
    }
    private fun escucharNotificaciones() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            uiState = uiState.copy(cargando = false)
            return
        }
        // Filtramos por destinatario y ordenamos en memoria para no requerir índice compuesto en Firestore
        listenerRegistration = db.collection("notificaciones")
            .whereEqualTo("destinatarioUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    uiState = uiState.copy(cargando = false)
                    return@addSnapshotListener
                }
                val lista = snapshot.documents.mapNotNull { doc ->
                    val notif = doc.toObject(Notificacion::class.java)
                    notif?.copy(id = doc.id)
                }.sortedByDescending { it.fecha }
                uiState = uiState.copy(
                    cargando = false,
                    notificaciones = lista
                )
            }
    }
    fun marcarComoLeida(notificacionId: String) {
        if (notificacionId.isBlank()) return
        db.collection("notificaciones").document(notificacionId)
            .update("leida", true)
    }
    fun marcarTodasComoLeidas() {
        val noLeidas = uiState.notificaciones.filter { !it.leida }
        for (n in noLeidas) {
            if (n.id.isNotBlank()) {
                db.collection("notificaciones").document(n.id).update("leida", true)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}