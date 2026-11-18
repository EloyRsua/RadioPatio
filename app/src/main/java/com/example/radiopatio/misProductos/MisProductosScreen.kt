package com.example.radiopatio.misProductos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
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

private val AzulPrincipal = Color(0xFF133E54)
private val VerdeBoton = Color(0xFF0F8A5F)
private val TextoPrincipal = Color(0xFF0F172A)
private val TextoSecundario = Color(0xFF64748B)
private val FondoApp = Color(0xFFF8FAFC)

@Composable
fun MisProductosScreen(
    onVolver: () -> Unit,
    onProductoClick: (String) -> Unit = {},
    viewModel: MisProductosViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    MisProductosContent(
        productos = uiState.productosFiltrados,
        cargando = uiState.cargando,
        pestanaSeleccionada = uiState.pestanaSeleccionada,
        onCambiarPestana = { viewModel.onPestanaSeleccionada(it) },
        onVolver = onVolver,
        onProductoClick = onProductoClick,
        onEliminarProducto = { producto -> viewModel.eliminarProducto(producto) }
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
    onProductoClick: (String) -> Unit,
    onEliminarProducto: (Producto) -> Unit = {}
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                        text = if (pestanaSeleccionada == 0) "No tienes artículos en venta" else "No tienes artículos en préstamo",
                        color = TextoSecundario
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(productos, key = { it.firestoreId }) { producto ->
                        TarjetaMiArticulo(
                            producto = producto,
                            onClick = { onProductoClick(producto.firestoreId) },
                            onEliminar = { onEliminarProducto(producto) }
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
    onClick: () -> Unit,
    onEliminar: () -> Unit = {}
) {
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura de la foto
            if (!producto.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = producto.imagenUrl,
                    contentDescription = producto.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(65.dp)
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (producto.esParaComprar) producto.precio else "Préstamo",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AzulPrincipal
                )
                Spacer(modifier = Modifier.height(6.dp))
                BadgeEstado(producto.estado, producto.fechaDevolucion)
            }

            // Botón eliminar (solo si está disponible)
            if (producto.estado == EstadoProducto.DISPONIBLE) {
                IconButton(onClick = { mostrarDialogoEliminar = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar artículo",
                        tint = Color(0xFFB91C1C)
                    )
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar artículo", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Text("¿Seguro que quieres eliminar \"${producto.titulo}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        onEliminar()
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
fun BadgeEstado(estado: EstadoProducto, fechaDevolucion: String = "") {
    val (fondo, texto, etiqueta) = when (estado) {
        EstadoProducto.DISPONIBLE -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Disponible")
        EstadoProducto.EN_ENTREGA -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "En proceso de entrega")
        EstadoProducto.VENDIDO -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "Vendido")
        EstadoProducto.PRESTADO -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), if (fechaDevolucion.isNotBlank()) "Prestado · Devuelve: $fechaDevolucion" else "Prestado")
        EstadoProducto.DEVUELTO -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "Devuelto")
        EstadoProducto.EN_CONFLICTO -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "En conflicto")
    }
    Surface(color = fondo, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = etiqueta,
            color = texto,
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
        onProductoClick = {}
    )
}