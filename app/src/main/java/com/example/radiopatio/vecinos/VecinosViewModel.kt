package com.example.radiopatio.vecinos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla de Vecinos y Comunidad (Patrón UiState).
 */
data class VecinosUiState(
    val comunidadIdActual: String = "",
    val comunidad: Comunidad? = null,
    val usuarioActual: Usuario? = null,
    val vecinos: List<Usuario> = emptyList(),
    val comunidadesDisponibles: List<Comunidad> = emptyList(),
    val textoBusqueda: String = "",
    val filtroRol: String = "Todos",
    val cargando: Boolean = true,
    val procesandoComunidad: Boolean = false,
    val errorMensaje: String? = null,
    val mostrarDialogoComunidad: Boolean = false
) {
    val rolesDisponibles: List<String>
        get() {
            val roles = vecinos
                .filter { comunidadIdActual.isNotBlank() && it.comunidadId == comunidadIdActual }
                .map { it.rol.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            return listOf("Todos") + roles
        }

    /**
     * Lista filtrada aplicando aislamiento obligatorio por `comunidadIdActual` en lógica de negocio,
     * además de búsqueda por nombre, piso/puerta o rol.
     */
    val vecinosFiltrados: List<Usuario>
        get() = vecinos.filter { vecino ->
            val coincideComunidad = comunidadIdActual.isNotBlank() && vecino.comunidadId == comunidadIdActual
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

@OptIn(ExperimentalCoroutinesApi::class)
class VecinosViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(VecinosUiState())
        private set

    init {
        observarComunidadYVecinosEnTiempoReal()
        observarCatalogoComunidades()
    }

    private fun observarComunidadYVecinosEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(
                            Triple<Usuario?, Comunidad?, List<Usuario>>(usuario, null, emptyList())
                        )
                    } else {
                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            userRepository.observarVecinosPorComunidad(comId)
                        ) { comunidadDoc, vecinosComunidad ->
                            val comunidadEfectiva = comunidadDoc ?: Comunidad(
                                id = comId,
                                nombre = usuario?.comunidadNombre?.takeIf { it.isNotBlank() } ?: comId,
                                direccion = "Comunidad registrada",
                                totalVecinos = vecinosComunidad.size
                            )
                            Triple(usuario, comunidadEfectiva, vecinosComunidad)
                        }
                    }
                }
                .collect { (usuario, comunidad, vecinos) ->
                    val comId = usuario?.comunidadId.orEmpty()
                    val filtroRolActual = if (
                        uiState.filtroRol == "Todos" ||
                        vecinos.any { it.rol.equals(uiState.filtroRol, ignoreCase = true) }
                    ) {
                        uiState.filtroRol
                    } else {
                        "Todos"
                    }

                    uiState = uiState.copy(
                        comunidadIdActual = comId,
                        usuarioActual = usuario,
                        comunidad = comunidad,
                        vecinos = vecinos.filter { comId.isNotBlank() && it.comunidadId == comId },
                        filtroRol = filtroRolActual,
                        cargando = false
                    )
                }
        }
    }

    private fun observarCatalogoComunidades() {
        viewModelScope.launch {
            comunidadRepository.observarComunidades().collect { comunidades ->
                uiState = uiState.copy(comunidadesDisponibles = comunidades)
            }
        }
    }

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onFiltroRolChange(nuevoRol: String) {
        uiState = uiState.copy(filtroRol = nuevoRol)
    }

    fun onAbrirDialogoComunidad() {
        uiState = uiState.copy(mostrarDialogoComunidad = true, errorMensaje = null)
    }

    fun onCerrarDialogoComunidad() {
        uiState = uiState.copy(mostrarDialogoComunidad = false, errorMensaje = null)
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun onUnirseAComunidad(comunidad: Comunidad) {
        viewModelScope.launch {
            uiState = uiState.copy(procesandoComunidad = true, errorMensaje = null)
            val resultado = userRepository.asignarComunidadUsuario(
                comunidadId = comunidad.id,
                comunidadNombre = comunidad.nombre
            )
            uiState = if (resultado.isSuccess) {
                uiState.copy(
                    procesandoComunidad = false,
                    mostrarDialogoComunidad = false
                )
            } else {
                uiState.copy(
                    procesandoComunidad = false,
                    errorMensaje = resultado.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo unir a la comunidad."
                )
            }
        }
    }

    fun onCrearYUnirseAComunidad(
        nombre: String,
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        descripcion: String,
        totalPortales: Int
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(procesandoComunidad = true, errorMensaje = null)
            val uid = uiState.usuarioActual?.uid.orEmpty()
            val creacion = comunidadRepository.crearComunidad(
                nombre = nombre,
                direccion = direccion,
                ciudad = ciudad,
                codigoPostal = codigoPostal,
                descripcion = descripcion,
                totalPortales = totalPortales,
                createdByUid = uid
            )

            creacion.onSuccess { nuevaComunidad ->
                val asignacion = userRepository.asignarComunidadUsuario(
                    comunidadId = nuevaComunidad.id,
                    comunidadNombre = nuevaComunidad.nombre,
                    nuevoRol = "Presidente"
                )
                uiState = if (asignacion.isSuccess) {
                    uiState.copy(
                        procesandoComunidad = false,
                        mostrarDialogoComunidad = false
                    )
                } else {
                    uiState.copy(
                        procesandoComunidad = false,
                        errorMensaje = asignacion.exceptionOrNull()?.localizedMessage
                            ?: "Comunidad creada, pero no se pudo asignar al perfil."
                    )
                }
            }.onFailure { error ->
                uiState = uiState.copy(
                    procesandoComunidad = false,
                    errorMensaje = error.localizedMessage ?: "No se pudo crear la comunidad."
                )
            }
        }
    }
}

