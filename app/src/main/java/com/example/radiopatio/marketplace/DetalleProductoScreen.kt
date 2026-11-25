package com.example.radiopatio.marketplace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder

// Colores de RadioPatio
private val AzulPrincipal = Color(0xFF133E54)
private val VerdeBoton = Color(0xFF0F8A5F)
private val TextoPrincipal = Color(0xFF0F172A)
private val TextoSecundario = Color(0xFF64748B)
private val FondoApp = Color(0xFFF8FAFC)

/**
 * Screen con ViewModel y ciclo de vida
 */
@Composable
fun DetalleProductoScreen(
    firestoreId: String,
    onVolver: () -> Unit,
    viewModel: DetalleProductoViewModel = viewModel(
        factory = DetalleProductoViewModelFactory(firestoreId)
    )
) {
    val uiState = viewModel.uiState

    DetalleProductoContent(
        uiState = uiState,
        onVolver = onVolver,
        onComprarOPrestar = { viewModel.onComprarOPrestar() },
        onConfirmarEntregaVendedor = { viewModel.onConfirmarEntregaVendedor() },
        onConfirmarRecepcionComprador = { viewModel.onConfirmarRecepcionComprador() },
        onReportarConflicto = { motivo -> viewModel.onReportarConflicto(motivo) },
        onToggleFavorito = { viewModel.toggleFavorito() }
    )
}

