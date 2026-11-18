package com.example.radiopatio.misProductos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.example.radiopatio.marketplace.CategoriaProducto
import com.example.radiopatio.marketplace.EstadoProducto
import com.example.radiopatio.marketplace.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UIState inmutable
 */
data class MisProductosUiState(
    val productos: List<Producto> = emptyList(),
    val cargando: Boolean = true,
    val pestanaSeleccionada: Int = 0 // 0 = Ventas, 1 = Prestamos
) {
    val productosFiltrados: List<Producto>
        get() = if (pestanaSeleccionada == 0) {
            productos.filter { it.esParaComprar }
        } else {
            productos.filter { !it.esParaComprar }
        }
}

class MisProductosViewModel : ViewModel() {
    var uiState by mutableStateOf(MisProductosUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        cargarMisProductos()
    }

    fun onPestanaSeleccionada(indice: Int) {
        uiState = uiState.copy(pestanaSeleccionada = indice)
    }

    fun cargarMisProductos() {
        val uid = auth.currentUser?.uid ?: return
        uiState = uiState.copy(cargando = true)

        viewModelScope.launch {
            try {
                var comId = ""
                try {
                    val userSnap = db.collection("users").document(uid).get().await()
                    comId = userSnap.getString("comunidadId").orEmpty()
                } catch (e: Exception) {
                    // ignore
                }

                val documentos = mutableListOf<DocumentSnapshot>()

                // 1. Buscamos en la subcolección de la comunidad activa
                if (comId.isNotBlank()) {
                    try {
                        val snapComunidad = db.subcoleccionComunidad(comId, "productos")
                            .whereEqualTo("autorUid", uid)
                            .get()
                            .await()
                        documentos.addAll(snapComunidad.documents)
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                // 2. Buscamos en todas las subcolecciones productos con collectionGroup
                try {
                    val snapGroup = db.collectionGroup("productos")
                        .whereEqualTo("autorUid", uid)
                        .get()
                        .await()
                    for (doc in snapGroup.documents) {
                        if (documentos.none { it.id == doc.id }) {
                            documentos.add(doc)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                // 3. Fallback a la raíz por si quedara alguno previo
                try {
                    val snapRaiz = db.collection("productos")
                        .whereEqualTo("autorUid", uid)
                        .get()
                        .await()
                    for (doc in snapRaiz.documents) {
                        if (documentos.none { it.id == doc.id }) {
                            documentos.add(doc)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                val lista = documentos.map { doc ->
                    val estadoStr = doc.getString("estado") ?: EstadoProducto.DISPONIBLE.name
                    val estado = try {
                        EstadoProducto.valueOf(estadoStr)
                    } catch (e: Exception) {
                        EstadoProducto.DISPONIBLE
                    }

                    @Suppress("UNCHECKED_CAST")
                    val fotos = doc.get("fotos") as? List<String> ?: emptyList()

                    Producto(
                        firestoreId = doc.id,
                        comunidadId = doc.getString("comunidadId") ?: comId,
                        autorUid = doc.getString("autorUid") ?: "",
                        vendedorUid = doc.getString("vendedorUid") ?: doc.getString("autorUid") ?: "",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        precio = doc.getString("precio") ?: "",
                        ubicacionYTiempo = doc.getString("ubicacion") ?: doc.getString("ubicacionYTiempo") ?: "",
                        categoriaProducto = try {
                            CategoriaProducto.valueOf(doc.getString("categoria") ?: CategoriaProducto.TODOS.name)
                        } catch (e: Exception) {
                            CategoriaProducto.TODOS
                        },
                        esParaComprar = doc.getBoolean("esParaComprar") ?: true,
                        marca = doc.getString("marca") ?: "",
                        color = doc.getString("color") ?: "",
                        estadoUso = doc.getString("estadoUso") ?: "",
                        fotos = fotos,
                        imagenUrl = fotos.firstOrNull() ?: doc.getString("imagenUrl"),
                        estado = estado,
                        fechaDevolucion = doc.getString("fechaDevolucion") ?: ""
                    )
                }

                uiState = uiState.copy(productos = lista, cargando = false)
            } catch (e: Exception) {
                uiState = uiState.copy(cargando = false)
            }
        }
    }

    fun onActualizarEstado(productoId: String, nuevoEstado: EstadoProducto, fechaDevolucion: String = "") {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid.orEmpty()
            val comId = try {
                val userSnap = db.collection("users").document(uid).get().await()
                userSnap.getString("comunidadId").orEmpty()
            } catch (e: Exception) {
                ""
            }

            val actualizaciones = mapOf(
                "estado" to nuevoEstado.name,
                "fechaDevolucion" to fechaDevolucion
            )

            // Intentar primero en subcolección de comunidad
            var actualizado = false
            if (comId.isNotBlank()) {
                try {
                    db.subcoleccionComunidad(comId, "productos").document(productoId)
                        .update(actualizaciones)
                        .await()
                    actualizado = true
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Fallback en raíz
            if (!actualizado) {
                try {
                    db.collection("productos").document(productoId)
                        .update(actualizaciones)
                        .await()
                } catch (e: Exception) {
                    // ignore
                }
            }

            cargarMisProductos()
        }
    }

    fun eliminarProducto(producto: Producto, onSuccess: () -> Unit = {}) {
        val productoId = producto.firestoreId
        if (productoId.isBlank()) return

        viewModelScope.launch {
            val uid = auth.currentUser?.uid.orEmpty()
            val comId = producto.comunidadId.ifBlank {
                try {
                    val userSnap = db.collection("users").document(uid).get().await()
                    userSnap.getString("comunidadId").orEmpty()
                } catch (e: Exception) {
                    ""
                }
            }

            // 1. Intentar borrar en la subcolección de la comunidad
            if (comId.isNotBlank()) {
                try {
                    db.subcoleccionComunidad(comId, "productos").document(productoId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 2. Fallback por si estaba en la colección raíz
            try {
                db.collection("productos").document(productoId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // ignore
            }

            // 3. Actualizar la lista local en pantalla de inmediato
            uiState = uiState.copy(
                productos = uiState.productos.filterNot { it.firestoreId == productoId }
            )
            onSuccess()
        }
    }
}
