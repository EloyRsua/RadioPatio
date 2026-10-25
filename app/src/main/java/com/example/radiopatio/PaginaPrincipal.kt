package com.example.radiopatio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme
import androidx.compose.foundation.lazy.items

/**
 * Pantalla principal con Sticky List: la noticia se queda anclada arriba al hacer scroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginaPrincipal(
    modifier: Modifier = Modifier
) {
    // Lista de publicaciones (fácil de ampliar o recibir desde base de datos)
    val listaPublicaciones = listOf(
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
    )

    Scaffold(
        modifier = modifier,
        topBar = { BarraSuperior() },
    ) { rellenoInterior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(rellenoInterior),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Campo para redactar publicación (hace scroll normal)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                ) {
                    CampoCrearPublicacion()
                }
            }

            // 2. Encabezado fijo (Sticky Header)
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB)) // Fondo opaco para que no se transparente el contenido
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    NoticiaFijada()
                }
            }

            // 3. Todas las publicaciones
            items(listaPublicaciones) { publicacion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TarjetaPublicacion(publicacion = publicacion)
                }
            }
        }
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

///**
// * Barra de navegación inferior fija.
// */
//@Composable
//private fun BarraNavegacionInferior() {
//    Surface(shadowElevation = 8.dp) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Color(0xFFEAEFF2))
//                .navigationBarsPadding()
//                .padding(vertical = 6.dp, horizontal = 24.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = {}) {
//                Icon(Icons.Outlined.Home, contentDescription = "Inicio", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
//            }
//            IconButton(onClick = {}) {
//                Icon(Icons.Outlined.Person, contentDescription = "Vecinos", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
//            }
//            Box(
//                modifier = Modifier
//                    .size(52.dp)
//                    .clip(CircleShape)
//                    .background(Color(0xFF389F82)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(Icons.Outlined.Add, contentDescription = "Publicar", tint = Color.White, modifier = Modifier.size(28.dp))
//            }
//            IconButton(onClick = {}) {
//                Icon(Icons.Outlined.ShoppingBasket, contentDescription = "Mercadillo", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
//            }
//            IconButton(onClick = {}) {
//                Icon(Icons.Outlined.DateRange, contentDescription = "Eventos", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
//            }
//        }
//    }
//}

/**
 * Noticia fijada destacada en verde.
 */
@Composable
private fun NoticiaFijada() {
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
                text = "📌 NOTICIA FIJADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F8A5F)
            )
            Text(text = "Hasta el 12 de octubre.", fontSize = 11.sp, color = Color(0xFF0F8A5F))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fiestas del Barrio 2026 🎉",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = "Este sábado celebramos las fiestas en el parque del Oeste. Música, comida y actividades.",
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
            text = "¿Qué quieres compartir hoy?",
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
 * Tarjeta individual para mostrar una publicación.
 */
@Composable
fun TarjetaPublicacion(publicacion: Publicacion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = "Me gusta", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
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

/**
 * Previsualización para Android Studio.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaginaPrincipalPreview() {
    RadioPatioTheme {
        PaginaPrincipal()
    }
}