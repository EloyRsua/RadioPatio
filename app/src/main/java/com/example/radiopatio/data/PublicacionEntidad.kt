package com.example.radiopatio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "publicaciones")
data class PublicacionEntidad(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "autor")
    val autor: String,

    @ColumnInfo(name = "inicial")
    val inicial: String,

    @ColumnInfo(name = "tiempo_ubica")
    val tiempoYUbica: String,

    @ColumnInfo(name = "contenido")
    val contenido: String,

    @ColumnInfo(name = "me_gusta")
    val meGusta: Int,

    @ColumnInfo(name = "comentarios")
    val comentarios: Int
)