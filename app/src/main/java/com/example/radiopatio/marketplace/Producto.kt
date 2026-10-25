package com.example.radiopatio.marketplace

// Definimos las categorias posibles
enum class CategoriaProducto(val nombre: String){
    TODOS("Todos"),
    ELECTRONICA("Electrónica"),
    JUGUETES("Juguetes"),
    MUEBLES("Muebles"),
    ROPA("Ropa"),
    HERRAMIENTAS("Herramientas")
}

data class Producto(
    val id: Int,
    val titulo: String,
    val precio: String,
    val ubicacionYTiempo: String,
    val categoriaProducto: CategoriaProducto,
    val esParaComprar: Boolean,
    val imagenRes: Int? = null // Id del recurso drawable (foto local)
)
