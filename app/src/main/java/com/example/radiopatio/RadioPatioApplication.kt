package com.example.radiopatio

import android.app.Application
import androidx.room.Room
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.data.RadioPatioDatabase
import com.example.radiopatio.marketplace.MercadilloRepository
import com.example.radiopatio.usuarios.UserRepository

class RadioPatioApplication : Application() {

    val database: RadioPatioDatabase by lazy {
        Room.databaseBuilder(
            this,
            RadioPatioDatabase::class.java,
            "radiopatio.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val mercadilloRepository: MercadilloRepository by lazy {
        MercadilloRepository(database.productoDao())
    }

    val userRepository: UserRepository by lazy {
        UserRepository()
    }

    val comunidadRepository: ComunidadRepository by lazy {
        ComunidadRepository()
    }
}