package com.example.radiopatio.registroInicio

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.R

@Composable
fun RegistroUsuario(
    onLoginClick: (email: String, pass: String, onError: () -> Unit) -> Unit = { _, _, _ -> },
    onIrANuevoRegistroClick: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val textoSecundario = Color(0xFF64748B)

    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF2))
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_registro),
            contentDescription = "Fondo barrio",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RadioPatio",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = azulPrincipal
            )
            Text(
                text = "Tu comunidad de vecinos conectada",
                fontSize = 14.sp,
                color = textoSecundario
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo Email
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Correo electrónico", fontSize = 14.sp, color = textoSecundario) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = textoSecundario) },
                singleLine = true,
                enabled = !cargando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Contraseña
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Contraseña", fontSize = 14.sp, color = textoSecundario) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = textoSecundario) },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !cargando
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = textoSecundario
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !cargando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Iniciar Sesión con Validaciones y Estado de Carga
            Button(
                onClick = {
                    val emailLimpio = email.trim()
                    val passLimpia = password.trim()

                    if (emailLimpio.isEmpty() || passLimpia.isEmpty()) {
                        Toast.makeText(context, "Por favor, introduce tu correo y contraseña", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!emailLimpio.contains("@")) {
                        Toast.makeText(context, "Por favor, introduce un correo electrónico válido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (passLimpia.length < 6) {
                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    cargando = true
                    onLoginClick(emailLimpio, passLimpia) {
                        // Callback onError: Restablece el botón si hay un error en Firebase
                        cargando = false
                    }
                },
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = azulPrincipal,
                    disabledContainerColor = azulPrincipal.copy(alpha = 0.6f)
                )
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Iniciar Sesión",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text("¿Aún no tienes cuenta? ", fontSize = 13.sp, color = textoSecundario)
                Text(
                    text = "Regístrate aquí",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = verdeBoton,
                    modifier = Modifier.clickable(enabled = !cargando) { onIrANuevoRegistroClick() }
                )
            }
        }
    }
}