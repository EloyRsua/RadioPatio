package com.example.radiopatio.marketplace

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

/**
 * Repositorio de Cloud Firestore para el módulo de Mercadillo,
 * con sincronización en tiempo real y aislamiento obligatorio por `comunidadId`.
 */
class MercadilloRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Mapea un [DocumentSnapshot] de Firestore a [Producto].
     */
    fun documentToProducto(doc: DocumentSnapshot): Producto? {
        val comunidadId = doc.getString("comunidadId") ?: return null
        if (comunidadId.isBlank()) return null

        val categoriaRaw = doc.getString("categoria") ?: CategoriaProducto.MUEBLES.name
        val categoria = CategoriaProducto.entries.find {
            it.name.equals(categoriaRaw, ignoreCase = true) ||
                it.nombre.equals(categoriaRaw, ignoreCase = true)
        } ?: CategoriaProducto.MUEBLES

        val numericId = (doc.getLong("id")?.toInt())
            ?: abs(doc.id.hashCode())

        return Producto(
            id = numericId,
            titulo = doc.getString("titulo") ?: "",
            precio = doc.getString("precio") ?: "",
            ubicacionYTiempo = doc.getString("ubicacionYTiempo") ?: "Comunidad",
            categoriaProducto = categoria,
            esParaComprar = doc.getBoolean("esParaComprar") ?: true,
            imagenRes = null,
            firestoreId = doc.id,
            comunidadId = comunidadId,
            vendedorUid = doc.getString("vendedorUid") ?: "",
            vendedorNombre = doc.getString("vendedorNombre") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L
        )
    }

    /**
     * Observa en tiempo real los anuncios del mercadillo pertenecientes exclusivamente a [comunidadId].
     */
    fun observarProductosDeComunidad(comunidadId: String): Flow<List<Producto>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_PRODUCTOS)
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.documents
                        ?.mapNotNull { documentToProducto(it) }
                        ?.filter { it.comunidadId == comunidadId }
                        ?.sortedByDescending { it.timestamp }
                        ?: emptyList()

                    trySend(lista)
                }
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Publica un nuevo anuncio en Firestore asociándolo obligatoriamente a [comunidadId].
     */
    suspend fun crearProducto(
        producto: Producto,
        comunidadId: String,
        vendedorUid: String,
        vendedorNombre: String
    ): Result<Producto> {
        if (comunidadId.isBlank()) {
            return Result.failure(IllegalStateException("Debes pertenecer a una comunidad para publicar anuncios."))
        }
        if (producto.titulo.isBlank()) {
            return Result.failure(IllegalArgumentException("El título del artículo es obligatorio."))
        }

        return try {
            val docRef = db.collection(COLECCION_PRODUCTOS).document()
            val timestamp = System.currentTimeMillis()
            val creado = producto.copy(
                id = abs(docRef.id.hashCode()),
                firestoreId = docRef.id,
                comunidadId = comunidadId,
                vendedorUid = vendedorUid,
                vendedorNombre = vendedorNombre,
                timestamp = timestamp
            )

            val payload = mapOf(
                "id" to creado.id,
                "firestoreId" to creado.firestoreId,
                "comunidadId" to creado.comunidadId,
                "titulo" to creado.titulo,
                "precio" to creado.precio,
                "ubicacionYTiempo" to creado.ubicacionYTiempo,
                "categoria" to creado.categoriaProducto.name,
                "esParaComprar" to creado.esParaComprar,
                "vendedorUid" to creado.vendedorUid,
                "vendedorNombre" to creado.vendedorNombre,
                "timestamp" to creado.timestamp
            )

            docRef.set(payload).await()
            Result.success(creado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un anuncio verificando que pertenezca a la comunidad activa.
     */
    suspend fun eliminarProducto(
        producto: Producto,
        comunidadIdActual: String
    ): Result<Unit> {
        if (comunidadIdActual.isBlank() || producto.comunidadId != comunidadIdActual) {
            return Result.failure(SecurityException("No puedes eliminar un anuncio de otra comunidad."))
        }
        if (producto.firestoreId.isBlank()) {
            return Result.failure(IllegalArgumentException("El anuncio no tiene identificador remoto."))
        }

        return try {
            db.collection(COLECCION_PRODUCTOS)
                .document(producto.firestoreId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_PRODUCTOS = "productos"
    }
}
