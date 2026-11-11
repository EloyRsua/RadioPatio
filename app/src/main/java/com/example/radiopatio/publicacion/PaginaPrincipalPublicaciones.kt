package com.example.radiopatio.publicacion

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.R
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.RadioPatioViewModel
import com.example.radiopatio.RadioPatioViewModelFactory
import com.example.radiopatio.TablonUiState
import com.example.radiopatio.data.PublicacionEntidad
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.ui.theme.RadioPatioTheme

@Composable
fun PaginaPrincipal(
    modifier: Modifier = Modifier,
    onIrACrearPublicacion: () -> Unit = {},
    onIrAComunidad: () -> Unit = {},
    viewModel: RadioPatioViewModel = viewModel(
        factory = RadioPatioViewModelFactory(
            publicacionDao = (LocalContext.current.applicationContext as RadioPatioApplication).database.publicacionDao(),
            comunidadDao = (LocalContext.current.applicationContext as RadioPatioApplication).database.comunidadDao()
        )
    )
) {
    val context = LocalContext.current
    val errorMsg = viewModel.uiState.errorMensaje
    LaunchedEffect(errorMsg) {
        errorMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    PaginaPrincipalContent(
        uiState = viewModel.uiState,
        modifier = modifier,
        onTextoBusquedaChange = viewModel::onTextoBusquedaChange,
        onIrACrearPublicacion = onIrACrearPublicacion,
        onIrAComunidad = onIrAComunidad,
        onEliminarPublicacion = viewModel::eliminarPublicacion
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginaPrincipalContent(
    uiState: TablonUiState,
    modifier: Modifier = Modifier,
    onTextoBusquedaChange: (String) -> Unit = {},
    onIrACrearPublicacion: () -> Unit = {},
    onIrAComunidad: () -> Unit = {},
    onEliminarPublicacion: (PublicacionEntidad) -> Unit = {}
) {
    var mostrarBuscador by remember { mutableStateOf(false) }
    var publicacionSeleccionada by remember { mutableStateOf<PublicacionEntidad?>(null) }

    val publicacionesVisibles = uiState.publicacionesFiltradas
    val uidActual = uiState.usuarioActual?.uid.orEmpty()
    val nombreUsuarioActual = uiState.usuarioActual?.nombre.orEmpty()
    val emailUsuarioActual = uiState.usuarioActual?.email.orEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperior(
                nombreComunidad = uiState.comunidadNombre,
                buscadorActivo = mostrarBuscador,
                onToggleBuscador = {
                    mostrarBuscador = !mostrarBuscador
                    if (!mostrarBuscador) onTextoBusquedaChange("")
                }
            )
        },
        floatingActionButton = {
            if (uiState.comunidadIdActual.isNotBlank()) {
                FloatingActionButton(
                    onClick = onIrACrearPublicacion,
                    containerColor = Color(0xFF389F82),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva publicación")
                }
            }
        }
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(rellenoInterior)
        ) {
            when {
                uiState.cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF389F82)
                    )
                }

                uiState.comunidadIdActual.isBlank() -> {
                    SinComunidadTablonCard(
                        tieneSolicitudPendiente = uiState.usuarioActual?.solicitudesPendientesComunidades?.isNotEmpty() == true,
                        onIrAComunidad = onIrAComunidad,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AnimatedVisibility(visible = mostrarBuscador) {
                                OutlinedTextField(
                                    value = uiState.textoBusqueda,
                                    onValueChange = onTextoBusquedaChange,
                                    placeholder = { Text("Buscar avisos en el tablón...", fontSize = 13.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (uiState.textoBusqueda.isNotBlank()) {
                                            IconButton(onClick = { onTextoBusquedaChange("") }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Limpiar")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            ) {
                                CampoCrearPublicacion(
                                    onCrearClick = onIrACrearPublicacion
                                )
                            }
                        }

                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9FAFB))
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                NoticiaFijada(nombreComunidad = uiState.comunidadNombre)
                            }
                        }

                        if (publicacionesVisibles.isEmpty()) {
                            item {
                                EstadoTablonVacio(
                                    hayBusqueda = uiState.textoBusqueda.isNotBlank(),
                                    onCrearClick = onIrACrearPublicacion
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = publicacionesVisibles,
                                key = { index, pub ->
                                    if (pub.firestoreId.isNotBlank()) {
                                        "${pub.firestoreId}-$index"
                                    } else {
                                        "local-${pub.id}-$index"
                                    }
                                }
                            ) { _, entidad ->
                                val esMiPublicacion =
                                    (uidActual.isNotBlank() && entidad.autorUid == uidActual) ||
                                        (entidad.autorUid.isBlank() && (
                                            (nombreUsuarioActual.isNotBlank() &&
                                                entidad.autor.contains(nombreUsuarioActual, ignoreCase = true)) ||
                                                (emailUsuarioActual.isNotBlank() &&
                                                    entidad.autor.contains(
                                                        emailUsuarioActual.substringBefore("@"),
                                                        ignoreCase = true
                                                    ))
                                            ))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    TarjetaPublicacion(
                                        publicacion = Publicacion(
                                            autor = entidad.autor,
                                            inicial = entidad.inicial,
                                            tiempoYUbica = entidad.tiempoYUbica,
                                            contenido = entidad.contenido,
                                            meGusta = entidad.meGusta,
                                            comentarios = entidad.comentarios
                                        ),
                                        esMiPublicacion = esMiPublicacion,
                                        onEliminarClick = {
                                            onEliminarPublicacion(entidad)
                                        },
                                        modifier = Modifier.clickable {
                                            publicacionSeleccionada = entidad
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo a pantalla completa con el detalle de la publicación seleccionada
        publicacionSeleccionada?.let { entidad ->
            PublicacionDetalleDialog(
                publicacion = entidad,
                onDismiss = {
                    publicacionSeleccionada = null
                }
            )
        }
    }
}

@Composable
private fun BarraSuperior(
    nombreComunidad: String,
    buscadorActivo: Boolean,
    onToggleBuscador: () -> Unit
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_radiopatio),
                contentDescription = "Logo Radio Patio",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RadioPatio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF133E54)
                )
                if (nombreComunidad.isNotBlank()) {
                    Text(
                        text = nombreComunidad,
                        fontSize = 11.sp,
                        color = Color(0xFF0F8A5F),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onToggleBuscador) {
                Icon(
                    imageVector = if (buscadorActivo) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFF133E54)
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Avisos", tint = Color(0xFF133E54))
            }
        }
    }
}

@Composable
private fun NoticiaFijada(nombreComunidad: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEAF5EC))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📌 TABLÓN COMUNITARIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F8A5F)
            )
            Text(
                text = nombreComunidad.ifBlank { "En directo" },
                fontSize = 11.sp,
                color = Color(0xFF0F8A5F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Espacio exclusivo de tu comunidad",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = "Comparte avisos, consultas y noticias en tiempo real únicamente con tus vecinos.",
            fontSize = 12.sp,
            color = Color(0xFF455A64)
        )
    }
}

