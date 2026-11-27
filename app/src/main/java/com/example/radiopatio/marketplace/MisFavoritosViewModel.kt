package com.example.radiopatio.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class MisFavoritosUiState(
    val favoritos: List<Producto> = emptyList(),
    val cargando: Boolean = true
)

class MisFavoritosViewModelFactory(
    private val mercadilloRepository: MercadilloRepository,
) : ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MisFavoritosViewModel(mercadilloRepository) as T
    }
}

class MisFavoritosViewModel(
    private val mercadilloRepository: MercadilloRepository,
) : ViewModel() {

    var uiState by mutableStateOf(MisFavoritosUiState())
        private set

    val currentUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    init {
        observarFavoritos()
    }

    private fun observarFavoritos() {
        viewModelScope.launch {
            mercadilloRepository.observarProductos().collect{ todosLosProductos ->
                val soloFavoritos = todosLosProductos.filter {
                    it.esFavoritoDe(currentUid)
                }
                uiState = uiState.copy(
                    favoritos = soloFavoritos,
                    cargando = false
                )
            }
        }
    }

    fun toggleFavorito(producto: Producto) {
        val uid = currentUid
        if (uid.isBlank() || producto.id.isBlank() || producto.comunidadId.isBlank()) return
        viewModelScope.launch {
            mercadilloRepository.toggleMeGustaProducto(
                comunidadId = producto.comunidadId,
                productoId = producto.id,
                usuarioUid = uid
            )
        }
    }
}