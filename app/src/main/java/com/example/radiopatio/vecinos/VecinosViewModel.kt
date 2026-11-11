package com.example.radiopatio.vecinos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.comunidad.SolicitudTraspasoAdmin
import com.example.radiopatio.informacionUsuario.ROL_PRESIDENTE
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla de Vecinos y Comunidad (Patrón UiState).
 */
data class VecinosUiState(
    val comunidadIdActual: String = "",
    val comunidad: Comunidad? = null,
    val usuarioActual: Usuario? = null,
    val vecinos: List<Usuario> = emptyList(),
    val comunidadesAdministradas: List<Comunidad> = emptyList(),
    val comunidadesSolicitadasPendientes: List<Comunidad> = emptyList(),
    val solicitudesTraspasoPendientes: List<SolicitudTraspasoAdmin> = emptyList(),
    val textoBusqueda: String = "",
    val filtroRol: String = "Todos",
    val cargando: Boolean = true,
    val mostrarDialogoGestionarPresidencia: Boolean = false,
    val procesandoPresidencia: Boolean = false,
    val mostrarDialogoEditarPisos: Boolean = false,
    val procesandoPisos: Boolean = false,
    val procesandoCambioComunidad: Boolean = false,
    val procesandoSolicitudAdmin: Boolean = false,
    val mensajeExito: String? = null,
    val errorMensaje: String? = null
) {
    val rolesDisponibles: List<String>
        get() {
            val roles = vecinos
                .filter { comunidadIdActual.isNotBlank() && it.perteneceAComunidad(comunidadIdActual) }
                .map { it.rol.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            return listOf("Todos") + roles
        }

    /**
     * Indica si el usuario autenticado actual es el administrador de fincas único de esta comunidad.
     */
    val esAdministradorActual: Boolean
        get() {
            val uid = usuarioActual?.uid.orEmpty()
            if (comunidadIdActual.isBlank() || uid.isBlank()) return false
            return usuarioActual?.esAdministradorFincas == true &&
                comunidad?.administradorUid == uid
        }

    /**
     * Miembro que ocupa actualmente la presidencia de la comunidad, o null si aún no hay ninguno.
     */
    val presidenteActualComunidad: Usuario?
        get() {
            if (comunidadIdActual.isBlank()) return null
            val miembrosVecinos = vecinos.filter {
                it.perteneceAComunidad(comunidadIdActual) &&
                    it.uid.isNotBlank() &&
                    !it.esAdministradorFincas
            }
            val presUid = comunidad?.presidenteUid?.trim().orEmpty()
            return if (presUid.isNotBlank()) {
                miembrosVecinos.find { it.uid == presUid }
            } else {
                miembrosVecinos.firstOrNull { it.rol.equals(ROL_PRESIDENTE, ignoreCase = true) }
            }
        }

    /**
     * Vecinos de la comunidad elegibles para ser designados o recibir la presidencia
     * por parte del Administrador de fincas (cualquier vecino si aún no hay presidente,
     * o cualquier otro vecino distinto al presidente actual).
     */
    val candidatosPresidencia: List<Usuario>
        get() {
            if (!esAdministradorActual || comunidadIdActual.isBlank()) return emptyList()
            val presActualUid = presidenteActualComunidad?.uid.orEmpty()
            return vecinos.filter {
                it.perteneceAComunidad(comunidadIdActual) &&
                    it.uid.isNotBlank() &&
                    !it.esAdministradorFincas &&
                    it.uid != presActualUid
            }
        }

    /**
     * Lista filtrada aplicando aislamiento obligatorio por `comunidadIdActual` en lógica de negocio,
     * además de búsqueda por nombre, piso/puerta o rol.
     */
    val vecinosFiltrados: List<Usuario>
        get() = vecinos.filter { vecino ->
            val coincideComunidad = comunidadIdActual.isNotBlank() && vecino.perteneceAComunidad(comunidadIdActual)
            val consulta = textoBusqueda.trim()
            val coincideTexto = consulta.isBlank() ||
                vecino.nombre.contains(consulta, ignoreCase = true) ||
                vecino.vivienda.contains(consulta, ignoreCase = true) ||
                vecino.rol.contains(consulta, ignoreCase = true)
            val coincideRol = filtroRol == "Todos" || vecino.rol.equals(filtroRol, ignoreCase = true)

            coincideComunidad && coincideTexto && coincideRol
        }
}

class VecinosViewModelFactory(
    private val userRepository: UserRepository = UserRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VecinosViewModel::class.java)) {
            return VecinosViewModel(userRepository, comunidadRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

private data class VecinosSnapshot(
    val usuario: Usuario?,
    val comunidad: Comunidad?,
    val vecinos: List<Usuario>,
    val todasComunidades: List<Comunidad>,
    val solicitudesAdmin: List<SolicitudTraspasoAdmin>
)

@OptIn(ExperimentalCoroutinesApi::class)
class VecinosViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(VecinosUiState())
        private set

    init {
        observarComunidadYVecinosEnTiempoReal()
    }

    private fun observarComunidadYVecinosEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        comunidadRepository.observarComunidades().map { todas ->
                            VecinosSnapshot(
                                usuario = usuario,
                                comunidad = null,
                                vecinos = emptyList(),
                                todasComunidades = todas,
                                solicitudesAdmin = emptyList()
                            )
                        }
                    } else {
                        combine(
                            comunidadRepository.observarComunidades(),
                            userRepository.observarVecinosPorComunidad(comId),
                            comunidadRepository.observarSolicitudesPendientesAdmin(comId)
                        ) { todas, vecinosComunidad, solicitudes ->
                            val comunidadDoc = todas.find { it.id == comId }
                            val comunidadEfectiva = comunidadDoc ?: Comunidad(
                                id = comId,
                                nombre = usuario?.comunidadNombre?.takeIf { it.isNotBlank() } ?: comId,
                                direccion = "Comunidad registrada",
                                totalVecinos = vecinosComunidad.size
                            )
                            VecinosSnapshot(
                                usuario = usuario,
                                comunidad = comunidadEfectiva,
                                vecinos = vecinosComunidad,
                                todasComunidades = todas,
                                solicitudesAdmin = solicitudes
                            )
                        }
                    }
                }
                .collect { snap ->
                    val usuario = snap.usuario
                    val comId = usuario?.comunidadId.orEmpty()
                    val uid = usuario?.uid.orEmpty()
                    val esAdmin = usuario?.esAdministradorFincas == true

                    val administradas = if (esAdmin) {
                        snap.todasComunidades
                            .filter { com -> usuario.perteneceAComunidad(com.id) || (uid.isNotBlank() && com.administradorUid == uid) }
                            .sortedBy { it.nombre.lowercase() }
                    } else {
                        emptyList()
                    }

                    val pendientesEnviadas = if (esAdmin) {
                        snap.todasComunidades
                            .filter { com -> com.id in usuario.solicitudesPendientesComunidades }
                            .sortedBy { it.nombre.lowercase() }
                    } else {
                        emptyList()
                    }

                    val solicitudesVisibles = if (esAdmin && snap.comunidad?.administradorUid == uid && uid.isNotBlank()) {
                        snap.solicitudesAdmin
                    } else {
                        emptyList()
                    }

                    val filtroRolActual = if (
                        uiState.filtroRol == "Todos" ||
                        snap.vecinos.any { it.rol.equals(uiState.filtroRol, ignoreCase = true) }
                    ) {
                        uiState.filtroRol
                    } else {
                        "Todos"
                    }

                    uiState = uiState.copy(
                        comunidadIdActual = comId,
                        usuarioActual = usuario,
                        comunidad = snap.comunidad,
                        vecinos = snap.vecinos.filter { comId.isNotBlank() && it.perteneceAComunidad(comId) },
                        comunidadesAdministradas = administradas,
                        comunidadesSolicitadasPendientes = pendientesEnviadas,
                        solicitudesTraspasoPendientes = solicitudesVisibles,
                        filtroRol = filtroRolActual,
                        cargando = false
                    )
                }
        }
    }

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onFiltroRolChange(nuevoRol: String) {
        uiState = uiState.copy(filtroRol = nuevoRol)
    }

    fun onAbrirDialogoGestionarPresidencia() {
        if (!uiState.esAdministradorActual) {
            uiState = uiState.copy(errorMensaje = "Solo el administrador de fincas puede designar o cambiar la presidencia.")
            return
        }
        uiState = uiState.copy(mostrarDialogoGestionarPresidencia = true, errorMensaje = null)
    }

    fun onCerrarDialogoGestionarPresidencia() {
        if (!uiState.procesandoPresidencia) {
            uiState = uiState.copy(mostrarDialogoGestionarPresidencia = false)
        }
    }

    fun onAbrirDialogoEditarPisos() {
        if (!uiState.esAdministradorActual || uiState.comunidad?.esEdificio != true) {
            uiState = uiState.copy(
                errorMensaje = "Solo el administrador de fincas de un edificio puede editar los pisos."
            )
            return
        }
        uiState = uiState.copy(mostrarDialogoEditarPisos = true, errorMensaje = null)
    }

    fun onCerrarDialogoEditarPisos() {
        if (!uiState.procesandoPisos) {
            uiState = uiState.copy(mostrarDialogoEditarPisos = false)
        }
    }

    fun onActualizarNumeroPisos(nuevoNumeroPisos: Int) {
        val comId = uiState.comunidadIdActual
        val uidActual = uiState.usuarioActual?.uid.orEmpty()
        if (!uiState.esAdministradorActual || comId.isBlank() || uiState.procesandoPisos) {
            uiState = uiState.copy(
                errorMensaje = "Solo el administrador de fincas de este edificio puede dictaminar los pisos."
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(procesandoPisos = true, errorMensaje = null)
            val resultado = comunidadRepository.actualizarNumeroPisos(
                comunidadId = comId,
                administradorUid = uidActual,
                numeroPisos = nuevoNumeroPisos
            )
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoPisos = false,
                    mostrarDialogoEditarPisos = false,
                    mensajeExito = "Número de pisos actualizado a $nuevoNumeroPisos."
                )
            } else {
                uiState.copy(
                    procesandoPisos = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo actualizar el número de pisos."
                )
            }
        }
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun onLimpiarMensajeExito() {
        uiState = uiState.copy(mensajeExito = null)
    }

    fun onCambiarComunidadAdministrada(comunidad: Comunidad) {
        if (uiState.procesandoCambioComunidad || comunidad.id == uiState.comunidadIdActual) return
        viewModelScope.launch {
            uiState = uiState.copy(procesandoCambioComunidad = true, errorMensaje = null)
            val resultado = userRepository.cambiarComunidadActivaAdministrador(comunidad)
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoCambioComunidad = false,
                    mensajeExito = "Cambiado a ${comunidad.nombre}"
                )
            } else {
                uiState.copy(
                    procesandoCambioComunidad = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo cambiar de comunidad."
                )
            }
        }
    }

    fun onAceptarSolicitudAdmin(solicitud: SolicitudTraspasoAdmin) {
        val comId = uiState.comunidadIdActual
        val uidActual = uiState.usuarioActual?.uid.orEmpty()
        if (!uiState.esAdministradorActual || comId.isBlank() || uiState.procesandoSolicitudAdmin) {
            uiState = uiState.copy(errorMensaje = "Solo el administrador actual puede aceptar este traspaso.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(procesandoSolicitudAdmin = true, errorMensaje = null)
            val resultado = comunidadRepository.aceptarSolicitudTraspasoAdmin(
                comunidadId = comId,
                administradorActualUid = uidActual,
                solicitanteUid = solicitud.solicitanteUid
            )
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    mensajeExito = "Administración traspasada a ${solicitud.solicitanteNombre}."
                )
            } else {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo aceptar la solicitud de traspaso."
                )
            }
        }
    }

    fun onRechazarSolicitudAdmin(solicitud: SolicitudTraspasoAdmin) {
        val comId = uiState.comunidadIdActual
        val uidActual = uiState.usuarioActual?.uid.orEmpty()
        if (!uiState.esAdministradorActual || comId.isBlank() || uiState.procesandoSolicitudAdmin) return

        viewModelScope.launch {
            uiState = uiState.copy(procesandoSolicitudAdmin = true, errorMensaje = null)
            val resultado = comunidadRepository.rechazarSolicitudTraspasoAdmin(
                comunidadId = comId,
                administradorActualUid = uidActual,
                solicitanteUid = solicitud.solicitanteUid
            )
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    mensajeExito = "Solicitud de ${solicitud.solicitanteNombre} rechazada."
                )
            } else {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo rechazar la solicitud."
                )
            }
        }
    }

    fun onCancelarSolicitudEnviada(comunidad: Comunidad) {
        val uidActual = uiState.usuarioActual?.uid.orEmpty()
        if (uidActual.isBlank() || uiState.procesandoSolicitudAdmin) return

        viewModelScope.launch {
            uiState = uiState.copy(procesandoSolicitudAdmin = true, errorMensaje = null)
            val resultado = comunidadRepository.cancelarSolicitudTraspasoAdmin(comunidad.id, uidActual)
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    mensajeExito = "Solicitud en ${comunidad.nombre} cancelada."
                )
            } else {
                uiState.copy(
                    procesandoSolicitudAdmin = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo cancelar la solicitud."
                )
            }
        }
    }

    fun onAsignarPresidencia(nuevoPresidente: Usuario) {
        val comId = uiState.comunidadIdActual
        val uidActual = uiState.usuarioActual?.uid.orEmpty()
        val presidenteActual = uiState.presidenteActualComunidad

        if (!uiState.esAdministradorActual) {
            uiState = uiState.copy(
                errorMensaje = "Solo el administrador de fincas de esta comunidad puede designar o cambiar la presidencia.",
                mostrarDialogoGestionarPresidencia = false
            )
            return
        }
        if (comId.isBlank() ||
            !nuevoPresidente.perteneceAComunidad(comId) ||
            nuevoPresidente.uid.isBlank() ||
            nuevoPresidente.esAdministradorFincas
        ) {
            uiState = uiState.copy(
                errorMensaje = "El vecino seleccionado no es elegible para ocupar la presidencia."
            )
            return
        }
        if (nuevoPresidente.uid == presidenteActual?.uid) {
            uiState = uiState.copy(
                errorMensaje = "${nuevoPresidente.nombre} ya es el presidente actual."
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(procesandoPresidencia = true, errorMensaje = null)
            val resultado = comunidadRepository.asignarOCambiarPresidencia(
                comunidadId = comId,
                administradorUid = uidActual,
                nuevoPresidenteUid = nuevoPresidente.uid,
                presidentePrevioFallbackUid = presidenteActual?.uid.orEmpty()
            )
            uiState = if (resultado.isSuccess) {
                val verbo = if (presidenteActual == null) "designado" else "cambiado"
                uiState.copy(
                    procesandoPresidencia = false,
                    mostrarDialogoGestionarPresidencia = false,
                    mensajeExito = "Presidente $verbo: ${nuevoPresidente.nombre}"
                )
            } else {
                uiState.copy(
                    procesandoPresidencia = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo actualizar la presidencia."
                )
            }
        }
    }
}
