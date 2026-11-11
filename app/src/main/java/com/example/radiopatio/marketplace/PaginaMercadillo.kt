package com.example.radiopatio.marketplace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.usuarios.UserRepository

/**
 * Screen: Responsable de mantener y gestionar el estado mediante el ViewModel.
 * Actúa como intermediario entre la logica de negocio y la interfaz visual
 */
@Composable
fun MercadilloScreen(
    onProductoClick: (String) -> Unit = {},
    onIrACrearProducto: () -> Unit = {}
){
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication

    val userRepository = remember { UserRepository() }

    val viewModel: MercadilloViewModel = viewModel(
        factory = MercadilloViewModelFactory(app.database.productoDao(), userRepository)
    )

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
        onSubirProducto = onIrACrearProducto
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
    onProductoClick: (String) -> Unit,
    onSubirProducto: () -> Unit
) {
    Scaffold(
        topBar = {
            BarraSuperiorMercadillo(
                onSubirProducto = onSubirProducto
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
                            onClick = { onProductoClick(producto.firestoreId) }, // Conectamos el clic de la tarjeta con el id
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
            onSubirProducto = {}
        )
    }
}