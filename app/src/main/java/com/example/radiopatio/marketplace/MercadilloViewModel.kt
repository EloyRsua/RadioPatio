package com.example.radiopatio.marketplace

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * UiState inmutable para la pantalla del Mercadillo
 * Agrupa todos los datos que definen el estado de la UI
 */
data class MercadilloUiState(
    val textoBusqueda: String = "",
    val esModoComprar: Boolean = true,
    val productosTotales: List<Producto> = emptyList()
){
    // Se recalcula automaticamente cuando cambia la busqueda o el modo
    val productosFiltrados: List<Producto>
        get() = productosTotales.filter { producto ->
            producto.titulo.contains(textoBusqueda, ignoreCase = true)
        }
}

/**
 * Gestiona el estaod y la lógica de la pantalla
 */
class MercadilloViewModel : ViewModel(){

    private val listaInicial = listOf(
        Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h"),
        Producto(2, "Altavoz BT", "30€", "2ºC - Hace 2h"),
        Producto(3, "Chaqueta", "45€", "6ºA - Hace 3h"),
        Producto(4, "Osito de peluche", "3€", "3ºD - Hace 3h"),
        Producto(5, "Portátil", "220€", "4ºB - Hace 5h"),
        Producto(6, "Taladro Bosch", "25€", "Bajo A - Ayer")
    )

    var uiState by mutableStateOf(MercadilloUiState(productosTotales = listaInicial))
        private set

    fun onTextoBusquedaChange(nuevoTexto: String){
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onCambiarModo(esComprar: Boolean){
        uiState = uiState.copy(esModoComprar = esComprar)
    }
}