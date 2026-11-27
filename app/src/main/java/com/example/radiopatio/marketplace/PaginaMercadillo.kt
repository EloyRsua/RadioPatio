package com.example.radiopatio.marketplace

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.RadioPatioApplication
import com.example.radiopatio.ui.theme.RadioPatioTheme
import com.example.radiopatio.usuarios.UserRepository

/**
 * Screen: Responsable de mantener y gestionar el estado mediante el ViewModel.
 * Actúa como intermediario entre la lógica de negocio y la interfaz visual.
 */
@Composable
fun MercadilloScreen(
    onProductoClick: (String) -> Unit = {},
    onIrACrearProducto: () -> Unit = {},
    onIrAComunidad: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as RadioPatioApplication

    val viewModel: MercadilloViewModel = viewModel(
        factory = MercadilloViewModelFactory(
            mercadilloRepository = app.mercadilloRepository,
            userRepository = app.userRepository,
            comunidadRepository = app.comunidadRepository
        )
    )

    val uiState = viewModel.uiState
    val cargando = uiState.actualizacion is EstadoActualizacion.Actualizando &&
            uiState.productosTotales.isEmpty()

    LaunchedEffect(uiState.actualizacion) {
        val estado = uiState.actualizacion
        if (estado is EstadoActualizacion.Error) {
            Toast.makeText(context, estado.mensaje, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    MercadilloContent(
        productos = uiState.productosFiltrados,
        currentUid = uiState.currentUid,
        textoBusqueda = uiState.textoBusqueda,
        onTextoBusquedaChange = { viewModel.onTextoBusquedaChange(it) },
        esModoComprar = uiState.esModoComprar,
        onCambiarModo = { viewModel.onCambiarModo(it) },
        categoriaSeleccionada = uiState.categoriaSeleccionada,
        onCategoriaChange = { viewModel.onCategoriaSeleccionada(it) },
        onProductoClick = onProductoClick,
        onEliminarProducto = { producto ->
            viewModel.onEliminarProducto(producto)
            Toast.makeText(context, "Artículo eliminado", Toast.LENGTH_SHORT).show()
        },
        onSubirProducto = onIrACrearProducto,
        onToggleFavorito = { producto ->
            viewModel.toggleFavorito(producto)
        },
        nombreComunidad = uiState.comunidadNombre,
        tieneComunidad = uiState.comunidadActualId.isNotBlank(),
        cargando = cargando,
        onIrAComunidad = onIrAComunidad
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
    currentUid: String = "",
    textoBusqueda: String,
    onTextoBusquedaChange: (String) -> Unit,
    esModoComprar: Boolean,
    onCambiarModo: (Boolean) -> Unit,
    categoriaSeleccionada: CategoriaProducto,
    onCategoriaChange: (CategoriaProducto) -> Unit,
    onProductoClick: (String) -> Unit,
    onEliminarProducto: (Producto) -> Unit = {},
    onSubirProducto: () -> Unit = {},
    onToggleFavorito: (Producto) -> Unit = {},
    nombreComunidad: String = "",
    tieneComunidad: Boolean = true,
    cargando: Boolean = false,
    onIrAComunidad: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            BarraSuperiorMercadillo(nombreComunidad = nombreComunidad)
        },
        floatingActionButton = {
            if (tieneComunidad) {
                FloatingActionButton(
                    onClick = onSubirProducto,
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
                                    onAbrirDialogo = onSubirProducto
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
                                        TarjetaProducto(
                                            producto = producto,
                                            currentUid = currentUid,
                                            onClick = { onProductoClick(producto.id) },
                                            onEliminarClick = { onEliminarProducto(producto) },
                                            onFavoritoClick = { onToggleFavorito(producto) },
                                            modifier = Modifier.weight(1f)
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
                Producto(
                    id = "1",
                    comunidadId = "c1",
                    titulo = "Mesa auxiliar",
                    precio = "15€",
                    ubicacion = "1ºA",
                    fechaCreacion = System.currentTimeMillis() - 3600_000L,
                    categoriaProducto = CategoriaProducto.MUEBLES,
                    esParaComprar = true
                ),
                Producto(
                    id = "2",
                    comunidadId = "c1",
                    titulo = "Altavoz BT",
                    precio = "30€",
                    ubicacion = "2ºC",
                    fechaCreacion = System.currentTimeMillis() - 7200_000L,
                    categoriaProducto = CategoriaProducto.ELECTRONICA,
                    esParaComprar = true
                )
            ),
            textoBusqueda = "",
            onTextoBusquedaChange = {},
            esModoComprar = true,
            onCambiarModo = {},
            categoriaSeleccionada = CategoriaProducto.TODOS,
            onCategoriaChange = {},
            onProductoClick = {},
            onSubirProducto = {},
            nombreComunidad = "Residencial Montecerrao",
            tieneComunidad = true,
            cargando = false
        )
    }
}