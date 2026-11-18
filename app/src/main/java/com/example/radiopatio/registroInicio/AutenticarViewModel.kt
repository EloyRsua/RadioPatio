package com.example.radiopatio.registroInicio

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.ROL_ADMINISTRADOR_FINCAS
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AutenticarUiState(
    val cargando: Boolean = false,
    val esAdministrador: Boolean = false,
    val datosComunidad: DatosRegistroComunidad = DatosRegistroComunidad(),
    val errorMensaje: String? = null
)

class AutenticarViewModelFactory(
    private val userRepository: UserRepository,
    private val comunidadRepository: ComunidadRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutenticarViewModel::class.java)) {
            return AutenticarViewModel(userRepository, comunidadRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class AutenticarViewModel(
    private val userRepository: UserRepository,
    private val comunidadRepository: ComunidadRepository
) : ViewModel() {

    var uiState by mutableStateOf(AutenticarUiState())
        private set

    private val auth = FirebaseAuth.getInstance()

    fun onTipoCuentaChange(nuevoEsAdmin: Boolean) {
        val datosActuales = uiState.datosComunidad
        val nuevosDatos = if (!nuevoEsAdmin && (datosActuales.crearNueva || datosActuales.solicitarTraspaso)) {
            DatosRegistroComunidad()
        } else {
            datosActuales
        }
        uiState = uiState.copy(esAdministrador = nuevoEsAdmin, datosComunidad = nuevosDatos)
    }

    fun onDatosComunidadChange(nuevosDatos: DatosRegistroComunidad) {
        uiState = uiState.copy(datosComunidad = nuevosDatos)
    }

    fun resetearEstadoRegistro() {
        uiState = uiState.copy(
            cargando = false,
            esAdministrador = false,
            datosComunidad = DatosRegistroComunidad(),
            errorMensaje = null
        )
    }

    fun limpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun iniciarSesion(
        email: String,
        password: String,
        onExito: () -> Unit
    ) {
        val emailLimpio = email.trim()
        val passLimpia = password.trim()

        if (emailLimpio.isBlank() || passLimpia.isBlank()) {
            uiState = uiState.copy(errorMensaje = "Por favor, introduce email y contraseña")
            return
        }
        if (!emailLimpio.contains("@")) {
            uiState = uiState.copy(errorMensaje = "Por favor, introduce un correo electrónico válido")
            return
        }
        if (passLimpia.length < 6) {
            uiState = uiState.copy(errorMensaje = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        uiState = uiState.copy(cargando = true, errorMensaje = null)

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(emailLimpio, passLimpia).await()
                uiState = uiState.copy(cargando = false)
                onExito()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    errorMensaje = "Error al iniciar sesión: Correo o contraseña incorrectos."
                )
            }
        }
    }

    fun registrarNuevoUsuario(
        nombre: String,
        apellidos: String,
        vivienda: String,
        email: String,
        pass: String,
        photoUri: Uri?,
        onExito: (mensajeBienvenida: String) -> Unit
    ) {
        val esAdmin = uiState.esAdministrador
        val datosComunidad = uiState.datosComunidad

        uiState = uiState.copy(cargando = true, errorMensaje = null)

        viewModelScope.launch {
            try {
                if (datosComunidad.crearNueva && !esAdmin) {
                    throw SecurityException("Solo los administradores de fincas pueden crear comunidades.")
                }

                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("No se pudo obtener el UID")

                val photoUrl = userRepository.subirAvatarSiExiste(uid, photoUri)
                var mensajeBienvenida = "¡Bienvenido a RadioPatio, $nombre!"

                if (esAdmin) {
                    when {
                        datosComunidad.crearNueva -> {
                            val comunidadCreada = comunidadRepository.crearComunidad(
                                nombre = datosComunidad.nuevaComunidadNombre,
                                direccion = datosComunidad.nuevaComunidadDireccion,
                                ciudad = datosComunidad.nuevaComunidadCiudad,
                                codigoPostal = datosComunidad.nuevaComunidadCp,
                                descripcion = datosComunidad.nuevaComunidadDescripcion,
                                tipo = datosComunidad.nuevaComunidadTipo,
                                numeroPisos = datosComunidad.nuevaComunidadPisos,
                                createdByUid = uid,
                                creadorEsAdministrador = true
                            ).getOrThrow()

                            userRepository.registrarUsuarioEnFirestore(
                                uid = uid,
                                nombre = nombre,
                                apellidos = apellidos,
                                vivienda = vivienda,
                                email = email,
                                comunidadId = comunidadCreada.id,
                                comunidadNombre = comunidadCreada.nombre,
                                rol = ROL_ADMINISTRADOR_FINCAS,
                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                comunidadesIds = listOf(comunidadCreada.id),
                                photoUrl = photoUrl
                            ).getOrThrow()
                        }

                        datosComunidad.comunidadExistente != null -> {
                            val com = datosComunidad.comunidadExistente
                            userRepository.registrarUsuarioEnFirestore(
                                uid = uid,
                                nombre = nombre,
                                apellidos = apellidos,
                                vivienda = vivienda,
                                email = email,
                                comunidadId = "",
                                comunidadNombre = "",
                                rol = ROL_ADMINISTRADOR_FINCAS,
                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                comunidadesIds = emptyList(),
                                photoUrl = photoUrl
                            ).getOrThrow()

                            val unionDirecta = if (!datosComunidad.solicitarTraspaso && com.administradorUid.isBlank()) {
                                userRepository.unirseComoAdministradorAComunidadSinAdmin(com.id)
                            } else {
                                Result.failure(IllegalStateException("Requiere solicitud"))
                            }

                            if (unionDirecta.isFailure) {
                                val solicitante = Usuario(
                                    nombre = "$nombre $apellidos".trim().ifBlank { nombre },
                                    inicial = nombre.firstOrNull()?.uppercase() ?: "A",
                                    vivienda = vivienda,
                                    email = email,
                                    telefono = "",
                                    rol = ROL_ADMINISTRADOR_FINCAS,
                                    uid = uid,
                                    photoUrl = photoUrl,
                                    tipoCuenta = ROL_ADMINISTRADOR_FINCAS
                                )
                                comunidadRepository.solicitarTraspasoAdmin(com.id, solicitante).getOrThrow()
                                mensajeBienvenida = "Cuenta creada. Solicitud enviada al administrador de ${com.nombre}."
                            }
                        }

                        else -> {
                            userRepository.registrarUsuarioEnFirestore(
                                uid = uid,
                                nombre = nombre,
                                apellidos = apellidos,
                                vivienda = vivienda,
                                email = email,
                                comunidadId = "",
                                comunidadNombre = "",
                                rol = ROL_ADMINISTRADOR_FINCAS,
                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                comunidadesIds = emptyList(),
                                photoUrl = photoUrl
                            ).getOrThrow()
                        }
                    }
                } else {
                    val com = datosComunidad.comunidadExistente
                        ?: throw IllegalArgumentException("Debes seleccionar una comunidad para registrarte.")

                    userRepository.registrarUsuarioEnFirestore(
                        uid = uid,
                        nombre = nombre,
                        apellidos = apellidos,
                        vivienda = vivienda,
                        email = email,
                        comunidadId = "",
                        comunidadNombre = "",
                        rol = ROL_VECINO,
                        tipoCuenta = ROL_VECINO,
                        comunidadesIds = emptyList(),
                        photoUrl = photoUrl
                    ).getOrThrow()

                    val nombreCompleto = "$nombre $apellidos".trim().ifBlank { nombre }
                    val solicitante = Usuario(
                        nombre = nombreCompleto,
                        inicial = nombre.firstOrNull()?.uppercase() ?: "V",
                        vivienda = vivienda,
                        email = email,
                        telefono = "",
                        rol = ROL_VECINO,
                        uid = uid,
                        photoUrl = photoUrl,
                        tipoCuenta = ROL_VECINO
                    )
                    comunidadRepository.solicitarIngresoVecino(
                        comunidadId = com.id,
                        solicitante = solicitante,
                        viviendaIndicada = vivienda
                    ).getOrThrow()
                    mensajeBienvenida = "Cuenta creada. Tu solicitud de ingreso en ${com.nombre} queda pendiente de verificación por el administrador."
                }

                resetearEstadoRegistro()
                onExito(mensajeBienvenida)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    errorMensaje = "Error al registrar: ${e.localizedMessage ?: "No se pudo completar el registro"}"
                )
            }
        }
    }
}