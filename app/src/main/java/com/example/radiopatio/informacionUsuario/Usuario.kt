package com.example.radiopatio.informacionUsuario

data class Usuario(
    val nombre: String,
    val inicial: String,
    val vivienda: String,
    val email: String,
    val telefono: String,
    val rol: String = "Vecino/a",
    val fechaAlta: String = "Enero 2026",
    val totalPublicaciones: Int = 0
)