package com.example.radiopatio.comunidad

import java.security.MessageDigest
import java.security.SecureRandom

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
    val tipo: String = TIPO_URBANIZACION,
    val numeroPisos: Int = 0,
    val totalVecinos: Int = 0,
    val photoUrl: String = "",
    val codigo: String = "",
    val presidenteUid: String = "",
    val administradorUid: String = "",
    val createdByUid: String = ""
) {
    val tipoNormalizado: String
        get() = normalizarTipo(tipo)

    val esEdificio: Boolean
        get() = tipoNormalizado == TIPO_EDIFICIO

    val codigoCompartible: String
        get() = normalizarCodigo(codigo).ifBlank { codigoDesdeId(id) }

    fun coincideConCodigoDirecto(consulta: String): Boolean {
        val consultaCodigo = normalizarCodigo(consulta)
        if (consultaCodigo.length < LONGITUD_CODIGO) return false
        return (codigoCompartible.isNotBlank() && normalizarCodigo(codigoCompartible) == consultaCodigo) ||
            (id.isNotBlank() && id.equals(consulta.trim(), ignoreCase = true))
    }

    /**
     * Devuelve la dirección formateada para mostrar en tarjetas de comunidad.
     */
    fun direccionCompleta(): String {
        val partes = listOf(direccion.trim(), codigoPostal.trim(), ciudad.trim())
            .filter { it.isNotBlank() }
        return if (partes.isEmpty()) "Dirección no especificada" else partes.joinToString(" · ")
    }

    companion object {
        const val TIPO_EDIFICIO = "Edificio"
        const val TIPO_URBANIZACION = "Urbanización"
        private const val ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val LONGITUD_CODIGO = 6

        val TIPOS_COMUNIDAD = listOf(TIPO_EDIFICIO, TIPO_URBANIZACION)

        /**
         * Unifica tipos heredados (Residencial, Mancomunidad, Urbanización) en
         * los dos únicos tipos vigentes: [TIPO_EDIFICIO] y [TIPO_URBANIZACION].
         */
        fun normalizarTipo(tipo: String?): String {
            return if (tipo?.trim().equals(TIPO_EDIFICIO, ignoreCase = true)) {
                TIPO_EDIFICIO
            } else {
                TIPO_URBANIZACION
            }
        }

        fun normalizarCodigo(codigo: String?): String {
            return codigo?.trim()?.uppercase()?.replace(Regex("[^A-Z0-9]"), "").orEmpty()
        }

        /**
         * Deriva de forma determinista un código compartible de 6 caracteres a partir del ID
         * para que comunidades ya existentes sin campo guardado también tengan código único.
         */
        fun codigoDesdeId(comunidadId: String): String {
            val limpio = comunidadId.trim()
            if (limpio.isBlank()) return ""
            val hash = MessageDigest.getInstance("SHA-256").digest(limpio.toByteArray(Charsets.UTF_8))
            return buildString(LONGITUD_CODIGO) {
                for (i in 0 until LONGITUD_CODIGO) {
                    val b = hash[i].toInt() and 0xFF
                    append(ALFABETO_CODIGO[b % ALFABETO_CODIGO.length])
                }
            }
        }

        fun generarCodigoAleatorio(): String {
            val random = SecureRandom()
            return buildString(LONGITUD_CODIGO) {
                repeat(LONGITUD_CODIGO) {
                    append(ALFABETO_CODIGO[random.nextInt(ALFABETO_CODIGO.length)])
                }
            }
        }
    }
}

