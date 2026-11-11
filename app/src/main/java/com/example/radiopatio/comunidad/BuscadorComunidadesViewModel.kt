package com.example.radiopatio.comunidad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val textoBusqueda: String = "",
    val tipoSeleccionado: String = "Todos",
    val ciudadSeleccionada: String = "Todas",
    val soloVerificadas: Boolean = false,
    val ordenSeleccionado: OrdenComunidad = OrdenComunidad.RELEVANCIA,
    val cargando: Boolean = true,
    val procesando: Boolean = false,
    val mostrarDialogoCrear: Boolean = false,
    val errorMensaje: String? = null
) {
    val tiposDisponibles: List<String>
        get() {
            val base = listOf("Residencial", "Urbanización", "Edificio")
            val deFirestore = comunidadesTotales
                .map { it.tipo.trim() }
                .filter { it.isNotBlank() }
            return listOf("Todos") + (base + deFirestore).distinctBy { normalizarTexto(it) }
        }

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
            soloVerificadas,
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
            soloVerificadas = soloVerificadas,
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
 * Motor puro de búsqueda multipalabra insensible a tildes, filtrado y ordenación de comunidades.
 */
fun filtrarYOrdenarComunidades(
    comunidades: List<Comunidad>,
    textoBusqueda: String,
    tipoSeleccionado: String = "Todos",
    ciudadSeleccionada: String = "Todas",
    soloVerificadas: Boolean = false,
    ordenSeleccionado: OrdenComunidad = OrdenComunidad.RELEVANCIA
): List<Comunidad> {
    val consultaNorm = normalizarTexto(textoBusqueda)
    val tokens = consultaNorm.split(Regex("\\s+")).filter { it.isNotBlank() }
    val tipoNorm = normalizarTexto(tipoSeleccionado)
    val ciudadNorm = normalizarTexto(ciudadSeleccionada)

    val filtradas = comunidades.filter { com ->
        val coincideTipo = tipoSeleccionado == "Todos" ||
            normalizarTexto(com.tipo) == tipoNorm
        val coincideCiudad = ciudadSeleccionada == "Todas" ||
            normalizarTexto(com.ciudad) == ciudadNorm
        val coincideVerificada = !soloVerificadas || com.verificada

        val coincideTexto = if (tokens.isEmpty()) {
            true
        } else {
            val textoIndexado = normalizarTexto(
                listOf(
                    com.nombre,
                    com.direccion,
                    com.ciudad,
                    com.codigoPostal,
                    com.tipo,
                    com.descripcion
                ).joinToString(" ")
            )
            tokens.all { token -> textoIndexado.contains(token) }
        }

        coincideTipo && coincideCiudad && coincideVerificada && coincideTexto
    }

    return when (ordenSeleccionado) {
        OrdenComunidad.RELEVANCIA -> {
            if (tokens.isEmpty()) {
                filtradas.sortedWith(
                    compareByDescending<Comunidad> { it.verificada }
                        .thenByDescending { it.totalVecinos }
                        .thenBy { normalizarTexto(it.nombre) }
                )
            } else {
                filtradas.sortedWith(
                    compareByDescending<Comunidad> { puntuarRelevancia(it, consultaNorm, tokens) }
                        .thenByDescending { it.verificada }
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
    consultaCompleta: String,
    tokens: List<String>
): Int {
    val nombre = normalizarTexto(comunidad.nombre)
    val direccion = normalizarTexto(comunidad.direccion)
    val ciudad = normalizarTexto(comunidad.ciudad)
    val cp = normalizarTexto(comunidad.codigoPostal)

    var puntos = 0
    if (nombre == consultaCompleta) puntos += 100
    if (nombre.startsWith(consultaCompleta)) puntos += 60
    if (nombre.contains(consultaCompleta)) puntos += 40
    if (cp == consultaCompleta) puntos += 35

    tokens.forEach { token ->
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
                userRepository.observarComunidadIdActual()
            ) { comunidades, comIdActual ->
                Pair(comunidades, comIdActual)
            }.collect { (comunidades, comIdActual) ->
                uiState = uiState.copy(
                    comunidadesTotales = comunidades,
                    comunidadActualId = comIdActual,
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

    fun onToggleSoloVerificadas() {
        uiState = uiState.copy(soloVerificadas = !uiState.soloVerificadas)
    }

    fun onOrdenSeleccionadoChange(orden: OrdenComunidad) {
        uiState = uiState.copy(ordenSeleccionado = orden)
    }

    fun onLimpiarFiltros() {
        uiState = uiState.copy(
            textoBusqueda = "",
            tipoSeleccionado = "Todos",
            ciudadSeleccionada = "Todas",
            soloVerificadas = false,
            ordenSeleccionado = OrdenComunidad.RELEVANCIA
        )
    }

    fun onAbrirDialogoCrear() {
        uiState = uiState.copy(mostrarDialogoCrear = true, errorMensaje = null)
    }

    fun onCerrarDialogoCrear() {
        uiState = uiState.copy(mostrarDialogoCrear = false, errorMensaje = null)
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun onUnirseAComunidad(comunidad: Comunidad, onExito: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(procesando = true, errorMensaje = null)
            val resultado = userRepository.asignarComunidadUsuario(
                comunidadId = comunidad.id,
                comunidadNombre = comunidad.nombre,
                nuevoRol = "Vecino/a"
            )
            if (resultado.isSuccess) {
                uiState = uiState.copy(procesando = false)
                onExito()
            } else {
                uiState = uiState.copy(
                    procesando = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo cambiar de comunidad."
                )
            }
        }
    }

    fun onCrearYUnirseAComunidad(
        nombre: String,
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        tipo: String,
        descripcion: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(procesando = true, errorMensaje = null)
            val uid = userRepository.getCurrentUserData().getOrNull()?.uid.orEmpty()
            val creacion = comunidadRepository.crearComunidad(
                nombre = nombre,
                direccion = direccion,
                ciudad = ciudad,
                codigoPostal = codigoPostal,
                descripcion = descripcion,
                tipo = tipo,
                createdByUid = uid
            )

            creacion.onSuccess { nuevaComunidad ->
                val asignacion = userRepository.asignarComunidadUsuario(
                    comunidadId = nuevaComunidad.id,
                    comunidadNombre = nuevaComunidad.nombre,
                    nuevoRol = "Presidente"
                )
                if (asignacion.isSuccess) {
                    uiState = uiState.copy(
                        procesando = false,
                        mostrarDialogoCrear = false
                    )
                    onExito()
                } else {
                    uiState = uiState.copy(
                        procesando = false,
                        errorMensaje = asignacion.exceptionOrNull()?.localizedMessage
                            ?: "Comunidad creada, pero no se pudo asignar al usuario."
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

