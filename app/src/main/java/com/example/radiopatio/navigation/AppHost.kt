package com.example.radiopatio.navigation

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.radiopatio.marketplace.MercadilloViewModel
import com.example.radiopatio.marketplace.MercadilloViewModelFactory
import com.example.radiopatio.misProductos.MisProductosScreen
import com.example.radiopatio.marketplace.CrearProductoScreen
import com.example.radiopatio.publicacion.PantallaCrearPublicacion
import com.example.radiopatio.registroInicio.RegistroNuevoUsuario
import com.example.radiopatio.registroInicio.RegistroUsuario
import com.example.radiopatio.usuarios.UserRepository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    val viewModel: RadioPatioViewModel = viewModel(
        factory = RadioPatioViewModelFactory(app.database.publicacionDao())
    )

    NavHost(
        navController = navController,
        startDestination = InicioSesion,
        modifier = modifier
    ) {

        // -------------------------------------------------------------
        // FLUJO DE AUTENTICACIÓN
        // -------------------------------------------------------------

        composable<InicioSesion> {
            RegistroUsuario(
                onLoginClick = { email, password, onError ->
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Por favor, introduce email y contraseña", Toast.LENGTH_SHORT).show()
                        onError()
                        return@RegistroUsuario
                    }

                    coroutineScope.launch {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            // 1. Iniciar sesión en Firebase Auth
                            auth.signInWithEmailAndPassword(email, password).await()

                            Toast.makeText(context, "Bienvenido de nuevo", Toast.LENGTH_SHORT).show()
                            navController.navigate(Inicio) {
                                popUpTo(InicioSesion) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            // 2. Si falla la autenticación, avisamos y desbloqueamos el botón
                            Toast.makeText(
                                context,
                                "Error al iniciar sesión: Correo o contraseña incorrectos.",
                                Toast.LENGTH_LONG
                            ).show()

                            // Restablece la variable 'cargando = false' en RegistroUsuario
                            onError()
                        }
                    }
                },
                onIrANuevoRegistroClick = {
                    navController.navigate(RegistroNuevo)
                }
            )
        }

        composable<RegistroNuevo> {
            RegistroNuevoUsuario(
                onRegisterClick = { nombre, apellidos, vivienda, email, pass, photoUri ->
                    coroutineScope.launch {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            val db = FirebaseFirestore.getInstance()

                            // 1. Crear usuario en Firebase Auth (rápido)
                            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                            val uid = authResult.user?.uid

                            if (uid != null) {
                                // 2. Guardar en Firestore en segundo plano (sin bloquear la navegación)
                                val nuevoUsuario = mapOf(
                                    "uid" to uid,
                                    "nombre" to nombre,
                                    "apellidos" to apellidos,
                                    "piso" to vivienda,
                                    "email" to email,
                                    "role" to "Vecino/a",
                                    "photoUrl" to (photoUri?.toString() ?: "")
                                )

                                // Lanzamos la escritura sin suspender la corrutina principal
                                db.collection("users").document(uid).set(nuevoUsuario)

                                // 3. Navegación INMEDIATA a la primera pulsación
                                Toast.makeText(context, "¡Bienvenido a RadioPatio, $nombre!", Toast.LENGTH_SHORT).show()
                                navController.navigate(Inicio) {
                                    popUpTo(InicioSesion) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            if (e.message?.contains("already in use") == true) {
                                // Si ya existe, iniciamos sesión y entramos directamente
                                try {
                                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass).await()
                                    navController.navigate(Inicio) {
                                        popUpTo(InicioSesion) { inclusive = true }
                                    }
                                } catch (loginError: Exception) {
                                    Toast.makeText(context, "Este correo ya está registrado.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
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
                }
            )
        }

        composable<Mercadillo> {
            MercadilloScreen(
                onIrACrearProducto = {
                    navController.navigate(CrearProducto)
                }
            )
        }

        // Pantalla para la creacion de productos
        composable<CrearProducto> {
            val userRepository = remember { UserRepository() }
            val mercadilloViewModel: MercadilloViewModel = viewModel(
                factory = MercadilloViewModelFactory(app.database.productoDao(), userRepository)
            )

            CrearProductoScreen(
                onVolver = {
                    navController.popBackStack()
                },
                onPublicar = { titulo, descripcion, precio, esParaComprar, categoria, marca, color, estadoUso, fotosUris ->
                    mercadilloViewModel.publicarNuevoProducto(
                        titulo = titulo,
                        descripcion = descripcion,
                        precio = precio,
                        esParaComprar = esParaComprar,
                        categoria = categoria,
                        marca = marca,
                        color = color,
                        estadoUso = estadoUso,
                        fotosUris = fotosUris
                    ) {
                        Toast.makeText(context, "¡Artículo publicado con éxito!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Vuelve automáticamente al mercadillo
                    }
                }
            )
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

                    viewModel.crearPublicacion(nuevaEntidad)
                    navController.popBackStack()
                }
            )
        }

        composable<Perfil> {
            PerfilUsuarioScreen(
                onIrAMisProductos = {
                    navController.navigate(MisProductos)
                }
            )
        }

        composable<MisProductos>{
            MisProductosScreen(
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}