package com.example.radiopatio.marketplace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.radiopatio.ui.theme.AzulPrincipal
import com.example.radiopatio.ui.theme.BordeGris
import com.example.radiopatio.ui.theme.FondoSuaveVerde
import com.example.radiopatio.ui.theme.RadioPatioTheme
import com.example.radiopatio.ui.theme.TextoSecundario
import com.example.radiopatio.ui.theme.VerdePrecio
import com.example.radiopatio.ui.theme.VerdePrincipal
import com.example.radiopatio.ui.theme.VerdeSecundario
import com.google.firebase.auth.FirebaseAuth

/**
 * Componente visual para la tarjeta de un producto en el mercadillo
 */
@Composable
fun TarjetaProducto(
    producto: Producto,
    onClick: () -> Unit,
    onEliminarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val esMiProducto = currentUid.isNotBlank() &&
            (producto.autorUid == currentUid || producto.vendedorUid == currentUid)

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val colorBorde = if (esMiProducto) VerdeSecundario else BordeGris
    val grosorBorde = if (esMiProducto) 1.5.dp else 1.dp
    val colorFondoTarjeta = if (esMiProducto) FondoSuaveVerde else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colorFondoTarjeta)
            .border(grosorBorde, colorBorde, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        // Hueco de la imagen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F4F7)),
            contentAlignment = Alignment.Center
        ) {
            if (!producto.imagenUrl.isNullOrBlank()) {
                AsyncImage(
                    model = producto.imagenUrl,
                    contentDescription = producto.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (producto.imagenRes != null) {
                Image(
                    painter = painterResource(id = producto.imagenRes),
                    contentDescription = producto.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFF90A4AE),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Insignia superior izquierda según estado o autoría
            when {
                producto.estado == EstadoProducto.EN_ENTREGA -> {
                    val esMiCompra = producto.compradorUid.isNotBlank() && producto.compradorUid == currentUid
                    Surface(
                        color = if (esMiCompra) VerdePrincipal else Color(0xD9B45309),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = if (esMiCompra) "RESERVADO POR TI" else "RESERVADO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                producto.estado == EstadoProducto.PRESTADO -> {
                    Surface(
                        color = Color(0xFF1E40AF),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "EN PRÉSTAMO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                esMiProducto -> {
                    Surface(
                        color = AzulPrincipal,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "TU ANUNCIO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fila con Título y Papelera (solo si es tuyo y está disponible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = producto.titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AzulPrincipal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (esMiProducto && producto.estado == EstadoProducto.DISPONIBLE) {
                IconButton(
                    onClick = { mostrarDialogoEliminar = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar artículo",
                        tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Precio en verde
        Text(
            text = producto.precio,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = VerdePrecio
        )

        // Ubicación y antigüedad
        Text(
            text = if (esMiProducto) "Tu artículo · ${producto.ubicacionYTiempo}" else producto.ubicacionYTiempo,
            fontSize = 11.sp,
            color = if (esMiProducto) VerdePrincipal else Color.Gray,
            fontWeight = if (esMiProducto) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // Diálogo de confirmación al pulsar la papelera
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = {
                Text(
                    text = "Eliminar artículo",
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
            },
            text = {
                Text("¿Seguro que quieres eliminar \"${producto.titulo}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        onEliminarClick()
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold
                    )
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

@Preview(showBackground = true)
@Composable
fun TarjetaProductoPreview() {
    RadioPatioTheme {
        TarjetaProducto(
            producto = Producto(1, "Mesa auxiliar", "15€", "1ºA - Hace 1h", CategoriaProducto.MUEBLES, true),
            onClick = {},
            modifier = Modifier
                .padding(16.dp)
                .width(180.dp)
        )
    }
}