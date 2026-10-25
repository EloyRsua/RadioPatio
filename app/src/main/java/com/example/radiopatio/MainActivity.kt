package com.example.radiopatio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.radiopatio.marketplace.MercadilloScreen
import com.example.radiopatio.ui.theme.RadioPatioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioPatioTheme {
                //PaginaPrincipal()
                MercadilloScreen()
            }
        }
    }
}