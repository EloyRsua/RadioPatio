package com.example.radiopatio.informacionUsuario

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.ui.SelectorAvatarDiceBear
import com.example.radiopatio.ui.theme.AzulPrincipal
import com.example.radiopatio.ui.theme.RadioPatioTheme
import com.example.radiopatio.ui.theme.TextoPrincipal
import com.example.radiopatio.ui.theme.TextoSecundario
import com.example.radiopatio.ui.theme.VerdePrincipal
import com.example.radiopatio.usuarios.UserRepository

@Composable
fun PerfilUsuarioScreen(
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    onIrAMisProductos: () -> Unit = {}
) {
    val context = LocalContext.current
    val comunidadRepository = remember { ComunidadRepository() }
    val userRepository = remember { UserRepository(comunidadRepository) }

    val viewModel: PerfilUsuarioViewModel = viewModel(
        factory = PerfilUsuarioViewModelFactory(
            userRepository = userRepository,
            comunidadRepository = comunidadRepository
        )
    )

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.limpiarError()
        }
    }

    PerfilUsuarioContent(
        usuario = uiState.usuario,
        nombreComunidad = uiState.nombreComunidad,
        cargando = uiState.cargando,
        guardandoFoto = uiState.guardandoFoto,
        onVolver = onVolver,
        onCerrarSesion = {
            viewModel.cerrarSesion(onCerrarSesion)
        },
        onIrAMisProductos = onIrAMisProductos,
        onGuardarFotoPerfil = { nuevaUrl ->
            viewModel.actualizarFotoPerfil(nuevaUrl) {
                Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilUsuarioContent(
    usuario: Usuario?,
    nombreComunidad: String,
    cargando: Boolean,
    guardandoFoto: Boolean = false,
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    onIrAMisProductos: () -> Unit = {},
    onGuardarFotoPerfil: (String) -> Unit = {}
) {
    val usuarioActual = usuario ?: Usuario(
        nombre = "Vecino Registrado",
        inicial = "V",
        vivienda = "Sin vivienda asignada",
        email = "vecino@radiopatio.com",
        telefono = ""
    )

    var mostrarDialogoFoto by remember { mutableStateOf(false) }
    var fotoEditada by remember(usuarioActual.photoUrl) { mutableStateOf(usuarioActual.photoUrl) }

    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoEditada = uri.toString()
        }
    }

    if (mostrarDialogoFoto) {
        AlertDialog(
            onDismissRequest = { if (!guardandoFoto) mostrarDialogoFoto = false },
            title = {
                Text(
                    text = "Cambiar foto o avatar",
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
            },
            text = {
                SelectorAvatarDiceBear(
                    semillaBase = usuarioActual.uid.ifBlank { usuarioActual.nombre.ifBlank { usuarioActual.email } },
                    urlActual = fotoEditada,
                    onSeleccionarUrl = { fotoEditada = it },
                    onAbrirGaleria = { galeriaLauncher.launch("image/*") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fotoEditada.isNotBlank()) {
                            onGuardarFotoPerfil(fotoEditada)
                            mostrarDialogoFoto = false
                        }
                    },
                    enabled = !guardandoFoto && fotoEditada.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePrincipal)
                ) {
                    Text(if (guardandoFoto) "Guardando..." else "Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoFoto = false },
                    enabled = !guardandoFoto
                ) {
                    Text("Cancelar", color = TextoSecundario)
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
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
                    color = VerdePrincipal
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(AzulPrincipal),
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

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            fotoEditada = usuarioActual.photoUrl
                            mostrarDialogoFoto = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = VerdePrincipal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cambiar foto o avatar",
                            color = VerdePrincipal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = usuarioActual.nombre,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )
                    Text(
                        text = usuarioActual.email,
                        fontSize = 14.sp,
                        color = TextoSecundario
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
                                color = VerdePrincipal
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (usuarioActual.esAdministradorFincas) "Comunidad activa" else "Comunidad",
                                    fontSize = 13.sp,
                                    color = TextoSecundario
                                )
                                Text(
                                    text = nombreComunidad.ifBlank { "Sin comunidad" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulPrincipal
                                )
                            }

                            if (usuarioActual.esAdministradorFincas) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Comunidades gestionadas", fontSize = 13.sp, color = TextoSecundario)
                                    Text(
                                        text = "${usuarioActual.comunidadesIds.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VerdePrincipal
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Piso / Vivienda", fontSize = 13.sp, color = TextoSecundario)
                                Text(
                                    text = usuarioActual.vivienda,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulPrincipal
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rol", fontSize = 13.sp, color = TextoSecundario)
                                Text(
                                    text = usuarioActual.rol,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulPrincipal
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Miembro desde", fontSize = 13.sp, color = TextoSecundario)
                                Text(
                                    text = usuarioActual.fechaAlta,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulPrincipal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón: "Mis Artículos"
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
                                    tint = VerdePrincipal
                                )
                                Column {
                                    Text(
                                        text = "Mis Artículos",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipal
                                    )
                                    Text(
                                        text = "Gestiona tus ventas y préstamos",
                                        fontSize = 12.sp,
                                        color = TextoSecundario
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Ir a mis artículos",
                                tint = TextoSecundario,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón Cerrar Sesión
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

@Preview(showBackground = true)
@Composable
fun PerfilUsuarioContentPreview() {
    RadioPatioTheme {
        PerfilUsuarioContent(
            usuario = Usuario(
                nombre = "Laura Gómez",
                inicial = "L",
                vivienda = "Portal 2, 3ºB",
                email = "laura.gomez@email.com",
                telefono = "600123456"
            ),
            nombreComunidad = "Residencial Montecerrao",
            cargando = false
        )
    }
}