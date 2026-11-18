package com.example.radiopatio.mensajeria

/**
 * Representa un mensaje individual dentro de una conversación.
 */
data class Mensaje(
    val id: String = "",
    val conversacionId: String = "",
    val remitenteUid: String = "",
    val remitenteNombre: String = "",
    val texto: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val leido: Boolean = false
)