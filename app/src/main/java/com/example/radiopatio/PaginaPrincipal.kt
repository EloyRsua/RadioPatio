package com.example.radiopatio

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Secciones de navegación disponibles en la barra inferior.
 */
enum class SeccionRadioPatio {
    INICIO,
    VECINOS,
    PUBLICAR,
    MERCADILLO,
    EVENTOS
}

/**
 * UiState inmutable para la pantalla principal (Feed).
 * Aplica el patrón UiState para agrupar las propiedades de estado de la pantalla.
 */
data class PaginaPrincipalUiState(
    val seccionActual: SeccionRadioPatio = SeccionRadioPatio.INICIO,
    val listaPublicaciones: List<Publicacion> = listOf(
        Publicacion(
            autor = "Lucía Fernández",
            inicial = "L",
            tiempoYUbica = "Hace 2 h · Calle Mayor",
            contenido = "¡Buenas vecin@s! ☀️ Este sábado organizamos una quedada en el parque para tomar algo. ¡Os esperamos!",
            meGusta = 24,
            comentarios = 6
        ),
        Publicacion(
            autor = "María García",
            inicial = "M",
            tiempoYUbica = "Hace 3 h · Calle Mayor",
            contenido = "Se ha encontrado un peluche en el portal, se ha dejado en la garita del conserje.",
            meGusta = 0,
            comentarios = 0
        ),
        Publicacion(
            autor = "Carlos Ruiz",
            inicial = "C",
            tiempoYUbica = "Hace 5 h · Portal 4",
            contenido = "¿Alguien sabe si mañana cortan el agua por obras en la caldera?",
            meGusta = 4,
            comentarios = 8
        ),
        Publicacion(
            autor = "Ana Belén",
            inicial = "A",
            tiempoYUbica = "Hace 6 h · 2º B",
            contenido = "Tengo cajas de cartón limpias para mudanza por si a alguien le hacen falta antes de tirarlas al contenedor azul.",
            meGusta = 9,
            comentarios = 3
        ),
        Publicacion(
            autor = "Javier Gómez",
            inicial = "J",
            tiempoYUbica = "Ayer · Garaje",
            contenido = "Recordad aparcar dentro de las líneas en la planta -1 para no dificultar la maniobra de salida.",
            meGusta = 15,
            comentarios = 2
        )
    ),
    val mostrarDialogoRedactar: Boolean = false
)

/**
 * Función de entrada retrocompatible que delega en [PaginaPrincipalScreen].
 */
@Composable
fun PaginaPrincipal(seccionInicial: SeccionRadioPatio = SeccionRadioPatio.INICIO) {
    PaginaPrincipalScreen(seccionInicial = seccionInicial)
}

/**
 * Screen (Stateful): Mantiene y gestiona el estado de la pantalla principal (State Hoisting).
 * Intermediario que pasa el estado a [PaginaPrincipalContent] o a otras pantallas de sección.
 */
@Composable
fun PaginaPrincipalScreen(
    seccionInicial: SeccionRadioPatio = SeccionRadioPatio.INICIO
) {
    var uiState by remember { mutableStateOf(PaginaPrincipalUiState(seccionActual = seccionInicial)) }

    when (uiState.seccionActual) {
        SeccionRadioPatio.EVENTOS -> {
            CalendarioEventosScreen(
                onVolverAInicio = {
                    uiState = uiState.copy(seccionActual = SeccionRadioPatio.INICIO)
                },
                barraInferior = {
                    BarraNavegacionInferior(
                        seccionActual = uiState.seccionActual,
                        onSeleccionarSeccion = { nuevaSeccion ->
                            uiState = uiState.copy(seccionActual = nuevaSeccion)
                        },
                        onCrearPulsado = {
                            uiState = uiState.copy(seccionActual = SeccionRadioPatio.EVENTOS)
                        }
                    )
                }
            )
        }

        SeccionRadioPatio.VECINOS, SeccionRadioPatio.MERCADILLO -> {
            PantallaSeccionEnDesarrollo(
                seccion = uiState.seccionActual,
                onVolverAInicio = {
                    uiState = uiState.copy(seccionActual = SeccionRadioPatio.INICIO)
                },
                onIrAEventos = {
                    uiState = uiState.copy(seccionActual = SeccionRadioPatio.EVENTOS)
                },
                barraInferior = {
                    BarraNavegacionInferior(
                        seccionActual = uiState.seccionActual,
                        onSeleccionarSeccion = { nuevaSeccion ->
                            uiState = uiState.copy(seccionActual = nuevaSeccion)
                        },
                        onCrearPulsado = {
                            uiState = uiState.copy(mostrarDialogoRedactar = true)
                        }
                    )
                }
            )
        }

        SeccionRadioPatio.INICIO, SeccionRadioPatio.PUBLICAR -> {
            PaginaPrincipalContent(
                uiState = uiState,
                onSeleccionarSeccion = { nuevaSeccion ->
                    uiState = uiState.copy(seccionActual = nuevaSeccion)
                },
                onAbrirDialogoRedactar = {
                    uiState = uiState.copy(mostrarDialogoRedactar = true)
                },
                onCerrarDialogoRedactar = {
                    uiState = uiState.copy(mostrarDialogoRedactar = false)
                },
                onPublicar = { nuevoTexto ->
                    if (nuevoTexto.isNotBlank()) {
                        val nueva = Publicacion(
                            autor = "Tú (Vecino)",
                            inicial = "T",
                            tiempoYUbica = "Ahora mismo · Comunidad",
                            contenido = nuevoTexto.trim(),
                            meGusta = 0,
                            comentarios = 0
                        )
                        uiState = uiState.copy(
                            listaPublicaciones = listOf(nueva) + uiState.listaPublicaciones,
                            mostrarDialogoRedactar = false
                        )
                    } else {
                        uiState = uiState.copy(mostrarDialogoRedactar = false)
                    }
                },
                onToggleMeGusta = { index ->
                    val listaActualizada = uiState.listaPublicaciones.mapIndexed { i, pub ->
                        if (i == index) {
                            pub.copy(meGusta = pub.meGusta + 1)
                        } else pub
                    }
                    uiState = uiState.copy(listaPublicaciones = listaActualizada)
                },
                onVerEnCalendario = {
                    uiState = uiState.copy(seccionActual = SeccionRadioPatio.EVENTOS)
                }
            )
        }
    }
}

