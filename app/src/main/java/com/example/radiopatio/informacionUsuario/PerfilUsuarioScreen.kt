package com.example.radiopatio.informacionUsuario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun PerfilUsuarioScreen(
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val textoSecundario = Color(0xFF64748B)

    val userRepository = remember { UserRepository() }
    val comunidadRepository = remember { ComunidadRepository() }

    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var nombreComunidad by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        userRepository.observarUsuarioActual()
            .flatMapLatest { user ->
                val comId = user?.comunidadId.orEmpty()
                if (comId.isBlank()) {
                    flowOf(Pair(user, ""))
                } else {
                    comunidadRepository.observarComunidadPorId(comId).map { comunidad ->
                        val nombre = comunidad?.nombre?.takeIf { it.isNotBlank() }
                            ?: user?.comunidadNombre
                            ?: comId
                        Pair(user, nombre)
                    }
                }
            }
            .collect { (user, comNombre) ->
                usuario = user
                nombreComunidad = comNombre
                cargando = false
            }
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
                    vivienda = "Sin vivienda asignada",
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
                        if (usuarioActual.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = usuarioActual.photoUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                                Text(
                                    text = if (usuarioActual.esAdministradorFincas) "Comunidad activa" else "Comunidad",
                                    fontSize = 13.sp,
                                    color = textoSecundario
                                )
                                Text(
                                    text = nombreComunidad.ifBlank { "Sin comunidad" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = azulPrincipal
                                )
                            }

                            if (usuarioActual.esAdministradorFincas) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Comunidades gestionadas", fontSize = 13.sp, color = textoSecundario)
                                    Text(
                                        text = "${usuarioActual.comunidadesIds.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = verdeBoton
                                    )
                                }
                            }

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

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onCerrarSesion,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cerrar sesión",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
