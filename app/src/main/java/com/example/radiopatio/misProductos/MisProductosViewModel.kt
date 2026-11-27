package com.example.radiopatio.misProductos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.marketplace.MercadilloRepository
import com.example.radiopatio.marketplace.Producto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
/**
 * UIState inmutable para la pantalla de Mis Artículos
 */
data class MisProductosUiState(
    val productos: List<Producto> = emptyList(),
    val cargando: Boolean = true,
    val pestanaSeleccionada: Int = 0 // 0 = Ventas, 1 = Prestamos
) {
    val productosFiltrados: List<Producto>
        get() = if (pestanaSeleccionada == 0) {
            productos.filter { it.esParaComprar }
        } else {
            productos.filter { !it.esParaComprar }
        }
}

/**
 * Factoría para inyectar dependencias en MisProductosViewModel.
 */
class MisProductosViewModelFactory(
    private val mercadilloRepository: MercadilloRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MisProductosViewModel(mercadilloRepository) as T
    }
}

/**
 * ViewModel para gestionar los productos publicados por el usuario actual.
 * - Observa Room como Single Source of Truth reactivo.
 * - Delega el borrado de anuncios en MercadilloRepository.
 */
class MisProductosViewModel(
    private val mercadilloRepository: MercadilloRepository
) : ViewModel() {

    var uiState by mutableStateOf(MisProductosUiState())
        private set

    val currentUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    init {
        observarMisProductos()
    }

    fun onPestanaSeleccionada(indice: Int) {
        uiState = uiState.copy(pestanaSeleccionada = indice)
    }

    /**
     * Observa Room y filtra los productos cuyo vendedor es el usuario logueado.
     */
    private fun observarMisProductos() {

        if(currentUid.isBlank()){
            uiState = uiState.copy(cargando = false)
            return
        }

        viewModelScope.launch {
            mercadilloRepository.observarProductos().collect{ todos ->
                val misProductos = todos.filter { it.vendedorUid == currentUid }
                uiState = uiState.copy(
                    productos = misProductos,
                    cargando = false
                )
            }
        }

    }

    /**
     * Elimina el anuncio delegando en el repositorio.
     */
    fun eliminarProducto(
        producto: Producto,
        onSuccess: () -> Unit = {}
    ) {
        if(producto.id.isBlank() || producto.comunidadId.isBlank()) return
        viewModelScope.launch {
            val resultado = mercadilloRepository.eliminarProducto(
                producto = producto,
                comunidadIdActual = producto.comunidadId
            )
            if(resultado.isSuccess){
                onSuccess()
            }
        }
    }
}
