package com.example.radiopatio.data.producto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntidad (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "titulo")
    val titulo: String,

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

    @ColumnInfo(name = "image_res")
    val imagenRes: Int? = null
)