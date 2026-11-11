package com.example.radiopatio.registroInicio

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
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
import androidx.compose.ui.platform.LocalContext
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
 * Datos de comunidad seleccionados o introducidos durante el registro de un nuevo usuario.
 */
data class DatosRegistroComunidad(
    val crearNueva: Boolean = false,
    val solicitarTraspaso: Boolean = false,
    val comunidadExistente: Comunidad? = null,
    val nuevaComunidadNombre: String = "",
    val nuevaComunidadDireccion: String = "",
    val nuevaComunidadCiudad: String = "",
    val nuevaComunidadCp: String = "",
    val nuevaComunidadTipo: String = Comunidad.TIPO_URBANIZACION,
    val nuevaComunidadPisos: Int = 0,
    val nuevaComunidadDescripcion: String = ""
) {
    fun estaConfigurada(esAdministrador: Boolean = false): Boolean {
        val pisosValidos = !nuevaComunidadTipo.equals(Comunidad.TIPO_EDIFICIO, ignoreCase = true) ||
            nuevaComunidadPisos > 0
        return when {
            crearNueva -> esAdministrador &&
                nuevaComunidadNombre.isNotBlank() &&
                nuevaComunidadDireccion.isNotBlank() &&
                pisosValidos
            esAdministrador -> true
            else -> comunidadExistente != null && !solicitarTraspaso
        }
    }
}

/**
 * Pantalla para el registro de nuevos usuarios (Vecino/a o Administrador de fincas)
 * con selector unificado de comunidad, repetición de contraseña y validaciones completas.
 */
