package com.example.radiopatio.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun BuscadorMercadillo(
    textoBusqueda: String,
    onTextoChange: (String) -> Unit,
    categoriaSeleccionada: CategoriaProducto,
    onCategoriaChange: (CategoriaProducto) -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Caja de busqueda
        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = onTextoChange,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            placeholder = {
                Text("Buscar en el mercadillo", color = Color.Gray, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Buscar",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp))
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFB0BEC5),
                focusedBorderColor = Color(0xFF133E54),
                cursorColor = Color(0xFF133E54)
            ),
            singleLine = true
        )

        // Boton de filtros
        Box{
            var menuExpandido by remember { mutableStateOf(false) }

            IconButton(onClick = { menuExpandido = true}) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filtros",
                    // Se pone verde si hay un filtro activo, si no, azul oscuro
                    tint = if (categoriaSeleccionada != CategoriaProducto.TODOS) Color(0xFF389F82) else Color(0xFF133E54),
                    modifier = Modifier.size(28.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false},
                modifier = Modifier.background(Color.White)
            ) {
                CategoriaProducto.entries.forEach{ categoria ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = categoria.nombre,
                                fontWeight = if (categoria == categoriaSeleccionada) FontWeight.Bold else FontWeight.Normal,
                                color = if (categoria == categoriaSeleccionada) Color(0xFF0F8A5F) else Color.Black
                            )
                        },
                        onClick = {
                            onCategoriaChange(categoria)
                            menuExpandido = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Pestañas tipo pastilla para alternar entre "Comprar" y "Prestar".
 */
@Composable
fun SelectorModoMercadillo(
    esComprarSeleccionado: Boolean,
    onSeleccionarModo: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE8ECEF))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Opción: COMPRAR
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (esComprarSeleccionado) Color(0xFF0D324D) else Color.Transparent)
                .clickable { onSeleccionarModo(true) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = if (esComprarSeleccionado) Color.White else Color(0xFF133E54),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Comprar",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (esComprarSeleccionado) Color.White else Color(0xFF133E54)
            )
        }
        // Opción: PRESTAR
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (!esComprarSeleccionado) Color(0xFF0D324D) else Color.Transparent)
                .clickable { onSeleccionarModo(false) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Handshake,
                contentDescription = null,
                tint = if (!esComprarSeleccionado) Color.White else Color(0xFF133E54),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Prestar",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (!esComprarSeleccionado) Color.White else Color(0xFF133E54)
            )
        }
    }
}

/**
 * Previsualización del buscador y el selector
 */
@Preview(showBackground = true)
@Composable
fun ControlesMercadilloPreview() {
    RadioPatioTheme {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            BuscadorMercadillo(
                textoBusqueda = "",
                onTextoChange = {},
                categoriaSeleccionada = CategoriaProducto.TODOS,
                onCategoriaChange = {}
            )
            SelectorModoMercadillo(
                esComprarSeleccionado = true,
                onSeleccionarModo = {}
            )
        }
    }
}
