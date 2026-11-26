package com.example.radiopatio.data.producto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntidad (

    // Identificadores
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "comunidad_id")
    val comunidadId: String = "",

    @ColumnInfo(name = "vendedor_uid")
    val vendedorUid: String = "",

    @ColumnInfo(name = "vendedor_nombre")
    val vendedorNombre: String = "",

    @ColumnInfo(name = "comprador_uid")
    val compradorUid: String = "",

    // Valores del producto
    @ColumnInfo(name = "titulo")
    val titulo: String,

    @ColumnInfo(name = "descripcion")
    val descripcion: String = "",

    @ColumnInfo(name = "precio")
    val precio: String,

    @ColumnInfo(name = "ubicacion")
    val ubicacion: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long,

    @ColumnInfo(name = "categoria")
    val categoria: String,

    @ColumnInfo(name = "es_para_comprar")
    val esParaComprar: Boolean,

    @ColumnInfo(name = "marca")
    val marca: String = "",

    @ColumnInfo(name = "color")
    val color: String = "",

    @ColumnInfo(name = "estado_uso")
    val estadoUso: String = "",

    @ColumnInfo(name = "fotos")
    val fotosSerializadas: String = "",

    @ColumnInfo(name = "estado")
    val estado: String = "DISPONIBLE",

    // Valores de compra y préstamo
    @ColumnInfo(name = "comprador_nombre")
    val compradorNombre: String = "",

    @ColumnInfo(name = "comprador_piso")
    val compradorPiso: String = "",

    @ColumnInfo(name = "fecha_compra")
    val fechaCompra: Long = 0L,

    @ColumnInfo(name = "plazo_dias_entrega")
    val plazoDiasEntrega: Int = 7,

    @ColumnInfo(name = "dias_prestamo")
    val diasPrestamo: Int = 7,

    @ColumnInfo(name = "fecha_limite_entrega")
    val fechaLimiteEntrega: Long = 0L,

    @ColumnInfo(name = "vendedor_confirmo_entrega")
    val vendedorConfirmoEntrega: Boolean = false,

    @ColumnInfo(name = "comprador_confirmo_recepcion")
    val compradorConfirmoRecepcion: Boolean = false,

    @ColumnInfo(name = "motivo_conflicto")
    val motivoConflicto: String = "",

    @ColumnInfo(name = "fecha_devolucion")
    val fechaDevolucion: String = "",

    @ColumnInfo(name = "likes")
    val likesSerializados: String = ""


)