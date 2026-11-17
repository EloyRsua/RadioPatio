package com.example.radiopatio.publicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearPublicacion(
    autorInicial: String = "",
    ubicacionInicial: String = "",
    publicando: Boolean = false,
    onVolver: () -> Unit = {},
    onPublicarClick: (autor: String, ubicacion: String, contenido: String) -> Unit = { _, _, _ -> }
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)

    // Estados manuales editables (pre-rellenados con la sesión del usuario si está disponible)
    var autor by remember(autorInicial) { mutableStateOf(autorInicial) }
    var ubicacion by remember(ubicacionInicial) { mutableStateOf(ubicacionInicial) }
    var contenido by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Publicación", fontWeight = FontWeight.Bold, color = azulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = azulPrincipal)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Campo 1: Nombre del autor
            OutlinedTextField(
                value = autor,
                onValueChange = { autor = it },
                label = { Text("Tu nombre o apodo") },
                placeholder = { Text("Ej. Cayetano Vega") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Campo 2: Ubicación / Piso
            OutlinedTextField(
                value = ubicacion,
                onValueChange = { ubicacion = it },
                label = { Text("Ubicación o Piso") },
                placeholder = { Text("Ej. 45, 3F / Portal 1") },
                leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Campo 3: Mensaje principal
            OutlinedTextField(
                value = contenido,
                onValueChange = { contenido = it },
                label = { Text("Mensaje") },
                placeholder = { Text("¿Qué quieres compartir hoy?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            // Botón Publicar
            Button(
                onClick = {
                    onPublicarClick(
                        autor.ifBlank { "Vecino Anónimo" },
                        ubicacion.ifBlank { "Comunidad" },
                        contenido.trim()
                    )
                },
                enabled = !publicando && contenido.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = verdeBoton)
            ) {
                if (publicando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Publicar anuncio",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}