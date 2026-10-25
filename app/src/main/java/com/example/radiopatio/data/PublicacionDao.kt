package com.example.radiopatio.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PublicacionDao {
    @Query("SELECT * FROM publicaciones ORDER BY id DESC")
    suspend fun obtenerPublicaciones(): List<PublicacionEntidad>

    @Query("SELECT * FROM publicaciones WHERE id = :id")
    suspend fun buscarPorId(id: Int): PublicacionEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(publicacion: PublicacionEntidad): Long

    @Update
    suspend fun editar(publicacion: PublicacionEntidad)

    @Delete
    suspend fun eliminar(publicacion: PublicacionEntidad)
}