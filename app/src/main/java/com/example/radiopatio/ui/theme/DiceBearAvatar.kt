package com.example.radiopatio.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

object DiceBearAvatars {
    val estilos = listOf(
        "avataaars" to "Clásico",
        "lorelei" to "Ilustrado",
        "adventurer" to "Aventurero",
        "bottts" to "Robot",
        "thumbs" to "Pulgar"
    )

    fun construirUrl(
        seed: String,
        estilo: String = "avataaars",
        tamanoPx: Int = 200
    ): String {
        val semillaLimpia = seed.trim().ifBlank { "vecino-radiopatio" }
        val estiloValido = estilos.firstOrNull { it.first == estilo }?.first ?: "avataaars"
        val seedCodificada = Uri.encode(semillaLimpia)
        val tamano = tamanoPx.coerceIn(48, 256)
        return "https://api.dicebear.com/9.x/$estiloValido/png?seed=$seedCodificada&size=$tamano"
    }

    fun esUrlRemota(uri: Uri?): Boolean {
        val scheme = uri?.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }
}

@Composable
fun SelectorAvatarDiceBear(
    semillaBase: String,
    urlActual: String,
    onSeleccionarUrl: (String) -> Unit,
    onAbrirGaleria: () -> Unit,
    modifier: Modifier = Modifier,
    cantidadOpciones: Int = 36
) {
    var estilo by rememberSaveable { mutableStateOf("avataaars") }
    var opcionSeleccionada by rememberSaveable(semillaBase) { mutableIntStateOf(0) }

    val baseLimpia = remember(semillaBase) {
        semillaBase.trim().ifBlank { "vecino" }
    }

    val opcionesAvatar = remember(baseLimpia, estilo, cantidadOpciones) {
        val total = cantidadOpciones.coerceIn(4, 100)
        (0 until total).map { indice ->
            val seedOpcion = if (indice == 0) baseLimpia else "$baseLimpia-op-$indice"
            indice to DiceBearAvatars.construirUrl(
                seed = seedOpcion,
                estilo = estilo,
                tamanoPx = 120
            )
        }
    }

    val urlActivaPorDefecto = remember(opcionesAvatar, opcionSeleccionada) {
        opcionesAvatar.firstOrNull { it.first == opcionSeleccionada }?.second
            ?: opcionesAvatar.first().second
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = urlActual.ifBlank { urlActivaPorDefecto },
                    contentDescription = "Vista previa de avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avatar o foto de perfil",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF133E54)
                    )
                    Text(
                        text = "Desliza para ver más avatares o elige una foto del móvil.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAbrirGaleria,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Foto del móvil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onSeleccionarUrl(urlActual.ifBlank { urlActivaPorDefecto }) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8A5F))
                ) {
                    Text("Usar avatar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DiceBearAvatars.estilos) { (idEstilo, etiqueta) ->
                    FilterChip(
                        selected = estilo == idEstilo,
                        onClick = {
                            estilo = idEstilo
                            val nuevaUrl = if (opcionSeleccionada == 0) {
                                DiceBearAvatars.construirUrl(seed = baseLimpia, estilo = idEstilo)
                            } else {
                                DiceBearAvatars.construirUrl(
                                    seed = "$baseLimpia-op-$opcionSeleccionada",
                                    estilo = idEstilo
                                )
                            }
                            onSeleccionarUrl(nuevaUrl)
                        },
                        label = { Text(etiqueta, fontSize = 12.sp) }
                    )
                }
            }

            Text(
                text = "Elige un avatar",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(opcionesAvatar, key = { it.first }) { (indiceOpcion, urlOpcion) ->
                    val activa = urlActual == urlOpcion
                    AsyncImage(
                        model = urlOpcion,
                        contentDescription = "Opción de avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                opcionSeleccionada = indiceOpcion
                                onSeleccionarUrl(urlOpcion)
                            }
                            .then(
                                if (activa) {
                                    Modifier.border(2.5.dp, Color(0xFF0F8A5F), CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}