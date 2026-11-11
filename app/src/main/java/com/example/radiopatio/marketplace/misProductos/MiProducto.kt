package com.example.radiopatio.marketplace.misProductos

enum class EstadoProducto(val etiqueta: String){
    DISPONIBLE("Disponible"),
    VENDIDO("Vendido"),
    PRESTADO("Prestado")
}

data class MiProducto(
    val id:String = "",
    val titulo: String = "",
    val precio: String = "",
    val esParaComprar: Boolean = true,
    val estado: EstadoProducto = EstadoProducto.DISPONIBLE,
    val fechaDevolucion: String = ""

)