package com.example.radiopatio.marketplace.misProductos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * UIState inmutable
 */
data class MisProductosUiState(
    val productos: List<MiProducto> = emptyList(),
    val cargando: Boolean = true,
    val pestanaSeleccionada: Int = 0 // 0 = Ventas, 1 = Prestamos
) {
    val productosFiltrados: List<MiProducto>
        get() = if(pestanaSeleccionada == 0){
            productos.filter { it.esParaComprar }
        } else{
            productos.filter { !it.esParaComprar }
        }
}

class MisProductosViewModel : ViewModel() {
    var uiState by mutableStateOf(MisProductosUiState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init{
        cargarMisProductos()
    }

    fun onPestanaSeleccionada(indice: Int){
        uiState = uiState.copy(pestanaSeleccionada = indice)
    }

    fun cargarMisProductos(){
        val uid = auth.currentUser?.uid ?: return
        uiState = uiState.copy(cargando = true)

        db.collection("productos")
            .whereEqualTo("autorUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.map { doc ->
                    val estadoStr = doc.getString("estado") ?: EstadoProducto.DISPONIBLE.name
                    val estado = try{
                        EstadoProducto.valueOf(estadoStr)
                    } catch(e:Exception){
                        EstadoProducto.DISPONIBLE
                    }

                    MiProducto(
                        id = doc.id,
                        titulo = doc.getString("titulo") ?: "",
                        precio = doc.getString("precio") ?: "",
                        esParaComprar = doc.getBoolean("esParaComprar") ?: true,
                        estado = estado,
                        fechaDevolucion = doc.getString("fechaDevolucion") ?: ""
                    )
                }

                uiState = uiState.copy(productos = lista, cargando = false)
            }.addOnFailureListener{
                uiState = uiState.copy(cargando = false)
            }
    }

    fun onActualizarEstado(productoId: String, nuevoEstado: EstadoProducto, fechaDevolucion: String = "") {
        val actualizaciones = mapOf(
            "estado" to nuevoEstado.name,
            "fechaDevolucion" to fechaDevolucion
        )
        db.collection("productos").document(productoId).update(actualizaciones)
            .addOnSuccessListener {
                cargarMisProductos()
            }
    }


}