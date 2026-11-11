package com.example.radiopatio.marketplace

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Screen: Responsable de mantener y gestionar el estado mediante el ViewModel.
 * Actúa como intermediario entre la lógica de negocio y la interfaz visual.
 */
@Composable
fun MercadilloScreen(
    onProductoClick: (Int) -> Unit = {},
    onIrAComunidad: () -> Unit = {},
    viewModel: MercadilloViewModel = viewModel(factory = MercadilloViewModelFactory())
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    MercadilloContent(
        productos = uiState.productosFiltrados,
        textoBusqueda = uiState.textoBusqueda,
        onTextoBusquedaChange = { viewModel.onTextoBusquedaChange(it) },
        esModoComprar = uiState.esModoComprar,
        onCambiarModo = { viewModel.onCambiarModo(it) },
        categoriaSeleccionada = uiState.categoriaSeleccionada,
        onCategoriaChange = { viewModel.onCategoriaSeleccionada(it) },
        onProductoClick = onProductoClick,
        nombreComunidad = uiState.comunidadNombre,
        tieneComunidad = uiState.comunidadActualId.isNotBlank(),
        cargando = uiState.cargando,
        uidUsuarioActual = uiState.usuarioActual?.uid.orEmpty(),
        onIrAComunidad = onIrAComunidad,
        mostrarDialogo = uiState.mostrarDialogoNuevoProducto,
        onAbrirDialogo = { viewModel.onAbrirDialogoNuevoProducto() },
        onCerrarDialogo = { viewModel.onCerrarDialogoNuevoProducto() },
        onCrearProducto = { viewModel.onCrearProducto(it) },
        onEliminarProducto = { viewModel.onEliminarProducto(it) }
    )
}

