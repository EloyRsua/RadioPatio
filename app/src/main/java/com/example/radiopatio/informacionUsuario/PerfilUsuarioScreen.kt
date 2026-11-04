package com.example.radiopatio.informacionUsuario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilUsuarioScreen(
    onVolver: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val textoSecundario = Color(0xFF64748B)

    val userRepository = remember { UserRepository() }
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Intentar obtener desde Firestore
        val result = userRepository.getCurrentUserData()
        result.onSuccess { usuarioCargado ->
            if (usuarioCargado != null) {
                usuario = usuarioCargado
            } else if (currentUser != null) {
                // Si Firestore no tiene el documento, usaremos el email/nombre de la cuenta iniciada
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = azulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = azulPrincipal)
                    }
                }
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
                }
            }
        }
    }
}