package com.example.radiopatio.notificacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificacionesScreen(
    onVolver: () -> Unit,
    onProductoClick: (String) -> Unit,
    viewModel: NotificacionesViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    NotificacionesContent(
        uiState = uiState,
        onVolver = onVolver,
        onNotificacionClick = { notificacion ->
            viewModel.marcarComoLeida(notificacion.id)
            if (notificacion.productoId.isNotBlank()) {
                onProductoClick(notificacion.productoId)
            }
        },
        onMarcarTodasLeidas = {
            viewModel.marcarTodasComoLeidas()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesContent(
    uiState: NotificacionesUiState,
    onVolver: () -> Unit,
    onNotificacionClick: (Notificacion) -> Unit,
    onMarcarTodasLeidas: () -> Unit
) {
    val colorAzul = Color(0xFF133E54)
    val colorVerde = Color(0xFF0F8A5F)
    val hayNoLeidas = uiState.notificaciones.any { !it.leida }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notificaciones",
                        fontWeight = FontWeight.Bold,
                        color = colorAzul
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = colorAzul
                        )
                    }
                },
                actions = {
                    if (hayNoLeidas) {
                        IconButton(onClick = onMarcarTodasLeidas) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = "Marcar todas como leídas",
                                tint = colorVerde
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colorVerde
                    )
                }
                uiState.notificaciones.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes notificaciones",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aquí recibirás avisos sobre tus compras, ventas y préstamos en RadioPatio.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.notificaciones, key = { it.id }) { notif ->
                            TarjetaNotificacionItem(
                                notificacion = notif,
                                onClick = { onNotificacionClick(notif) }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TarjetaNotificacionItem(
    notificacion: Notificacion,
    onClick: () -> Unit
) {
    val colorVerde = Color(0xFF0F8A5F)
    val fondo = if (!notificacion.leida) Color(0xFFEAF5EC) else Color.White
    val borde = if (!notificacion.leida) Color(0xFF0F8A5F).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        border = BorderStroke(1.dp, borde),
        shadowElevation = if (!notificacion.leida) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (!notificacion.leida) colorVerde else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = if (!notificacion.leida) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notificacion.titulo,
                        fontSize = 14.sp,
                        fontWeight = if (!notificacion.leida) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    if (!notificacion.leida) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorVerde)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notificacion.mensaje,
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatearFecha(notificacion.fecha),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
private fun formatearFecha(milis: Long): String {
    if (milis <= 0L) return ""
    val diff = System.currentTimeMillis() - milis
    val minutos = diff / (60 * 1000)
    val horas = diff / (60 * 60 * 1000)
    val dias = diff / (24 * 60 * 60 * 1000)
    return when {
        minutos < 1 -> "Ahora mismo"
        minutos < 60 -> "Hace $minutos min"
        horas < 24 -> "Hace $horas h"
        dias == 1L -> "Ayer"
        dias < 7 -> "Hace $dias días"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(milis))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificacionesContentPreview() {
    val muestra = listOf(
        Notificacion(
            id = "1",
            titulo = "¡Artículo vendido!",
            mensaje = "Un vecino ha comprado \"Taladro Bosch\". Tienes 7 días para realizar la entrega.",
            fecha = System.currentTimeMillis() - 15 * 60 * 1000,
            leida = false
        ),
        Notificacion(
            id = "2",
            titulo = "Producto entregado por el vendedor",
            mensaje = "El vendedor ha marcado \"Juego de mesa Catan\" como entregado.",
            fecha = System.currentTimeMillis() - 3 * 3600 * 1000,
            leida = true
        )
    )
    NotificacionesContent(
        uiState = NotificacionesUiState(cargando = false, notificaciones = muestra),
        onVolver = {},
        onNotificacionClick = {},
        onMarcarTodasLeidas = {}
    )
}