package com.example.radiopatio.informacionUsuario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilUsuarioScreen(
    usuario: Usuario = Usuario(
        nombre = "Lucía Fernández",
        inicial = "L",
        vivienda = "Portal 3, 2ºB",
        email = "lucia.fernandez@email.com",
        telefono = "+34 612 345 678",
        totalPublicaciones = 12
    ),
    onVolver: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeSecundario = Color(0xFF0F8A5F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil del Vecino",
                        fontWeight = FontWeight.Bold,
                        color = azulPrincipal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = azulPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera: Avatar e Identificación
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(azulPrincipal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = usuario.inicial,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = usuario.nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    Surface(
                        modifier = Modifier.padding(top = 6.dp),
                        color = Color(0xFFEAF5EC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = usuario.rol,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = verdeSecundario
                        )
                    }
                }
            }

            // Datos de Contacto y Vivienda
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Información Comunitaria",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    FilaInfo(
                        icono = Icons.Outlined.Home,
                        titulo = "Ubicación",
                        valor = usuario.vivienda
                    )
                    HorizontalDivider(color = Color(0xFFECEFF1))

                    FilaInfo(
                        icono = Icons.Outlined.Email,
                        titulo = "Correo Electrónico",
                        valor = usuario.email
                    )
                    HorizontalDivider(color = Color(0xFFECEFF1))

                    FilaInfo(
                        icono = Icons.Outlined.Phone,
                        titulo = "Teléfono",
                        valor = usuario.telefono
                    )
                }
            }

            // Métrica rápida de actividad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Actividad en RadioPatio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Miembro desde ${usuario.fechaAlta}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        color = Color(0xFFF0F4F8),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${usuario.totalPublicaciones} posts",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = azulPrincipal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaInfo(
    icono: ImageVector,
    titulo: String,
    valor: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = Color(0xFF133E54),
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = titulo,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Text(
                text = valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}