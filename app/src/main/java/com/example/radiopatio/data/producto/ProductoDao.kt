package com.example.radiopatio.data.producto

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos ORDER BY id DESC")
    suspend fun obtenerProductos(): List<ProductoEntidad>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun buscarPorId(id: Int): ProductoEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: ProductoEntidad): Long

    @Update
    suspend fun editar(producto: ProductoEntidad)

    @Delete
    suspend fun eliminar(producto: ProductoEntidad)
}