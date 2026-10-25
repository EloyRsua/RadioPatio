package com.example.radiopatio.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PublicacionEntidad::class], version = 1, exportSchema = false)
abstract class RadioPatioDatabase : RoomDatabase() {
    abstract fun publicacionDao(): PublicacionDao
}