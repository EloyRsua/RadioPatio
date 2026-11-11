package com.example.radiopatio.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.PerfilUsuarioScreen
import com.example.radiopatio.marketplace.MercadilloScreen
import com.example.radiopatio.publicacion.PantallaCrearPublicacion
import com.example.radiopatio.registroInicio.RegistroNuevoUsuario
import com.example.radiopatio.registroInicio.RegistroUsuario
import com.example.radiopatio.usuarios.UserRepository
import com.example.radiopatio.vecinos.VecinosScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication
    val coroutineScope = rememberCoroutineScope()

    val comunidadRepository = remember { ComunidadRepository() }
    val userRepository = remember { UserRepository(comunidadRepository) }

    val viewModel: RadioPatioViewModel = viewModel(
        factory = RadioPatioViewModelFactory(
            publicacionDao = app.database.publicacionDao(),
            userRepository = userRepository,
            comunidadRepository = comunidadRepository
        )
    )

    val destinoInicial = remember {
        if (FirebaseAuth.getInstance().currentUser != null) Inicio else InicioSesion
    }

    NavHost(
        navController = navController,
        startDestination = destinoInicial,
        modifier = modifier
    ) {

        // -------------------------------------------------------------
        // FLUJO DE AUTENTICACIÓN
        // -------------------------------------------------------------

        composable<InicioSesion> {
            RegistroUsuario(
                onLoginClick = { email, password ->
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Por favor, introduce email y contraseña", Toast.LENGTH_SHORT).show()
                        return@RegistroUsuario
                    }

                    coroutineScope.launch {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            auth.signInWithEmailAndPassword(email, password).await()

                            Toast.makeText(context, "Bienvenido de nuevo", Toast.LENGTH_SHORT).show()
                            navController.navigate(Inicio) {
                                popUpTo(InicioSesion) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error al iniciar sesión: ${e.localizedMessage ?: "Credenciales incorrectas"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onIrANuevoRegistroClick = {
                    navController.navigate(RegistroNuevo)
                }
            )
        }

        composable<RegistroNuevo> {
            val comunidadesDisponibles by comunidadRepository
                .observarComunidades()
                .collectAsState(initial = emptyList())
            var registrando by remember { mutableStateOf(false) }

            RegistroNuevoUsuario(
                comunidadesDisponibles = comunidadesDisponibles,
                cargando = registrando,
                onRegisterClick = { nombre, apellidos, vivienda, email, pass, photoUri, datosComunidad ->
                    coroutineScope.launch {
                        registrando = true
                        try {
                            val auth = FirebaseAuth.getInstance()
                            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                            val uid = authResult.user?.uid

                            if (uid != null) {
                                val (comunidadId, comunidadNombre, rol) = if (datosComunidad.crearNueva) {
                                    val comunidadCreada = comunidadRepository.crearComunidad(
                                        nombre = datosComunidad.nuevaComunidadNombre,
                                        direccion = datosComunidad.nuevaComunidadDireccion,
                                        createdByUid = uid
                                    ).getOrThrow()
                                    Triple(comunidadCreada.id, comunidadCreada.nombre, "Presidente")
                                } else {
                                    val com = datosComunidad.comunidadExistente
                                    Triple(com?.id.orEmpty(), com?.nombre.orEmpty(), "Vecino/a")
                                }

                                val photoUrl = userRepository.subirAvatarSiExiste(uid, photoUri)

                                userRepository.registrarUsuarioEnFirestore(
                                    uid = uid,
                                    nombre = nombre,
                                    apellidos = apellidos,
                                    vivienda = vivienda,
                                    email = email,
                                    comunidadId = comunidadId,
                                    comunidadNombre = comunidadNombre,
                                    rol = rol,
                                    photoUrl = photoUrl
                                ).getOrThrow()

                                Toast.makeText(context, "¡Bienvenido a RadioPatio, $nombre!", Toast.LENGTH_SHORT).show()
                                navController.navigate(Inicio) {
                                    popUpTo(InicioSesion) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error al registrar: ${e.localizedMessage ?: "No se pudo completar el registro"}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            registrando = false
                        }
                    }
                },
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        // -------------------------------------------------------------
        // PANTALLAS PRINCIPALES
        // -------------------------------------------------------------

        composable<Inicio> {
            PaginaPrincipal(
                onIrACrearPublicacion = {
                    navController.navigate(Publicar)
                },
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                },
                viewModel = viewModel
            )
        }

        composable<Mercadillo> {
            MercadilloScreen(
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Vecinos> {
            VecinosScreen()
        }

        composable<Eventos> {
            CalendarioEventosScreen(
                onVolverAInicio = {
                    navController.popBackStack()
                },
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Publicar> {
            PantallaCrearPublicacion(
                autorInicial = viewModel.uiState.usuarioActual?.nombre.orEmpty(),
                ubicacionInicial = viewModel.uiState.usuarioActual?.vivienda.orEmpty(),
                onVolver = {
                    navController.popBackStack()
                },
                onPublicarClick = { autor, ubicacion, contenido ->
                    viewModel.crearPublicacion(
                        autor = autor,
                        ubicacion = ubicacion,
                        contenido = contenido
                    ) { exito ->
                        if (exito) {
                            navController.popBackStack()
                        } else {
                            Toast.makeText(
                                context,
                                viewModel.uiState.errorMensaje ?: "No se pudo publicar el aviso",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        composable<Perfil> {
            PerfilUsuarioScreen(
                onVolver = {
                    navController.popBackStack()
                },
                onCerrarSesion = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(InicioSesion) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
