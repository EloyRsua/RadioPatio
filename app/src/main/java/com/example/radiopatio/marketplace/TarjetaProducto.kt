package com.example.radiopatio.marketplace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Componente visual para la tarjeta visual de un producto
 */
@Composable
fun TarjetaProducto(
    producto: Producto,
    onClick: () -> Unit,
    modifier: Modifier
){
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD6DDE1),
                RoundedCornerShape(14.dp))
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
        ){
            if(producto.imagenRes!=null){
                Image(painter = painterResource(id = producto.imagenRes),
                    contentDescription = producto.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            }
            else{
                // Icono por defecto mientras no tengamos imagen real
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription =  null,
                    tint = Color(0xFF90A4AE),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Titulo
        Text(
            text = producto.titulo,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF133E54),
            maxLines = 1
        )

        // Precio en verde
        Text(
            text = producto.precio,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF2E9473)
        )

        // Ubicacion y antiguedad
        Text(
            text = producto.ubicacionYTiempo,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TarjetaProductoPreview() {
    RadioPatioTheme {
        TarjetaProducto(
            producto = Producto(
                id = 1,
                titulo = "Mesa auxiliar",
                precio = "15€",
                ubicacionYTiempo = "1ºA - Hace 1h"
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp).width(180.dp)
        )
    }
}
