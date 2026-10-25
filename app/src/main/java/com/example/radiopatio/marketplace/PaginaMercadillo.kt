package com.example.radiopatio.marketplace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.R
import com.example.radiopatio.ui.theme.RadioPatioTheme

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
        onProductoClick = onProductoClick
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
    onProductoClick: (Int) -> Unit
) {
    Scaffold(
        topBar = { BarraSuperiorMercadillo() },
        bottomBar = { BarraNavegacionMercadillo() }
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
                    onTextoChange = onTextoBusquedaChange
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
}

/**
 * Barra superior con logo RadioPatio
 */
@Composable
private fun BarraSuperiorMercadillo() {
    Surface (shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_radiopatio),
                contentDescription = "Logo Radio Patio",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "RADIOPATIO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF133E54),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFF133E54)
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color(0xFF133E54)
                )
            }
        }
    }
}

/**
 * Barra inferior con el icono del mercadillo destacado
 */
@Composable
private fun BarraNavegacionMercadillo() {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDCE5EB))
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Inicio
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Home, contentDescription = "Inicio", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
            }
            // 2. Vecinos
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Groups, contentDescription = "Vecinos", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
            }
            // 3. Botón central verde (+)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CA77D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            // 4. Mercadillo (Icono activo)
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = "Mercadillo",
                    tint = Color(0xFF0D324D),
                    modifier = Modifier.size(32.dp)
                )
            }
            // 5. Eventos
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Eventos", tint = Color(0xFF133E54), modifier = Modifier.size(28.dp))
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
                Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h"),
                Producto(2, "Altavoz BT", "30€", "2ºC - Hace 2h"),
            ),
            textoBusqueda = "",
            onTextoBusquedaChange = {},
            esModoComprar = true,
            onCambiarModo = {},
            onProductoClick = {}
        )
    }
}