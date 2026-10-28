package com.example.radiopatio

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radiopatio.ui.theme.RadioPatioTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * UiState inmutable para la pantalla del Calendario de Eventos.
 * Agrupa todos los datos observables que definen el estado de la UI (Patrón UiState).
 */
data class CalendarioUiState(
    val mesActual: YearMonth = YearMonth.of(2026, 10),
    val diaSeleccionado: LocalDate? = null,
    val categoriaSeleccionada: CategoriaEvento = CategoriaEvento.TODOS,
    val listaEventos: List<Evento> = DatosEjemploEventos.obtenerEventos(),
    val mostrarDialogoNuevoEvento: Boolean = false
) {
    val eventosFiltrados: List<Evento>
        get() = listaEventos.filter { evento ->
            val coincideMes = YearMonth.from(evento.fecha) == mesActual
            val coincideDia = diaSeleccionado == null || evento.fecha == diaSeleccionado
            val coincideCategoria = categoriaSeleccionada == CategoriaEvento.TODOS || evento.categoria == categoriaSeleccionada
            coincideMes && coincideDia && coincideCategoria
        }.sortedBy { it.fecha }

    val diasConEventos: Map<Int, List<Evento>>
        get() = listaEventos
            .filter { YearMonth.from(it.fecha) == mesActual }
            .groupBy { it.fecha.dayOfMonth }
}

/**
 * Screen (Stateful): Responsable de mantener y gestionar el estado (State Hoisting).
 * Conecta los eventos de la UI con las mutaciones de estado y delega la renderización en [CalendarioEventosContent].
 */
@Composable
fun CalendarioEventosScreen(
    onVolverAInicio: () -> Unit = {},
    barraInferior: @Composable () -> Unit = {}
) {
    var uiState by remember { mutableStateOf(CalendarioUiState()) }

    CalendarioEventosContent(
        uiState = uiState,
        onMesAnterior = {
            uiState = uiState.copy(mesActual = uiState.mesActual.minusMonths(1))
        },
        onMesSiguiente = {
            uiState = uiState.copy(mesActual = uiState.mesActual.plusMonths(1))
        },
        onIrAHoy = {
            uiState = uiState.copy(
                mesActual = YearMonth.of(2026, 10),
                diaSeleccionado = LocalDate.of(2026, 10, 15)
            )
        },
        onSeleccionarDia = { dia ->
            val nuevoDia = if (uiState.diaSeleccionado == dia) null else dia
            uiState = uiState.copy(diaSeleccionado = nuevoDia)
        },
        onLimpiarFiltroDia = {
            uiState = uiState.copy(diaSeleccionado = null)
        },
        onSeleccionarCategoria = { cat ->
            uiState = uiState.copy(categoriaSeleccionada = cat)
        },
        onToggleAsistencia = { eventoId ->
            val nuevaLista = uiState.listaEventos.map { evento ->
                if (evento.id == eventoId) {
                    val nuevoEstado = !evento.asistiendoUsuario
                    val totalAsistentes = if (nuevoEstado) evento.asistentes + 1 else maxOf(0, evento.asistentes - 1)
                    evento.copy(asistiendoUsuario = nuevoEstado, asistentes = totalAsistentes)
                } else evento
            }
            uiState = uiState.copy(listaEventos = nuevaLista)
        },
        onToggleRecordatorio = { eventoId ->
            val nuevaLista = uiState.listaEventos.map { evento ->
                if (evento.id == eventoId) {
                    evento.copy(recordatorioActivado = !evento.recordatorioActivado)
                } else evento
            }
            uiState = uiState.copy(listaEventos = nuevaLista)
        },
        onAbrirDialogoNuevoEvento = {
            uiState = uiState.copy(mostrarDialogoNuevoEvento = true)
        },
        onCerrarDialogoNuevoEvento = {
            uiState = uiState.copy(mostrarDialogoNuevoEvento = false)
        },
        onCrearEvento = { nuevoEvento ->
            val nuevaLista = listOf(nuevoEvento) + uiState.listaEventos
            val enfocarDia = if (YearMonth.from(nuevoEvento.fecha) == uiState.mesActual) nuevoEvento.fecha else uiState.diaSeleccionado
            uiState = uiState.copy(
                listaEventos = nuevaLista,
                mostrarDialogoNuevoEvento = false,
                diaSeleccionado = enfocarDia
            )
        },
        onVolverAInicio = onVolverAInicio,
        barraInferior = barraInferior
    )
}

/**
 * Content (Stateless): Responsable exclusivamente de representar la UI a partir del [uiState] recibido.
 * No gestiona estado propio. Todos los eventos se emiten hacia el exterior mediante funciones lambda.
 */
