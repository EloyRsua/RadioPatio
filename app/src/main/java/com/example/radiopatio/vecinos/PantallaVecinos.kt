package com.example.radiopatio.vecinos

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.radiopatio.comunidad.Comunidad
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
    viewModel: VecinosViewModel = viewModel(factory = VecinosViewModelFactory())
) {
    val uiState = viewModel.uiState

    VecinosContent(
        uiState = uiState,
        onTextoBusquedaChange = viewModel::onTextoBusquedaChange,
        onFiltroRolChange = viewModel::onFiltroRolChange,
        onAbrirDialogoComunidad = viewModel::onAbrirDialogoComunidad,
        onCerrarDialogoComunidad = viewModel::onCerrarDialogoComunidad,
        onUnirseAComunidad = viewModel::onUnirseAComunidad,
        onCrearYUnirseAComunidad = viewModel::onCrearYUnirseAComunidad,
        onLimpiarError = viewModel::onLimpiarError
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
    onAbrirDialogoComunidad: () -> Unit,
    onCerrarDialogoComunidad: () -> Unit,
    onUnirseAComunidad: (Comunidad) -> Unit,
    onCrearYUnirseAComunidad: (
        nombre: String,
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        descripcion: String,
        totalPortales: Int
    ) -> Unit,
    onLimpiarError: () -> Unit = {}
) {
    val vecinosFiltrados = uiState.vecinosFiltrados

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
                        onSeleccionarOCrear = onAbrirDialogoComunidad,
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
                        // 1. Datos generales de la comunidad actual
                        item {
                            uiState.comunidad?.let { comunidad ->
                                TarjetaInfoComunidad(
                                    comunidad = comunidad,
                                    totalMiembrosReales = uiState.vecinos.size,
                                    onCambiarComunidad = onAbrirDialogoComunidad
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
                            items(
                                items = vecinosFiltrados,
                                key = { it.uid.ifBlank { "${it.email}_${it.nombre}_${it.vivienda}" } }
                            ) { vecino ->
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

    if (uiState.mostrarDialogoComunidad) {
        DialogoSeleccionarOCrearComunidad(
            comunidadActualId = uiState.comunidadIdActual,
            comunidadesDisponibles = uiState.comunidadesDisponibles,
            procesando = uiState.procesandoComunidad,
            errorMensaje = uiState.errorMensaje,
            onDismiss = onCerrarDialogoComunidad,
            onLimpiarError = onLimpiarError,
            onUnirseAComunidad = onUnirseAComunidad,
            onCrearYUnirseAComunidad = onCrearYUnirseAComunidad
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
    onCambiarComunidad: () -> Unit
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
                        text = "Cambiar",
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
                    icono = Icons.Outlined.MeetingRoom,
                    texto = "${comunidad.totalPortales} portal${if (comunidad.totalPortales == 1) "" else "es"}",
                    modifier = Modifier.weight(1f)
                )
                DatoComunidadPill(
                    icono = Icons.Outlined.Apartment,
                    texto = comunidad.tipo.ifBlank { "Residencial" },
                    modifier = Modifier.weight(1f)
                )
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
            // Avatar (foto remota si existe, con inicial como respaldo)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AzulPrincipal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vecino.inicial.ifBlank { "V" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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
private fun TarjetaSinComunidad(
    onSeleccionarOCrear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = "Aún no perteneces a una comunidad",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrincipal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Únete a tu edificio o urbanización para ver a tus vecinos, publicaciones del tablón, mercadillo y eventos.",
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
                Text("Seleccionar o crear comunidad", fontWeight = FontWeight.Bold)
            }
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
private fun DialogoSeleccionarOCrearComunidad(
    comunidadActualId: String,
    comunidadesDisponibles: List<Comunidad>,
    procesando: Boolean,
    errorMensaje: String?,
    onDismiss: () -> Unit,
    onLimpiarError: () -> Unit,
    onUnirseAComunidad: (Comunidad) -> Unit,
    onCrearYUnirseAComunidad: (
        nombre: String,
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        descripcion: String,
        totalPortales: Int
    ) -> Unit
) {
    var modoCrear by remember { mutableStateOf(comunidadesDisponibles.isEmpty()) }
    var filtroComunidad by remember { mutableStateOf("") }

    var nombreNueva by remember { mutableStateOf("") }
    var direccionNueva by remember { mutableStateOf("") }
    var ciudadNueva by remember { mutableStateOf("") }
    var cpNuevo by remember { mutableStateOf("") }
    var portalesNuevo by remember { mutableStateOf("1") }
    var descripcionNueva by remember { mutableStateOf("") }

    val comunidadesFiltradas = remember(comunidadesDisponibles, filtroComunidad) {
        val q = filtroComunidad.trim()
        if (q.isBlank()) {
            comunidadesDisponibles
        } else {
            comunidadesDisponibles.filter {
                it.nombre.contains(q, ignoreCase = true) ||
                    it.direccion.contains(q, ignoreCase = true) ||
                    it.ciudad.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        title = {
            Text(
                text = if (modoCrear) "Nueva Comunidad" else "Comunidades en RadioPatio",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !modoCrear,
                        onClick = {
                            modoCrear = false
                            onLimpiarError()
                        },
                        label = { Text("Unirse (${comunidadesDisponibles.size})", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = modoCrear,
                        onClick = {
                            modoCrear = true
                            onLimpiarError()
                        },
                        label = { Text("Crear nueva", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!errorMensaje.isNullOrBlank()) {
                    Text(
                        text = errorMensaje,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }

                if (!modoCrear) {
                    OutlinedTextField(
                        value = filtroComunidad,
                        onValueChange = { filtroComunidad = it },
                        placeholder = { Text("Buscar por nombre o calle", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (comunidadesFiltradas.isEmpty()) {
                        Text(
                            text = "No se encontraron comunidades. Puedes crear una nueva desde la pestaña \"Crear nueva\".",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        comunidadesFiltradas.forEach { comunidad ->
                            val esActual = comunidad.id == comunidadActualId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (esActual) VerdeSuave else Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (esActual) VerdeAccion else BordeSuave,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !procesando) {
                                        onUnirseAComunidad(comunidad)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comunidad.nombre,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AzulPrincipal
                                        )
                                        Text(
                                            text = comunidad.direccionCompleta(),
                                            fontSize = 11.sp,
                                            color = Color(0xFF607D8B)
                                        )
                                    }
                                    if (esActual) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Actual",
                                            tint = VerdeOscuro,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = nombreNueva,
                        onValueChange = { nombreNueva = it },
                        label = { Text("Nombre de la comunidad *", fontSize = 12.sp) },
                        placeholder = { Text("Ej: Residencial Montecerrao") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = direccionNueva,
                        onValueChange = { direccionNueva = it },
                        label = { Text("Dirección *", fontSize = 12.sp) },
                        placeholder = { Text("Ej: Calle Mayor, 12") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ciudadNueva,
                            onValueChange = { ciudadNueva = it },
                            label = { Text("Ciudad", fontSize = 12.sp) },
                            placeholder = { Text("Oviedo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cpNuevo,
                            onValueChange = { cpNuevo = it },
                            label = { Text("C.P.", fontSize = 12.sp) },
                            placeholder = { Text("33006") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    OutlinedTextField(
                        value = portalesNuevo,
                        onValueChange = { soloDigitos ->
                            if (soloDigitos.all { it.isDigit() }) {
                                portalesNuevo = soloDigitos
                            }
                        },
                        label = { Text("Número de portales", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcionNueva,
                        onValueChange = { descripcionNueva = it },
                        label = { Text("Descripción", fontSize = 12.sp) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (modoCrear) {
                Button(
                    onClick = {
                        onCrearYUnirseAComunidad(
                            nombreNueva,
                            direccionNueva,
                            ciudadNueva,
                            cpNuevo,
                            descripcionNueva,
                            portalesNuevo.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        )
                    },
                    enabled = !procesando && nombreNueva.isNotBlank() && direccionNueva.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro)
                ) {
                    if (procesando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Crear y unirme")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !procesando
            ) {
                Text("Cerrar", color = Color.Gray)
            }
        }
    )
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
        totalPortales = 3,
        totalVecinos = 3,
        verificada = true
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
            onAbrirDialogoComunidad = {},
            onCerrarDialogoComunidad = {},
            onUnirseAComunidad = {},
            onCrearYUnirseAComunidad = { _, _, _, _, _, _ -> }
        )
    }
}

