package com.example.radiopatio.registroInicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
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
import com.example.radiopatio.R
import com.example.radiopatio.ui.theme.RadioPatioTheme


/**
 * Pantalla de registro sin huecos ni cortes entre la foto y la tarjeta.
 */
@Composable
fun PantallaRegistro() {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF2))
    ) {
        // -------------------------------------------------------------
        // 1. ZONA SUPERIOR: Imagen que baja lo suficiente para solapar
        // -------------------------------------------------------------
        Image(
            painter = painterResource(id = R.drawable.fondo_registro),
            contentDescription = "Cabecera del barrio",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f) // Baja hasta el 48% para quedar bien por debajo de la tarjeta
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        // -------------------------------------------------------------
        // 2. ZONA INFERIOR: Tarjeta blanca superpuesta directamente sobre la imagen
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 22.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera: Logo de RadioPatio + Marca + Eslogan
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_radiopatio),
                        contentDescription = "Logo RadioPatio",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "RadioPatio",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = azulPrincipal,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "La red de tu comunidad y vecinos",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = verdeBoton
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Crear cuenta",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Regístrate para enterarte de lo que pasa en tu barrio.",
                    fontSize = 13.sp,
                    color = textoSecundario
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campos de entrada
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Campo: Nombre
                CampoTextoFalso(
                    icono = Icons.Outlined.Person,
                    texto = "Nombre y apellidos",
                    fondo = fondoGrisClaro,
                    colorBorde = bordeGris,
                    colorTexto = textoSecundario
                )

                // Campo: Correo seleccionado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.8.dp, bordeGris, RoundedCornerShape(25.dp))
                        .background(fondoGrisClaro)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = textoSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Correo",
                        fontSize = 14.sp,
                        color = textoSecundario,
                        fontWeight = FontWeight.Medium
                    )

                }

                // Campo: Contraseña
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, bordeGris, RoundedCornerShape(25.dp))
                        .background(fondoGrisClaro)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = textoSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Contraseña",
                        fontSize = 14.sp,
                        color = textoSecundario,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Aceptar normas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(verdeBoton),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Acepto las normas de la comunidad",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(verdeBoton),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Registrarme",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pie: Iniciar sesión
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tienes una cuenta? ",
                    fontSize = 13.sp,
                    color = textoSecundario
                )
                Text(
                    text = "Inicia sesión",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulPrincipal
                )
            }
        }
    }
}

/** Componente auxiliar para los campos estáticos **/
@Composable
private fun CampoTextoFalso(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    fondo: Color,
    colorBorde: Color,
    colorTexto: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .border(1.dp, colorBorde, RoundedCornerShape(25.dp))
            .background(fondo)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = colorTexto,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = texto,
            fontSize = 14.sp,
            color = colorTexto
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaRegistroPreview() {
    RadioPatioTheme {
        PantallaRegistro()
    }
}