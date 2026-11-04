package com.example.radiopatio.registroInicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.R
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Pantalla de inicio de sesión para usuarios ya registrados
 */
@Composable
fun RegistroUsuario(
    onLoginClick: (email: String, pass: String) -> Unit = { _, _ -> },
    onIrANuevoRegistroClick: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF2))
    ) {
        // 1. ZONA SUPERIOR: Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.fondo_registro),
            contentDescription = "Cabecera del barrio",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        // 2. ZONA INFERIOR: Tarjeta blanca
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
            // Cabecera: Logo + Título
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "Iniciar sesión",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Introduce tus datos para acceder a tu comunidad.",
                    fontSize = 13.sp,
                    color = textoSecundario
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Formulario de login
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Campo: Correo
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Correo electrónico", fontSize = 14.sp, color = textoSecundario) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = textoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fondoGrisClaro,
                        unfocusedContainerColor = fondoGrisClaro,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, bordeGris, RoundedCornerShape(25.dp))
                )

                // Campo: Contraseña
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Contraseña", fontSize = 14.sp, color = textoSecundario) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = textoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = textoSecundario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fondoGrisClaro,
                        unfocusedContainerColor = fondoGrisClaro,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, bordeGris, RoundedCornerShape(25.dp))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón Entrar / Iniciar Sesión
            Button(
                onClick = { onLoginClick(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = verdeBoton,
                    disabledContainerColor = verdeBoton.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Iniciar sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pie: Enlace hacia el Registro de Nuevo Usuario
            Row(
                modifier = Modifier.clickable { onIrANuevoRegistroClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes una cuenta aún? ",
                    fontSize = 13.sp,
                    color = textoSecundario
                )
                Text(
                    text = "Regístrate aquí",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulPrincipal
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistroUsuarioPreview() {
    RadioPatioTheme {
        RegistroUsuario()
    }
}