@Composable
fun RegistroNuevoUsuario(
    datosComunidad: DatosRegistroComunidad = DatosRegistroComunidad(),
    esAdministrador: Boolean = false,
    cargando: Boolean = false,
    onTipoCuentaChange: (Boolean) -> Unit = {},
    onAbrirBuscadorComunidades: () -> Unit = {},
    onRegisterClick: (
        nombre: String,
        apellidos: String,
        vivienda: String,
        email: String,
        pass: String,
        photoUri: Uri?,
        esAdministrador: Boolean,
        datosComunidad: DatosRegistroComunidad
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onVolverClick: () -> Unit = {}
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    val context = LocalContext.current

    var nombre by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var vivienda by rememberSaveable { mutableStateOf("") }
    val comExistenteEdificio = datosComunidad.comunidadExistente
    val edificioConPisos = !esAdministrador &&
        !datosComunidad.crearNueva &&
        comExistenteEdificio?.esEdificio == true &&
        comExistenteEdificio.numeroPisos > 0
    val maxPisosEdificio = if (edificioConPisos) comExistenteEdificio?.numeroPisos ?: 0 else 0
    var pisoSeleccionado by rememberSaveable(comExistenteEdificio?.id, maxPisosEdificio) {
        mutableStateOf(if (maxPisosEdificio > 0) 1 else 0)
    }
    var puertaEdificio by rememberSaveable(comExistenteEdificio?.id) { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var aceptoNormas by rememberSaveable { mutableStateOf(true) }
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = photoUriString?.let { Uri.parse(it) }

    val viviendaValida = when {
        esAdministrador -> true
        edificioConPisos -> pisoSeleccionado in 1..maxPisosEdificio
        else -> vivienda.isNotBlank()
    }

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
                        text = if (esAdministrador) {
                            "Registro de Administrador"
                        } else {
                            "Registro de Nuevo Vecino"
                        },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de tipo de cuenta: Vecino/a vs Administrador de fincas
            SelectorTipoCuentaRegistro(
                esAdministrador = esAdministrador,
                onSeleccionar = onTipoCuentaChange
            )

            Spacer(modifier = Modifier.height(12.dp))

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

                // Selector unificado que abre la ventana de búsqueda avanzada de comunidades
                SelectorComunidadRegistro(
                    datosComunidad = datosComunidad,
                    esAdministrador = esAdministrador,
                    onClick = onAbrirBuscadorComunidades
                )

                // Campo Vivienda / Despacho o selector de pisos si el administrador fijó pisos del edificio
                if (edificioConPisos) {
                    SelectorPisoEdificioRegistro(
                        numeroPisos = maxPisosEdificio,
                        pisoSeleccionado = pisoSeleccionado,
                        puerta = puertaEdificio,
                        onPisoChange = { pisoSeleccionado = it },
                        onPuertaChange = { puertaEdificio = it }
                    )
                } else {
                    CampoEntradaTexto(
                        value = vivienda,
                        onValueChange = { vivienda = it },
                        placeholder = if (esAdministrador) {
                            "Despacho o vivienda (opcional)"
                        } else {
                            "Piso / Vivienda (ej. Portal 1, 2ºA)"
                        },
                        icono = Icons.Outlined.Home,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CampoEntradaTexto(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Correo electrónico",
                    icono = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo Contraseña
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

                // Campo Repetir Contraseña
                CampoEntradaTexto(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Repetir contraseña",
                    icono = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible },
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

            // Botón Crear mi cuenta con validaciones de campos y contraseñas
            Button(
                onClick = {
                    val nombreLimpio = nombre.trim()
                    val apellidosLimpios = apellidos.trim()
                    val emailLimpio = email.trim()
                    val passLimpia = password.trim()
                    val confirmPassLimpia = confirmPassword.trim()

                    // 1. Validar campos obligatorios vacíos
                    if (nombreLimpio.isEmpty() || !viviendaValida || emailLimpio.isEmpty() || passLimpia.isEmpty() || confirmPassLimpia.isEmpty()) {
                        Toast.makeText(context, "Por favor, rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 2. Validar comunidad seleccionada o configurada según tipo de cuenta
                    if (!datosComunidad.estaConfigurada(esAdministrador)) {
                        Toast.makeText(context, "Por favor, selecciona o configura tu comunidad", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 3. Validar aceptación de las normas
                    if (!aceptoNormas) {
                        Toast.makeText(context, "Debes aceptar las normas de la comunidad", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 4. Validar formato de correo electrónico
                    if (!emailLimpio.contains("@")) {
                        Toast.makeText(context, "Por favor, introduce un correo electrónico válido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 5. Validar longitud de la contraseña
                    if (passLimpia.length < 6) {
                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 6. Validar que ambas contraseñas coincidan exactamente
                    if (passLimpia != confirmPassLimpia) {
                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val viviendaFinal = when {
                        edificioConPisos -> {
                            val puertaLimpia = puertaEdificio.trim()
                            if (puertaLimpia.isNotBlank()) "${pisoSeleccionado}º $puertaLimpia" else "${pisoSeleccionado}º"
                        }
                        vivienda.trim().isNotBlank() -> vivienda.trim()
                        esAdministrador -> "Administración"
                        else -> ""
                    }

                    onRegisterClick(
                        nombreLimpio,
                        apellidosLimpios,
                        viviendaFinal,
                        emailLimpio,
                        passLimpia,
                        photoUri,
                        esAdministrador,
                        datosComunidad
                    )
                },
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = verdeBoton)
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
private fun SelectorTipoCuentaRegistro(
    esAdministrador: Boolean,
    onSeleccionar: (Boolean) -> Unit
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoSuave = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Tipo de cuenta",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(false, "Vecino/a", Icons.Outlined.Person),
                Triple(true, "Admin. de fincas", Icons.Outlined.AdminPanelSettings)
            ).forEach { (esAdminOpcion, etiqueta, icono) ->
                val seleccionado = esAdministrador == esAdminOpcion
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSeleccionar(esAdminOpcion) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (seleccionado) Color(0xFFEAF5EC) else fondoSuave,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (seleccionado) verdeBoton else bordeGris
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = if (seleccionado) verdeBoton else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = etiqueta,
                            fontSize = 12.sp,
                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                            color = if (seleccionado) azulPrincipal else Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorPisoEdificioRegistro(
    numeroPisos: Int,
    pisoSeleccionado: Int,
    puerta: String,
    onPisoChange: (Int) -> Unit,
    onPuertaChange: (String) -> Unit
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(fondoGrisClaro)
            .border(1.dp, bordeGris, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Selecciona tu piso (1º - ${numeroPisos}º) *",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = azulPrincipal
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items((1..numeroPisos).toList()) { piso ->
                val seleccionado = piso == pisoSeleccionado
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (seleccionado) verdeBoton else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (seleccionado) verdeBoton else bordeGris
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPisoChange(piso) }
                ) {
                    Text(
                        text = "${piso}º",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (seleccionado) Color.White else azulPrincipal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        CampoEntradaTexto(
            value = puerta,
            onValueChange = onPuertaChange,
            placeholder = "Puerta / Letra (opcional, ej. A o Izq)",
            icono = Icons.Outlined.Home,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SelectorComunidadRegistro(
    datosComunidad: DatosRegistroComunidad,
    esAdministrador: Boolean,
    onClick: () -> Unit
) {
    val azulPrincipal = Color(0xFF133E54)
    val verdeBoton = Color(0xFF0F8A5F)
    val fondoGrisClaro = Color(0xFFF4F6F8)
    val bordeGris = Color(0xFFE2E8F0)
    val textoSecundario = Color(0xFF64748B)

    val tieneSeleccionConcreta = datosComunidad.crearNueva || datosComunidad.comunidadExistente != null
    val titulo = when {
        datosComunidad.crearNueva && datosComunidad.nuevaComunidadNombre.isNotBlank() ->
            "Nueva: ${datosComunidad.nuevaComunidadNombre}"
        datosComunidad.solicitarTraspaso && datosComunidad.comunidadExistente != null ->
            "Solicitar: ${datosComunidad.comunidadExistente.nombre}"
        datosComunidad.comunidadExistente != null ->
            datosComunidad.comunidadExistente.nombre
        esAdministrador -> "Crear o buscar comunidad (opcional)"
        else -> "Buscar tu comunidad"
    }
    val subtitulo = when {
        datosComunidad.crearNueva && datosComunidad.nuevaComunidadDireccion.isNotBlank() ->
            listOf(
                datosComunidad.nuevaComunidadDireccion,
                datosComunidad.nuevaComunidadCiudad,
                if (datosComunidad.nuevaComunidadTipo.equals(Comunidad.TIPO_EDIFICIO, true) &&
                    datosComunidad.nuevaComunidadPisos > 0
                ) {
                    "${datosComunidad.nuevaComunidadPisos} pisos"
                } else ""
            ).filter { it.isNotBlank() }.joinToString(" · ")
        datosComunidad.solicitarTraspaso && datosComunidad.comunidadExistente != null ->
            "Se enviará solicitud al administrador actual"
        datosComunidad.comunidadExistente != null -> {
            val base = datosComunidad.comunidadExistente.direccionCompleta()
            val detallePisos = if (datosComunidad.comunidadExistente.esEdificio &&
                datosComunidad.comunidadExistente.numeroPisos > 0
            ) {
                "$base · ${datosComunidad.comunidadExistente.numeroPisos} pisos"
            } else {
                base
            }
            if (esAdministrador) detallePisos else "$detallePisos · Solicitud al administrador"
        }
        esAdministrador -> "Puedes crear tu 1ª comunidad, solicitar una o entrar sin ella"
        else -> "Pulsa para buscar con filtros y elegir tu comunidad"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (tieneSeleccionConcreta) Color(0xFFEAF5EC) else fondoGrisClaro)
            .border(
                width = 1.dp,
                color = if (tieneSeleccionConcreta) Color(0xFF389F82) else bordeGris,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (tieneSeleccionConcreta) Icons.Outlined.Apartment else Icons.Outlined.Search,
            contentDescription = null,
            tint = if (tieneSeleccionConcreta) verdeBoton else textoSecundario,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = if (tieneSeleccionConcreta) FontWeight.Bold else FontWeight.Medium,
                color = if (tieneSeleccionConcreta) azulPrincipal else textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitulo,
                fontSize = 11.sp,
                color = if (tieneSeleccionConcreta) Color(0xFF455A64) else textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Abrir buscador de comunidades",
            tint = if (tieneSeleccionConcreta) verdeBoton else textoSecundario
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
                    nombre = "Urbanización Montecerrao",
                    direccion = "Calle Fuertes Acevedo, 42",
                    ciudad = "Oviedo",
                    tipo = Comunidad.TIPO_URBANIZACION
                )
            )
        )
    }
}
