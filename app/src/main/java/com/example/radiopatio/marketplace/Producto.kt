package com.example.radiopatio.marketplace

data class Producto(
    val id: Int,
    val titulo: String,
    val precio: String,
    val ubicacionYTiempo: String,
    val imagenRes: Int? = null // Id del recurso drawable (foto local)
)