@Composable
fun CalendarioEventosContent(
    uiState: CalendarioUiState,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onIrAHoy: () -> Unit,
    onSeleccionarDia: (LocalDate) -> Unit,
    onLimpiarFiltroDia: () -> Unit,
    onSeleccionarCategoria: (CategoriaEvento) -> Unit,
    onToggleAsistencia: (String) -> Unit,
    onToggleRecordatorio: (String) -> Unit,
    onAbrirDialogoNuevoEvento: () -> Unit,
    onCerrarDialogoNuevoEvento: () -> Unit,
    onCrearEvento: (Evento) -> Unit,
    onVolverAInicio: () -> Unit,
    barraInferior: @Composable () -> Unit = {}
) {
    val eventosFiltrados = uiState.eventosFiltrados
    val diasConEventos = uiState.diasConEventos

    Scaffold(
        topBar = {
            BarraSuperiorEventos(
                onVolver = onVolverAInicio,
                onNuevoEvento = onAbrirDialogoNuevoEvento
            )
        },
        bottomBar = barraInferior,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAbrirDialogoNuevoEvento,
                containerColor = Color(0xFF389F82),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Proponer Evento")
            }
        }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(relleno),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Selector de Mes y Navegación
            item {
                SelectorMesHeader(
                    mesActual = uiState.mesActual,
                    onMesAnterior = onMesAnterior,
                    onMesSiguiente = onMesSiguiente,
                    onIrAHoy = onIrAHoy
                )
            }

            // 2. Cuadrícula del Calendario
            item {
                TarjetaCalendarioMensual(
                    mesActual = uiState.mesActual,
                    diaSeleccionado = uiState.diaSeleccionado,
                    diasConEventos = diasConEventos,
                    onSeleccionarDia = onSeleccionarDia
                )
            }

            // 3. Filtro por Categorías
            item {
                FiltroCategoriasEventos(
                    categoriaSeleccionada = uiState.categoriaSeleccionada,
                    onCategoriaSeleccionada = onSeleccionarCategoria
                )
            }

            // 4. Encabezado de la lista con conteo y botón para limpiar filtro de día
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.diaSeleccionado != null) {
                                "Eventos del ${uiState.diaSeleccionado.dayOfMonth} de ${
                                    uiState.mesActual.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
                                }"
                            } else {
                                "Todos los eventos de ${
                                    uiState.mesActual.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
                                }"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${eventosFiltrados.size} actividad${if (eventosFiltrados.size == 1) "" else "es"} disponible${if (eventosFiltrados.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.diaSeleccionado != null) {
                        TextButton(
                            onClick = onLimpiarFiltroDia,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Ver todo el mes",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 5. Lista de Eventos o Estado Vacío
            if (eventosFiltrados.isEmpty()) {
                item {
                    EstadoEventosVacio(
                        diaSeleccionado = uiState.diaSeleccionado,
                        onProponerEvento = onAbrirDialogoNuevoEvento,
                        onVerTodos = onLimpiarFiltroDia
                    )
                }
            } else {
                items(eventosFiltrados, key = { it.id }) { evento ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        TarjetaEvento(
                            evento = evento,
                            onToggleAsistencia = { onToggleAsistencia(evento.id) },
                            onToggleRecordatorio = { onToggleRecordatorio(evento.id) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Proponer / Crear un Nuevo Evento
    if (uiState.mostrarDialogoNuevoEvento) {
        DialogoNuevoEvento(
            diaInicial = uiState.diaSeleccionado ?: LocalDate.of(2026, uiState.mesActual.monthValue, 15),
            onDismiss = onCerrarDialogoNuevoEvento,
            onCrearEvento = onCrearEvento
        )
    }
}

/**
 * Barra superior específica para la pantalla de Calendario de Eventos.
 */
@Composable
private fun BarraSuperiorEventos(
    onVolver: () -> Unit,
    onNuevoEvento: () -> Unit
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calendario de Eventos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Actividades y avisos de la comunidad",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNuevoEvento) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    contentDescription = "Añadir Evento",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Cabecera con selector de mes y botones anterior / siguiente / hoy.
 */
@Composable
private fun SelectorMesHeader(
    mesActual: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onIrAHoy: () -> Unit
) {
    val nombreMes = mesActual.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() }
    val anio = mesActual.year

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$nombreMes $anio",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                modifier = Modifier.clickable { onIrAHoy() }
            ) {
                Text(
                    text = "Hoy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row {
            IconButton(
                onClick = onMesAnterior,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Mes anterior",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onMesSiguiente,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Mes siguiente",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Tarjeta mensual que contiene la cuadrícula con los días de la semana y los días del mes.
 */
@Composable
private fun TarjetaCalendarioMensual(
    mesActual: YearMonth,
    diaSeleccionado: LocalDate?,
    diasConEventos: Map<Int, List<Evento>>,
    onSeleccionarDia: (LocalDate) -> Unit
) {
    val diasSemana = listOf("L", "M", "X", "J", "V", "S", "D")
    val primerDiaMes = mesActual.atDay(1)
    val totalDiasMes = mesActual.lengthOfMonth()
    val offsetPrimerDia = primerDiaMes.dayOfWeek.value - 1
    val hoy = LocalDate.of(2026, 10, 15)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                diasSemana.forEachIndexed { index, dia ->
                    val esFinDeSemana = index >= 5
                    Text(
                        text = dia,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esFinDeSemana) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalCeldas = offsetPrimerDia + totalDiasMes
            val totalFilas = (totalCeldas + 6) / 7

            for (fila in 0 until totalFilas) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (columna in 0..6) {
                        val celdaIndex = fila * 7 + columna
                        val numeroDia = celdaIndex - offsetPrimerDia + 1

                        if (numeroDia in 1..totalDiasMes) {
                            val fechaCelda = mesActual.atDay(numeroDia)
                            val esSeleccionado = diaSeleccionado == fechaCelda
                            val esHoy = fechaCelda == hoy
                            val eventosDelDia = diasConEventos[numeroDia].orEmpty()

                            CeldaDiaCalendario(
                                dia = numeroDia,
                                esSeleccionado = esSeleccionado,
                                esHoy = esHoy,
                                eventos = eventosDelDia,
                                onClick = { onSeleccionarDia(fechaCelda) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Celda individual de cada día en el calendario.
 */
@Composable
private fun CeldaDiaCalendario(
    dia: Int,
    esSeleccionado: Boolean,
    esHoy: Boolean,
    eventos: List<Evento>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondoColor by animateColorAsState(
        targetValue = when {
            esSeleccionado -> MaterialTheme.colorScheme.primary
            esHoy -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        label = "fondoDia"
    )

    val textoColor by animateColorAsState(
        targetValue = when {
            esSeleccionado -> MaterialTheme.colorScheme.onPrimary
            esHoy -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "textoDia"
    )

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fondoColor)
            .then(
                if (esHoy && !esSeleccionado) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dia.toString(),
            fontSize = 13.sp,
            fontWeight = if (esSeleccionado || esHoy) FontWeight.Bold else FontWeight.Normal,
            color = textoColor
        )

        if (eventos.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                eventos.take(3).forEach { evento ->
                    val colorPunto = if (esSeleccionado) MaterialTheme.colorScheme.secondary else evento.categoria.colorTexto
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .padding(horizontal = 0.5.dp)
                            .clip(CircleShape)
                            .background(colorPunto)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Fila horizontal deslizable con chips de categorías para filtrar.
 */
@Composable
private fun FiltroCategoriasEventos(
    categoriaSeleccionada: CategoriaEvento,
    onCategoriaSeleccionada: (CategoriaEvento) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CategoriaEvento.values()) { categoria ->
            val esActivo = categoriaSeleccionada == categoria

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (esActivo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = if (esActivo) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                shadowElevation = if (esActivo) 2.dp else 0.dp,
                modifier = Modifier.clickable { onCategoriaSeleccionada(categoria) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoria.emoji,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoria.nombre,
                        fontSize = 12.sp,
                        fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Medium,
                        color = if (esActivo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar un evento vecinal con información completa y acciones interactivas.
 */
@Composable
fun TarjetaEvento(
    evento: Evento,
    onToggleAsistencia: () -> Unit,
    onToggleRecordatorio: () -> Unit
) {
    var expandirDescripcion by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (evento.esDestacado) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (evento.esDestacado) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⭐ EVENTO DESTACADO DEL BARRIO",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = evento.fecha.dayOfMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = evento.fecha.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = evento.categoria.colorFondo,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${evento.categoria.emoji} ${evento.categoria.nombre}",
                            color = evento.categoria.colorTexto,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = evento.titulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Hora",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = evento.hora,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = "Ubicación",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = evento.ubicacion,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Organizador",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Convoca: ${evento.organizador}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = evento.descripcion,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expandirDescripcion) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expandirDescripcion = !expandirDescripcion }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onToggleAsistencia,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (evento.asistiendoUsuario) Color(0xFF0F8A5F) else Color(0xFF389F82),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (evento.asistiendoUsuario) Icons.Filled.Check else Icons.Outlined.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (evento.asistiendoUsuario) "¡Apuntad@! (${evento.asistentes})" else "Asistir (${evento.asistentes})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleRecordatorio,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (evento.recordatorioActivado) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Recordatorio",
                            tint = if (evento.recordatorioActivado) Color(0xFFE65100) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Compartir evento",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estado que se muestra cuando no hay actividades para el filtro seleccionado.
 */
@Composable
private fun EstadoEventosVacio(
    diaSeleccionado: LocalDate?,
    onProponerEvento: () -> Unit,
    onVerTodos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sin eventos en esta fecha",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "No hay actividades comunitarias programadas con los filtros actuales.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onVerTodos,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Ver todo el mes", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = onProponerEvento,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Proponer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}

/**
 * Diálogo modal para proponer un nuevo evento a la comunidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevoEvento(
    diaInicial: LocalDate,
    onDismiss: () -> Unit,
    onCrearEvento: (Evento) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CategoriaEvento.FIESTAS) }
    var hora by remember { mutableStateOf("18:00 - 20:00") }
    var ubicacion by remember { mutableStateOf("") }
    var organizador by remember { mutableStateOf("Vecino RadioPatio") }
    var descripcion by remember { mutableStateOf("") }
    var diaSeleccionado by remember { mutableStateOf(diaInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Proponer Evento",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de la actividad *", fontSize = 12.sp) },
                    placeholder = { Text("Ej: Quedada de juegos de mesa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CategoriaEvento.values().filter { it != CategoriaEvento.TODOS }) { cat ->
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text("${cat.emoji} ${cat.nombre}", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Lugar / Ubicación *", fontSize = 12.sp) },
                    placeholder = { Text("Ej: Parque del Oeste o Portal 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "${diaSeleccionado.dayOfMonth}/${diaSeleccionado.monthValue}/${diaSeleccionado.year}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it },
                        label = { Text("Horario", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Detalles", fontSize = 12.sp) },
                    placeholder = { Text("Explica en qué consiste el evento y qué deben traer los vecinos.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && ubicacion.isNotBlank()) {
                        val nuevoEvento = Evento(
                            id = "evt-${System.currentTimeMillis()}",
                            titulo = titulo.trim(),
                            descripcion = descripcion.ifBlank { "Evento organizado por vecinos del barrio." },
                            fecha = diaSeleccionado,
                            hora = hora.ifBlank { "Por determinar" },
                            ubicacion = ubicacion.trim(),
                            categoria = categoria,
                            organizador = organizador,
                            asistentes = 1,
                            asistiendoUsuario = true,
                            recordatorioActivado = false,
                            esDestacado = false
                        )
                        onCrearEvento(nuevoEvento)
                    }
                },
                enabled = titulo.isNotBlank() && ubicacion.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF389F82))
            ) {
                Text("Publicar Evento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

// -----------------------------------------------------------------------------------------
// Previews aplicadas sobre la función Content con diferentes estados (según Práctica 02)
// -----------------------------------------------------------------------------------------

@Preview(name = "Calendario con Eventos", showBackground = true, showSystemUi = true)
@Composable
fun CalendarioEventosContentPreview() {
    RadioPatioTheme {
        CalendarioEventosContent(
            uiState = CalendarioUiState(),
            onMesAnterior = {},
            onMesSiguiente = {},
            onIrAHoy = {},
            onSeleccionarDia = {},
            onLimpiarFiltroDia = {},
            onSeleccionarCategoria = {},
            onToggleAsistencia = {},
            onToggleRecordatorio = {},
            onAbrirDialogoNuevoEvento = {},
            onCerrarDialogoNuevoEvento = {},
            onCrearEvento = {},
            onVolverAInicio = {}
        )
    }
}

@Preview(name = "Calendario Sin Eventos", showBackground = true)
@Composable
fun CalendarioEventosVacioPreview() {
    RadioPatioTheme {
        CalendarioEventosContent(
            uiState = CalendarioUiState(
                listaEventos = emptyList(),
                diaSeleccionado = LocalDate.of(2026, 10, 1)
            ),
            onMesAnterior = {},
            onMesSiguiente = {},
            onIrAHoy = {},
            onSeleccionarDia = {},
            onLimpiarFiltroDia = {},
            onSeleccionarCategoria = {},
            onToggleAsistencia = {},
            onToggleRecordatorio = {},
            onAbrirDialogoNuevoEvento = {},
            onCerrarDialogoNuevoEvento = {},
            onCrearEvento = {},
            onVolverAInicio = {}
        )
    }
}
