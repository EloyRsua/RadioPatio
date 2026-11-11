package com.example.radiopatio.registroInicio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.radiopatio.R
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.ui.theme.RadioPatioTheme

/**
 * Datos de comunidad seleccionados o introducidos durante el registro de un nuevo vecino.
 */
data class DatosRegistroComunidad(
    val crearNueva: Boolean = false,
    val comunidadExistente: Comunidad? = null,
    val nuevaComunidadNombre: String = "",
    val nuevaComunidadDireccion: String = "",
    val nuevaComunidadCiudad: String = "",
    val nuevaComunidadCp: String = "",
    val nuevaComunidadTipo: String = "Residencial",
    val nuevaComunidadDescripcion: String = ""
) {
    fun estaConfigurada(): Boolean {
        return if (crearNueva) {
            nuevaComunidadNombre.isNotBlank() && nuevaComunidadDireccion.isNotBlank()
        } else {
            comunidadExistente != null
        }
    }
}

/**
 * Pantalla para el registro de nuevos usuarios con datos de vivienda y selector unificado de comunidad.
 */
@Composable
fun RegistroNuevoUsuario(
    datosComunidad: DatosRegistroComunidad = DatosRegistroComunidad(),
    cargando: Boolean = false,
    onAbrirBuscadorComunidades: () -> Unit = {},
    onRegisterClick: (
        nombre: String,
        apellidos: String,
        vivienda: String,
        email: String,
        pass: String,
        photoUri: Uri?,
        datosComunidad: DatosRegistroComunidad
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onVolverClick: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    var nombre by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var vivienda by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var aceptoNormas by rememberSaveable { mutableStateOf(true) }
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = photoUriString?.let { Uri.parse(it) }

    val formularioValido = !cargando &&
        aceptoNormas &&
        nombre.isNotBlank() &&
        vivienda.isNotBlank() &&
        email.isNotBlank() &&
        password.length >= 6 &&
        datosComunidad.estaConfigurada()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUriString = uri?.toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF2))
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_registro),
            contentDescription = "Cabecera del barrio",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera con selector de Foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RadioPatio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = azulPrincipal
                    )
                    Text(
                        text = "Registro de Nuevo Vecino",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Avatar interactivo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, verdeBoton, CircleShape)
                        .background(fondoGrisClaro)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Añadir foto",
                                tint = verdeBoton,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Foto",
                                fontSize = 9.sp,
                                color = textoSecundario,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Formulario
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampoEntradaTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = "Nombre",
                        icono = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f)
                    )
                    CampoEntradaTexto(
                        value = apellidos,
                        onValueChange = { apellidos = it },
                        placeholder = "Apellidos",
                        icono = null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo Vivienda / Piso
                CampoEntradaTexto(
                    value = vivienda,
                    onValueChange = { vivienda = it },
                    placeholder = "Piso / Vivienda (ej. Portal 1, 2ºA)",
                    icono = Icons.Outlined.Home,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector unificado que abre la ventana de búsqueda avanzada de comunidades
                SelectorComunidadRegistro(
                    datosComunidad = datosComunidad,
                    onClick = onAbrirBuscadorComunidades
                )

                CampoEntradaTexto(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Correo electrónico",
                    icono = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                CampoEntradaTexto(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Contraseña (mín. 6 caracteres)",
                    icono = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aceptoNormas = !aceptoNormas }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (aceptoNormas) verdeBoton else fondoGrisClaro)
                            .border(1.dp, if (aceptoNormas) verdeBoton else bordeGris, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (aceptoNormas) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Acepto las normas de la comunidad",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (formularioValido) {
                        onRegisterClick(
                            nombre.trim(),
                            apellidos.trim(),
                            vivienda.trim(),
                            email.trim(),
                            password,
                            photoUri,
                            datosComunidad
                        )
                    }
                },
                enabled = formularioValido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = verdeBoton,
                    disabledContainerColor = verdeBoton.copy(alpha = 0.5f)
                )
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Crear mi cuenta",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Volver atrás",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textoSecundario,
                modifier = Modifier.clickable(enabled = !cargando) { onVolverClick() }
            )
        }
    }
}

@Composable
private fun SelectorComunidadRegistro(
    datosComunidad: DatosRegistroComunidad,
    onClick: () -> Unit
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    val seleccionada = datosComunidad.estaConfigurada()
    val titulo = when {
        datosComunidad.crearNueva && datosComunidad.nuevaComunidadNombre.isNotBlank() ->
            "Nueva: ${datosComunidad.nuevaComunidadNombre}"
        datosComunidad.comunidadExistente != null ->
            datosComunidad.comunidadExistente.nombre
        else -> "Buscar o crear tu comunidad"
    }
    val subtitulo = when {
        datosComunidad.crearNueva && datosComunidad.nuevaComunidadDireccion.isNotBlank() ->
            listOf(
                datosComunidad.nuevaComunidadDireccion,
                datosComunidad.nuevaComunidadCiudad
            ).filter { it.isNotBlank() }.joinToString(" · ")
        datosComunidad.comunidadExistente != null ->
            datosComunidad.comunidadExistente.direccionCompleta()
        else -> "Pulsa para buscar con filtros o dar de alta una"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (seleccionada) Color(0xFFEAF5EC) else fondoGrisClaro)
            .border(
                width = 1.dp,
                color = if (seleccionada) Color(0xFF389F82) else bordeGris,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (seleccionada) Icons.Outlined.Apartment else Icons.Outlined.Search,
            contentDescription = null,
            tint = if (seleccionada) verdeBoton else textoSecundario,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                color = if (seleccionada) azulPrincipal else textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitulo,
                fontSize = 11.sp,
                color = if (seleccionada) Color(0xFF455A64) else textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Abrir buscador de comunidades",
            tint = if (seleccionada) verdeBoton else textoSecundario
        )
    }
}

@Composable
private fun CampoEntradaTexto(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, fontSize = 14.sp, color = textoSecundario) },
        leadingIcon = if (icono != null) {
            { Icon(imageVector = icono, contentDescription = null, tint = textoSecundario, modifier = Modifier.size(20.dp)) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Ver contraseña",
                        tint = textoSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fondoGrisClaro,
            unfocusedContainerColor = fondoGrisClaro,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .border(1.dp, bordeGris, RoundedCornerShape(25.dp))
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistroNuevoUsuarioPreview() {
    RadioPatioTheme {
        RegistroNuevoUsuario(
            datosComunidad = DatosRegistroComunidad(
                comunidadExistente = Comunidad(
                    id = "com_1",
                    nombre = "Residencial Montecerrao",
                    direccion = "Calle Fuertes Acevedo, 42",
                    ciudad = "Oviedo"
                )
            )
        )
    }
}