/**
 * Content (Stateless): Representa exclusivamente la interfaz de usuario a partir del estado.
 * No mantiene variables con remember para el estado de datos. Recibe valores y emite eventos (lambdas).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginaPrincipalContent(
    uiState: PaginaPrincipalUiState,
    onSeleccionarSeccion: (SeccionRadioPatio) -> Unit,
    onAbrirDialogoRedactar: () -> Unit,
    onCerrarDialogoRedactar: () -> Unit,
    onPublicar: (String) -> Unit,
    onToggleMeGusta: (Int) -> Unit,
    onVerEnCalendario: () -> Unit
) {
    Scaffold(
        topBar = { BarraSuperior() },
        bottomBar = {
            BarraNavegacionInferior(
                seccionActual = uiState.seccionActual,
                onSeleccionarSeccion = onSeleccionarSeccion,
                onCrearPulsado = onAbrirDialogoRedactar
            )
        }
    ) { rellenoInterior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(rellenoInterior),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Campo para redactar publicación
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        .clickable { onAbrirDialogoRedactar() }
                ) {
                    CampoCrearPublicacion()
                }
            }

            // 2. Encabezado fijo (Sticky Header) con enlace al Calendario de Eventos
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    NoticiaFijada(onVerEnCalendario = onVerEnCalendario)
                }
            }

            // 3. Publicaciones
            if (uiState.listaPublicaciones.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay publicaciones comunitarias todavía. ¡Sé el primero en compartir!",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(uiState.listaPublicaciones) { index, publicacion ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        TarjetaPublicacion(
                            publicacion = publicacion,
                            onMeGustaPulsado = { onToggleMeGusta(index) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.mostrarDialogoRedactar) {
        DialogoCrearPublicacion(
            onDismiss = onCerrarDialogoRedactar,
            onPublicar = onPublicar
        )
    }
}

/**
 * Barra superior fija con logotipo, título y botones de acción.
 */
@Composable
private fun BarraSuperior() {
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

            Text(
                text = "RadioPatio",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF133E54),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = Color(0xFF133E54))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Avisos", tint = Color(0xFF133E54))
            }
        }
    }
}

/**
 * Barra de navegación inferior dinámica y sensible a la pestaña seleccionada.
 */
