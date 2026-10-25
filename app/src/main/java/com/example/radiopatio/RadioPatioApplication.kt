package com.example.radiopatio

import android.app.Application
import androidx.room.Room
import com.example.radiopatio.data.RadioPatioDatabase

class RadioPatioApplication : Application() {

    val database: RadioPatioDatabase by lazy {
        Room.databaseBuilder(
            this,
            RadioPatioDatabase::class.java,
            "radiopatio.db"
        ).build()
    }
}