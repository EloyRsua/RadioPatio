package com.example.radiopatio.vecinos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.SolicitudTraspasoAdmin
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.ui.theme.RadioPatioTheme

private val AzulPrincipal = Color(0xFF133E54)
private val VerdeAccion = Color(0xFF389F82)
private val VerdeOscuro = Color(0xFF0F8A5F)
private val VerdeSuave = Color(0xFFEAF5EC)
private val FondoPantalla = Color(0xFFF9FAFB)
private val BordeSuave = Color(0xFFECEFF1)

/**
 * Screen (Stateful): Conecta la interfaz de Vecinos y Comunidad con [VecinosViewModel]
 * sincronizado en tiempo real con Cloud Firestore.
 */
@Composable
fun VecinosScreen(
    onIrABuscarComunidad: () -> Unit = {},
    viewModel: VecinosViewModel = viewModel(factory = VecinosViewModelFactory())
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    LaunchedEffect(uiState.mensajeExito) {
        uiState.mensajeExito?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onLimpiarMensajeExito()
        }
    }

    VecinosContent(
        uiState = uiState,
        onTextoBusquedaChange = viewModel::onTextoBusquedaChange,
        onFiltroRolChange = viewModel::onFiltroRolChange,
        onIrABuscarComunidad = onIrABuscarComunidad,
        onAbrirDialogoTransferirPresidencia = viewModel::onAbrirDialogoTransferirPresidencia,
        onCerrarDialogoTransferirPresidencia = viewModel::onCerrarDialogoTransferirPresidencia,
        onConfirmarTransferenciaPresidencia = viewModel::onTransferirPresidencia,
        onAbrirDialogoEditarPisos = viewModel::onAbrirDialogoEditarPisos,
        onCerrarDialogoEditarPisos = viewModel::onCerrarDialogoEditarPisos,
        onActualizarNumeroPisos = viewModel::onActualizarNumeroPisos,
        onCambiarComunidadAdministrada = viewModel::onCambiarComunidadAdministrada,
        onAceptarSolicitudAdmin = viewModel::onAceptarSolicitudAdmin,
        onRechazarSolicitudAdmin = viewModel::onRechazarSolicitudAdmin,
        onCancelarSolicitudEnviada = viewModel::onCancelarSolicitudEnviada
    )
}

/**
 * Content (Stateless): Renderiza los datos de la comunidad y el directorio de vecinos.
 */
