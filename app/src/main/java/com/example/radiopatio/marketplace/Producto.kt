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
    EN_ENTREGA("En proceso de entrega"),
    VENDIDO("Vendido"),
    PRESTADO("Prestado"),
    DEVUELTO("Devuelto"),
    EN_CONFLICTO("En conflicto")
}

data class Producto(
    val id: Int = 0,
    val titulo: String = "",
    val precio: String = "",
    val ubicacionYTiempo: String = "",
    val categoriaProducto: CategoriaProducto = CategoriaProducto.TODOS,
    val esParaComprar: Boolean = true,
    val imagenRes: Int? = null,
    val imagenUrl: String? = null,
    val firestoreId: String = "",
    val comunidadId: String = "",
    val autorUid: String = "",
    val vendedorUid: String = "",
    val vendedorNombre: String = "",
    val timestamp: Long = 0L,
    val descripcion: String = "",
    val marca: String = "",
    val color: String = "",
    val estadoUso: String = "",
    val fotos: List<String> = emptyList(),
    val estado: EstadoProducto = EstadoProducto.DISPONIBLE,

    val compradorUid: String = "",
    val compradorNombre: String = "",
    val compradorPiso: String = "",
    val fechaCompra: Long = 0L,
    val plazoDiasEntrega: Int = 7,
    val diasPrestamo: Int = 7,
    val fechaLimiteEntrega: Long = 0L,
    val vendedorConfirmoEntrega: Boolean = false,
    val compradorConfirmoRecepcion: Boolean = false,
    val motivoConflicto: String = "",
    val fechaDevolucion: String = "",
    val likes: List<String> = emptyList() // uids de los usuarios que han dado like
) {
    fun esFavoritoDe(uid: String): Boolean {
        if(uid.isBlank()) return false
        return likes.contains(uid)
    }
}