/**
 * Content: Responsable exclusivamente de representar la UI a partir de los datos recibidos.
 * No gestiona estado propio de negocio.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MercadilloContent(
    productos: List<Producto>,
    textoBusqueda: String,
    onTextoBusquedaChange: (String) -> Unit,
    esModoComprar: Boolean,
    onCambiarModo: (Boolean) -> Unit,
    categoriaSeleccionada: CategoriaProducto,
    onCategoriaChange: (CategoriaProducto) -> Unit,
    onProductoClick: (Int) -> Unit,
    nombreComunidad: String = "",
    tieneComunidad: Boolean = true,
    cargando: Boolean = false,
    uidUsuarioActual: String = "",
    onIrAComunidad: () -> Unit = {},
    mostrarDialogo: Boolean,
    onAbrirDialogo: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onCrearProducto: (Producto) -> Unit,
    onEliminarProducto: (Producto) -> Unit = {}
) {
    Scaffold(
        topBar = {
            BarraSuperiorMercadillo(nombreComunidad = nombreComunidad)
        },
        floatingActionButton = {
            if (tieneComunidad) {
                FloatingActionButton(
                    onClick = onAbrirDialogo,
                    containerColor = Color(0xFF389F82),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Subir Artículo")
                }
            }
        }
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(rellenoInterior)
        ) {
            when {
                cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF389F82)
                    )
                }

                !tieneComunidad -> {
                    SinComunidadMercadilloCard(
                        onIrAComunidad = onIrAComunidad,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Buscador
                        item {
                            BuscadorMercadillo(
                                textoBusqueda = textoBusqueda,
                                onTextoChange = onTextoBusquedaChange,
                                categoriaSeleccionada = categoriaSeleccionada,
                                onCategoriaChange = onCategoriaChange
                            )
                        }
                        // 2. ENCABEZADO PEGAJOSO (Sticky Header)
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(bottom = 6.dp)
                            ) {
                                SelectorModoMercadillo(
                                    esComprarSeleccionado = esModoComprar,
                                    onSeleccionarModo = onCambiarModo
                                )
                            }
                        }

                        if (productos.isEmpty()) {
                            val hayFiltros = textoBusqueda.isNotBlank() ||
                                categoriaSeleccionada != CategoriaProducto.TODOS
                            item {
                                EstadoMercadilloVacio(
                                    esModoComprar = esModoComprar,
                                    hayFiltros = hayFiltros,
                                    onLimpiarFiltros = {
                                        onTextoBusquedaChange("")
                                        onCategoriaChange(CategoriaProducto.TODOS)
                                    },
                                    onAbrirDialogo = onAbrirDialogo
                                )
                            }
                        } else {
                            // 3. Cuadrícula de 2 columnas usando chunked(2)
                            val filasDeProductos = productos.chunked(2)
                            items(filasDeProductos) { fila ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (producto in fila) {
                                        val esMio = uidUsuarioActual.isNotBlank() &&
                                            producto.vendedorUid == uidUsuarioActual
                                        TarjetaProducto(
                                            producto = producto,
                                            onClick = { onProductoClick(producto.id) },
                                            modifier = Modifier.weight(1f),
                                            esMiProducto = esMio,
                                            onEliminarClick = { onEliminarProducto(producto) }
                                        )
                                    }
                                    if (fila.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevoProducto(
            onDismiss = onCerrarDialogo,
            onCrearProducto = onCrearProducto
        )
    }
}

@Composable
private fun SinComunidadMercadilloCard(
    onIrAComunidad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = null,
                tint = Color(0xFF0F8A5F),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mercadillo aislado por comunidad",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF133E54),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Únete a tu comunidad en la pestaña Vecinos para comprar, vender o prestar artículos solo con tus vecinos.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onIrAComunidad,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8A5F)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Ir a Vecinos y Comunidad", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EstadoMercadilloVacio(
    esModoComprar: Boolean,
    hayFiltros: Boolean = false,
    onLimpiarFiltros: () -> Unit = {},
    onAbrirDialogo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            tint = Color(0xFF90A4AE),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when {
                hayFiltros -> "Sin artículos que coincidan"
                esModoComprar -> "No hay artículos en venta"
                else -> "No hay artículos para prestar"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF133E54),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hayFiltros) {
                "Prueba con otro término de búsqueda o categoría."
            } else {
                "Publica el primer anuncio para los vecinos de tu comunidad."
            },
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (hayFiltros) {
            OutlinedButton(
                onClick = onLimpiarFiltros,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Limpiar filtros", fontSize = 12.sp, color = Color(0xFF133E54))
            }
        } else {
            Button(
                onClick = onAbrirDialogo,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Subir artículo", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevoProducto(
    onDismiss: () -> Unit,
    onCrearProducto: (Producto) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CategoriaProducto.MUEBLES) }
    var esParaComprar by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    contentDescription = null,
                    tint = Color(0xFF389F82),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Subir Artículo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF133E54)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = esParaComprar,
                        onClick = { esParaComprar = true },
                        label = { Text("Para Vender", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !esParaComprar,
                        onClick = { esParaComprar = false },
                        label = { Text("Para Prestar", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Nombre del artículo *", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = {
                        Text(
                            if (esParaComprar) "Precio (€) *" else "Fianza / Condiciones",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CategoriaProducto.values().filter { it != CategoriaProducto.TODOS }) { cat ->
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text(cat.nombre, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && precio.isNotBlank()) {
                        val nuevoProducto = Producto(
                            id = 0,
                            titulo = titulo.trim(),
                            precio = if (esParaComprar && !precio.contains("€")) {
                                "${precio.trim()}€"
                            } else {
                                precio.trim()
                            },
                            ubicacionYTiempo = "Mi portal · Ahora mismo",
                            categoriaProducto = categoria,
                            esParaComprar = esParaComprar
                        )
                        onCrearProducto(nuevoProducto)
                    }
                },
                enabled = titulo.isNotBlank() && precio.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82))
            ) {
                Text("Publicar Artículo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun BarraSuperiorMercadillo(nombreComunidad: String = "") {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mercadillo vecinal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0XFF133E54)
                )
                Text(
                    text = if (nombreComunidad.isNotBlank()) {
                        "Compra, vende y presta en $nombreComunidad"
                    } else {
                        "Compra, vende y presta en tu comunidad"
                    },
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MercadilloPreview() {
    RadioPatioTheme {
        MercadilloContent(
            productos = listOf(
                Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h", CategoriaProducto.MUEBLES, true, comunidadId = "c1"),
                Producto(2, "Altavoz BT", "30€", "2ºC - Hace 2h", CategoriaProducto.ELECTRONICA, true, comunidadId = "c1"),
            ),
            textoBusqueda = "",
            onTextoBusquedaChange = {},
            esModoComprar = true,
            onCambiarModo = {},
            categoriaSeleccionada = CategoriaProducto.TODOS,
            onCategoriaChange = {},
            onProductoClick = {},
            nombreComunidad = "Residencial Montecerrao",
            tieneComunidad = true,
            cargando = false,
            mostrarDialogo = false,
            onAbrirDialogo = {},
            onCerrarDialogo = {},
            onCrearProducto = {}
        )
    }
}
