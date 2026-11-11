package com.example.radiopatio.marketplace

// Definimos las categorías posibles
enum class CategoriaProducto(val nombre: String) {
    TODOS("Todos"),
    ELECTRONICA("Electrónica"),
    JUGUETES("Juguetes"),
    MUEBLES("Muebles"),
    ROPA("Ropa"),
    HERRAMIENTAS("Herramientas")
}

// Estados posibles del producto
enum class EstadoProducto(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    VENDIDO("Vendido"),
    PRESTADO("Prestado")
}

data class Producto(
    val id: Int = 0,
    val titulo: String = "",
    val precio: String = "",
    val ubicacionYTiempo: String = "",
    val categoriaProducto: CategoriaProducto = CategoriaProducto.TODOS,
    val esParaComprar: Boolean = true,
    val imagenUrl: String? = null,
    val firestoreId: String = "",
    val autorUid: String = "",
    val descripcion: String = "",
    val marca: String = "",
    val color: String = "",
    val estadoUso: String = "",
    val fotos: List<String> = emptyList(),
    val estado: EstadoProducto = EstadoProducto.DISPONIBLE,
    val fechaDevolucion: String = ""
)
