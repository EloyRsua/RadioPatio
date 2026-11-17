package com.example.radiopatio.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicacionDao {
    /**
     * Consulta obligatoriamente filtrada por comunidadId a nivel de base de datos local (Room).
     */
    @Query("SELECT * FROM publicaciones WHERE comunidad_id = :comunidadId ORDER BY timestamp DESC, id DESC")
    fun observarPublicacionesPorComunidad(comunidadId: String): Flow<List<PublicacionEntidad>>

    @Query("SELECT * FROM publicaciones WHERE comunidad_id = :comunidadId ORDER BY timestamp DESC, id DESC")
    suspend fun obtenerPublicacionesPorComunidad(comunidadId: String): List<PublicacionEntidad>

    @Query("SELECT * FROM publicaciones WHERE id = :id AND comunidad_id = :comunidadId")
    suspend fun buscarPorIdYComunidad(id: Int, comunidadId: String): PublicacionEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(publicacion: PublicacionEntidad): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(publicaciones: List<PublicacionEntidad>)

    @Update
    suspend fun editar(publicacion: PublicacionEntidad)

    @Delete
    suspend fun eliminar(publicacion: PublicacionEntidad)

    @Query("DELETE FROM publicaciones WHERE comunidad_id = :comunidadId")
    suspend fun eliminarPorComunidad(comunidadId: String)

    @Query("DELETE FROM publicaciones WHERE firestore_id = :firestoreId AND comunidad_id = :comunidadId")
    suspend fun eliminarPorFirestoreId(firestoreId: String, comunidadId: String)

    @Transaction
    suspend fun reemplazarPublicacionesComunidad(
        comunidadId: String,
        publicaciones: List<PublicacionEntidad>
    ) {
        eliminarPorComunidad(comunidadId)
        if (publicaciones.isNotEmpty()) {
            insertarTodas(publicaciones)
        }
    }
}