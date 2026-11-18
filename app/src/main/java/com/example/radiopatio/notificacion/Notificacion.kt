package com.example.radiopatio.notificacion

import com.google.firebase.firestore.FirebaseFirestore

data class Notificacion(
    val id: String = "",
    val destinatarioUid: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val fecha: Long = 0L,
    val leida: Boolean = false,
    val productoId: String = ""
)

object NotificacionesHelper {
    /**
     * Envía una notificación a Firestore dirigida a un vecino.
     */
    fun enviarNotificacion(
        destinatarioUid: String,
        titulo: String,
        mensaje: String,
        productoId: String = ""
    ) {
        if (destinatarioUid.isBlank()) return
        val data = hashMapOf(
            "destinatarioUid" to destinatarioUid,
            "titulo" to titulo,
            "mensaje" to mensaje,
            "fecha" to System.currentTimeMillis(),
            "leida" to false,
            "productoId" to productoId
        )
        FirebaseFirestore.getInstance()
            .collection("notificaciones")
            .add(data)
    }
}