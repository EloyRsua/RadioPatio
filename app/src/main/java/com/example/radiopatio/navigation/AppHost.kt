package com.example.radiopatio.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.radiopatio.eventos.CalendarioEventosScreen
import com.example.radiopatio.publicacion.PaginaPrincipal
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.RadioPatioViewModel
import com.example.radiopatio.RadioPatioViewModelFactory
import com.example.radiopatio.comunidad.BuscadorComunidadesScreen
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.comunidad.CrearComunidadScreen
import com.example.radiopatio.informacionUsuario.PerfilUsuarioScreen
import com.example.radiopatio.informacionUsuario.ROL_ADMINISTRADOR_FINCAS
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.marketplace.MercadilloScreen
import com.example.radiopatio.publicacion.PantallaCrearPublicacion
import com.example.radiopatio.registroInicio.DatosRegistroComunidad
import com.example.radiopatio.registroInicio.RegistroNuevoUsuario
import com.example.radiopatio.registroInicio.RegistroUsuario
import com.example.radiopatio.usuarios.UserRepository
import com.example.radiopatio.vecinos.VecinosScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val DatosRegistroComunidadSaver = listSaver<DatosRegistroComunidad, Any?>(
    save = { datos ->
        listOf(
            datos.crearNueva,
            datos.solicitarTraspaso,
            datos.comunidadExistente?.id ?: "",
            datos.comunidadExistente?.nombre ?: "",
            datos.comunidadExistente?.direccion ?: "",
            datos.comunidadExistente?.ciudad ?: "",
            datos.comunidadExistente?.codigoPostal ?: "",
            datos.comunidadExistente?.tipoNormalizado ?: Comunidad.TIPO_URBANIZACION,
            datos.comunidadExistente?.numeroPisos ?: 0,
            datos.comunidadExistente?.codigoCompartible ?: "",
            datos.comunidadExistente?.administradorUid ?: "",
            datos.nuevaComunidadNombre,
            datos.nuevaComunidadDireccion,
            datos.nuevaComunidadCiudad,
            datos.nuevaComunidadCp,
            datos.nuevaComunidadTipo,
            datos.nuevaComunidadPisos,
            datos.nuevaComunidadDescripcion
        )
    },
    restore = { lista ->
        val comId = lista[2] as String
        val comExistente = if (comId.isNotBlank()) {
            Comunidad(
                id = comId,
                nombre = lista[3] as String,
                direccion = lista[4] as String,
                ciudad = lista[5] as String,
                codigoPostal = lista[6] as String,
                tipo = Comunidad.normalizarTipo(lista[7] as String),
                numeroPisos = lista[8] as Int,
                codigo = lista[9] as String,
                administradorUid = lista[10] as String
            )
        } else null

        DatosRegistroComunidad(
            crearNueva = lista[0] as Boolean,
            solicitarTraspaso = lista[1] as Boolean,
            comunidadExistente = comExistente,
            nuevaComunidadNombre = lista[11] as String,
            nuevaComunidadDireccion = lista[12] as String,
            nuevaComunidadCiudad = lista[13] as String,
            nuevaComunidadCp = lista[14] as String,
            nuevaComunidadTipo = Comunidad.normalizarTipo(lista[15] as String),
            nuevaComunidadPisos = lista[16] as Int,
            nuevaComunidadDescripcion = lista[17] as String
        )
    }
)

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
            comunidadDao = app.database.comunidadDao(),
            userRepository = userRepository,
            comunidadRepository = comunidadRepository
        )
    )

    val destinoInicial = remember {
        if (FirebaseAuth.getInstance().currentUser != null) Inicio else InicioSesion
    }

    // Estado compartido entre RegistroNuevo y la ventana unificada BuscarComunidad
    var datosComunidadRegistro by rememberSaveable(stateSaver = DatosRegistroComunidadSaver) {
        mutableStateOf(DatosRegistroComunidad())
    }
    var esAdministradorRegistro by rememberSaveable { mutableStateOf(false) }

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
                onLoginClick = { email, password, onError ->
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Por favor, introduce email y contraseña", Toast.LENGTH_SHORT).show()
                        onError()
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
                                "Error al iniciar sesión: Correo o contraseña incorrectos.",
                                Toast.LENGTH_LONG
                            ).show()
                            onError()
                        }
                    }
                },
                onIrANuevoRegistroClick = {
                    datosComunidadRegistro = DatosRegistroComunidad()
                    esAdministradorRegistro = false
                    navController.navigate(RegistroNuevo)
                }
            )
        }

        composable<RegistroNuevo> {
            var registrando by remember { mutableStateOf(false) }

            RegistroNuevoUsuario(
                datosComunidad = datosComunidadRegistro,
                esAdministrador = esAdministradorRegistro,
                cargando = registrando,
                onTipoCuentaChange = { nuevoEsAdmin ->
                    esAdministradorRegistro = nuevoEsAdmin
                    if (!nuevoEsAdmin && (datosComunidadRegistro.crearNueva || datosComunidadRegistro.solicitarTraspaso)) {
                        datosComunidadRegistro = DatosRegistroComunidad()
                    }
                },
                onAbrirBuscadorComunidades = {
                    navController.navigate(
                        BuscarComunidad(
                            modoRegistro = true,
                            esAdministradorRegistro = esAdministradorRegistro
                        )
                    )
                },
                onRegisterClick = { nombre, apellidos, vivienda, email, pass, photoUri, esAdmin, datosComunidad ->
                    coroutineScope.launch {
                        registrando = true
                        try {
                            if (datosComunidad.crearNueva && !esAdmin) {
                                throw SecurityException("Solo los administradores de fincas pueden crear comunidades.")
                            }

                            val auth = FirebaseAuth.getInstance()
                            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                            val uid = authResult.user?.uid

                            if (uid != null) {
                                val photoUrl = userRepository.subirAvatarSiExiste(uid, photoUri)
                                var mensajeBienvenida = "¡Bienvenido a RadioPatio, $nombre!"

                                if (esAdmin) {
                                    when {
                                        datosComunidad.crearNueva -> {
                                            val comunidadCreada = comunidadRepository.crearComunidad(
                                                nombre = datosComunidad.nuevaComunidadNombre,
                                                direccion = datosComunidad.nuevaComunidadDireccion,
                                                ciudad = datosComunidad.nuevaComunidadCiudad,
                                                codigoPostal = datosComunidad.nuevaComunidadCp,
                                                descripcion = datosComunidad.nuevaComunidadDescripcion,
                                                tipo = datosComunidad.nuevaComunidadTipo,
                                                numeroPisos = datosComunidad.nuevaComunidadPisos,
                                                createdByUid = uid,
                                                creadorEsAdministrador = true
                                            ).getOrThrow()

                                            userRepository.registrarUsuarioEnFirestore(
                                                uid = uid,
                                                nombre = nombre,
                                                apellidos = apellidos,
                                                vivienda = vivienda,
                                                email = email,
                                                comunidadId = comunidadCreada.id,
                                                comunidadNombre = comunidadCreada.nombre,
                                                rol = ROL_ADMINISTRADOR_FINCAS,
                                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                                comunidadesIds = listOf(comunidadCreada.id),
                                                photoUrl = photoUrl
                                            ).getOrThrow()
                                        }

                                        datosComunidad.comunidadExistente != null -> {
                                            val com = datosComunidad.comunidadExistente
                                            userRepository.registrarUsuarioEnFirestore(
                                                uid = uid,
                                                nombre = nombre,
                                                apellidos = apellidos,
                                                vivienda = vivienda,
                                                email = email,
                                                comunidadId = "",
                                                comunidadNombre = "",
                                                rol = ROL_ADMINISTRADOR_FINCAS,
                                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                                comunidadesIds = emptyList(),
                                                photoUrl = photoUrl
                                            ).getOrThrow()

                                            val unionDirecta = if (!datosComunidad.solicitarTraspaso && com.administradorUid.isBlank()) {
                                                userRepository.unirseComoAdministradorAComunidadSinAdmin(com.id)
                                            } else {
                                                Result.failure(IllegalStateException("Requiere solicitud"))
                                            }

                                            if (unionDirecta.isFailure) {
                                                val solicitante = Usuario(
                                                    nombre = "$nombre $apellidos".trim().ifBlank { nombre },
                                                    inicial = nombre.firstOrNull()?.uppercase() ?: "A",
                                                    vivienda = vivienda,
                                                    email = email,
                                                    telefono = "",
                                                    rol = ROL_ADMINISTRADOR_FINCAS,
                                                    uid = uid,
                                                    photoUrl = photoUrl,
                                                    tipoCuenta = ROL_ADMINISTRADOR_FINCAS
                                                )
                                                comunidadRepository.solicitarTraspasoAdmin(com.id, solicitante).getOrThrow()
                                                mensajeBienvenida = "Cuenta creada. Solicitud enviada al administrador de ${com.nombre}."
                                            }
                                        }

                                        else -> {
                                            userRepository.registrarUsuarioEnFirestore(
                                                uid = uid,
                                                nombre = nombre,
                                                apellidos = apellidos,
                                                vivienda = vivienda,
                                                email = email,
                                                comunidadId = "",
                                                comunidadNombre = "",
                                                rol = ROL_ADMINISTRADOR_FINCAS,
                                                tipoCuenta = ROL_ADMINISTRADOR_FINCAS,
                                                comunidadesIds = emptyList(),
                                                photoUrl = photoUrl
                                            ).getOrThrow()
                                        }
                                    }
                                } else {
                                    val com = datosComunidad.comunidadExistente
                                        ?: throw IllegalArgumentException("Debes seleccionar una comunidad para registrarte.")

                                    userRepository.registrarUsuarioEnFirestore(
                                        uid = uid,
                                        nombre = nombre,
                                        apellidos = apellidos,
                                        vivienda = vivienda,
                                        email = email,
                                        comunidadId = "",
                                        comunidadNombre = "",
                                        rol = ROL_VECINO,
                                        tipoCuenta = ROL_VECINO,
                                        comunidadesIds = emptyList(),
                                        photoUrl = photoUrl
                                    ).getOrThrow()

                                    val nombreCompleto = "$nombre $apellidos".trim().ifBlank { nombre }
                                    val solicitante = Usuario(
                                        nombre = nombreCompleto,
                                        inicial = nombre.firstOrNull()?.uppercase() ?: "V",
                                        vivienda = vivienda,
                                        email = email,
                                        telefono = "",
                                        rol = ROL_VECINO,
                                        uid = uid,
                                        photoUrl = photoUrl,
                                        tipoCuenta = ROL_VECINO
                                    )
                                    comunidadRepository.solicitarIngresoVecino(
                                        comunidadId = com.id,
                                        solicitante = solicitante,
                                        viviendaIndicada = vivienda
                                    ).getOrThrow()
                                    mensajeBienvenida = "Cuenta creada. Tu solicitud de ingreso en ${com.nombre} queda pendiente de verificación por el administrador."
                                }

                                datosComunidadRegistro = DatosRegistroComunidad()
                                esAdministradorRegistro = false
                                Toast.makeText(context, mensajeBienvenida, Toast.LENGTH_LONG).show()
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
                    datosComunidadRegistro = DatosRegistroComunidad()
                    esAdministradorRegistro = false
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
                comunidadPreseleccionadaId = datosComunidadRegistro.comunidadExistente?.id.orEmpty(),
                onVolver = {
                    navController.popBackStack()
                },
                onComunidadSeleccionadaEnRegistro = { comunidad, solicitarTraspaso ->
                    datosComunidadRegistro = DatosRegistroComunidad(
                        crearNueva = false,
                        solicitarTraspaso = solicitarTraspaso,
                        comunidadExistente = comunidad
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
                        datosComunidadRegistro = DatosRegistroComunidad(
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
