package com.example.radiopatio.publicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Componente modular independiente para renderizar las tarjetas de publicación.
 */
@Composable
fun TarjetaPublicacion(
    publicacion: Publicacion,
    onEliminarClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Cabecera: Avatar, Autor, Tiempo/Ubicación y Botón de Eliminar
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF133E54)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = publicacion.inicial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = publicacion.autor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = publicacion.tiempoYUbica,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Botón de eliminar (papelera)
            IconButton(onClick = onEliminarClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar publicación",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenido del mensaje
        Text(
            text = publicacion.contenido,
            fontSize = 13.sp,
            color = Color(0xFF263238)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Barra inferior: Me gusta, Comentarios y Compartir
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Me gusta",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${publicacion.meGusta}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comentarios",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${publicacion.comentarios}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Compartir",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaPublicacionPreview() {
    RadioPatioTheme {
        TarjetaPublicacion(
            publicacion = Publicacion(
                autor = "Lucía Fernández",
                inicial = "L",
                tiempoYUbica = "Hace 2 h · Calle Mayor",
                contenido = "¡Buenas vecin@s! Este sábado quedada en el parque.",
                meGusta = 24,
                comentarios = 6
            )
        )
    }
}