package com.example.radiopatio.comunidad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class OrdenComunidad(val etiqueta: String) {
    RELEVANCIA("Relevancia"),
    MAS_VECINOS("Más vecinos"),
    NOMBRE_AZ("Nombre A-Z")
}

/**
 * Estado inmutable del buscador unificado de comunidades (registro y cambio de comunidad).
 */
data class BuscadorComunidadesUiState(
    val comunidadesTotales: List<Comunidad> = emptyList(),
    val comunidadActualId: String = "",
    val usuarioActual: Usuario? = null,
    val textoBusqueda: String = "",
    val tipoSeleccionado: String = "Todos",
    val ciudadSeleccionada: String = "Todas",
    val ordenSeleccionado: OrdenComunidad = OrdenComunidad.RELEVANCIA,
    val cargando: Boolean = true,
    val procesando: Boolean = false,
    val mensajeExito: String? = null,
    val errorMensaje: String? = null
) {
    fun puedeCrearComunidad(
        modoRegistro: Boolean,
        esAdministradorRegistro: Boolean
    ): Boolean {
        return if (modoRegistro) {
            esAdministradorRegistro
        } else {
            usuarioActual?.esAdministradorFincas == true
        }
    }
    val tiposDisponibles: List<String>
        get() = listOf("Todos") + Comunidad.TIPOS_COMUNIDAD

    val ciudadesDisponibles: List<String>
        get() {
            val ciudades = comunidadesTotales
                .map { it.ciudad.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { normalizarTexto(it) }
                .sortedBy { normalizarTexto(it) }
            return listOf("Todas") + ciudades
        }

    val cantidadFiltrosActivos: Int
        get() = listOf(
            tipoSeleccionado != "Todos",
            ciudadSeleccionada != "Todas",
            ordenSeleccionado != OrdenComunidad.RELEVANCIA
        ).count { it }

    val hayFiltrosActivos: Boolean
        get() = textoBusqueda.isNotBlank() || cantidadFiltrosActivos > 0

    val comunidadesFiltradas: List<Comunidad>
        get() = filtrarYOrdenarComunidades(
            comunidades = comunidadesTotales,
            textoBusqueda = textoBusqueda,
            tipoSeleccionado = tipoSeleccionado,
            ciudadSeleccionada = ciudadSeleccionada,
            ordenSeleccionado = ordenSeleccionado
        )
}

internal fun normalizarTexto(texto: String): String {
    val descompuesto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
    return Regex("\\p{InCombiningDiacriticalMarks}+")
        .replace(descompuesto, "")
        .lowercase()
}

/**
 * Motor puro de búsqueda directa por código, multipalabra insensible a tildes,
 * filtrado y ordenación de comunidades.
 */
fun filtrarYOrdenarComunidades(
    comunidades: List<Comunidad>,
    textoBusqueda: String,
    tipoSeleccionado: String = "Todos",
    ciudadSeleccionada: String = "Todas",
    ordenSeleccionado: OrdenComunidad = OrdenComunidad.RELEVANCIA
): List<Comunidad> {
    val consultaNorm = normalizarTexto(textoBusqueda)
    val tokens = consultaNorm.split(Regex("\\s+")).filter { it.isNotBlank() }
    val tipoNorm = normalizarTexto(tipoSeleccionado)
    val ciudadNorm = normalizarTexto(ciudadSeleccionada)

    val filtradas = comunidades.filter { com ->
        if (com.coincideConCodigoDirecto(textoBusqueda)) {
            return@filter true
        }

        val tipoComunidad = Comunidad.normalizarTipo(com.tipo)
        val coincideTipo = tipoSeleccionado == "Todos" ||
            normalizarTexto(tipoComunidad) == tipoNorm
        val coincideCiudad = ciudadSeleccionada == "Todas" ||
            normalizarTexto(com.ciudad) == ciudadNorm

        val coincideTexto = if (tokens.isEmpty()) {
            true
        } else {
            val textoIndexado = normalizarTexto(
                listOf(
                    com.nombre,
                    com.codigoCompartible,
                    com.direccion,
                    com.ciudad,
                    com.codigoPostal,
                    tipoComunidad,
                    com.descripcion
                ).joinToString(" ")
            )
            tokens.all { token -> textoIndexado.contains(token) }
        }

        coincideTipo && coincideCiudad && coincideTexto
    }

    return when (ordenSeleccionado) {
        OrdenComunidad.RELEVANCIA -> {
            if (tokens.isEmpty()) {
                filtradas.sortedWith(
                    compareByDescending<Comunidad> { it.totalVecinos }
                        .thenBy { normalizarTexto(it.nombre) }
                )
            } else {
                filtradas.sortedWith(
                    compareByDescending<Comunidad> { puntuarRelevancia(it, textoBusqueda, consultaNorm, tokens) }
                        .thenByDescending { it.totalVecinos }
                        .thenBy { normalizarTexto(it.nombre) }
                )
            }
        }
        OrdenComunidad.MAS_VECINOS -> filtradas.sortedWith(
            compareByDescending<Comunidad> { it.totalVecinos }
                .thenBy { normalizarTexto(it.nombre) }
        )
        OrdenComunidad.NOMBRE_AZ -> filtradas.sortedBy { normalizarTexto(it.nombre) }
    }
}

private fun puntuarRelevancia(
    comunidad: Comunidad,
    consultaCruda: String,
    consultaCompleta: String,
    tokens: List<String>
): Int {
    if (comunidad.coincideConCodigoDirecto(consultaCruda)) return 1000

    val codigoNorm = normalizarTexto(comunidad.codigoCompartible)
    val nombre = normalizarTexto(comunidad.nombre)
    val direccion = normalizarTexto(comunidad.direccion)
    val ciudad = normalizarTexto(comunidad.ciudad)
    val cp = normalizarTexto(comunidad.codigoPostal)

    var puntos = 0
    if (codigoNorm.isNotBlank() && codigoNorm.startsWith(consultaCompleta)) puntos += 250
    if (nombre == consultaCompleta) puntos += 100
    if (nombre.startsWith(consultaCompleta)) puntos += 60
    if (nombre.contains(consultaCompleta)) puntos += 40
    if (cp == consultaCompleta) puntos += 35

    tokens.forEach { token ->
        if (codigoNorm.contains(token)) puntos += 30
        if (nombre.contains(token)) puntos += 20
        if (direccion.contains(token)) puntos += 12
        if (ciudad.contains(token)) puntos += 10
        if (cp.contains(token)) puntos += 10
    }
    return puntos
}

class BuscadorComunidadesViewModelFactory(
    private val comunidadRepository: ComunidadRepository = ComunidadRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuscadorComunidadesViewModel::class.java)) {
            return BuscadorComunidadesViewModel(comunidadRepository, userRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class BuscadorComunidadesViewModel(
    private val comunidadRepository: ComunidadRepository = ComunidadRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BuscadorComunidadesUiState())
        private set

    init {
        observarComunidadesYUsuario()
    }

    private fun observarComunidadesYUsuario() {
        viewModelScope.launch {
            combine(
                comunidadRepository.observarComunidades(),
                userRepository.observarUsuarioActual()
            ) { comunidades, usuario ->
                Pair(comunidades, usuario)
            }.collect { (comunidades, usuario) ->
                uiState = uiState.copy(
                    comunidadesTotales = comunidades,
                    usuarioActual = usuario,
                    comunidadActualId = usuario?.comunidadId.orEmpty(),
                    cargando = false
                )
            }
        }
    }

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onTipoSeleccionadoChange(tipo: String) {
        uiState = uiState.copy(tipoSeleccionado = tipo)
    }

    fun onCiudadSeleccionadaChange(ciudad: String) {
        uiState = uiState.copy(ciudadSeleccionada = ciudad)
    }

    fun onOrdenSeleccionadoChange(orden: OrdenComunidad) {
        uiState = uiState.copy(ordenSeleccionado = orden)
    }

    fun onLimpiarFiltros() {
        uiState = uiState.copy(
            textoBusqueda = "",
            tipoSeleccionado = "Todos",
            ciudadSeleccionada = "Todas",
            ordenSeleccionado = OrdenComunidad.RELEVANCIA
        )
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun onLimpiarMensajeExito() {
        uiState = uiState.copy(mensajeExito = null)
    }

    fun onUnirseAComunidad(comunidad: Comunidad, onExito: () -> Unit) {
        val usuario = uiState.usuarioActual
        viewModelScope.launch {
            uiState = uiState.copy(procesando = true, errorMensaje = null, mensajeExito = null)

            if (usuario?.esAdministradorFincas == true) {
                if (usuario.perteneceAComunidad(comunidad.id) || comunidad.administradorUid == usuario.uid) {
                    val cambio = userRepository.cambiarComunidadActivaAdministrador(comunidad)
                    if (cambio.isSuccess) {
                        uiState = uiState.copy(procesando = false)
                        onExito()
                    } else {
                        uiState = uiState.copy(
                            procesando = false,
                            errorMensaje = cambio.exceptionOrNull()?.localizedMessage
                                ?: "No se pudo cambiar a la comunidad seleccionada."
                        )
                    }
                    return@launch
                }

                if (comunidad.administradorUid.isBlank()) {
                    val union = userRepository.unirseComoAdministradorAComunidadSinAdmin(comunidad.id)
                    if (union.isSuccess) {
                        uiState = uiState.copy(procesando = false)
                        onExito()
                    } else {
                        uiState = uiState.copy(
                            procesando = false,
                            errorMensaje = union.exceptionOrNull()?.localizedMessage
                                ?: "No se pudo asignar la administración."
                        )
                    }
                    return@launch
                }

                val solicitud = comunidadRepository.solicitarTraspasoAdmin(comunidad.id, usuario)
                uiState = if (solicitud.isSuccess) {
                    uiState.copy(
                        procesando = false,
                        mensajeExito = "Solicitud enviada al administrador de ${comunidad.nombre}. Podrás acceder cuando acepte el traspaso."
                    )
                } else {
                    uiState.copy(
                        procesando = false,
                        errorMensaje = solicitud.exceptionOrNull()?.localizedMessage
                            ?: "No se pudo enviar la solicitud de administración."
                    )
                }
                return@launch
            }

            if (usuario != null && usuario.perteneceAComunidad(comunidad.id)) {
                uiState = uiState.copy(procesando = false)
                onExito()
                return@launch
            }

            onSolicitarIngresoVecinoInternal(comunidad, viviendaIndicada = usuario?.vivienda.orEmpty())
        }
    }

    fun onSolicitarIngresoVecino(
        comunidad: Comunidad,
        viviendaIndicada: String,
        onEnviada: () -> Unit = {}
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(procesando = true, errorMensaje = null, mensajeExito = null)
            val exito = onSolicitarIngresoVecinoInternal(comunidad, viviendaIndicada)
            if (exito) {
                onEnviada()
            }
        }
    }

    private suspend fun onSolicitarIngresoVecinoInternal(
        comunidad: Comunidad,
        viviendaIndicada: String
    ): Boolean {
        val usuario = uiState.usuarioActual
        if (usuario == null || usuario.uid.isBlank()) {
            uiState = uiState.copy(
                procesando = false,
                errorMensaje = "Debes iniciar sesión para solicitar el ingreso."
            )
            return false
        }
        if (usuario.esAdministradorFincas) {
            uiState = uiState.copy(
                procesando = false,
                errorMensaje = "Las cuentas de administrador usan solicitudes de traspaso."
            )
            return false
        }

        val solicitud = comunidadRepository.solicitarIngresoVecino(
            comunidadId = comunidad.id,
            solicitante = usuario,
            viviendaIndicada = viviendaIndicada
        )
        return if (solicitud.isSuccess) {
            uiState = uiState.copy(
                procesando = false,
                mensajeExito = "Solicitud enviada al administrador de ${comunidad.nombre}. No formarás parte hasta que la apruebe."
            )
            true
        } else {
            uiState = uiState.copy(
                procesando = false,
                errorMensaje = solicitud.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo enviar la solicitud de ingreso."
            )
            false
        }
    }

    fun onCrearYUnirseAComunidad(
        nombre: String,
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        tipo: String,
        numeroPisos: Int = 0,
        descripcion: String,
        onExito: () -> Unit
    ) {
        val usuario = uiState.usuarioActual
        if (usuario?.esAdministradorFincas != true) {
            uiState = uiState.copy(
                errorMensaje = "Solo los administradores de fincas pueden crear comunidades."
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(procesando = true, errorMensaje = null)
            val uid = usuario.uid.ifBlank { userRepository.getCurrentUserData().getOrNull()?.uid.orEmpty() }
            val creacion = comunidadRepository.crearComunidad(
                nombre = nombre,
                direccion = direccion,
                ciudad = ciudad,
                codigoPostal = codigoPostal,
                descripcion = descripcion,
                tipo = tipo,
                numeroPisos = numeroPisos,
                createdByUid = uid,
                creadorEsAdministrador = true
            )

            creacion.onSuccess { nuevaComunidad ->
                val asignacion = userRepository.asignarComunidadCreadaAdministrador(nuevaComunidad)
                if (asignacion.isSuccess) {
                    uiState = uiState.copy(procesando = false)
                    onExito()
                } else {
                    uiState = uiState.copy(
                        procesando = false,
                        errorMensaje = asignacion.exceptionOrNull()?.localizedMessage
                            ?: "Comunidad creada, pero no se pudo asignar al administrador."
                    )
                }
            }.onFailure { error ->
                uiState = uiState.copy(
                    procesando = false,
                    errorMensaje = error.localizedMessage ?: "No se pudo crear la comunidad."
                )
            }
        }
    }
}

