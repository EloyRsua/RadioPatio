package com.example.radiopatio.comunidad

/**
 * Solicitud de ingreso de un vecino pendiente de verificación por el Administrador de fincas.
 * La unión a la comunidad no es efectiva hasta que el administrador la aprueba.
 */
data class SolicitudIngresoVecino(
    val id: String = "",
    val comunidadId: String = "",
    val comunidadNombre: String = "",
    val solicitanteUid: String = "",
    val solicitanteNombre: String = "",
    val solicitanteEmail: String = "",
    val solicitanteVivienda: String = "",
    val solicitantePhotoUrl: String = "",
    val estado: String = SolicitudTraspasoAdmin.ESTADO_PENDIENTE,
    val timestamp: Long = 0L
)

