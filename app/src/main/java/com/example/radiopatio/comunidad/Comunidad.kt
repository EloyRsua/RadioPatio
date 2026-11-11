package com.example.radiopatio.comunidad

/**
 * Modelo de datos de una comunidad de vecinos sincronizada con Cloud Firestore.
 */
data class Comunidad(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val codigoPostal: String = "",
    val descripcion: String = "",
    val tipo: String = "Residencial",
    val totalVecinos: Int = 0,
    val photoUrl: String = "",
    val verificada: Boolean = false,
    val presidenteUid: String = "",
    val createdByUid: String = ""
) {
    /**
     * Devuelve la dirección formateada para mostrar en tarjetas de comunidad.
     */
    fun direccionCompleta(): String {
        val partes = listOf(direccion.trim(), codigoPostal.trim(), ciudad.trim())
            .filter { it.isNotBlank() }
        return if (partes.isEmpty()) "Dirección no especificada" else partes.joinToString(" · ")
    }
}