/**
 * Content sin estado para UI pura y @Preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleProductoContent(
    uiState: DetalleProductoUiState,
    onVolver: () -> Unit,
    onComprarOPrestar: () -> Unit,
    onConfirmarEntregaVendedor: () -> Unit,
    onConfirmarRecepcionComprador: () -> Unit,
    onReportarConflicto: (String) -> Unit,
    onToggleFavorito: () -> Unit
) {
    var mostrarDialogoConfirmarCompra by remember { mutableStateOf(false) }
    var mostrarDialogoConflicto by remember { mutableStateOf(false) }
    var motivoTexto by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Artículo", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
                    }
                },
                actions = {
                    val producto = uiState.producto
                    
                    if (producto != null) {
                        val totalLikes = producto.likes.size

                        if (uiState.esPropietario) {
                            // Tu propio artículo: mostramos solo el contador informativo sin click
                            if (totalLikes > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$totalLikes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AzulPrincipal
                                    )
                                }
                            }
                        } else {
                            // Artículo de otro vecino: botón interactivo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (totalLikes > 0) {
                                    Text(
                                        text = "$totalLikes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (uiState.esFavorito) Color(0xFFE53935) else AzulPrincipal
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(onClick = onToggleFavorito) {
                                    Icon(
                                        imageVector = if (uiState.esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (uiState.esFavorito) "Quitar de favoritos" else "Añadir a favoritos",
                                        tint = if (uiState.esFavorito) Color(0xFFE53935) else AzulPrincipal
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
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
        } else if (uiState.producto == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No se ha encontrado el producto", color = TextoSecundario)
            }
        } else {
            val producto = uiState.producto

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(FondoApp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Fotos del producto
                if (producto.fotos.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color(0xFFF1F5F9)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(producto.fotos) { fotoUrl ->
                            AsyncImage(
                                model = fotoUrl,
                                contentDescription = producto.titulo,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // 2. Información del producto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Título y precio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = producto.titulo,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrincipal,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (producto.esParaComprar) producto.precio else "Préstamo (${producto.diasPrestamo} días)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerdeBoton
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Etiqueta de Estado
                    BadgeEstadoDetalle(producto.estado)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tarjeta de características
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilaCaracteristica("Categoría", producto.categoriaProducto.nombre)
                            if (producto.marca.isNotBlank()) FilaCaracteristica("Marca", producto.marca)
                            if (producto.color.isNotBlank()) FilaCaracteristica("Color", producto.color)
                            if (producto.estadoUso.isNotBlank()) FilaCaracteristica("Condición", producto.estadoUso)
                            if (producto.ubicacionYTiempo.isNotBlank()) FilaCaracteristica("Ubicación", producto.ubicacionYTiempo)
                            if (!producto.esParaComprar) {
                                FilaCaracteristica("Duración del préstamo", "${producto.diasPrestamo} días")
                                if (producto.fechaDevolucion.isNotBlank()) {
                                    FilaCaracteristica("Fecha de devolución", producto.fechaDevolucion)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción
                    Text("Descripción", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextoPrincipal)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = producto.descripcion.ifBlank { "El propietario no ha añadido descripción." },
                        fontSize = 14.sp,
                        color = TextoSecundario,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    when {
                        // CASO A: Es el comprador y está en proceso de entrega
                        uiState.esComprador && producto.estado == EstadoProducto.EN_ENTREGA -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFB45309))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Plazo de entrega: quedan ${uiState.diasRestantes} días",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Cuando el vecino te entregue el producto, confirma la recepción aquí. Si pasan los días y no se entrega, se autocompletará.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF78350F)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onConfirmarRecepcionComprador,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton)
                                    ) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("He recibido el producto correctamente")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { mostrarDialogoConflicto = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                    ) {
                                        Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tengo un problema / Reclamar")
                                    }
                                }
                            }
                        }

                        // CASO B: Es el vendedor y el producto fue comprado/pedido
                        uiState.esPropietario && producto.estado == EstadoProducto.EN_ENTREGA -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = AzulPrincipal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (producto.esParaComprar) "¡Artículo vendido!" else "¡Préstamo solicitado!",
                                            fontWeight = FontWeight.Bold,
                                            color = AzulPrincipal,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Ficha destacada con el vecino comprador y su puerta
                                    val compradorTexto = producto.compradorNombre.ifBlank { "Un vecino" }
                                    val pisoTexto = if (producto.compradorPiso.isNotBlank()) " · ${producto.compradorPiso}" else ""

                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Entregar a:",
                                                fontSize = 13.sp,
                                                color = TextoSecundario,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$compradorTexto$pisoTexto",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = AzulPrincipal
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Tienes ${uiState.diasRestantes} días para entregarlo en mano.",
                                        fontSize = 13.sp,
                                        color = TextoSecundario
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = onConfirmarEntregaVendedor,
                                        enabled = !producto.vendedorConfirmoEntrega,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                                    ) {
                                        Text(if (producto.vendedorConfirmoEntrega) "Ya has marcado como entregado (esperando confirmación)" else "Marcar como entregado al vecino")
                                    }
                                }
                            }
                        }

                        // CASO C: Producto Disponible y NO es el propietario
                        !uiState.esPropietario && producto.estado == EstadoProducto.DISPONIBLE -> {
                            Button(
                                onClick = { mostrarDialogoConfirmarCompra = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (producto.esParaComprar) "Comprar Artículo (${producto.precio})" else "Solicitar Préstamo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // CASO C.2: Producto en Entrega y NO eres ni el vendedor ni el comprador
                        !uiState.esPropietario && !uiState.esComprador && producto.estado == EstadoProducto.EN_ENTREGA -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFB45309))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Artículo Reservado", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        Text("Este artículo está en proceso de entrega con otro vecino.", fontSize = 12.sp, color = Color(0xFF78350F))
                                    }
                                }
                            }
                        }

                        // CASO D: Finalizado o en Conflicto
                        producto.estado == EstadoProducto.VENDIDO || producto.estado == EstadoProducto.DEVUELTO -> {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ Transacción finalizada con éxito",
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        producto.estado == EstadoProducto.EN_CONFLICTO -> {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("⚠ Artículo en Conflicto / Reclamación", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                    if (producto.motivoConflicto.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Motivo: ${producto.motivoConflicto}", color = Color(0xFF7F1D1D), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para confirmar la compra o préstamo
    if (mostrarDialogoConfirmarCompra) {
        val plazo = uiState.producto?.plazoDiasEntrega ?: 7
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmarCompra = false },
            title = { Text("Confirmar ${if (uiState.producto?.esParaComprar == true) "Compra" else "Préstamo"}", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Text(
                    if (uiState.producto?.esParaComprar == true)
                        "¿Quieres comprar este artículo? El vendedor dispondrá de ${plazo} días para entregártelo."
                    else
                        "¿Quieres solicitar el préstamo de este artículo? El dueño dispondrá de ${plazo} días para entregártelo y dispondrás de él durante ${uiState.producto?.diasPrestamo ?: 7} días."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoConfirmarCompra = false
                        onComprarOPrestar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmarCompra = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }

    // Diálogo para reportar problema / disputa
    if (mostrarDialogoConflicto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConflicto = false },
            title = { Text("Reportar Problema", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Column {
                    Text("Indica el motivo por el cual no se ha podido completar la entrega:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = motivoTexto,
                        onValueChange = { motivoTexto = it },
                        placeholder = { Text("Ej: El vendedor no acudió / el artículo está dañado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motivoTexto.isNotBlank()) {
                            onReportarConflicto(motivoTexto)
                            mostrarDialogoConflicto = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Abrir Disputa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConflicto = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun FilaCaracteristica(clave: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(clave, color = TextoSecundario, fontSize = 14.sp)
        Text(valor, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun BadgeEstadoDetalle(estado: EstadoProducto) {
    val (fondo, texto) = when (estado) {
        EstadoProducto.DISPONIBLE -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        EstadoProducto.EN_ENTREGA -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        EstadoProducto.VENDIDO -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        EstadoProducto.PRESTADO -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        EstadoProducto.DEVUELTO -> Color(0xFFEFF6FF) to Color(0xFF0F8A5F)
        EstadoProducto.EN_CONFLICTO -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
    }

    Surface(color = fondo, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = estado.etiqueta,
            color = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetalleProductoContentPreview() {
    DetalleProductoContent(
        uiState = DetalleProductoUiState(
            cargando = false,
            producto = Producto(
                titulo = "Taladro Percutor Bosch",
                precio = "35€",
                ubicacionYTiempo = "Portal 2, 3ºB",
                categoriaProducto = CategoriaProducto.HERRAMIENTAS,
                descripcion = "En perfecto estado, incluye brocas y maletín.",
                marca = "Bosch",
                estadoUso = "Como nuevo",
                estado = EstadoProducto.DISPONIBLE
            ),
            esPropietario = false
        ),
        onVolver = {},
        onComprarOPrestar = {},
        onConfirmarEntregaVendedor = {},
        onConfirmarRecepcionComprador = {},
        onToggleFavorito = {},
        onReportarConflicto = {}
    )
}