package com.example.radiopatio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * UiState inmutable para la pantalla del Calendario de Eventos.
 * Agrupa todos los datos observables y aplica filtrado multi-inquilino obligatorio.
 */
data class CalendarioUiState(
    val mesActual: YearMonth = YearMonth.now(),
    val fechaHoy: LocalDate = LocalDate.now(),
    val diaSeleccionado: LocalDate? = null,
    val categoriaSeleccionada: CategoriaEvento = CategoriaEvento.TODOS,
    val comunidadActualId: String = "",
    val comunidadNombre: String = "",
    val usuarioActual: Usuario? = null,
    val listaEventos: List<Evento> = emptyList(),
    val mostrarDialogoNuevoEvento: Boolean = false,
    val cargando: Boolean = true,
    val errorMensaje: String? = null
) {
    val eventosFiltrados: List<Evento>
        get() = listaEventos.filter { evento ->
            val coincideComunidad = comunidadActualId.isNotBlank() &&
                evento.comunidadId == comunidadActualId
            val coincideMes = YearMonth.from(evento.fecha) == mesActual
            val coincideDia = diaSeleccionado == null || evento.fecha == diaSeleccionado
            val coincideCategoria = categoriaSeleccionada == CategoriaEvento.TODOS ||
                evento.categoria == categoriaSeleccionada
            coincideComunidad && coincideMes && coincideDia && coincideCategoria
        }.sortedBy { it.fecha }

    val diasConEventos: Map<Int, List<Evento>>
        get() = listaEventos
            .filter {
                comunidadActualId.isNotBlank() &&
                    it.comunidadId == comunidadActualId &&
                    YearMonth.from(it.fecha) == mesActual
            }
            .groupBy { it.fecha.dayOfMonth }
}

class CalendarioViewModelFactory(
    private val userRepository: UserRepository = UserRepository(),
    private val calendarioRepository: CalendarioRepository = CalendarioRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarioViewModel::class.java)) {
            return CalendarioViewModel(userRepository, calendarioRepository, comunidadRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarioViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val calendarioRepository: CalendarioRepository = CalendarioRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(CalendarioUiState())
        private set

    init {
        observarCalendarioEnTiempoReal()
    }

    private fun observarCalendarioEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    val uid = usuario?.uid.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(Triple<Usuario?, String, List<Evento>>(usuario, "", emptyList()))
                    } else {
                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            calendarioRepository.observarEventosDeComunidad(comId, uid)
                        ) { comunidad, eventos ->
                            val nombreCom = comunidad?.nombre?.takeIf { it.isNotBlank() }
                                ?: usuario?.comunidadNombre
                                ?: ""
                            Triple(usuario, nombreCom, eventos)
                        }
                    }
                }
                .collect { (usuario, nombreCom, eventos) ->
                    val comId = usuario?.comunidadId.orEmpty()
                    uiState = uiState.copy(
                        comunidadActualId = comId,
                        comunidadNombre = nombreCom,
                        usuarioActual = usuario,
                        listaEventos = eventos.filter { comId.isNotBlank() && it.comunidadId == comId },
                        cargando = false
                    )
                }
        }
    }

    fun onMesAnterior() {
        uiState = uiState.copy(mesActual = uiState.mesActual.minusMonths(1))
    }

    fun onMesSiguiente() {
        uiState = uiState.copy(mesActual = uiState.mesActual.plusMonths(1))
    }

    fun onIrAHoy() {
        val hoy = LocalDate.now()
        uiState = uiState.copy(
            fechaHoy = hoy,
            mesActual = YearMonth.from(hoy),
            diaSeleccionado = hoy
        )
    }

    fun onSeleccionarDia(dia: LocalDate) {
        val nuevoDia = if (uiState.diaSeleccionado == dia) null else dia
        uiState = uiState.copy(diaSeleccionado = nuevoDia)
    }

    fun onLimpiarFiltroDia() {
        uiState = uiState.copy(diaSeleccionado = null)
    }

    fun onSeleccionarCategoria(categoria: CategoriaEvento) {
        uiState = uiState.copy(categoriaSeleccionada = categoria)
    }

    fun onAbrirDialogoNuevoEvento() {
        uiState = uiState.copy(mostrarDialogoNuevoEvento = true, errorMensaje = null)
    }

    fun onCerrarDialogoNuevoEvento() {
        uiState = uiState.copy(mostrarDialogoNuevoEvento = false, errorMensaje = null)
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun onToggleAsistencia(eventoId: String) {
        val comId = uiState.comunidadActualId
        val uid = uiState.usuarioActual?.uid.orEmpty()
        val evento = uiState.listaEventos.find { it.id == eventoId } ?: return
        if (comId.isBlank() || evento.comunidadId != comId || uid.isBlank()) return

        viewModelScope.launch {
            calendarioRepository.toggleAsistencia(evento, uid, comId)
        }
    }

    fun onToggleRecordatorio(eventoId: String) {
        val comId = uiState.comunidadActualId
        val uid = uiState.usuarioActual?.uid.orEmpty()
        val evento = uiState.listaEventos.find { it.id == eventoId } ?: return
        if (comId.isBlank() || evento.comunidadId != comId || uid.isBlank()) return

        viewModelScope.launch {
            calendarioRepository.toggleRecordatorio(evento, uid, comId)
        }
    }

    fun onCrearEvento(nuevoEvento: Evento) {
        val comId = uiState.comunidadActualId
        if (comId.isBlank()) {
            uiState = uiState.copy(errorMensaje = "Debes pertenecer a una comunidad para crear eventos.")
            return
        }
        val uid = uiState.usuarioActual?.uid.orEmpty()
        val organizadorNombre = uiState.usuarioActual?.nombre?.takeIf { it.isNotBlank() }
            ?: nuevoEvento.organizador

        val eventoConUsuario = nuevoEvento.copy(
            comunidadId = comId,
            creadorUid = uid,
            organizador = organizadorNombre
        )

        viewModelScope.launch {
            val resultado = calendarioRepository.crearEvento(eventoConUsuario, comId, uid)
            uiState = if (resultado.isSuccess) {
                val enfocarDia = if (YearMonth.from(eventoConUsuario.fecha) == uiState.mesActual) {
                    eventoConUsuario.fecha
                } else {
                    uiState.diaSeleccionado
                }
                uiState.copy(
                    mostrarDialogoNuevoEvento = false,
                    diaSeleccionado = enfocarDia,
                    errorMensaje = null
                )
            } else {
                uiState.copy(
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo publicar el evento."
                )
            }
        }
    }
}
