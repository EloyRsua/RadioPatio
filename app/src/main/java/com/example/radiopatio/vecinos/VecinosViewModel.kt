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
    val textoBusqueda: String = "",
    val filtroRol: String = "Todos",
    val cargando: Boolean = true,
    val errorMensaje: String? = null
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

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onFiltroRolChange(nuevoRol: String) {
        uiState = uiState.copy(filtroRol = nuevoRol)
    }
}
