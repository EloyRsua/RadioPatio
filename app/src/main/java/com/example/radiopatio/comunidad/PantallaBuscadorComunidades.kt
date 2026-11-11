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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MeetingRoom
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
    val totalPortales: Int,
    val descripcion: String
)

/**
 * Ventana unificada de búsqueda y filtrado de comunidades, compartida tanto en el flujo
 * de registro de nueva cuenta como al cambiar de comunidad desde Vecinos.
 */
@Composable
fun BuscadorComunidadesScreen(
    modoRegistro: Boolean,
    comunidadPreseleccionadaId: String = "",
    onVolver: () -> Unit,
    onComunidadSeleccionadaEnRegistro: (Comunidad) -> Unit = {},
    onNuevaComunidadEnRegistro: (NuevaComunidadFormulario) -> Unit = {},
    onComunidadCambiada: () -> Unit = {},
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

    val idResaltado = if (modoRegistro) {
        comunidadPreseleccionadaId
    } else {
        uiState.comunidadActualId
    }

    BuscadorComunidadesContent(
        uiState = uiState,
        comunidadSeleccionadaId = idResaltado,
        modoRegistro = modoRegistro,
        onVolver = onVolver,
        onTextoBusquedaChange = viewModel::onTextoBusquedaChange,
        onTipoSeleccionadoChange = viewModel::onTipoSeleccionadoChange,
        onCiudadSeleccionadaChange = viewModel::onCiudadSeleccionadaChange,
        onToggleSoloVerificadas = viewModel::onToggleSoloVerificadas,
        onOrdenSeleccionadoChange = viewModel::onOrdenSeleccionadoChange,
        onLimpiarFiltros = viewModel::onLimpiarFiltros,
        onAbrirDialogoCrear = viewModel::onAbrirDialogoCrear,
        onCerrarDialogoCrear = viewModel::onCerrarDialogoCrear,
        onSeleccionarComunidad = { comunidad ->
            if (modoRegistro) {
                onComunidadSeleccionadaEnRegistro(comunidad)
            } else {
                viewModel.onUnirseAComunidad(comunidad) {
                    onComunidadCambiada()
                }
            }
        },
        onConfirmarCrearComunidad = { formulario ->
            if (modoRegistro) {
                viewModel.onCerrarDialogoCrear()
                onNuevaComunidadEnRegistro(formulario)
            } else {
                viewModel.onCrearYUnirseAComunidad(
                    nombre = formulario.nombre,
                    direccion = formulario.direccion,
                    ciudad = formulario.ciudad,
                    codigoPostal = formulario.codigoPostal,
                    tipo = formulario.tipo,
                    totalPortales = formulario.totalPortales,
                    descripcion = formulario.descripcion
                ) {
                    onComunidadCambiada()
                }
            }
        }
    )
}

