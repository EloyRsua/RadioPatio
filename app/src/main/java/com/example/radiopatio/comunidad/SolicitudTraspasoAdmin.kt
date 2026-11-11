package com.example.radiopatio.comunidad

/**
 * Solicitud de traspaso de administración de fincas sobre una comunidad existente.
 * El solicitante permanece fuera de la comunidad hasta que el administrador actual acepta.
 */
data class SolicitudTraspasoAdmin(
    val id: String = "",
    val comunidadId: String = "",
    val comunidadNombre: String = "",
    val solicitanteUid: String = "",
    val solicitanteNombre: String = "",
    val solicitanteEmail: String = "",
    val solicitantePhotoUrl: String = "",
    val administradorActualUid: String = "",
    val estado: String = ESTADO_PENDIENTE,
    val timestamp: Long = 0L
) {
    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ACEPTADA = "ACEPTADA"
        const val ESTADO_RECHAZADA = "RECHAZADA"
        const val ESTADO_CANCELADA = "CANCELADA"
    }
}

