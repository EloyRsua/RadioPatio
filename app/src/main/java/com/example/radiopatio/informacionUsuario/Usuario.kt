package com.example.radiopatio.informacionUsuario

const val ROL_VECINO = "Vecino/a"
const val ROL_PRESIDENTE = "Presidente"
const val ROL_ADMINISTRADOR_FINCAS = "Administrador de fincas"

/**
 * Modelo de dominio de un usuario registrado en RadioPatio.
 * Los administradores de fincas pueden pertenecer a múltiples comunidades mediante [comunidadesIds],
 * manteniendo [comunidadId] como la comunidad activa seleccionada actualmente.
 */
data class Usuario(
    val nombre: String,
    val inicial: String,
    val vivienda: String,
    val email: String,
    val telefono: String,
    val rol: String = ROL_VECINO,
    val fechaAlta: String = "Enero 2026",
    val totalPublicaciones: Int = 0,
    val uid: String = "",
    val comunidadId: String = "",
    val comunidadNombre: String = "",
    val photoUrl: String = "",
    val tipoCuenta: String = rol,
    val comunidadesIds: List<String> = if (comunidadId.isNotBlank()) listOf(comunidadId) else emptyList(),
    val solicitudesPendientesComunidades: List<String> = emptyList()
) {
    val esAdministradorFincas: Boolean
        get() = rol.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true) ||
            tipoCuenta.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true)

    fun perteneceAComunidad(targetComunidadId: String): Boolean {
        if (targetComunidadId.isBlank()) return false
        return comunidadId == targetComunidadId || targetComunidadId in comunidadesIds
    }
}