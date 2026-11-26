package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class MisFavoritosUiState(
    val favoritos: List<Producto> = emptyList(),
    val cargando: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class MisFavoritosViewModel(
    private val mercadilloRepository: MercadilloRepository = MercadilloRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var uiState by mutableStateOf(MisFavoritosUiState())
        private set

    private val currentUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    init {
        observarFavoritos()
    }

    private fun observarFavoritos() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    if (comId.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        // Reutilizamos el repositorio limpio del mercadillo
                        mercadilloRepository.observarProductosDeComunidad(comId)
                    }
                }
                .collect { todosLosProductos ->
                    // Filtramos únicamente los productos donde el usuario actual dio like
                    val soloFavoritos = todosLosProductos.filter { it.esFavoritoDe(currentUid) }
                    uiState = uiState.copy(
                        favoritos = soloFavoritos,
                        cargando = false
                    )
                }
        }
    }

    fun toggleFavorito(producto: Producto) {
        val uid = currentUid
        if (uid.isBlank() || producto.firestoreId.isBlank() || producto.comunidadId.isBlank()) return
        viewModelScope.launch {
            mercadilloRepository.toggleMeGustaProducto(
                comunidadId = producto.comunidadId,
                productoId = producto.firestoreId,
                usuarioUid = uid
            )
        }
    }
}