@Composable
fun BarraNavegacionInferior(
    seccionActual: SeccionRadioPatio = SeccionRadioPatio.INICIO,
    onSeleccionarSeccion: (SeccionRadioPatio) -> Unit = {},
    onCrearPulsado: () -> Unit = {}
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAEFF2))
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inicio
            val esInicio = seccionActual == SeccionRadioPatio.INICIO
            IconButton(onClick = { onSeleccionarSeccion(SeccionRadioPatio.INICIO) }) {
                Icon(
                    imageVector = if (esInicio) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Inicio",
                    tint = if (esInicio) Color(0xFF389F82) else Color(0xFF133E54),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Vecinos
            val esVecinos = seccionActual == SeccionRadioPatio.VECINOS
            IconButton(onClick = { onSeleccionarSeccion(SeccionRadioPatio.VECINOS) }) {
                Icon(
                    imageVector = if (esVecinos) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Vecinos",
                    tint = if (esVecinos) Color(0xFF389F82) else Color(0xFF133E54),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Publicar (+)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF389F82))
                    .clickable { onCrearPulsado() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Publicar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Mercadillo
            val esMercadillo = seccionActual == SeccionRadioPatio.MERCADILLO
            IconButton(onClick = { onSeleccionarSeccion(SeccionRadioPatio.MERCADILLO) }) {
                Icon(
                    imageVector = if (esMercadillo) Icons.Filled.ShoppingBasket else Icons.Outlined.ShoppingBasket,
                    contentDescription = "Mercadillo",
                    tint = if (esMercadillo) Color(0xFF389F82) else Color(0xFF133E54),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Eventos (Acceso al Calendario de Eventos)
            val esEventos = seccionActual == SeccionRadioPatio.EVENTOS
            IconButton(onClick = { onSeleccionarSeccion(SeccionRadioPatio.EVENTOS) }) {
                Icon(
                    imageVector = if (esEventos) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                    contentDescription = "Eventos",
                    tint = if (esEventos) Color(0xFF389F82) else Color(0xFF133E54),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Noticia fijada destacada en verde con acceso directo al Calendario de Eventos.
 */
@Composable
private fun NoticiaFijada(
    onVerEnCalendario: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEAF5EC))
            .clickable { onVerEnCalendario() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📌 NOTICIA FIJADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F8A5F)
            )
            Text(
                text = "Ver en calendario 📅",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F8A5F)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fiestas del Barrio 2026 🎉",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = "Este sábado celebramos las fiestas en el parque del Oeste. Música, comida y actividades. ¡Toca para ver el programa completo!",
            fontSize = 12.sp,
            color = Color(0xFF455A64)
        )
    }
}

/**
 * Campo de texto para empezar a redactar una publicación.
 */
@Composable
private fun CampoCrearPublicacion() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "¿Qué quieres compartir hoy con el barrio?",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F8A5F)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Diálogo modal para redactar una publicación en el muro del barrio.
 */
@Composable
private fun DialogoCrearPublicacion(
    onDismiss: () -> Unit,
    onPublicar: (String) -> Unit
) {
    var texto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Compartir con el vecindario",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF133E54)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    placeholder = { Text("Escribe aquí tu aviso, pregunta o sugerencia...") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublicar(texto) },
                enabled = texto.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82))
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

/**
 * Pantalla informativa para secciones en construcción.
 */
@Composable
private fun PantallaSeccionEnDesarrollo(
    seccion: SeccionRadioPatio,
    onVolverAInicio: () -> Unit,
    onIrAEventos: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    Scaffold(
        topBar = { BarraSuperior() },
        bottomBar = barraInferior
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(relleno)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFFEAEFF2),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Construction,
                        contentDescription = null,
                        tint = Color(0xFF133E54),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (seccion == SeccionRadioPatio.VECINOS) "Comunidad y Directorio de Vecinos" else "Mercadillo y Prestes Vecinales",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF133E54),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Este módulo estará disponible próximamente en las siguientes fases del proyecto.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onVolverAInicio) {
                    Text("Volver a Inicio", color = Color(0xFF133E54))
                }
                Button(
                    onClick = onIrAEventos,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82))
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Calendario")
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar una publicación (Stateless).
 */
@Composable
fun TarjetaPublicacion(
    publicacion: Publicacion,
    onMeGustaPulsado: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFECEFF1)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF133E54)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(publicacion.inicial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = publicacion.autor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = publicacion.tiempoYUbica, fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = publicacion.contenido,
                fontSize = 13.sp,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onMeGustaPulsado() }
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Me gusta",
                        tint = if (publicacion.meGusta > 0) Color(0xFFE53935) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${publicacion.meGusta}", fontSize = 12.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentarios", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${publicacion.comentarios}", fontSize = 12.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Previews aplicadas sobre la función Content con diferentes estados (según Práctica 02)
// -----------------------------------------------------------------------------------------

@Preview(name = "Feed Principal con Publicaciones", showBackground = true, showSystemUi = true)
@Composable
fun PaginaPrincipalContentPreview() {
    RadioPatioTheme {
        PaginaPrincipalContent(
            uiState = PaginaPrincipalUiState(),
            onSeleccionarSeccion = {},
            onAbrirDialogoRedactar = {},
            onCerrarDialogoRedactar = {},
            onPublicar = {},
            onToggleMeGusta = {},
            onVerEnCalendario = {}
        )
    }
}

@Preview(name = "Feed Vacío", showBackground = true)
@Composable
fun PaginaPrincipalVaciaPreview() {
    RadioPatioTheme {
        PaginaPrincipalContent(
            uiState = PaginaPrincipalUiState(listaPublicaciones = emptyList()),
            onSeleccionarSeccion = {},
            onAbrirDialogoRedactar = {},
            onCerrarDialogoRedactar = {},
            onPublicar = {},
            onToggleMeGusta = {},
            onVerEnCalendario = {}
        )
    }
}