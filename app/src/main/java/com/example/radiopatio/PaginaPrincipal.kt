package com.example.radiopatio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.publicacion.Publicacion
import com.example.radiopatio.publicacion.TarjetaPublicacion
import com.example.radiopatio.ui.theme.RadioPatioTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginaPrincipal(
    modifier: Modifier = Modifier,
    onIrACrearPublicacion: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication

    val viewModel: RadioPatioViewModel = viewModel(
        factory = RadioPatioViewModelFactory(app.database.publicacionDao())
    )

    // Carga inicial desde Room
    LaunchedEffect(Unit) {
        viewModel.insertarDatosIniciales()
    }

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
                    NoticiaFijada()
                }
            }

            items(viewModel.publicaciones) { entidad ->
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
                        onEliminarClick = {
                            viewModel.eliminarPublicacion(entidad)
                        }
                    )
                }
            }
        }
    }
}

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
            Icon(
                Icons.Default.Add,
                contentDescription = "Crear publicación",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaginaPrincipalPreview() {
    RadioPatioTheme {
        PaginaPrincipal()
    }
}