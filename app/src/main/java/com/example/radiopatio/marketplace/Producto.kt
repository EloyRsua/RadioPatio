package com.example.radiopatio.marketplace

// Definimos las categorias posibles
enum class CategoriaProducto(val nombre: String) {
    TODOS("Todos"),
    ELECTRONICA("Electrónica"),
    JUGUETES("Juguetes"),
    MUEBLES("Muebles"),
    ROPA("Ropa"),
    HERRAMIENTAS("Herramientas")
}

data class Producto(
    val id: Int = 0,
    val titulo: String,
    val precio: String,
    val ubicacionYTiempo: String,
    val categoriaProducto: CategoriaProducto,
    val esParaComprar: Boolean,
    val imagenRes: Int? = null, // Id del recurso drawable (foto local)
    val firestoreId: String = "",
    val comunidadId: String = "",
    val vendedorUid: String = "",
    val vendedorNombre: String = "",
    val timestamp: Long = 0L
)
