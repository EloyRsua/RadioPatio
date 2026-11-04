package com.example.radiopatio.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.radiopatio.CalendarioEventosScreen
import com.example.radiopatio.PaginaPrincipal
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.RadioPatioViewModel
import com.example.radiopatio.RadioPatioViewModelFactory
import com.example.radiopatio.data.PublicacionEntidad
import com.example.radiopatio.informacionUsuario.PerfilUsuarioScreen
import com.example.radiopatio.marketplace.MercadilloScreen
import com.example.radiopatio.publicacion.PantallaCrearPublicacion
import com.example.radiopatio.registroInicio.RegistroNuevoUsuario
import com.example.radiopatio.registroInicio.RegistroUsuario

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier
) {
    // Contexto y ViewModel de Room para la navegación
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication

    val viewModel: RadioPatioViewModel = viewModel(
        factory = RadioPatioViewModelFactory(app.database.publicacionDao())
    )

    NavHost(
        navController = navController,
        startDestination = Inicio,
        modifier = modifier
    ) {

        // 1. Iniciar Sesión (Usuarios ya registrados)
        composable<InicioSesion> {
            RegistroUsuario(
                onLoginClick = { email, password ->
                    // TODO: Conectar con Firebase Auth (signInWithEmailAndPassword)
                    navController.navigate(Inicio) {
                        popUpTo(InicioSesion) { inclusive = true }
                    }
                },
                onIrANuevoRegistroClick = {
                    navController.navigate(RegistroNuevo)
                }
            )
        }

        // 2. Crear Cuenta (Nuevos Usuarios)
        composable<RegistroNuevo> {
            RegistroNuevoUsuario(
                onRegisterClick = { nombre, apellidos, email, pass, photoUri ->
                    // TODO: Conectar con UserRepository / Firebase Auth + Firestore
                    navController.navigate(Inicio) {
                        popUpTo(InicioSesion) { inclusive = true }
                    }
                },
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Inicio> {
            PaginaPrincipal(
                onIrACrearPublicacion = {
                    navController.navigate(Publicar)
                }
            )
        }

        composable<Mercadillo> {
            MercadilloScreen()
        }

        composable<Vecinos> {
            Text("Ventana Vecinos")
        }

        composable<Eventos> {
            CalendarioEventosScreen()
        }

        composable<Publicar> {
            PantallaCrearPublicacion(
                onVolver = {
                    navController.popBackStack()
                },
                onPublicarClick = { autor, ubicacion, contenido ->
                    val inicialAutor = autor.trim().firstOrNull()?.uppercase() ?: "V"
                    val nuevaEntidad = PublicacionEntidad(
                        autor = autor.ifBlank { "Vecino Anónimo" },
                        inicial = inicialAutor,
                        tiempoYUbica = "Ahora mismo · ${ubicacion.ifBlank { "Comunidad" }}",
                        contenido = contenido,
                        meGusta = 0,
                        comentarios = 0
                    )

                    // Inserción asíncrona en Room y vuelta al Feed
                    viewModel.crearPublicacion(nuevaEntidad)
                    navController.popBackStack()
                }
            )
        }
        composable<Perfil> {
            PerfilUsuarioScreen(
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}