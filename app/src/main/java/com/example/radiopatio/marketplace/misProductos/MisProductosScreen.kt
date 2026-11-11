package com.example.radiopatio.marketplace.misProductos

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Screen de mis productos
 */
@Composable
fun MisProductosScreen(
    onVolver: () -> Unit,
    viewModel: MisProductosViewModel = viewModel()
){
    val uiState = viewModel.uiState

    MisProductosContent(
        productos = uiState.productosFiltrados,
        cargando = uiState.cargando,
        pestanaSeleccionada = uiState.pestanaSeleccionada,
        onCambiarPestana = { viewModel.onPestanaSeleccionada(it) },
        onVolver = onVolver,
        onMarcarVendido = { id -> viewModel.onActualizarEstado(id, EstadoProducto.VENDIDO)},
        onMarcarPrestado = { id, fecha -> viewModel.onActualizarEstado(id, EstadoProducto.PRESTADO, fecha)},
        onMarcarDisponible = { id -> viewModel.onActualizarEstado(id, EstadoProducto.DISPONIBLE)}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisProductosContent(
    productos: List<MiProducto>,
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
                title = { Text("Mis Artículos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Pestañas (Ventas / Préstamos)
            PrimaryTabRow(
                selectedTabIndex = pestanaSeleccionada,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = pestanaSeleccionada == 0,
                    onClick = { onCambiarPestana(0) },
                    text = { Text("Mis Ventas") },
                    icon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null) }
                )
                Tab(
                    selected = pestanaSeleccionada == 1,
                    onClick = { onCambiarPestana(1) },
                    text = { Text("Mis Préstamos") },
                    icon = { Icon(Icons.Outlined.Event, contentDescription = null) }
                )
            }
            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            } else if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (pestanaSeleccionada == 0) "No has publicado artículos para venta" else "No tienes artículos en préstamo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(productos) { producto ->
                        TarjetaMiArticulo (
                            producto = producto,
                            onMarcarVendido = { onMarcarVendido(producto.id) },
                            onMarcarPrestado = { fecha -> onMarcarPrestado(producto.id, fecha) },
                            onMarcarDisponible = { onMarcarDisponible(producto.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaMiArticulo(
    producto: MiProducto,
    onMarcarVendido: () -> Unit,
    onMarcarPrestado: (String) -> Unit,
    onMarcarDisponible: () -> Unit
) {
    var mostrarDialogoPrestar by remember { mutableStateOf(false) }
    var fechaDevolucionTexto by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = producto.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (producto.esParaComprar) producto.precio else "Préstamo",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                        OutlinedButton(onClick = onMarcarVendido) {
                            Text("Marcar como Vendido")
                        }
                    } else {
                        TextButton(onClick = onMarcarDisponible) {
                            Text("Poner en Venta de nuevo")
                        }
                    }
                } else {
                    if (producto.estado == EstadoProducto.DISPONIBLE) {
                        OutlinedButton(onClick = { mostrarDialogoPrestar = true }) {
                            Text("Registrar Préstamo")
                        }
                    } else {
                        TextButton(onClick = onMarcarDisponible) {
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
            title = { Text("Registrar Préstamo") },
            text = {
                Column {
                    Text("¿Cuándo te devolverán este artículo?")
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
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPrestar = false }) {
                    Text("Cancelar")
                }
            }
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
/**
 * PREVIEW (Slide 7): Podemos previsualizar directamente MisProductosContent
 * pasándole datos ficticios sin necesidad de ViewModel ni conexión a Firebase.
 */
@Preview(showBackground = true)
@Composable
fun MisProductosContentPreview() {
    RadioPatioTheme {
        MisProductosContent(
            productos = listOf(
                MiProducto("1", "Mesa auxiliar", "15€", esParaComprar = true, estado = EstadoProducto.DISPONIBLE),
                MiProducto("2", "Lámpara de pie", "20€", esParaComprar = true, estado = EstadoProducto.VENDIDO)
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
}

