package com.example.radiopatio.navigation

import kotlinx.serialization.Serializable

// Pantalla principal
@Serializable
object Inicio

// Pantalla del mercadillo
@Serializable
object Mercadillo

// Pantalla de vecinos
@Serializable
object Vecinos

// Pantalla eventos
@Serializable
object Eventos

@Serializable
object Publicar

@Serializable
object Perfil

@Serializable
object InicioSesion

@Serializable
object RegistroNuevo

@Serializable
object MisProductos

@Serializable
object CrearProducto

@Serializable
data class DetalleProducto(val productoId: String)

@Serializable
object Notificaciones

@Serializable
data class BuscarComunidad(
    val modoRegistro: Boolean = false,
    val esAdministradorRegistro: Boolean = false
)

@Serializable
data class CrearComunidad(
    val modoRegistro: Boolean = false,
    val esAdministradorRegistro: Boolean = false,
    val nombreInicial: String = ""
)

@Serializable
object MisFavoritos
