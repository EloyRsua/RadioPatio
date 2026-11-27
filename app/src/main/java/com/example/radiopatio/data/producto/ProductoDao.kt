package com.example.radiopatio.data.producto

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY fecha_creacion DESC")
    fun observarTodos(): Flow<List<ProductoEntidad>>

    @Query("SELECT * FROM productos WHERE id = :id")
    fun observarPorId(id: String): Flow<ProductoEntidad?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(productos: List<ProductoEntidad>)

    @Query("DELETE FROM productos")
    suspend fun eliminarTodos()

    @Transaction
    suspend fun sustituirTodos(productos: List<ProductoEntidad>) {
        eliminarTodos()
        insertarTodos(productos)
    }
}