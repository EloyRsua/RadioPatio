package com.example.radiopatio.mensajeria

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AzulPrincipal = Color(0xFF133E54)
private val VerdeBoton = Color(0xFF0F8A5F)
private val TextoPrincipal = Color(0xFF0F172A)
private val TextoSecundario = Color(0xFF64748B)
private val FondoApp = Color(0xFFF8FAFC)
private val BurbujaOtro = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onVolver: () -> Unit
) {
    val uiState = viewModel.uiState
    var textoInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajes.lastIndex)
        }
    }

    uiState.errorMensaje?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.limpiarError() },
            title = { Text("Aviso", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = { Text(error, color = TextoPrincipal) },
            confirmButton = {
                TextButton(onClick = { viewModel.limpiarError() }) {
                    Text("Aceptar", color = VerdeBoton, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    val conv = uiState.conversacion
    val nombreInterlocutor = conv?.obtenerNombreInterlocutor(uiState.usuarioActualUid) ?: "Vecino"

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = nombreInterlocutor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = AzulPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Chat directo del artículo",
                                fontSize = 12.sp,
                                color = TextoSecundario
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = AzulPrincipal
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                if (conv != null && conv.productoTitulo.isNotBlank()) {
                    Surface(
                        color = FondoApp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (conv.productoImagenUrl.isNotBlank()) {
                                AsyncImage(
                                    model = conv.productoImagenUrl,
                                    contentDescription = conv.productoTitulo,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingBag,
                                        contentDescription = null,
                                        tint = TextoSecundario,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.productoTitulo,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextoPrincipal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (conv.productoPrecio.isNotBlank()) {
                                    Text(
                                        text = conv.productoPrecio,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = VerdeBoton
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoInput,
                        onValueChange = { textoInput = it },
                        placeholder = { Text("Escribe un mensaje...", color = TextoSecundario) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val enviar = textoInput
                            textoInput = ""
                            viewModel.enviarMensaje(enviar)
                        },
                        enabled = textoInput.isNotBlank() && !uiState.enviando,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (textoInput.isNotBlank() && !uiState.enviando) VerdeBoton
                                else Color(0xFFCBD5E1)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = FondoApp
    ) { paddingValues ->
        if (uiState.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VerdeBoton)
            }
        } else if (uiState.mensajes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no hay mensajes en este chat. Escribe para coordinar la entrega o resolver dudas.",
                    color = TextoSecundario,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.mensajes, key = { it.id.ifBlank { "${it.remitenteUid}_${it.timestamp}" } }) { msg ->
                    val esMio = msg.remitenteUid == uiState.usuarioActualUid
                    BurbujaMensaje(mensaje = msg, esMio = esMio)
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(
    mensaje: Mensaje,
    esMio: Boolean
) {
    val horaFormateada = remember(mensaje.timestamp) {
        if (mensaje.timestamp <= 0L) ""
        else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(mensaje.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (esMio) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (esMio) AzulPrincipal else BurbujaOtro,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (esMio) 16.dp else 4.dp,
                bottomEnd = if (esMio) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!esMio && mensaje.remitenteNombre.isNotBlank()) {
                    Text(
                        text = mensaje.remitenteNombre,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = mensaje.texto,
                    fontSize = 14.sp,
                    color = if (esMio) Color.White else TextoPrincipal
                )
                if (horaFormateada.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = horaFormateada,
                        fontSize = 10.sp,
                        color = if (esMio) Color(0xFFCBD5E1) else TextoSecundario,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}