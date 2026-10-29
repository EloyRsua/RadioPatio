package com.example.radiopatio.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.radiopatio.navigation.Eventos
import com.example.radiopatio.navigation.Inicio
import com.example.radiopatio.navigation.Mercadillo
import com.example.radiopatio.navigation.Perfil
import com.example.radiopatio.navigation.Vecinos

data class ElementoNavegacion(
    val titulo: String,
    val iconoSeleccionado: ImageVector,
    val iconoNoSeleccionado: ImageVector,
    val ruta: Any
)

@Composable
fun BarraNavegacionInferior(
    navController: NavHostController,
    rutaActual: Any?
) {
    val items = listOf(
        ElementoNavegacion("Inicio", Icons.Filled.Home, Icons.Outlined.Home, Inicio),
        ElementoNavegacion("Mercadillo", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart,
            Mercadillo
        ),
        ElementoNavegacion("Vecinos", Icons.Filled.People, Icons.Outlined.People, Vecinos),
        ElementoNavegacion("Eventos", Icons.Filled.DateRange, Icons.Outlined.DateRange, Eventos),
        ElementoNavegacion("Perfil", Icons.Filled.Person, Icons.Outlined.Person, Perfil)
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { elemento ->
            val estaSeleccionado = rutaActual?.javaClass?.name == elemento.ruta.javaClass.name

            NavigationBarItem(
                selected = estaSeleccionado,
                onClick = {
                    navController.navigate(elemento.ruta) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (estaSeleccionado) elemento.iconoSeleccionado else elemento.iconoNoSeleccionado,
                        contentDescription = elemento.titulo
                    )
                },
                label = { Text(text = elemento.titulo) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF133E54),
                    selectedTextColor = Color(0xFF133E54),
                    indicatorColor = Color(0xFFEAF5EC),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}