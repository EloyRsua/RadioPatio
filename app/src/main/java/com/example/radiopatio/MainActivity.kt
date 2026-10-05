package com.example.radiopatio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioPatioTheme {
                RadioPatioSimple()
            }
        }
    }
}

@Composable
fun RadioPatioSimple() {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Cabecera superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                // Pestañas superiores simplificadas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Feed", fontWeight = FontWeight.Bold, color = Color(0xFF133E54), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(30.dp)
                                .background(Color(0xFF133E54))
                        )
                    }
                    Text("Comunidad", color = Color.Gray, fontSize = 13.sp)
                    Text("Mercado", color = Color.Gray, fontSize = 13.sp)
                    Text("Mensajes", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        // Contenido estático sin scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Círculos de historias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Círculo 1: Tu historia (con botón +)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(54.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                                .border(1.5.dp, Color(0xFFBDBDBD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tú", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        }
                        // Botón "+" superpuesto
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF133E54)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tu historia", fontSize = 11.sp, color = Color.DarkGray)
                }

                // Círculo 2: Marcos (borde verde)
                StoryCircle(name = "Marcos", initial = "M", borderColor = Color(0xFF2E7D32))

                // Círculo 3: Lucía (borde rojo)
                StoryCircle(name = "Lucía", initial = "L", borderColor = Color(0xFFD32F2F))

                // Círculo 4: Vecinos (borde azul)
                StoryCircle(name = "Vecinos", initial = "V", borderColor = Color(0xFF1976D2))

                // Círculo 5: Eventos
                StoryCircle(name = "Eventos", initial = "📅", borderColor = Color(0xFFB0BEC5))
            }

            // 2. Noticia Destacada / Fijada
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

            // 3. Campo para crear publicación
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

            // 4. Tarjeta de Post
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
                        Text("L", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Lucia Fernández", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Hace 2 h · Calle Mayor", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¡Buenas vecin@s! ☀️ Este sábado organizamos una quedada en el parque para tomar algo. ¡Os esperamos!",
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
                        Text(text = "24", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentarios", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "6", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Botones inferiores de filtro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Todas", "Noticias", "Preguntas", "General").forEachIndexed { index, tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (index == 0) Color(0xFF133E54) else Color(0xFFEAEFF2))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == 0) Color.White else Color(0xFF455A64)
                        )
                    }
                }
            }
        }
    }
}

/** Componente auxiliar simple para cada círculo de historia **/
@Composable
fun StoryCircle(name: String, initial: String, borderColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .border(2.dp, borderColor, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE7F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, fontSize = 11.sp, color = Color.DarkGray)
    }
}

// Preview para visualizar en Android Studio
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadioPatioSimplePreview() {
    RadioPatioTheme {
        RadioPatioSimple()
    }
}