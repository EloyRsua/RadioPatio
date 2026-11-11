package com.example.radiopatio.misProductos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ShoppingBag
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
import com.example.radiopatio.marketplace.EstadoProducto
import com.example.radiopatio.marketplace.Producto

// Paleta de colores de RadioPatio
private val AzulPrincipal = Color(0xFF133E54)
private val VerdeBoton = Color(0xFF0F8A5F)
private val TextoPrincipal = Color(0xFF0F172A)
private val TextoSecundario = Color(0xFF64748B)
private val FondoApp = Color(0xFFF8FAFC)

/**
 * Screen de mis productos
 */
@Composable
fun MisProductosScreen(
    onVolver: () -> Unit,
    viewModel: MisProductosViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    MisProductosContent(
        productos = uiState.productosFiltrados,
        cargando = uiState.cargando,
        pestanaSeleccionada = uiState.pestanaSeleccionada,
        onCambiarPestana = { viewModel.onPestanaSeleccionada(it) },
        onVolver = onVolver,
        onMarcarVendido = { id -> viewModel.onActualizarEstado(id, EstadoProducto.VENDIDO) },
        onMarcarPrestado = { id, fecha -> viewModel.onActualizarEstado(id, EstadoProducto.PRESTADO, fecha) },
        onMarcarDisponible = { id -> viewModel.onActualizarEstado(id, EstadoProducto.DISPONIBLE) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisProductosContent(
    productos: List<Producto>,
    cargando: Boolean,
    pestanaSeleccionada: Int,
    onCambiarPestana: (Int) -> Unit,
    onVolver: () -> Unit,
    onMarcarVendido: (String) -> Unit,
    onMarcarPrestado: (String, String) -> Unit,
    onMarcarDisponible: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Artículos", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FondoApp)
        ) {
            // Pestañas (Ventas / Préstamos)
            PrimaryTabRow(
                selectedTabIndex = pestanaSeleccionada,
                containerColor = Color.White,
                contentColor = AzulPrincipal
            ) {
                Tab(
                    selected = pestanaSeleccionada == 0,
                    onClick = { onCambiarPestana(0) },
                    text = { Text("Mis Ventas") },
                    icon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null) },
                    selectedContentColor = AzulPrincipal,
                    unselectedContentColor = TextoSecundario
                )
                Tab(
                    selected = pestanaSeleccionada == 1,
                    onClick = { onCambiarPestana(1) },
                    text = { Text("Mis Préstamos") },
                    icon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                    selectedContentColor = AzulPrincipal,
                    unselectedContentColor = TextoSecundario
                )
            }

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdeBoton)
                }
            } else if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (pestanaSeleccionada == 0) "No has publicado artículos para venta" else "No tienes artículos en préstamo",
                        color = TextoSecundario
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(productos) { producto ->
                        TarjetaMiArticulo(
                            producto = producto,
                            onMarcarVendido = { onMarcarVendido(producto.firestoreId) },
                            onMarcarPrestado = { fecha -> onMarcarPrestado(producto.firestoreId, fecha) },
                            onMarcarDisponible = { onMarcarDisponible(producto.firestoreId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaMiArticulo(
    producto: Producto,
    onMarcarVendido: () -> Unit,
    onMarcarPrestado: (String) -> Unit,
    onMarcarDisponible: () -> Unit
) {
    var mostrarDialogoPrestar by remember { mutableStateOf(false) }
    var fechaDevolucionTexto by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura de la foto si existe
                if (!producto.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextoPrincipal,
                        maxLines = 1
                    )
                    Text(
                        text = if (producto.esParaComprar) producto.precio else "Préstamo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AzulPrincipal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Etiquetas de estado
            when (producto.estado) {
                EstadoProducto.DISPONIBLE -> {
                    BadgeEstado(
                        texto = "Disponible",
                        colorFondo = Color(0xFFDCFCE7),
                        colorTexto = Color(0xFF15803D)
                    )
                }
                EstadoProducto.VENDIDO -> {
                    BadgeEstado(
                        texto = "Vendido",
                        colorFondo = Color(0xFFFEE2E2),
                        colorTexto = Color(0xFFB91C1C)
                    )
                }
                EstadoProducto.PRESTADO -> {
                    BadgeEstado(
                        texto = if (producto.fechaDevolucion.isNotBlank()) "Prestado · Devuelve: ${producto.fechaDevolucion}" else "Prestado",
                        colorFondo = Color(0xFFFEF3C7),
                        colorTexto = Color(0xFFB45309)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (producto.esParaComprar) {
                    if (producto.estado == EstadoProducto.DISPONIBLE) {
                        OutlinedButton(
                            onClick = onMarcarVendido,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
                        ) {
                            Text("Marcar como Vendido")
                        }
                    } else {
                        TextButton(
                            onClick = onMarcarDisponible,
                            colors = ButtonDefaults.textButtonColors(contentColor = VerdeBoton)
                        ) {
                            Text("Poner en Venta de nuevo")
                        }
                    }
                } else {
                    if (producto.estado == EstadoProducto.DISPONIBLE) {
                        OutlinedButton(
                            onClick = { mostrarDialogoPrestar = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
                        ) {
                            Text("Registrar Préstamo")
                        }
                    } else {
                        TextButton(
                            onClick = onMarcarDisponible,
                            colors = ButtonDefaults.textButtonColors(contentColor = VerdeBoton)
                        ) {
                            Text("Marcar como Devuelto")
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoPrestar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPrestar = false },
            title = { Text("Registrar Préstamo", color = AzulPrincipal, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Cuándo te devolverán este artículo?", color = TextoPrincipal)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fechaDevolucionTexto,
                        onValueChange = { fechaDevolucionTexto = it },
                        placeholder = { Text("Ej: Mañana, 20 Nov...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarcarPrestado(fechaDevolucionTexto.ifBlank { "Sin fecha" })
                        mostrarDialogoPrestar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoPrestar = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextoSecundario)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun BadgeEstado(texto: String, colorFondo: Color, colorTexto: Color) {
    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MisProductosContentPreview() {
    MisProductosContent(
        productos = listOf(
            Producto(firestoreId = "1", titulo = "Mesa auxiliar", precio = "15€", esParaComprar = true, estado = EstadoProducto.DISPONIBLE),
            Producto(firestoreId = "2", titulo = "Lámpara de pie", precio = "20€", esParaComprar = true, estado = EstadoProducto.VENDIDO)
        ),
        cargando = false,
        pestanaSeleccionada = 0,
        onCambiarPestana = {},
        onVolver = {},
        onMarcarVendido = {},
        onMarcarPrestado = { _, _ -> },
        onMarcarDisponible = {}
    )
}
