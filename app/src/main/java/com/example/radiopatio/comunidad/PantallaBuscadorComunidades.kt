package com.example.radiopatio.comunidad

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.radiopatio.ui.theme.RadioPatioTheme

private val AzulPrincipal = Color(0xFF133E54)
private val VerdeAccion = Color(0xFF389F82)
private val VerdeOscuro = Color(0xFF0F8A5F)
private val VerdeSuave = Color(0xFFEAF5EC)
private val FondoPantalla = Color(0xFFF9FAFB)
private val BordeSuave = Color(0xFFECEFF1)

/**
 * Datos devueltos cuando se crea una nueva comunidad desde el buscador.
 */
data class NuevaComunidadFormulario(
    val nombre: String,
    val direccion: String,
    val ciudad: String,
    val codigoPostal: String,
    val tipo: String,
    val numeroPisos: Int = 0,
    val descripcion: String
)

/**
 * Ventana unificada de búsqueda y filtrado de comunidades, compartida tanto en el flujo
 * de registro de nueva cuenta como al cambiar de comunidad desde Vecinos.
 */
@Composable
fun BuscadorComunidadesScreen(
    modoRegistro: Boolean,
    esAdministradorRegistro: Boolean = false,
    comunidadPreseleccionadaId: String = "",
    onVolver: () -> Unit,
    onComunidadSeleccionadaEnRegistro: (Comunidad, Boolean) -> Unit = { _, _ -> },
    onIrACrearComunidad: (String) -> Unit = {},
    onComunidadCambiada: () -> Unit = {},
    viewModel: BuscadorComunidadesViewModel = viewModel(factory = BuscadorComunidadesViewModelFactory())
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var comunidadParaSolicitar by remember { mutableStateOf<Comunidad?>(null) }

    LaunchedEffect(uiState.errorMensaje) {
        uiState.errorMensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarError()
        }
    }

    LaunchedEffect(uiState.mensajeExito) {
        uiState.mensajeExito?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onLimpiarMensajeExito()
        }
    }

    val idResaltado = if (modoRegistro) {
        comunidadPreseleccionadaId
    } else {
        uiState.comunidadActualId
    }

    val esAdminFlujo = if (modoRegistro) {
        esAdministradorRegistro
    } else {
        uiState.usuarioActual?.esAdministradorFincas == true
    }

    val puedeCrear = uiState.puedeCrearComunidad(
        modoRegistro = modoRegistro,
        esAdministradorRegistro = esAdministradorRegistro
    )

    BuscadorComunidadesContent(
        uiState = uiState,
        comunidadSeleccionadaId = idResaltado,
        modoRegistro = modoRegistro,
        esAdminFlujo = esAdminFlujo,
        puedeCrearComunidad = puedeCrear,
        onVolver = onVolver,
        onTextoBusquedaChange = viewModel::onTextoBusquedaChange,
        onTipoSeleccionadoChange = viewModel::onTipoSeleccionadoChange,
        onCiudadSeleccionadaChange = viewModel::onCiudadSeleccionadaChange,
        onOrdenSeleccionadoChange = viewModel::onOrdenSeleccionadoChange,
        onLimpiarFiltros = viewModel::onLimpiarFiltros,
        onIrACrearComunidad = {
            if (puedeCrear) {
                onIrACrearComunidad(uiState.textoBusqueda.trim())
            } else {
                Toast.makeText(
                    context,
                    "Solo los administradores de fincas pueden crear comunidades.",
                    Toast.LENGTH_LONG
                ).show()
            }
        },
        onSeleccionarComunidad = { comunidad ->
            if (modoRegistro) {
                val requiereSolicitud = esAdministradorRegistro && comunidad.administradorUid.isNotBlank()
                onComunidadSeleccionadaEnRegistro(comunidad, requiereSolicitud)
            } else {
                val usuario = uiState.usuarioActual
                val yaPertenece = usuario?.perteneceAComunidad(comunidad.id) == true ||
                    (usuario != null && comunidad.administradorUid == usuario.uid)
                val yaSolicitada = usuario != null &&
                    comunidad.id in usuario.solicitudesPendientesComunidades

                when {
                    esAdminFlujo && !yaPertenece && yaSolicitada -> {
                        Toast.makeText(
                            context,
                            "Ya tienes una solicitud pendiente en ${comunidad.nombre}.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    esAdminFlujo && !yaPertenece && comunidad.administradorUid.isNotBlank() -> {
                        comunidadParaSolicitar = comunidad
                    }
                    else -> {
                        viewModel.onUnirseAComunidad(comunidad) {
                            onComunidadCambiada()
                        }
                    }
                }
            }
        }
    )

    comunidadParaSolicitar?.let { comunidadObjetivo ->
        AlertDialog(
            onDismissRequest = { if (!uiState.procesando) comunidadParaSolicitar = null },
            title = {
                Text(
                    text = "Solicitar administración",
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
            },
            text = {
                Text(
                    text = "${comunidadObjetivo.nombre} ya tiene un administrador de fincas asignado. " +
                        "Se enviará una solicitud de traspaso y permanecerás fuera de esta comunidad hasta que la acepte.",
                    fontSize = 13.sp,
                    color = Color(0xFF455A64)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        comunidadParaSolicitar = null
                        viewModel.onUnirseAComunidad(comunidadObjetivo) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
                ) {
                    Text("Enviar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = { comunidadParaSolicitar = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun BuscadorComunidadesContent(
    uiState: BuscadorComunidadesUiState,
    comunidadSeleccionadaId: String,
    modoRegistro: Boolean,
    esAdminFlujo: Boolean = false,
    puedeCrearComunidad: Boolean = false,
    onVolver: () -> Unit,
    onTextoBusquedaChange: (String) -> Unit,
    onTipoSeleccionadoChange: (String) -> Unit,
    onCiudadSeleccionadaChange: (String) -> Unit,
    onOrdenSeleccionadoChange: (OrdenComunidad) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onIrACrearComunidad: () -> Unit,
    onSeleccionarComunidad: (Comunidad) -> Unit
) {
    var filtrosExpandidos by remember { mutableStateOf(false) }
    val comunidadesFiltradas = uiState.comunidadesFiltradas

    Scaffold(
        topBar = {
            BarraSuperiorBuscadorComunidades(
                modoRegistro = modoRegistro,
                puedeCrearComunidad = puedeCrearComunidad,
                resultados = comunidadesFiltradas.size,
                total = uiState.comunidadesTotales.size,
                onVolver = onVolver,
                onNuevaComunidad = onIrACrearComunidad
            )
        },
        floatingActionButton = {
            if (puedeCrearComunidad) {
                FloatingActionButton(
                    onClick = onIrACrearComunidad,
                    containerColor = VerdeAccion,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Crear nueva comunidad")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoPantalla)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Zona fija de búsqueda y filtros rápidos
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.textoBusqueda,
                            onValueChange = onTextoBusquedaChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = "Código, nombre, calle, ciudad o C.P...",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Buscar",
                                    tint = AzulPrincipal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.textoBusqueda.isNotBlank()) {
                                    IconButton(onClick = { onTextoBusquedaChange("") }) {
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = AzulPrincipal,
                                unfocusedBorderColor = Color(0xFFD6DDE1),
                                cursorColor = AzulPrincipal
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (filtrosExpandidos || uiState.cantidadFiltrosActivos > 0) {
                                VerdeSuave
                            } else {
                                Color(0xFFF8FAFC)
                            },
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (uiState.cantidadFiltrosActivos > 0) VerdeAccion else Color(0xFFD6DDE1)
                                )
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .clickable { filtrosExpandidos = !filtrosExpandidos }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        if (uiState.cantidadFiltrosActivos > 0) {
                                            Badge(containerColor = VerdeOscuro) {
                                                Text(
                                                    text = "${uiState.cantidadFiltrosActivos}",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Filtros avanzados",
                                        tint = if (uiState.cantidadFiltrosActivos > 0) VerdeOscuro else AzulPrincipal
                                    )
                                }
                            }
                        }
                    }

                    // Chips rápidos horizontales
                    FilaChipsFiltrosRapidos(
                        uiState = uiState,
                        onTipoSeleccionadoChange = onTipoSeleccionadoChange,
                        onCiudadSeleccionadaChange = onCiudadSeleccionadaChange,
                        onOrdenSeleccionadoChange = onOrdenSeleccionadoChange
                    )

                    // Panel desplegable con filtros avanzados
                    AnimatedVisibility(visible = filtrosExpandidos) {
                        PanelFiltrosAvanzados(
                            uiState = uiState,
                            onCiudadSeleccionadaChange = onCiudadSeleccionadaChange,
                            onOrdenSeleccionadoChange = onOrdenSeleccionadoChange,
                            onLimpiarFiltros = onLimpiarFiltros
                        )
                    }
                }

                HorizontalDivider(color = BordeSuave)

                // Listado de resultados
                when {
                    uiState.cargando -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = VerdeAccion
                            )
                        }
                    }

                    comunidadesFiltradas.isEmpty() -> {
                        EstadoSinComunidades(
                            textoBusqueda = uiState.textoBusqueda,
                            hayFiltrosActivos = uiState.hayFiltrosActivos,
                            puedeCrearComunidad = puedeCrearComunidad,
                            onLimpiarFiltros = onLimpiarFiltros,
                            onCrearComunidad = onIrACrearComunidad
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${comunidadesFiltradas.size} comunidad${if (comunidadesFiltradas.size == 1) "" else "es"} encontrada${if (comunidadesFiltradas.size == 1) "" else "s"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF546E7A)
                                    )
                                    if (uiState.hayFiltrosActivos) {
                                        TextButton(
                                            onClick = onLimpiarFiltros,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = "Limpiar filtros",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = VerdeOscuro
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = comunidadesFiltradas,
                                key = { index, com -> com.id.ifBlank { "com-$index-${com.nombre}" } }
                            ) { _, comunidad ->
                                val usuario = uiState.usuarioActual
                                val esActiva = comunidad.id == comunidadSeleccionadaId &&
                                    comunidadSeleccionadaId.isNotBlank()
                                val yaAdministrada = !modoRegistro &&
                                    esAdminFlujo &&
                                    usuario != null &&
                                    (usuario.perteneceAComunidad(comunidad.id) || comunidad.administradorUid == usuario.uid)
                                val yaSolicitada = !modoRegistro &&
                                    esAdminFlujo &&
                                    usuario != null &&
                                    comunidad.id in usuario.solicitudesPendientesComunidades
                                val etiquetaAccion = when {
                                    esAdminFlujo && yaAdministrada -> "Cambiar"
                                    esAdminFlujo && yaSolicitada -> "Solicitada"
                                    esAdminFlujo && comunidad.administradorUid.isNotBlank() -> "Solicitar"
                                    esAdminFlujo -> "Administrar"
                                    modoRegistro -> "Elegir"
                                    else -> "Unirme"
                                }

                                TarjetaComunidadBuscador(
                                    comunidad = comunidad,
                                    esSeleccionada = esActiva,
                                    esCoincidenciaCodigo = comunidad.coincideConCodigoDirecto(uiState.textoBusqueda),
                                    etiquetaAccion = etiquetaAccion,
                                    esSecundaria = esAdminFlujo && (yaSolicitada || (comunidad.administradorUid.isNotBlank() && !yaAdministrada)),
                                    habilitado = !uiState.procesando,
                                    onClick = { onSeleccionarComunidad(comunidad) }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.procesando) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = VerdeOscuro,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Actualizando comunidad...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AzulPrincipal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraSuperiorBuscadorComunidades(
    modoRegistro: Boolean,
    puedeCrearComunidad: Boolean,
    resultados: Int,
    total: Int,
    onVolver: () -> Unit,
    onNuevaComunidad: () -> Unit
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = AzulPrincipal
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (modoRegistro) "Elige tu comunidad" else "Buscar comunidad",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
                Text(
                    text = "$resultados de $total comunidades disponibles",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (puedeCrearComunidad) {
                TextButton(onClick = onNuevaComunidad) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nueva",
                        color = VerdeOscuro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaChipsFiltrosRapidos(
    uiState: BuscadorComunidadesUiState,
    onTipoSeleccionadoChange: (String) -> Unit,
    onCiudadSeleccionadaChange: (String) -> Unit,
    onOrdenSeleccionadoChange: (OrdenComunidad) -> Unit
) {
    var menuOrdenAbierto by remember { mutableStateOf(false) }
    var menuCiudadAbierto by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chip Ordenar
        item {
            Box {
                FilterChip(
                    selected = uiState.ordenSeleccionado != OrdenComunidad.RELEVANCIA,
                    onClick = { menuOrdenAbierto = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(uiState.ordenSeleccionado.etiqueta, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeSuave,
                        selectedLabelColor = VerdeOscuro,
                        selectedLeadingIconColor = VerdeOscuro
                    )
                )

                DropdownMenu(
                    expanded = menuOrdenAbierto,
                    onDismissRequest = { menuOrdenAbierto = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    OrdenComunidad.entries.forEach { orden ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = orden.etiqueta,
                                    fontWeight = if (orden == uiState.ordenSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                    color = if (orden == uiState.ordenSeleccionado) VerdeOscuro else AzulPrincipal
                                )
                            },
                            onClick = {
                                onOrdenSeleccionadoChange(orden)
                                menuOrdenAbierto = false
                            }
                        )
                    }
                }
            }
        }

        // Chip Ciudad (si hay ciudades registradas)
        if (uiState.ciudadesDisponibles.size > 1) {
            item {
                Box {
                    FilterChip(
                        selected = uiState.ciudadSeleccionada != "Todas",
                        onClick = { menuCiudadAbierto = true },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (uiState.ciudadSeleccionada == "Todas") {
                                    "Ciudad"
                                } else {
                                    uiState.ciudadSeleccionada
                                },
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeSuave,
                            selectedLabelColor = VerdeOscuro
                        )
                    )

                    DropdownMenu(
                        expanded = menuCiudadAbierto,
                        onDismissRequest = { menuCiudadAbierto = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        uiState.ciudadesDisponibles.forEach { ciudad ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = ciudad,
                                        fontWeight = if (ciudad == uiState.ciudadSeleccionada) FontWeight.Bold else FontWeight.Normal,
                                        color = if (ciudad == uiState.ciudadSeleccionada) VerdeOscuro else AzulPrincipal
                                    )
                                },
                                onClick = {
                                    onCiudadSeleccionadaChange(ciudad)
                                    menuCiudadAbierto = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Chips Tipo de comunidad
        items(uiState.tiposDisponibles) { tipo ->
            val seleccionado = tipo == uiState.tipoSeleccionado
            FilterChip(
                selected = seleccionado,
                onClick = { onTipoSeleccionadoChange(tipo) },
                label = { Text(tipo, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AzulPrincipal,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun PanelFiltrosAvanzados(
    uiState: BuscadorComunidadesUiState,
    onCiudadSeleccionadaChange: (String) -> Unit,
    onOrdenSeleccionadoChange: (OrdenComunidad) -> Unit,
    onLimpiarFiltros: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opciones de filtrado",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrincipal
            )
            if (uiState.hayFiltrosActivos) {
                Text(
                    text = "Restablecer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VerdeOscuro,
                    modifier = Modifier.clickable { onLimpiarFiltros() }
                )
            }
        }

        if (uiState.ciudadesDisponibles.size > 1) {
            Text(
                text = "Ciudad",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(uiState.ciudadesDisponibles) { ciudad ->
                    FilterChip(
                        selected = uiState.ciudadSeleccionada == ciudad,
                        onClick = { onCiudadSeleccionadaChange(ciudad) },
                        label = { Text(ciudad, fontSize = 11.sp) }
                    )
                }
            }
        }

        Text(
            text = "Ordenar resultados por",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(OrdenComunidad.entries) { orden ->
                FilterChip(
                    selected = uiState.ordenSeleccionado == orden,
                    onClick = { onOrdenSeleccionadoChange(orden) },
                    label = { Text(orden.etiqueta, fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaComunidadBuscador(
    comunidad: Comunidad,
    esSeleccionada: Boolean,
    esCoincidenciaCodigo: Boolean = false,
    etiquetaAccion: String,
    esSecundaria: Boolean = false,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esSeleccionada) VerdeSuave else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    esSeleccionada -> VerdeAccion
                    esCoincidenciaCodigo -> VerdeOscuro
                    else -> BordeSuave
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (esCoincidenciaCodigo) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VerdeSuave
                ) {
                    Text(
                        text = "Coincidencia directa · Código ${comunidad.codigoCompartible}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeOscuro,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (esSeleccionada) Color.White else VerdeSuave),
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comunidad.nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF607D8B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comunidad.direccionCompleta(),
                            fontSize = 12.sp,
                            color = Color(0xFF546E7A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (esSeleccionada) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Seleccionada",
                        tint = VerdeOscuro,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (esSecundaria) VerdeSuave else AzulPrincipal
                    ) {
                        Text(
                            text = etiquetaAccion,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esSecundaria) VerdeOscuro else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (comunidad.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = comunidad.descripcion,
                    fontSize = 12.sp,
                    color = Color(0xFF455A64),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EtiquetaComunidad(
                    icono = Icons.Outlined.Apartment,
                    texto = comunidad.tipoNormalizado
                )
                if (comunidad.esEdificio && comunidad.numeroPisos > 0) {
                    EtiquetaComunidad(
                        icono = Icons.Outlined.Apartment,
                        texto = "${comunidad.numeroPisos} piso${if (comunidad.numeroPisos == 1) "" else "s"}"
                    )
                }
                EtiquetaComunidad(
                    icono = Icons.Outlined.Groups,
                    texto = "${comunidad.totalVecinos} vecino${if (comunidad.totalVecinos == 1) "" else "s"}"
                )
            }
        }
    }
}

@Composable
private fun EtiquetaComunidad(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = AzulPrincipal,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = texto,
                fontSize = 11.sp,
                color = AzulPrincipal,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EstadoSinComunidades(
    textoBusqueda: String,
    hayFiltrosActivos: Boolean,
    puedeCrearComunidad: Boolean,
    onLimpiarFiltros: () -> Unit,
    onCrearComunidad: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = Color(0xFF90A4AE),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No encontramos comunidades",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AzulPrincipal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                hayFiltrosActivos -> "Prueba con otros términos de búsqueda o limpia los filtros activos."
                puedeCrearComunidad -> "Da de alta la primera comunidad desde aquí."
                else -> "Solo los administradores de fincas pueden dar de alta nuevas comunidades."
            },
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hayFiltrosActivos) {
                OutlinedButton(
                    onClick = onLimpiarFiltros,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("Limpiar filtros", color = AzulPrincipal, fontSize = 12.sp)
                }
            }
            if (puedeCrearComunidad) {
                Button(
                    onClick = onCrearComunidad,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (textoBusqueda.isNotBlank()) {
                            "Crear \"${textoBusqueda.trim().take(18)}\""
                        } else {
                            "Crear comunidad"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BuscadorComunidadesContentPreview() {
    val comunidades = listOf(
        Comunidad(
            id = "c1",
            nombre = "Edificio Montecerrao",
            direccion = "Calle Fuertes Acevedo, 42",
            ciudad = "Oviedo",
            codigoPostal = "33006",
            descripcion = "Edificio de viviendas con garaje comunitario.",
            tipo = Comunidad.TIPO_EDIFICIO,
            numeroPisos = 6,
            totalVecinos = 28,
            codigo = "MNT42A"
        ),
        Comunidad(
            id = "c2",
            nombre = "Urbanización La Florida",
            direccion = "Paseo de la Florida, 15",
            ciudad = "Oviedo",
            codigoPostal = "33012",
            descripcion = "Urbanización familiar con pistas deportivas.",
            tipo = Comunidad.TIPO_URBANIZACION,
            totalVecinos = 42,
            codigo = "FLR15B"
        )
    )

    RadioPatioTheme {
        BuscadorComunidadesContent(
            uiState = BuscadorComunidadesUiState(
                comunidadesTotales = comunidades,
                comunidadActualId = "c1",
                cargando = false
            ),
            comunidadSeleccionadaId = "c1",
            modoRegistro = false,
            onVolver = {},
            onTextoBusquedaChange = {},
            onTipoSeleccionadoChange = {},
            onCiudadSeleccionadaChange = {},
            onOrdenSeleccionadoChange = {},
            onLimpiarFiltros = {},
            onIrACrearComunidad = {},
            onSeleccionarComunidad = {}
        )
    }
}
