package com.example.radiopatio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.radiopatio.comunidad.Comunidad

@Entity(tableName = "comunidades")
data class ComunidadEntidad(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "direccion")
    val direccion: String,

    @ColumnInfo(name = "ciudad")
    val ciudad: String = "",

    @ColumnInfo(name = "codigo_postal")
    val codigoPostal: String = "",

    @ColumnInfo(name = "descripcion")
    val descripcion: String = "",

    @ColumnInfo(name = "tipo")
    val tipo: String = "Residencial",

    @ColumnInfo(name = "numero_pisos")
    val numeroPisos: Int = 0,

    @ColumnInfo(name = "total_vecinos")
    val totalVecinos: Int = 0,

    @ColumnInfo(name = "presidente_uid")
    val presidenteUid: String = "",

    @ColumnInfo(name = "administrador_uid")
    val administradorUid: String = "",

    @ColumnInfo(name = "verificada")
    val verificada: Boolean = false
)

fun Comunidad.toEntidad(): ComunidadEntidad = ComunidadEntidad(
    id = id,
    nombre = nombre,
    direccion = direccion,
    ciudad = ciudad,
    codigoPostal = codigoPostal,
    descripcion = descripcion,
    tipo = tipo,
    numeroPisos = numeroPisos,
    totalVecinos = totalVecinos,
    presidenteUid = presidenteUid,
    administradorUid = administradorUid,
    verificada = verificada
)

fun ComunidadEntidad.toModelo(): Comunidad = Comunidad(
    id = id,
    nombre = nombre,
    direccion = direccion,
    ciudad = ciudad,
    codigoPostal = codigoPostal,
    descripcion = descripcion,
    tipo = tipo,
    numeroPisos = numeroPisos,
    totalVecinos = totalVecinos,
    presidenteUid = presidenteUid,
    administradorUid = administradorUid,
    verificada = verificada
)

