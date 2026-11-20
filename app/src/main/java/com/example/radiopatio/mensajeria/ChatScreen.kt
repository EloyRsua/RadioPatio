package com.example.radiopatio.mensajeria

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.AzulPrincipal
import com.example.radiopatio.ui.theme.TextoSecundario
import com.example.radiopatio.ui.theme.VerdePrincipal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val conversacion = uiState.conversacionActiva
    val miUid = uiState.usuarioActual?.uid.orEmpty()
    val interlocutor = conversacion?.obtenerNombreInterlocutor(miUid) ?: "Chat"

    var textoMensaje by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    LaunchedEffect(uiState.mensajesActivos.size) {
        if (uiState.mensajesActivos.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajesActivos.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = interlocutor,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrincipal,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        conversacion?.let {
                            val subtitulo = if (it.productoPrecio.isNotBlank()) {
                                "${it.productoTitulo} · ${it.productoPrecio}"
                            } else {
                                it.productoTitulo
                            }
                            Text(
                                text = subtitulo,
                                fontSize = 11.sp,
                                color = VerdePrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        placeholder = { Text("Escribe un mensaje...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textoMensaje.isNotBlank()) {
                                viewModel.enviarMensaje(textoMensaje)
                                textoMensaje = ""
                            }
                        },
                        enabled = textoMensaje.isNotBlank() && conversacion != null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (textoMensaje.isNotBlank()) VerdePrincipal else Color(0xFFCBD5E1),
                                shape = CircleShape
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F5F9))
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = VerdePrincipal
                )
            } else if (uiState.mensajesActivos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Envía el primer mensaje para coordinar la compra o el préstamo.",
                        fontSize = 13.sp,
                        color = TextoSecundario
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = uiState.mensajesActivos,
                        key = { it.id.ifBlank { "${it.timestamp}_${it.remitenteUid}_${it.texto.hashCode()}" } }
                    ) { mensaje ->
                        val esMio = mensaje.remitenteUid == miUid

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (esMio) VerdePrincipal else Color.White,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (esMio) 16.dp else 4.dp,
                                    bottomEnd = if (esMio) 4.dp else 16.dp
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text(
                                        text = mensaje.texto,
                                        color = if (esMio) Color.White else AzulPrincipal,
                                        fontSize = 14.sp
                                    )
                                    if (mensaje.timestamp > 0L) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = formatearHoraMensaje(mensaje.timestamp),
                                            color = if (esMio) Color.White.copy(alpha = 0.8f) else TextoSecundario,
                                            fontSize = 10.sp,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatearHoraMensaje(timestamp: Long): String {
    val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formato.format(Date(timestamp))
}