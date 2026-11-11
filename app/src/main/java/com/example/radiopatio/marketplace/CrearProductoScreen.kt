package com.example.radiopatio.marketplace

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Colores oficiales de RadioPatio
private val AzulPrincipal = Color(0xFF133E54)
private val VerdeBoton = Color(0xFF0F8A5F)
private val TextoPrincipal = Color(0xFF0F172A)
private val FondoApp = Color(0xFFF8FAFC)
private val BordeSuave = Color(0xFFE2E8F0)

@Composable
fun CrearProductoScreen(
    onVolver: () -> Unit,
    onPublicar: (
        titulo: String,
        descripcion: String,
        precio: String,
        esParaComprar: Boolean,
        categoria: CategoriaProducto,
        marca: String,
        color: String,
        estadoUso: String,
        fotosUris: List<String>
            ) -> Unit
){
    CrearProductoContent(
        onVolver = onVolver,
        onPublicar = onPublicar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearProductoContent(
    onVolver: () -> Unit,
    onPublicar: (
        titulo: String,
        descripcion: String,
        precio: String,
        esParaComprar: Boolean,
        categoria: CategoriaProducto,
        marca: String,
        color: String,
        estadoUso: String,
        fotosUris: List<String>
        ) -> Unit
) {
    val context = LocalContext.current

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var esParaComprar by remember { mutableStateOf(true) }
    var categoriaSeleccionada by remember { mutableStateOf(CategoriaProducto.TODOS) }
    var menuCategoriasExpandido by remember { mutableStateOf(false) }
    var marca by remember { mutableStateOf("") }
    var colorArticulo by remember { mutableStateOf("") }
    var estadoUsoSeleccionado by remember { mutableStateOf("Buen estado") }

    // Lista de fotos
    val listaFotos = remember { mutableStateListOf<Any>() }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if(uri != null){
            val uriCopiada = copiarUriACache(context, uri)
            listaFotos.add(uriCopiada)
        }
    }

    // Launcher para cámara directa
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uriFoto = guardarBitmapEnCache(context, bitmap)
            listaFotos.add(uriFoto) // Ahora siempre se añade un Uri
        }
    }

    val listaEstados = listOf("Nuevo con etiquetas", "Como nuevo", "Buen estado", "Usado")
    val categoriasDisponibles = CategoriaProducto.entries.filter { it != CategoriaProducto.TODOS }

    var intentoPublicar by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }

    val errorTitulo = intentoPublicar && titulo.isBlank()
    val errorPrecio = intentoPublicar && esParaComprar && precio.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicar Artículo", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AzulPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FondoApp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Selector de Modo: Venta vs Préstamo
            Text("¿Qué deseas hacer con el artículo?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextoPrincipal)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { esParaComprar = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esParaComprar) AzulPrincipal else Color.White,
                        contentColor = if (esParaComprar) Color.White else AzulPrincipal
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (!esParaComprar) androidx.compose.foundation.BorderStroke(1.dp, BordeSuave) else null
                ) {
                    Text("Poner en Venta")
                }
                Button(
                    onClick = { esParaComprar = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!esParaComprar) VerdeBoton else Color.White,
                        contentColor = if (!esParaComprar) Color.White else VerdeBoton
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (esParaComprar) androidx.compose.foundation.BorderStroke(1.dp, BordeSuave) else null
                ) {
                    Text("Ofrecer en Préstamo")
                }
            }
            // 2. Fotos del Producto
            Text("Fotos del producto", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextoPrincipal)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería")
                }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cámara")
                }
            }
            // Lista horizontal de fotos agregadas
            if (listaFotos.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listaFotos) { itemFoto ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, BordeSuave, RoundedCornerShape(10.dp))
                        ) {
                            if (itemFoto is Uri) {
                                AsyncImage(
                                    model = itemFoto,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (itemFoto is Bitmap) {
                                Image(
                                    bitmap = itemFoto.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Botón para eliminar foto
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { listaFotos.remove(itemFoto) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            // 3. Título y Descripción
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título del anuncio *") },
                placeholder = { Text("Ej: Taladro percutor Bosch") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorTitulo,
                supportingText = {
                    if(errorTitulo){
                        Text("El título es obligatorio", color = Color(0xFFDC2626), fontSize = 12.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulPrincipal,
                    errorBorderColor = Color(0xFFDC2626),
                    errorLabelColor = Color(0xFFDC2626),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                placeholder = { Text("Indica detalles, accesorios incluidos...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulPrincipal,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
            // 4. Precio (solo si es venta)
            if (esParaComprar) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio (€) *") },
                    placeholder = { Text("Ej: 25€") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorPrecio,
                    supportingText = {
                        if (errorPrecio) {
                            Text("Indica un precio válido", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrincipal,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }
            // 5. Categoría (Menú desplegable)
            ExposedDropdownMenuBox(
                expanded = menuCategoriasExpandido,
                onExpandedChange = { menuCategoriasExpandido = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (categoriaSeleccionada == CategoriaProducto.TODOS) "Selecciona una categoría *" else categoriaSeleccionada.nombre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuCategoriasExpandido) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrincipal,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = menuCategoriasExpandido,
                    onDismissRequest = { menuCategoriasExpandido = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    categoriasDisponibles.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nombre, color = TextoPrincipal) },
                            onClick = {
                                categoriaSeleccionada = categoria
                                menuCategoriasExpandido = false
                            }
                        )
                    }
                }
            }
            // 6. Marca y Color
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    placeholder = { Text("Ej: Ikea, Apple...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrincipal,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = colorArticulo,
                    onValueChange = { colorArticulo = it },
                    label = { Text("Color") },
                    placeholder = { Text("Ej: Azul, Negro...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrincipal,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }
            // 7. Estado de uso
            Text("Estado del artículo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextoPrincipal)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaEstados) { estado ->
                    FilterChip(
                        selected = estadoUsoSeleccionado == estado,
                        onClick = { estadoUsoSeleccionado = estado },
                        label = { Text(estado) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeBoton,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Botón de Publicar
            Button(
                onClick = {
                    intentoPublicar = true

                    if (titulo.isBlank() || (esParaComprar && precio.isBlank()) || publicando) {
                        return@Button
                    }

                    publicando = true

                    val fotosStrings = listaFotos.mapNotNull {
                        if (it is Uri) it.toString() else null
                    }
                    onPublicar(
                        titulo,
                        descripcion,
                        if (esParaComprar) (if (precio.contains("€")) precio else "$precio€") else "Préstamo",
                        esParaComprar,
                        if (categoriaSeleccionada == CategoriaProducto.TODOS) CategoriaProducto.MUEBLES else categoriaSeleccionada,
                        marca,
                        colorArticulo,
                        estadoUsoSeleccionado,
                        fotosStrings
                    )
                },
                enabled = !publicando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (publicando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publicar Anuncio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Función auxiliar para convertir el Bitmap de la cámara a Uri
fun guardarBitmapEnCache(context: android.content.Context, bitmap: Bitmap): Uri {
    val archivo = java.io.File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
    archivo.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return Uri.fromFile(archivo)
}

// Función auxiliar para copiar una imagen de la galería a la caché local de la app
fun copiarUriACache(context: android.content.Context, uri: Uri): Uri {
    return try {
        val archivo = java.io.File(context.cacheDir, "foto_galeria_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            archivo.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(archivo)
    } catch (e: Exception) {
        uri
    }
}

@Preview(showBackground = true)
@Composable
fun CrearProductoContentPreview() {
    CrearProductoContent(
        onVolver = {},
        onPublicar = { _, _, _, _, _, _, _, _, _ -> }
    )
}
