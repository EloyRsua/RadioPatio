package com.example.radiopatio.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ComunidadEntidad::class, PublicacionEntidad::class],
    version = 5,
    exportSchema = false
)
abstract class RadioPatioDatabase : RoomDatabase() {
    abstract fun comunidadDao(): ComunidadDao
    abstract fun publicacionDao(): PublicacionDao
}
