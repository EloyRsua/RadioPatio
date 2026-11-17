package com.example.radiopatio.comunidad

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiopatio.ui.theme.RadioPatioTheme

private val AzulPrincipal = Color(0xFF133E54)
private val VerdeAccion = Color(0xFF389F82)
private val VerdeOscuro = Color(0xFF0F8A5F)
private val VerdeSuave = Color(0xFFEAF5EC)
private val FondoPantalla = Color(0xFFF9FAFB)
private val BordeSuave = Color(0xFFD0D7DE)

/**
 * Pantalla completa para crear una nueva comunidad (Edificio o Urbanización).
 * Disponible únicamente para cuentas con rol de Administrador de fincas.
 */
@Composable
fun CrearComunidadScreen(
    modoRegistro: Boolean,
    esAdministradorRegistro: Boolean = false,
    nombreInicial: String = "",
    onVolver: () -> Unit,
    onNuevaComunidadEnRegistro: (NuevaComunidadFormulario) -> Unit = {},
    onComunidadCreada: () -> Unit = {},
    viewModel: BuscadorComunidadesViewModel = viewModel(factory = BuscadorComunidadesViewModelFactory())
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    val puedeCrear = uiState.puedeCrearComunidad(
        modoRegistro = modoRegistro,
        esAdministradorRegistro = esAdministradorRegistro
    )

    when {
        !modoRegistro && uiState.cargando -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoPantalla),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VerdeAccion)
            }
        }

        !puedeCrear -> {
            PantallaSinPermisoCreacion(onVolver = onVolver)
        }

        else -> {
            CrearComunidadContent(
                nombreInicial = nombreInicial,
                modoRegistro = modoRegistro,
                procesando = uiState.procesando,
                onVolver = { if (!uiState.procesando) onVolver() },
                onConfirmar = { formulario ->
                    if (modoRegistro) {
                        onNuevaComunidadEnRegistro(formulario)
                    } else {
                        viewModel.onCrearYUnirseAComunidad(
                            nombre = formulario.nombre,
                            direccion = formulario.direccion,
                            ciudad = formulario.ciudad,
                            codigoPostal = formulario.codigoPostal,
                            tipo = formulario.tipo,
                            numeroPisos = formulario.numeroPisos,
                            descripcion = formulario.descripcion
                        ) {
                            onComunidadCreada()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CrearComunidadContent(
    nombreInicial: String = "",
    modoRegistro: Boolean = false,
    procesando: Boolean = false,
    onVolver: () -> Unit = {},
    onConfirmar: (NuevaComunidadFormulario) -> Unit = {}
) {
    var nombre by rememberSaveable(nombreInicial) { mutableStateOf(nombreInicial) }
    var direccion by rememberSaveable { mutableStateOf("") }
    var ciudad by rememberSaveable { mutableStateOf("") }
    var codigoPostal by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(Comunidad.TIPO_URBANIZACION) }
    var numeroPisosTexto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }

    val esEdificio = tipo.equals(Comunidad.TIPO_EDIFICIO, ignoreCase = true)
    val numeroPisosValido = numeroPisosTexto.toIntOrNull()?.takeIf { it > 0 }
    val formularioValido = nombre.isNotBlank() &&
        direccion.isNotBlank() &&
        (!esEdificio || numeroPisosValido != null)

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VerdeOscuro,
        unfocusedBorderColor = BordeSuave,
        focusedLabelColor = VerdeOscuro
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = FondoPantalla,
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onVolver, enabled = !procesando) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nueva comunidad",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrincipal
                        )
                        Text(
                            text = if (modoRegistro) {
                                "Configuración inicial de tu primera comunidad"
                            } else {
                                "Alta exclusiva para Administradores de fincas"
                            },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp, color = Color.White) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirmar(
                                NuevaComunidadFormulario(
                                    nombre = nombre.trim(),
                                    direccion = direccion.trim(),
                                    ciudad = ciudad.trim(),
                                    codigoPostal = codigoPostal.trim(),
                                    tipo = tipo,
                                    numeroPisos = if (esEdificio) (numeroPisosValido ?: 0) else 0,
                                    descripcion = descripcion.trim()
                                )
                            )
                        },
                        enabled = !procesando && formularioValido,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Guardar comunidad",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bloque 1: Tipo de comunidad y pisos si es Edificio
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tipo de comunidad *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Comunidad.TIPOS_COMUNIDAD.forEach { itemTipo ->
                            val seleccionado = tipo == itemTipo
                            TarjetaTipoComunidad(
                                titulo = itemTipo,
                                icono = if (itemTipo == Comunidad.TIPO_EDIFICIO) {
                                    Icons.Outlined.Apartment
                                } else {
                                    Icons.Outlined.Home
                                },
                                seleccionado = seleccionado,
                                habilitado = !procesando,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    tipo = itemTipo
                                    if (!itemTipo.equals(Comunidad.TIPO_EDIFICIO, ignoreCase = true)) {
                                        numeroPisosTexto = ""
                                    }
                                }
                            )
                        }
                    }

                    Text(
                        text = if (esEdificio) {
                            "Dictamina el número de pisos disponibles para que los vecinos puedan elegir su vivienda al registrarse."
                        } else {
                            "Incluye conjuntos residenciales, urbanizaciones y mancomunidades."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF607D8B)
                    )

                    if (esEdificio) {
                        OutlinedTextField(
                            value = numeroPisosTexto,
                            onValueChange = { nuevo ->
                                numeroPisosTexto = nuevo.filter { it.isDigit() }.take(3)
                            },
                            label = { Text("Número de pisos del edificio *") },
                            placeholder = { Text("Ej: 6") },
                            supportingText = {
                                Text(
                                    text = "Campo obligatorio para edificios (mínimo 1 piso).",
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            enabled = !procesando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bloque 2: Datos generales de la comunidad
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Datos de la comunidad",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre de la comunidad *") },
                        placeholder = {
                            Text(
                                if (esEdificio) {
                                    "Ej: Edificio Fuertes Acevedo 42"
                                } else {
                                    "Ej: Urbanización Montecerrao"
                                }
                            )
                        },
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección *") },
                        placeholder = { Text("Ej: Calle Fuertes Acevedo, 42") },
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ciudad,
                            onValueChange = { ciudad = it },
                            label = { Text("Ciudad") },
                            placeholder = { Text("Oviedo") },
                            singleLine = true,
                            enabled = !procesando,
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = codigoPostal,
                            onValueChange = { codigoPostal = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("C.P.") },
                            placeholder = { Text("33006") },
                            singleLine = true,
                            enabled = !procesando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        placeholder = { Text("Información general visible para tus vecinos") },
                        minLines = 3,
                        maxLines = 5,
                        enabled = !procesando,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bloque informativo de administración
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VerdeSuave,
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (modoRegistro) {
                            "La comunidad se creará al finalizar el registro y quedarás asignado como su Administrador de fincas."
                        } else {
                            "Quedarás asignado como el Administrador de fincas de esta comunidad y pasará a ser tu comunidad activa."
                        },
                        fontSize = 12.sp,
                        color = AzulPrincipal
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaTipoComunidad(
    titulo: String,
    icono: ImageVector,
    seleccionado: Boolean,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) VerdeSuave else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (seleccionado) 1.5.dp else 1.dp,
            color = if (seleccionado) VerdeOscuro else Color(0xFFE2E8F0)
        ),
        modifier = modifier.clickable(enabled = habilitado, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = if (seleccionado) VerdeOscuro else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = titulo,
                    fontSize = 13.sp,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                    color = if (seleccionado) AzulPrincipal else Color(0xFF475569)
                )
            }
            if (seleccionado) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = VerdeOscuro,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PantallaSinPermisoCreacion(onVolver: () -> Unit) {
    Scaffold(
        containerColor = FondoPantalla,
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
                    }
                    Text(
                        text = "Nueva comunidad",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Solo los administradores de fincas pueden crear comunidades.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrincipal,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onVolver,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text("Volver", color = AzulPrincipal)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CrearComunidadContentPreview() {
    RadioPatioTheme {
        CrearComunidadContent()
    }
}

