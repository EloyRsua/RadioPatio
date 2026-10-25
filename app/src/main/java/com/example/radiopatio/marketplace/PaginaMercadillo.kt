package com.example.radiopatio.marketplace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.ui.theme.RadioPatioTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Screen: Responsable de mantener y gestionar el estado mediante el ViewModel.
 * Actúa como intermediario entre la logica de negocio y la interfaz visual
 */
@Composable
fun MercadilloScreen(
    onProductoClick: (Int) -> Unit = {},
    viewModel: MercadilloViewModel = viewModel()
){
    val uiState = viewModel.uiState

    MercadilloContent(
        productos = uiState.productosFiltrados,
        textoBusqueda = uiState.textoBusqueda,
        onTextoBusquedaChange = { viewModel.onTextoBusquedaChange(it) },
        esModoComprar = uiState.esModoComprar,
        onCambiarModo = { viewModel.onCambiarModo(it) },
        categoriaSeleccionada = uiState.categoriaSeleccionada,
        onCategoriaChange = { viewModel.onCategoriaSeleccionada(it) },
        onProductoClick = onProductoClick,

        mostrarDialogo = uiState.mostrarDialogoNuevoProducto,
        onAbrirDialogo = { viewModel.onAbrirDialogoNuevoProducto() },
        onCerrarDialogo = { viewModel.onCerrarDialogoNuevoProducto() },
        onCrearProducto = { viewModel.onCrearProducto(it) }
    )

}

/**
 * Content: Responsable exclusivamente de representar la UI a partir de los datos recibidos
 * No gestiona estado propio.
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
    mostrarDialogo: Boolean,
    onAbrirDialogo: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onCrearProducto: (Producto) -> Unit
) {
    Scaffold(
        topBar = {
            BarraSuperiorMercadillo(
                onSubirProducto = onAbrirDialogo
            )
        }
    ) { rellenoInterior ->
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(rellenoInterior),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Buscador (hace scroll normal)
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
                        .background(Color.White) // Fondo opaco para que no se superpongan los productos
                        .padding(bottom = 6.dp)
                ) {
                    SelectorModoMercadillo(
                        esComprarSeleccionado = esModoComprar,
                        onSeleccionarModo = onCambiarModo
                    )
                }
            }
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
                        TarjetaProducto(
                            producto = producto,
                            onClick = { onProductoClick(producto.id) }, // Conectamos el clic de la tarjeta con el id
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Si en la última fila solo hay 1 producto, dejamos un hueco vacío compensado
                    if (fila.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if(mostrarDialogo){
        DialogoNuevoProducto(
            onDismiss = onCerrarDialogo,
            onCrearProducto = onCrearProducto
        )
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
                // Tipo de transacción
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
                    label = { Text(if (esParaComprar) "Precio (€) *" else "Fianza / Condiciones", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                // Usamos FlowRow o un LazyRow para las categorías
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
                            id = (100..999).random(), // Generamos ID aleatorio temporal
                            titulo = titulo.trim(),
                            precio = if (precio.contains("€")) precio.trim() else "${precio.trim()}€",
                            ubicacionYTiempo = "Mi portal - Ahora mismo",
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
private fun BarraSuperiorMercadillo(
    onSubirProducto: () -> Unit
){
    Surface(shadowElevation = 2.dp){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mercadillo vecinal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0XFF133E54)
                )
                Text(
                    text = "Compra, vende y presta en tu comunidad",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onSubirProducto) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    contentDescription = "Subir Artículo",
                    tint = Color(0xFF389F82)
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
                Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h", CategoriaProducto.MUEBLES, true),
                Producto(2, "Altavoz BT", "30€", "2ºC - Hace 2h", CategoriaProducto.ELECTRONICA, true),
            ),
            textoBusqueda = "",
            onTextoBusquedaChange = {},
            esModoComprar = true,
            onCambiarModo = {},
            categoriaSeleccionada = CategoriaProducto.TODOS,
            onCategoriaChange = {},
            onProductoClick = {},
            mostrarDialogo = false,
            onAbrirDialogo = {},
            onCerrarDialogo = {},
            onCrearProducto = {}
        )
    }
}