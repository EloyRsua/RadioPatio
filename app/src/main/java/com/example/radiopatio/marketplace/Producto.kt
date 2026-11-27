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
    // Identificadores
    val id: String = "",
    val comunidadId: String = "",
    val vendedorUid: String = "",
    val compradorUid: String = "",

    // valores del producto
    val titulo: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val ubicacion: String = "",
    val fechaCreacion: Long = 0L,
    val categoriaProducto: CategoriaProducto = CategoriaProducto.TODOS,
    val esParaComprar: Boolean = true,
    val marca: String = "",
    val color: String = "",
    val estadoUso: String = "",
    val fotos: List<String> = emptyList(),
    val estado: EstadoProducto = EstadoProducto.DISPONIBLE,

    // valores de la compra y venta
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
    val likes: List<String> = emptyList(), // uids de los usuarios que han dado like

    val vendedorNombre: String = ""
) {
    val imagenUrl: String?
        get() = fotos.firstOrNull()

    fun esFavoritoDe(uid: String): Boolean {
        if(uid.isBlank()) return false
        return likes.contains(uid)
    }

    val ubicacionYTiempo: String
        get() {
            val base = ubicacion.ifBlank { "Comunidad" }
            if (fechaCreacion <= 0L) return base
            val diferencia = System.currentTimeMillis() - fechaCreacion
            val segundos = diferencia / 1000
            val minutos = segundos / 60
            val horas = minutos / 60
            val dias = horas / 24
            val tiempo = when {
                segundos < 60 -> "Ahora mismo"
                minutos < 60 -> "Hace $minutos min"
                horas < 24 -> "Hace $horas h"
                dias == 1L -> "Ayer"
                else -> "Hace $dias d"
            }
            return "$base · $tiempo"
        }
}
