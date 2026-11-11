package com.example.radiopatio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ComunidadDao {
    @Query("SELECT * FROM comunidades ORDER BY nombre ASC")
    fun observarComunidades(): Flow<List<ComunidadEntidad>>

    @Query("SELECT * FROM comunidades WHERE id = :comunidadId")
    fun observarPorId(comunidadId: String): Flow<ComunidadEntidad?>

    @Upsert
    suspend fun upsert(comunidad: ComunidadEntidad)

    @Upsert
    suspend fun upsertTodas(comunidades: List<ComunidadEntidad>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(comunidad: ComunidadEntidad)
}

