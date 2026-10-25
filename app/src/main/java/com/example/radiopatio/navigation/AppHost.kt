package com.example.radiopatio.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.radiopatio.PaginaPrincipal

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier
){
    NavHost(
        navController = navController,
        startDestination = Inicio,
        modifier = modifier
    ){
        composable<Inicio> {
            PaginaPrincipal()
        }

        composable<Mercadillo> {
            Text("Ventana Mercadillo")
        }

        composable<Vecinos> {
            Text("Ventana Vecinos")
        }

        composable<Eventos> {
            Text("Ventana Eventos")
        }

        composable<Publicar> {
            Text("Ventana Publicar")
        }
    }
}