@Composable
fun VecinosContent(
    uiState: VecinosUiState,
    onTextoBusquedaChange: (String) -> Unit,
    onFiltroRolChange: (String) -> Unit,
    onIrABuscarComunidad: () -> Unit = {},
    onAbrirDialogoTransferirPresidencia: () -> Unit = {},
    onCerrarDialogoTransferirPresidencia: () -> Unit = {},
    onConfirmarTransferenciaPresidencia: (Usuario) -> Unit = {},
    onAbrirDialogoEditarPisos: () -> Unit = {},
    onCerrarDialogoEditarPisos: () -> Unit = {},
    onActualizarNumeroPisos: (Int) -> Unit = {},
    onCambiarComunidadAdministrada: (Comunidad) -> Unit = {},
    onAceptarSolicitudAdmin: (SolicitudTraspasoAdmin) -> Unit = {},
    onRechazarSolicitudAdmin: (SolicitudTraspasoAdmin) -> Unit = {},
    onCancelarSolicitudEnviada: (Comunidad) -> Unit = {}
) {
    val vecinosFiltrados = uiState.vecinosFiltrados
    val candidatosPresidencia = uiState.candidatosPresidencia
    val esAdministrador = uiState.usuarioActual?.esAdministradorFincas == true

    Scaffold(
        topBar = {
            BarraSuperiorVecinos(
                nombreComunidad = uiState.comunidad?.nombre,
                totalVecinos = uiState.vecinos.size
            )
        }
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoPantalla)
                .padding(rellenoInterior)
        ) {
            when {
                uiState.cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VerdeAccion
                    )
                }

                uiState.comunidadIdActual.isBlank() -> {
                    TarjetaSinComunidad(
                        esAdministrador = esAdministrador,
                        solicitudesEnviadas = uiState.comunidadesSolicitadasPendientes,
                        procesandoSolicitud = uiState.procesandoSolicitudAdmin,
                        onSeleccionarOCrear = onIrABuscarComunidad,
                        onCancelarSolicitud = onCancelarSolicitudEnviada,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 0. Selector multi-comunidad exclusivo para Administrador de fincas
                        if (esAdministrador && uiState.comunidadesAdministradas.isNotEmpty()) {
                            item {
                                SelectorComunidadesAdministrador(
                                    comunidades = uiState.comunidadesAdministradas,
                                    comunidadActivaId = uiState.comunidadIdActual,
                                    procesandoCambio = uiState.procesandoCambioComunidad,
                                    onSeleccionarComunidad = onCambiarComunidadAdministrada,
                                    onGestionarOAgregar = onIrABuscarComunidad
                                )
                            }
                        }

                        // 0b. Solicitudes entrantes de traspaso (solo visibles para el administrador actual de esta comunidad)
                        if (uiState.esAdministradorActual && uiState.solicitudesTraspasoPendientes.isNotEmpty()) {
                            item {
                                TarjetaSolicitudesTraspasoAdmin(
                                    solicitudes = uiState.solicitudesTraspasoPendientes,
                                    procesando = uiState.procesandoSolicitudAdmin,
                                    onAceptar = onAceptarSolicitudAdmin,
                                    onRechazar = onRechazarSolicitudAdmin
                                )
                            }
                        }

                        // 0c. Solicitudes salientes enviadas a otras comunidades
                        if (esAdministrador && uiState.comunidadesSolicitadasPendientes.isNotEmpty()) {
                            item {
                                TarjetaSolicitudesEnviadasAdmin(
                                    comunidades = uiState.comunidadesSolicitadasPendientes,
                                    procesando = uiState.procesandoSolicitudAdmin,
                                    onCancelar = onCancelarSolicitudEnviada
                                )
                            }
                        }

                        // 1. Datos generales de la comunidad actual
                        item {
                            uiState.comunidad?.let { comunidad ->
                                TarjetaInfoComunidad(
                                    comunidad = comunidad,
                                    totalMiembrosReales = uiState.vecinos.size,
                                    esPresidenteActual = uiState.esPresidenteActual,
                                    esAdministradorCuenta = esAdministrador,
                                    esAdministradorDeEstaComunidad = uiState.esAdministradorActual,
                                    hayOtrosMiembros = candidatosPresidencia.isNotEmpty(),
                                    onCambiarComunidad = onIrABuscarComunidad,
                                    onTransferirPresidencia = onAbrirDialogoTransferirPresidencia,
                                    onEditarPisos = onAbrirDialogoEditarPisos
                                )
                            }
                        }

                        // 2. Buscador de vecinos dentro de la comunidad
                        item {
                            BuscadorVecinos(
                                textoBusqueda = uiState.textoBusqueda,
                                onTextoChange = onTextoBusquedaChange
                            )
                        }

                        // 3. Filtro por rol si existen múltiples roles
                        if (uiState.rolesDisponibles.size > 2) {
                            item {
                                FiltroRolesVecinos(
                                    roles = uiState.rolesDisponibles,
                                    rolSeleccionado = uiState.filtroRol,
                                    onRolSeleccionado = onFiltroRolChange
                                )
                            }
                        }

                        // 4. Cabecera de listado
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Miembros de la comunidad",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrincipal
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = VerdeSuave
                                ) {
                                    Text(
                                        text = "${vecinosFiltrados.size} vecino${if (vecinosFiltrados.size == 1) "" else "s"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VerdeOscuro,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // 5. Listado de vecinos o estado vacío
                        if (vecinosFiltrados.isEmpty()) {
                            item {
                                EstadoVecinosVacio(
                                    hayFiltro = uiState.textoBusqueda.isNotBlank() || uiState.filtroRol != "Todos",
                                    onLimpiarFiltros = {
                                        onTextoBusquedaChange("")
                                        onFiltroRolChange("Todos")
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = vecinosFiltrados,
                                key = { index, vecino ->
                                    vecino.uid.ifBlank { "${vecino.email}_${vecino.nombre}_$index" }
                                }
                            ) { _, vecino ->
                                TarjetaVecino(
                                    vecino = vecino,
                                    esUsuarioActual = uiState.usuarioActual?.uid?.isNotBlank() == true &&
                                        vecino.uid == uiState.usuarioActual.uid
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.mostrarDialogoTransferirPresidencia && uiState.esPresidenteActual) {
        DialogoTransferirPresidencia(
            nombreComunidad = uiState.comunidad?.nombre.orEmpty(),
            candidatos = candidatosPresidencia,
            procesando = uiState.procesandoTransferencia,
            onDismiss = onCerrarDialogoTransferirPresidencia,
            onConfirmar = onConfirmarTransferenciaPresidencia
        )
    }

    if (uiState.mostrarDialogoEditarPisos && uiState.esAdministradorActual && uiState.comunidad?.esEdificio == true) {
        DialogoEditarNumeroPisos(
            nombreEdificio = uiState.comunidad.nombre,
            pisosActuales = uiState.comunidad.numeroPisos,
            procesando = uiState.procesandoPisos,
            onDismiss = onCerrarDialogoEditarPisos,
            onConfirmar = onActualizarNumeroPisos
        )
    }
}

@Composable
private fun BarraSuperiorVecinos(
    nombreComunidad: String?,
    totalVecinos: Int
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vecinos y Comunidad",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
                Text(
                    text = if (!nombreComunidad.isNullOrBlank()) {
                        "$nombreComunidad · $totalVecinos miembro${if (totalVecinos == 1) "" else "s"}"
                    } else {
                        "Directorio y datos de tu comunidad"
                    },
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TarjetaInfoComunidad(
    comunidad: Comunidad,
    totalMiembrosReales: Int,
    esPresidenteActual: Boolean,
    esAdministradorCuenta: Boolean = false,
    esAdministradorDeEstaComunidad: Boolean = false,
    hayOtrosMiembros: Boolean,
    onCambiarComunidad: () -> Unit,
    onTransferirPresidencia: () -> Unit,
    onEditarPisos: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BordeSuave)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(VerdeSuave),
                    contentAlignment = Alignment.Center
                ) {
                    if (comunidad.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = comunidad.photoUrl,
                            contentDescription = comunidad.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Apartment,
                            contentDescription = null,
                            tint = VerdeOscuro,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comunidad.nombre.ifBlank { "Mi Comunidad" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrincipal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (comunidad.verificada) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Comunidad verificada",
                                tint = VerdeAccion,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF546E7A),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comunidad.direccionCompleta(),
                            fontSize = 12.sp,
                            color = Color(0xFF546E7A)
                        )
                    }
                }

                TextButton(
                    onClick = onCambiarComunidad,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (esAdministradorCuenta) "Gestionar" else "Cambiar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VerdeOscuro
                    )
                }
            }

            if (comunidad.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = comunidad.descripcion,
                    fontSize = 13.sp,
                    color = Color(0xFF455A64)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoComunidadPill(
                    icono = Icons.Outlined.Groups,
                    texto = "$totalMiembrosReales vecino${if (totalMiembrosReales == 1) "" else "s"}",
                    modifier = Modifier.weight(1f)
                )
                DatoComunidadPill(
                    icono = Icons.Outlined.Apartment,
                    texto = comunidad.tipo.ifBlank { "Residencial" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (comunidad.esEdificio) {
                Spacer(modifier = Modifier.height(8.dp))
                DatoComunidadPill(
                    icono = Icons.Outlined.Apartment,
                    texto = if (comunidad.numeroPisos > 0) {
                        "${comunidad.numeroPisos} piso${if (comunidad.numeroPisos == 1) "" else "s"}"
                    } else {
                        "Pisos sin definir"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (esAdministradorDeEstaComunidad && comunidad.esEdificio) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onEditarPisos,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = AzulPrincipal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (comunidad.numeroPisos > 0) {
                            "Editar pisos del edificio (${comunidad.numeroPisos})"
                        } else {
                            "Dictaminar pisos del edificio"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AzulPrincipal
                    )
                }
            }

            if (esPresidenteActual) {
                Spacer(modifier = Modifier.height(10.dp))
                if (hayOtrosMiembros) {
                    OutlinedButton(
                        onClick = onTransferirPresidencia,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = AzulPrincipal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transferir presidencia",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AzulPrincipal
                        )
                    }
                } else {
                    Text(
                        text = "Eres el presidente. Cuando se unan otros vecinos podrás transferir la presidencia.",
                        fontSize = 11.sp,
                        color = Color(0xFF607D8B)
                    )
                }
            }
        }
    }
}

@Composable
private fun DatoComunidadPill(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF4F7F9))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = AzulPrincipal,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AzulPrincipal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BuscadorVecinos(
    textoBusqueda: String,
    onTextoChange: (String) -> Unit
) {
    OutlinedTextField(
        value = textoBusqueda,
        onValueChange = onTextoChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Buscar por nombre, piso o rol...",
                color = Color.Gray,
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar vecino",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (textoBusqueda.isNotBlank()) {
                IconButton(onClick = { onTextoChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = AzulPrincipal,
            unfocusedBorderColor = Color(0xFFD6DDE1),
            cursorColor = AzulPrincipal
        )
    )
}

@Composable
private fun FiltroRolesVecinos(
    roles: List<String>,
    rolSeleccionado: String,
    onRolSeleccionado: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(roles) { rol ->
            val activo = rol.equals(rolSeleccionado, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (activo) AzulPrincipal else Color.White,
                border = if (activo) null else CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BordeSuave)
                ),
                modifier = Modifier.clickable { onRolSeleccionado(rol) }
            ) {
                Text(
                    text = rol,
                    fontSize = 12.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                    color = if (activo) Color.White else Color(0xFF37474F),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaVecino(
    vecino: Usuario,
    esUsuarioActual: Boolean
) {
    val esDirectiva = vecino.rol.contains("Presiden", ignoreCase = true) ||
        vecino.rol.contains("Admin", ignoreCase = true) ||
        vecino.rol.contains("Vocal", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BordeSuave)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarVecino(
                vecino = vecino,
                tamano = 48.dp,
                tamanoTexto = 18.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = vecino.nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (esUsuarioActual) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VerdeSuave
                        ) {
                            Text(
                                text = "Tú",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VerdeOscuro,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        tint = Color(0xFF607D8B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = vecino.vivienda.ifBlank { "Piso sin especificar" },
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Etiqueta de rol
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (esDirectiva) VerdeSuave else Color(0xFFEFF3F6)
            ) {
                Text(
                    text = vecino.rol.ifBlank { "Vecino/a" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esDirectiva) VerdeOscuro else AzulPrincipal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SelectorComunidadesAdministrador(
    comunidades: List<Comunidad>,
    comunidadActivaId: String,
    procesandoCambio: Boolean,
    onSeleccionarComunidad: (Comunidad) -> Unit,
    onGestionarOAgregar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BordeSuave)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comunidades administradas (${comunidades.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )
                }
                TextButton(
                    onClick = onGestionarOAgregar,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Nueva / Solicitar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeOscuro
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(comunidades, key = { it.id }) { comunidad ->
                    val esActiva = comunidad.id == comunidadActivaId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (esActiva) AzulPrincipal else VerdeSuave,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = !procesandoCambio && !esActiva) {
                                onSeleccionarComunidad(comunidad)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = comunidad.nombre.ifBlank { comunidad.id },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (esActiva) Color.White else AzulPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudesTraspasoAdmin(
    solicitudes: List<SolicitudTraspasoAdmin>,
    procesando: Boolean,
    onAceptar: (SolicitudTraspasoAdmin) -> Unit,
    onRechazar: (SolicitudTraspasoAdmin) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Solicitudes de traspaso de administración (${solicitudes.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }

            Text(
                text = "Si aceptas el cambio, el solicitante pasará a ser el único administrador de esta comunidad y tú dejarás de gestionarla.",
                fontSize = 12.sp,
                color = Color(0xFF78350F)
            )

            solicitudes.forEach { solicitud ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = solicitud.solicitanteNombre,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrincipal
                            )
                            if (solicitud.solicitanteEmail.isNotBlank()) {
                                Text(
                                    text = solicitud.solicitanteEmail,
                                    fontSize = 12.sp,
                                    color = Color(0xFF607D8B)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRechazar(solicitud) },
                                enabled = !procesando,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rechazar", color = Color(0xFFD32F2F), fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onAceptar(solicitud) },
                                enabled = !procesando,
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aceptar traspaso", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudesEnviadasAdmin(
    comunidades: List<Comunidad>,
    procesando: Boolean,
    onCancelar: (Comunidad) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BordeSuave)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Solicitudes pendientes de aceptación (${comunidades.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrincipal
            )
            comunidades.forEach { com ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = com.nombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AzulPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Esperando confirmación del administrador actual",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    TextButton(
                        onClick = { onCancelar(com) },
                        enabled = !procesando
                    ) {
                        Text("Cancelar", fontSize = 12.sp, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSinComunidad(
    esAdministrador: Boolean,
    solicitudesEnviadas: List<Comunidad>,
    procesandoSolicitud: Boolean,
    onSeleccionarOCrear: () -> Unit,
    onCancelarSolicitud: (Comunidad) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VerdeSuave),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Apartment,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (esAdministrador) {
                        "Sin comunidades asignadas todavía"
                    } else {
                        "Aún no perteneces a una comunidad"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (esAdministrador) {
                        "Crea una nueva comunidad o solicita el traspaso de administración de una existente."
                    } else {
                        "Únete a tu edificio o urbanización para ver a tus vecinos, publicaciones del tablón, mercadillo y eventos."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onSeleccionarOCrear,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (esAdministrador) "Crear o buscar comunidad" else "Buscar y unirme a comunidad",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (esAdministrador && solicitudesEnviadas.isNotEmpty()) {
            TarjetaSolicitudesEnviadasAdmin(
                comunidades = solicitudesEnviadas,
                procesando = procesandoSolicitud,
                onCancelar = onCancelarSolicitud
            )
        }
    }
}

@Composable
private fun EstadoVecinosVacio(
    hayFiltro: Boolean,
    onLimpiarFiltros: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonSearch,
            contentDescription = null,
            tint = Color(0xFF90A4AE),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hayFiltro) "No hay vecinos que coincidan" else "Aún no hay vecinos registrados",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AzulPrincipal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hayFiltro) {
                "Prueba a cambiar el término de búsqueda o el filtro de rol."
            } else {
                "Cuando otros vecinos se unan a esta comunidad aparecerán aquí en tiempo real."
            },
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        if (hayFiltro) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLimpiarFiltros,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Limpiar filtros", color = AzulPrincipal, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DialogoTransferirPresidencia(
    nombreComunidad: String,
    candidatos: List<Usuario>,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (Usuario) -> Unit
) {
    var seleccionado by remember(candidatos) { mutableStateOf<Usuario?>(null) }

    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    tint = VerdeOscuro,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transferir presidencia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AzulPrincipal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona al vecino de ${nombreComunidad.ifBlank { "tu comunidad" }} que asumirá la presidencia. Tú pasarás a tener el rol de Vecino/a.",
                    fontSize = 12.sp,
                    color = Color(0xFF546E7A)
                )

                candidatos.forEach { vecino ->
                    val esElegido = seleccionado?.uid == vecino.uid
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (esElegido) VerdeSuave else Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (esElegido) VerdeAccion else BordeSuave,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !procesando) { seleccionado = vecino }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarVecino(
                                vecino = vecino,
                                tamano = 36.dp,
                                tamanoTexto = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vecino.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AzulPrincipal
                                )
                                Text(
                                    text = "${vecino.vivienda} · ${vecino.rol}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF607D8B)
                                )
                            }
                            if (esElegido) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = VerdeOscuro,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { seleccionado?.let(onConfirmar) },
                enabled = !procesando && seleccionado != null,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
            ) {
                if (procesando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirmar transferencia")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !procesando) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun DialogoEditarNumeroPisos(
    nombreEdificio: String,
    pisosActuales: Int,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var pisosTexto by remember(pisosActuales) {
        mutableStateOf(if (pisosActuales > 0) pisosActuales.toString() else "")
    }
    val pisosValido = pisosTexto.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        containerColor = Color.White,
        title = {
            Text(
                text = "Pisos del edificio",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AzulPrincipal
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Como administrador de \"$nombreEdificio\", dictamina el número de pisos disponibles para los vecinos del edificio.",
                    fontSize = 13.sp,
                    color = Color(0xFF546E7A)
                )
                OutlinedTextField(
                    value = pisosTexto,
                    onValueChange = { nuevo ->
                        pisosTexto = nuevo.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Número de pisos *") },
                    placeholder = { Text("Ej. 6") },
                    singleLine = true,
                    enabled = !procesando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeOscuro,
                        unfocusedBorderColor = Color(0xFFD0D7DE)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { pisosValido?.let(onConfirmar) },
                enabled = !procesando && pisosValido != null,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
            ) {
                if (procesando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !procesando) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun AvatarVecino(
    vecino: Usuario,
    tamano: Dp,
    tamanoTexto: TextUnit
) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(AzulPrincipal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = vecino.inicial.ifBlank { "V" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = tamanoTexto
        )
        if (vecino.photoUrl.isNotBlank()) {
            AsyncImage(
                model = vecino.photoUrl,
                contentDescription = "Avatar de ${vecino.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VecinosContentPreview() {
    val comunidadEjemplo = Comunidad(
        id = "com_1",
        nombre = "Residencial Montecerrao",
        direccion = "Calle Fuertes Acevedo, 42",
        ciudad = "Oviedo",
        codigoPostal = "33006",
        descripcion = "Comunidad residencial con zonas verdes, garaje comunitario y pista deportiva.",
        tipo = "Residencial",
        totalVecinos = 3,
        verificada = true,
        presidenteUid = "u1"
    )
    val vecinosEjemplo = listOf(
        Usuario(
            nombre = "Carlos Ruiz",
            inicial = "C",
            vivienda = "Portal 3, 2ºA",
            email = "carlos@radiopatio.com",
            telefono = "",
            rol = "Presidente",
            uid = "u1",
            comunidadId = "com_1"
        ),
        Usuario(
            nombre = "Lucía Fernández",
            inicial = "L",
            vivienda = "Portal 1, 4ºC",
            email = "lucia@radiopatio.com",
            telefono = "",
            rol = "Vicepresidenta",
            uid = "u2",
            comunidadId = "com_1"
        ),
        Usuario(
            nombre = "Pelayo García",
            inicial = "P",
            vivienda = "Portal 2, 1ºB",
            email = "pelayo@radiopatio.com",
            telefono = "",
            rol = "Vecino/a",
            uid = "u3",
            comunidadId = "com_1"
        )
    )

    RadioPatioTheme {
        VecinosContent(
            uiState = VecinosUiState(
                comunidadIdActual = "com_1",
                comunidad = comunidadEjemplo,
                usuarioActual = vecinosEjemplo.first(),
                vecinos = vecinosEjemplo,
                cargando = false
            ),
            onTextoBusquedaChange = {},
            onFiltroRolChange = {},
            onIrABuscarComunidad = {}
        )
    }
}