@Composable
private fun CampoCrearPublicacion(
    onCrearClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(12.dp))
            .clickable { onCrearClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "¿Qué quieres compartir hoy con tu comunidad?",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Crear publicación",
            tint = Color(0xFF389F82),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SinComunidadTablonCard(
    tieneSolicitudPendiente: Boolean = false,
    onIrAComunidad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Apartment,
                contentDescription = null,
                tint = Color(0xFF0F8A5F),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (tieneSolicitudPendiente) {
                    "Solicitud pendiente de verificación"
                } else {
                    "Sin comunidad asignada"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF133E54),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (tieneSolicitudPendiente) {
                    "Tu acceso al tablón se activará en cuanto el administrador de tu comunidad verifique y acepte tu solicitud."
                } else {
                    "Para ver y publicar avisos en el tablón, selecciona o solicita unirte a tu comunidad en la pestaña Vecinos."
                },
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onIrAComunidad,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8A5F)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Ir a Vecinos y Comunidad", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EstadoTablonVacio(
    hayBusqueda: Boolean,
    onCrearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = Color(0xFF90A4AE),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (hayBusqueda) "Sin resultados" else "Aún no hay publicaciones en tu comunidad",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF133E54),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hayBusqueda) {
                "Prueba con otro término de búsqueda."
            } else {
                "Sé el primero en compartir un aviso con tus vecinos."
            },
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        if (!hayBusqueda) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onCrearClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Crear publicación", fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaginaPrincipalPreview() {
    val usuarioEjemplo = Usuario(
        nombre = "Lucía Fernández",
        inicial = "L",
        vivienda = "Portal 1, 4ºC",
        email = "lucia@radiopatio.com",
        telefono = "",
        uid = "u_lucia",
        comunidadId = "com_1",
        comunidadNombre = "Residencial Montecerrao"
    )
    val publicacionesEjemplo = listOf(
        PublicacionEntidad(
            id = 1,
            firestoreId = "p1",
            comunidadId = "com_1",
            autorUid = "u_lucia",
            autor = "Lucía Fernández",
            inicial = "L",
            tiempoYUbica = "Hace 2 h · Portal 1, 4ºC",
            contenido = "¡Buenas vecin@s! Este sábado organizamos una quedada en el parque.",
            meGusta = 24,
            comentarios = 6,
            timestamp = 2L
        ),
        PublicacionEntidad(
            id = 2,
            firestoreId = "p2",
            comunidadId = "com_1",
            autorUid = "u_carlos",
            autor = "Carlos Ruiz",
            inicial = "C",
            tiempoYUbica = "Hace 5 h · Portal 3, 2ºA",
            contenido = "¿Alguien sabe a qué hora vienen a revisar la caldera?",
            meGusta = 4,
            comentarios = 8,
            timestamp = 1L
        )
    )
    RadioPatioTheme {
        PaginaPrincipalContent(
            uiState = TablonUiState(
                comunidadIdActual = "com_1",
                comunidadNombre = "Residencial Montecerrao",
                usuarioActual = usuarioEjemplo,
                publicacionesTotales = publicacionesEjemplo,
                cargando = false
            )
        )
    }
}
