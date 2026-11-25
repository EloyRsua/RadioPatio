package com.example.radiopatio.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.comunidad.BuscadorComunidadesScreen
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.comunidad.CrearComunidadScreen
import com.example.radiopatio.eventos.CalendarioEventosScreen
import com.example.radiopatio.informacionUsuario.PerfilUsuarioScreen
import com.example.radiopatio.marketplace.CrearProductoScreen
import com.example.radiopatio.marketplace.DetalleProductoScreen
import com.example.radiopatio.marketplace.MercadilloScreen
import com.example.radiopatio.marketplace.MercadilloViewModel
import com.example.radiopatio.marketplace.MercadilloViewModelFactory
import com.example.radiopatio.marketplace.MisFavoritosScreen
import com.example.radiopatio.misProductos.MisProductosScreen
import com.example.radiopatio.notificacion.NotificacionesScreen
import com.example.radiopatio.publicacion.PaginaPrincipal
import com.example.radiopatio.publicacion.PantallaCrearPublicacion
import com.example.radiopatio.publicacion.PublicacionViewModel
import com.example.radiopatio.publicacion.PublicacionViewModelFactory
import com.example.radiopatio.registroInicio.AutenticarViewModel
import com.example.radiopatio.registroInicio.AutenticarViewModelFactory
import com.example.radiopatio.registroInicio.DatosRegistroComunidad
import com.example.radiopatio.registroInicio.RegistroNuevoUsuario
import com.example.radiopatio.registroInicio.RegistroUsuario
import com.example.radiopatio.usuarios.UserRepository
import com.example.radiopatio.vecinos.VecinosScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication

    val comunidadRepository = remember { ComunidadRepository() }
    val userRepository = remember { UserRepository(comunidadRepository) }

    val viewModel: PublicacionViewModel = viewModel(
        factory = PublicacionViewModelFactory(
            publicacionDao = app.database.publicacionDao(),
            comunidadDao = app.database.comunidadDao(),
            userRepository = userRepository,
            comunidadRepository = comunidadRepository
        )
    )

    val autenticarViewModel: AutenticarViewModel = viewModel(
        factory = AutenticarViewModelFactory(
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
                viewModel = autenticarViewModel,
                onLoginExitoso = {
                    Toast.makeText(context, "Bienvenido de nuevo", Toast.LENGTH_SHORT).show()
                    navController.navigate(Inicio) {
                        popUpTo(InicioSesion) { inclusive = true }
                    }
                },
                onIrANuevoRegistroClick = {
                    autenticarViewModel.resetearEstadoRegistro()
                    navController.navigate(RegistroNuevo)
                }
            )
        }

        composable<RegistroNuevo> {
            RegistroNuevoUsuario(
                viewModel = autenticarViewModel,
                onAbrirBuscadorComunidades = {
                    navController.navigate(
                        BuscarComunidad(
                            modoRegistro = true,
                            esAdministradorRegistro = autenticarViewModel.uiState.esAdministrador
                        )
                    )
                },
                onRegistroExitoso = { mensajeBienvenida ->
                    Toast.makeText(context, mensajeBienvenida, Toast.LENGTH_LONG).show()
                    navController.navigate(Inicio) {
                        popUpTo(InicioSesion) { inclusive = true }
                    }
                },
                onVolverClick = {
                    autenticarViewModel.resetearEstadoRegistro()
                    navController.popBackStack()
                }
            )
        }

        // -------------------------------------------------------------
        // VENTANA UNIFICADA DE BÚSQUEDA DE COMUNIDADES
        // -------------------------------------------------------------

        composable<BuscarComunidad> { backStackEntry ->
            val ruta = backStackEntry.toRoute<BuscarComunidad>()

            BuscadorComunidadesScreen(
                modoRegistro = ruta.modoRegistro,
                esAdministradorRegistro = ruta.esAdministradorRegistro,
                comunidadPreseleccionadaId = autenticarViewModel.uiState.datosComunidad.comunidadExistente?.id.orEmpty(),
                onVolver = {
                    navController.popBackStack()
                },
                onComunidadSeleccionadaEnRegistro = { comunidad, solicitarTraspaso ->
                    autenticarViewModel.onDatosComunidadChange(
                        DatosRegistroComunidad(
                            crearNueva = false,
                            solicitarTraspaso = solicitarTraspaso,
                            comunidadExistente = comunidad
                        )
                    )
                    navController.popBackStack()
                },
                onIrACrearComunidad = { nombreInicial ->
                    navController.navigate(
                        CrearComunidad(
                            modoRegistro = ruta.modoRegistro,
                            esAdministradorRegistro = ruta.esAdministradorRegistro,
                            nombreInicial = nombreInicial
                        )
                    )
                },
                onComunidadCambiada = {
                    navController.popBackStack()
                }
            )
        }

        composable<CrearComunidad> { backStackEntry ->
            val ruta = backStackEntry.toRoute<CrearComunidad>()

            CrearComunidadScreen(
                modoRegistro = ruta.modoRegistro,
                esAdministradorRegistro = ruta.esAdministradorRegistro,
                nombreInicial = ruta.nombreInicial,
                onVolver = {
                    if (navController.currentBackStackEntry?.destination?.hasRoute<CrearComunidad>() == true) {
                        navController.popBackStack()
                    }
                },
                onNuevaComunidadEnRegistro = { formulario ->
                    if (ruta.esAdministradorRegistro) {
                        autenticarViewModel.onDatosComunidadChange(
                            DatosRegistroComunidad(
                                crearNueva = true,
                                solicitarTraspaso = false,
                                comunidadExistente = null,
                                nuevaComunidadNombre = formulario.nombre,
                                nuevaComunidadDireccion = formulario.direccion,
                                nuevaComunidadCiudad = formulario.ciudad,
                                nuevaComunidadCp = formulario.codigoPostal,
                                nuevaComunidadTipo = Comunidad.normalizarTipo(formulario.tipo),
                                nuevaComunidadPisos = formulario.numeroPisos,
                                nuevaComunidadDescripcion = formulario.descripcion
                            )
                        )
                        Toast.makeText(
                            context,
                            "Comunidad \"${formulario.nombre}\" seleccionada para el registro.",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack<RegistroNuevo>(inclusive = false)
                    }
                },
                onComunidadCreada = {
                    Toast.makeText(
                        context,
                        "Comunidad creada y seleccionada como activa.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack<BuscarComunidad>(inclusive = true)
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
                onIrANotificaciones = {
                    navController.navigate(Notificaciones)
                },
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                },
                viewModel = viewModel
            )
        }

        composable<Notificaciones> {
            NotificacionesScreen(
                onVolver = {
                    navController.popBackStack()
                },
                onProductoClick = { firestoreId ->
                    navController.navigate(DetalleProducto(firestoreId))
                }
            )
        }

        composable<Mercadillo> {
            MercadilloScreen(
                onProductoClick = { firestoreId ->
                    navController.navigate(DetalleProducto(firestoreId))
                },
                onIrACrearProducto = {
                    navController.navigate(CrearProducto)
                },
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Pantalla de Detalle de Producto
        composable<DetalleProducto> { backStackEntry ->
            val ruta = backStackEntry.toRoute<DetalleProducto>()
            DetalleProductoScreen(
                firestoreId = ruta.firestoreId,
                onVolver = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla para la creación de productos
        composable<CrearProducto> {
            val userRepo = remember { UserRepository() }
            val mercadilloViewModel: MercadilloViewModel = viewModel(
                factory = MercadilloViewModelFactory(app.database.productoDao(), userRepo)
            )

            CrearProductoScreen(
                onVolver = {
                    navController.popBackStack()
                },
                onPublicar = { titulo, descripcion, precio, esParaComprar, categoria, marca, color, estadoUso, fotosUris, plazoDias ->
                    mercadilloViewModel.publicarNuevoProducto(
                        titulo = titulo,
                        descripcion = descripcion,
                        precio = precio,
                        esParaComprar = esParaComprar,
                        categoria = categoria,
                        marca = marca,
                        color = color,
                        estadoUso = estadoUso,
                        fotosUris = fotosUris,
                        plazoDias = plazoDias
                    ) {
                        Toast.makeText(context, "¡Artículo publicado con éxito!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<Vecinos> {
            VecinosScreen(
                onIrABuscarComunidad = {
                    navController.navigate(BuscarComunidad(modoRegistro = false))
                }
            )
        }

        composable<Eventos> {
            CalendarioEventosScreen(
                onIrAComunidad = {
                    navController.navigate(Vecinos) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Publicar> {
            var publicando by remember { mutableStateOf(false) }

            PantallaCrearPublicacion(
                autorInicial = viewModel.uiState.usuarioActual?.nombre.orEmpty(),
                ubicacionInicial = viewModel.uiState.usuarioActual?.vivienda.orEmpty(),
                publicando = publicando,
                onVolver = {
                    if (!publicando && navController.currentBackStackEntry?.destination?.hasRoute<Publicar>() == true) {
                        navController.popBackStack()
                    }
                },
                onPublicarClick = { autor, ubicacion, contenido ->
                    if (publicando) return@PantallaCrearPublicacion
                    publicando = true
                    viewModel.crearPublicacion(
                        autor = autor,
                        ubicacion = ubicacion,
                        contenido = contenido
                    ) { exito ->
                        publicando = false
                        if (exito) {
                            if (navController.currentBackStackEntry?.destination?.hasRoute<Publicar>() == true) {
                                navController.popBackStack()
                            }
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
                onCerrarSesion = {
                    navController.navigate(InicioSesion) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onIrAMisProductos = {
                    navController.navigate(MisProductos)
                },
                onIrAMisFavoritos = {
                    navController.navigate(MisFavoritos)
                }
            )
        }

        composable<MisProductos> {
            MisProductosScreen(
                onVolver = {
                    navController.popBackStack()
                },
                onProductoClick = { firestoreId ->
                    navController.navigate(DetalleProducto(firestoreId))
                }
            )
        }

        composable<MisFavoritos> {
            MisFavoritosScreen(
                onVolver = { navController.popBackStack() },
                onProductoClick = { firestoreId ->
                    navController.navigate(DetalleProducto(firestoreId))
                }
            )
        }
    }
}