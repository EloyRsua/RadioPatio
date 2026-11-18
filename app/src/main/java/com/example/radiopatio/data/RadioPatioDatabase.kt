package com.example.radiopatio.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad

@Database(
    entities = [ComunidadEntidad::class, PublicacionEntidad::class, ProductoEntidad::class],
    version = 6,
    exportSchema = false
)
abstract class RadioPatioDatabase : RoomDatabase() {
    abstract fun comunidadDao(): ComunidadDao
    abstract fun publicacionDao(): PublicacionDao
    abstract fun productoDao(): ProductoDao
}
