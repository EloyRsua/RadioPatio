package com.example.radiopatio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.radiopatio.navigation.AppNavHost
import com.example.radiopatio.navigation.Eventos
import com.example.radiopatio.navigation.Inicio
import com.example.radiopatio.navigation.Mercadillo
import com.example.radiopatio.navigation.Perfil
import com.example.radiopatio.navigation.Vecinos
import com.example.radiopatio.ui.theme.RadioPatioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            val entradaPilaNavegacion = navController.currentBackStackEntryAsState()
            val destinoActual = entradaPilaNavegacion?.value?.destination

            RadioPatioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            // 1. Item Inicio
                            NavigationBarItem(
                                selected = destinoActual?.hasRoute<Inicio>() == true,
                                onClick = {
                                    navController.navigate(Inicio)
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") }
                            )

                            // 2. Item Mercadillo
                            NavigationBarItem(
                                selected = destinoActual?.hasRoute<Mercadillo>() == true,
                                onClick = {
                                    navController.navigate(Mercadillo)
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.ShoppingBag,
                                        contentDescription = "Mercadillo"
                                    )
                                }
                            )

                            // 3. Item Vecinos
                            NavigationBarItem(
                                selected = destinoActual?.hasRoute<Vecinos>() == true,
                                onClick = {
                                    navController.navigate(Vecinos)
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Groups,
                                        contentDescription = "Vecinos"
                                    )
                                }
                            )

                            // 4. Item Eventos
                            NavigationBarItem(
                                selected = destinoActual?.hasRoute<Eventos>() == true,
                                onClick = {
                                    navController.navigate(Eventos)
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.CalendarMonth,
                                        contentDescription = "Eventos"
                                    )
                                }
                            )

                            // 5. Item Perfil
                            NavigationBarItem(
                                selected = destinoActual?.hasRoute<Perfil>() == true,
                                onClick = {
                                    navController.navigate(Perfil)
                                },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = "Perfil"
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}