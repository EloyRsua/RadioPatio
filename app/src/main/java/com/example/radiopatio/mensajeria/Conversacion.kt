package com.example.radiopatio.mensajeria

/**
 * Representa una sala de chat entre un vecino interesado (comprador) y el dueño del artículo (vendedor),
 * vinculada a un producto concreto dentro de una comunidad.
 */
data class Conversacion(
    val id: String = "",
    val comunidadId: String = "",
    val productoId: String = "",
    val productoTitulo: String = "",
    val productoPrecio: String = "",
    val productoImagenUrl: String = "",
    val vendedorUid: String = "",
    val vendedorNombre: String = "",
    val compradorUid: String = "",
    val compradorNombre: String = "",
    val participantesUids: List<String> = emptyList(),
    val ultimoMensaje: String = "",
    val ultimoMensajeTimestamp: Long = 0L,
    val ultimoRemitenteUid: String = "",
    val mensajesNoLeidosVendedor: Int = 0,
    val mensajesNoLeidosComprador: Int = 0
) {
    /**
     * Devuelve el nombre de la otra persona en el chat respecto al usuario que mira la pantalla.
     */
    fun obtenerNombreInterlocutor(miUid: String): String {
        return if (miUid == vendedorUid) {
            compradorNombre.ifBlank { "Vecino interesado" }
        } else {
            vendedorNombre.ifBlank { "Vendedor" }
        }
    }

    /**
     * Devuelve cuántos mensajes sin leer tiene el usuario actual en esta conversación.
     */
    fun obtenerNoLeidosPara(miUid: String): Int {
        return if (miUid == vendedorUid) {
            mensajesNoLeidosVendedor
        } else {
            mensajesNoLeidosComprador
        }
    }
}

/**
 * Genera un ID único para evitar conversaciones duplicadas
 * entre los mismos dos usuarios sobre el mismo producto.
 */
fun generarIdConversacion(productoId: String, compradorUid: String, vendedorUid: String): String {
    return "${productoId}_${compradorUid}_${vendedorUid}"
}