package com.example.radiopatio.informacionUsuario

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class PerfilUsuarioUiState(
    val usuario: Usuario? = null,
    val nombreComunidad: String = "",
    val cargando: Boolean = true,
    val errorMensaje: String? = null
)

class PerfilUsuarioViewModelFactory(
    private val userRepository: UserRepository = UserRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerfilUsuarioViewModel::class.java)) {
            return PerfilUsuarioViewModel(userRepository, comunidadRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilUsuarioViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(PerfilUsuarioUiState())
        private set

    init {
        observarPerfilEnTiempoReal()
    }

    private fun observarPerfilEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { user ->
                    val comId = user?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(Pair(user, ""))
                    } else {
                        comunidadRepository.observarComunidadPorId(comId).map { comunidad ->
                            val nombre = comunidad?.nombre?.takeIf { it.isNotBlank() }
                                ?: user?.comunidadNombre
                                ?: comId
                            Pair(user, nombre)
                        }
                    }
                }
                .collect { (user, comNombre) ->
                    uiState = uiState.copy(
                        usuario = user,
                        nombreComunidad = comNombre,
                        cargando = false
                    )
                }
        }
    }

    fun cerrarSesion(onSesionCerrada: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        onSesionCerrada()
    }
}