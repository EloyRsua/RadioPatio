package com.example.radiopatio.informacionUsuario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PerfilUsuarioScreen(
    onIrAMisProductos: () -> Unit = {}
) {
    val userRepository = remember { UserRepository() }
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val result = userRepository.getCurrentUserData()
        result.onSuccess { usuarioCargado ->
            if (usuarioCargado != null) {
                usuario = usuarioCargado
            } else if (currentUser != null) {
                val emailStr = currentUser.email ?: "usuario@radiopatio.com"
                val nombreStr = currentUser.displayName.takeIf { !it.isNullOrBlank() }
                    ?: emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }

                usuario = Usuario(
                    nombre = nombreStr,
                    inicial = nombreStr.firstOrNull()?.uppercase() ?: "V",
                    vivienda = "Portal 1, 2ºA",
                    email = emailStr,
                    telefono = ""
                )
            }
        }.onFailure {
            if (currentUser != null) {
                val emailStr = currentUser.email ?: "usuario@radiopatio.com"
                val nombreStr = emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }
                usuario = Usuario(
                    nombre = nombreStr,
                    inicial = nombreStr.firstOrNull()?.uppercase() ?: "V",
                    vivienda = "Portal 1, 2ºA",
                    email = emailStr,
                    telefono = ""
                )
            }
        }
        cargando = false
    }

    PerfilUsuarioContent(
        usuario = usuario,
        cargando = cargando,
        onIrAMisProductos = onIrAMisProductos
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilUsuarioContent(
    usuario: Usuario?,
    cargando: Boolean,
    onIrAMisProductos: () -> Unit
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val textoSecundario = Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = azulPrincipal) },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.TopCenter
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = verdeBoton
                )
            } else {
                val usuarioActual = usuario ?: Usuario(
                    nombre = "Vecino Registrado",
                    inicial = "V",
                    vivienda = "Portal 1, 2ºA",
                    email = "vecino@radiopatio.com",
                    telefono = ""
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar con inicial
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(azulPrincipal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = usuarioActual.inicial,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = usuarioActual.nombre,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = usuarioActual.email,
                        fontSize = 14.sp,
                        color = textoSecundario
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tarjeta de información de la comunidad
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
                                text = "Información de la Comunidad",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = verdeBoton
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Piso / Vivienda", fontSize = 13.sp, color = textoSecundario)
                                Text(usuarioActual.vivienda, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = azulPrincipal)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rol", fontSize = 13.sp, color = textoSecundario)
                                Text(usuarioActual.rol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = azulPrincipal)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Miembro desde", fontSize = 13.sp, color = textoSecundario)
                                Text(usuarioActual.fechaAlta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = azulPrincipal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón estilo Vinted: "Mis Artículos"
                    Card(
                        onClick = onIrAMisProductos,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = verdeBoton
                                )
                                Column {
                                    Text(
                                        text = "Mis Artículos",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Gestiona tus ventas y préstamos",
                                        fontSize = 12.sp,
                                        color = textoSecundario
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Ir a mis artículos",
                                tint = textoSecundario,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PerfilUsuarioContentPreview() {
    PerfilUsuarioContent(
        usuario = Usuario(
            nombre = "Carlos Martínez",
            inicial = "C",
            vivienda = "Portal 2, 3ºB",
            email = "carlos@radiopatio.com",
            telefono = "612345678"
        ),
        cargando = false,
        onIrAMisProductos = {}
    )
}