@Composable
fun BuscadorComunidadesContent(
    uiState: BuscadorComunidadesUiState,
    comunidadSeleccionadaId: String,
    modoRegistro: Boolean,
    onVolver: () -> Unit,
    onTextoBusquedaChange: (String) -> Unit,
    onTipoSeleccionadoChange: (String) -> Unit,
    onCiudadSeleccionadaChange: (String) -> Unit,
    onToggleSoloVerificadas: () -> Unit,
    onOrdenSeleccionadoChange: (OrdenComunidad) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onAbrirDialogoCrear: () -> Unit,
    onCerrarDialogoCrear: () -> Unit,
    onSeleccionarComunidad: (Comunidad) -> Unit,
    onConfirmarCrearComunidad: (NuevaComunidadFormulario) -> Unit
) {
    var filtrosExpandidos by remember { mutableStateOf(false) }
    val comunidadesFiltradas = uiState.comunidadesFiltradas

    Scaffold(
        topBar = {
            BarraSuperiorBuscadorComunidades(
                modoRegistro = modoRegistro,
                resultados = comunidadesFiltradas.size,
                total = uiState.comunidadesTotales.size,
                onVolver = onVolver,
                onNuevaComunidad = onAbrirDialogoCrear
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAbrirDialogoCrear,
                containerColor = VerdeAccion,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear nueva comunidad")
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
                                    text = "Nombre, calle, ciudad o C.P...",
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
                        onToggleSoloVerificadas = onToggleSoloVerificadas,
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
                            onLimpiarFiltros = onLimpiarFiltros,
                            onCrearComunidad = onAbrirDialogoCrear
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
                                TarjetaComunidadBuscador(
                                    comunidad = comunidad,
                                    esSeleccionada = comunidad.id == comunidadSeleccionadaId &&
                                        comunidadSeleccionadaId.isNotBlank(),
                                    modoRegistro = modoRegistro,
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

    if (uiState.mostrarDialogoCrear) {
        DialogoCrearComunidad(
            nombreInicial = uiState.textoBusqueda.trim(),
            procesando = uiState.procesando,
            onDismiss = onCerrarDialogoCrear,
            onConfirmar = onConfirmarCrearComunidad
        )
    }
}

@Composable
private fun BarraSuperiorBuscadorComunidades(
    modoRegistro: Boolean,
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
                    text = if (modoRegistro) "Elige tu comunidad" else "Cambiar de comunidad",
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

@Composable
private fun FilaChipsFiltrosRapidos(
    uiState: BuscadorComunidadesUiState,
    onToggleSoloVerificadas: () -> Unit,
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

        // Chip Verificadas
        item {
            FilterChip(
                selected = uiState.soloVerificadas,
                onClick = onToggleSoloVerificadas,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                label = { Text("Verificadas", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VerdeSuave,
                    selectedLabelColor = VerdeOscuro,
                    selectedLeadingIconColor = VerdeOscuro
                )
            )
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
    modoRegistro: Boolean,
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
                if (esSeleccionada) VerdeAccion else BordeSuave
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comunidad.nombre,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (comunidad.verificada) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verificada",
                                tint = VerdeAccion,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

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
                        color = AzulPrincipal
                    ) {
                        Text(
                            text = if (modoRegistro) "Elegir" else "Unirme",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
                    texto = comunidad.tipo.ifBlank { "Residencial" }
                )
                EtiquetaComunidad(
                    icono = Icons.Outlined.Groups,
                    texto = "${comunidad.totalVecinos} vecino${if (comunidad.totalVecinos == 1) "" else "s"}"
                )
                EtiquetaComunidad(
                    icono = Icons.Outlined.MeetingRoom,
                    texto = "${comunidad.totalPortales} portal${if (comunidad.totalPortales == 1) "" else "es"}"
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
            text = if (hayFiltrosActivos) {
                "Prueba con otros términos de búsqueda o limpia los filtros activos."
            } else {
                "Sé el primero en dar de alta tu edificio o urbanización."
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

@Composable
private fun DialogoCrearComunidad(
    nombreInicial: String,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (NuevaComunidadFormulario) -> Unit
) {
    var nombre by remember(nombreInicial) { mutableStateOf(nombreInicial) }
    var direccion by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Residencial") }
    var totalPortales by remember { mutableStateOf("1") }
    var descripcion by remember { mutableStateOf("") }

    val tipos = listOf("Residencial", "Urbanización", "Edificio", "Mancomunidad")

    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        title = {
            Text(
                text = "Crear nueva comunidad",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AzulPrincipal
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la comunidad *", fontSize = 12.sp) },
                    placeholder = { Text("Ej: Residencial Montecerrao") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección *", fontSize = 12.sp) },
                    placeholder = { Text("Ej: Calle Fuertes Acevedo, 42") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ciudad,
                        onValueChange = { ciudad = it },
                        label = { Text("Ciudad", fontSize = 12.sp) },
                        placeholder = { Text("Oviedo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = codigoPostal,
                        onValueChange = { codigoPostal = it },
                        label = { Text("C.P.", fontSize = 12.sp) },
                        placeholder = { Text("33006") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Text(
                    text = "Tipo de comunidad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tipos) { itemTipo ->
                        FilterChip(
                            selected = tipo == itemTipo,
                            onClick = { tipo = itemTipo },
                            label = { Text(itemTipo, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = totalPortales,
                    onValueChange = { digitos ->
                        if (digitos.all { it.isDigit() }) totalPortales = digitos
                    },
                    label = { Text("Número de portales", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción", fontSize = 12.sp) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmar(
                        NuevaComunidadFormulario(
                            nombre = nombre.trim(),
                            direccion = direccion.trim(),
                            ciudad = ciudad.trim(),
                            codigoPostal = codigoPostal.trim(),
                            tipo = tipo,
                            totalPortales = totalPortales.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            descripcion = descripcion.trim()
                        )
                    )
                },
                enabled = !procesando && nombre.isNotBlank() && direccion.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
            ) {
                if (procesando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar comunidad")
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BuscadorComunidadesContentPreview() {
    val comunidades = listOf(
        Comunidad(
            id = "c1",
            nombre = "Residencial Montecerrao",
            direccion = "Calle Fuertes Acevedo, 42",
            ciudad = "Oviedo",
            codigoPostal = "33006",
            descripcion = "Comunidad residencial con zonas verdes y garaje comunitario.",
            tipo = "Residencial",
            totalPortales = 3,
            totalVecinos = 28,
            verificada = true
        ),
        Comunidad(
            id = "c2",
            nombre = "Urbanización La Florida",
            direccion = "Paseo de la Florida, 15",
            ciudad = "Oviedo",
            codigoPostal = "33012",
            descripcion = "Urbanización familiar con pistas deportivas.",
            tipo = "Urbanización",
            totalPortales = 5,
            totalVecinos = 42,
            verificada = false
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
            onToggleSoloVerificadas = {},
            onOrdenSeleccionadoChange = {},
            onLimpiarFiltros = {},
            onAbrirDialogoCrear = {},
            onCerrarDialogoCrear = {},
            onSeleccionarComunidad = {},
            onConfirmarCrearComunidad = {}
        )
    }
}
