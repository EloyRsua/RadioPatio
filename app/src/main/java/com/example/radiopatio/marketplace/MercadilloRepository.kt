package com.example.radiopatio.marketplace

import android.util.Log
import com.example.radiopatio.comunidad.migrarDocumentosRaizAComunidad
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import com.google.firebase.firestore.FieldValue

/**
 * Repositorio de Cloud Firestore para el módulo de Mercadillo dentro de la subcolección
 * `comunidades/{comunidadId}/productos`, con sincronización en tiempo real
 * y aislamiento obligatorio por `comunidadId`.
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

        val estadoStr = doc.getString("estado") ?: EstadoProducto.DISPONIBLE.name
        val estado = try {
            EstadoProducto.valueOf(estadoStr)
        } catch (e: Exception) {
            EstadoProducto.DISPONIBLE
        }

        @Suppress("UNCHECKED_CAST")
        val fotos = doc.get("fotos") as? List<String> ?: emptyList()
        val autorUid = doc.getString("autorUid") ?: doc.getString("vendedorUid") ?: ""

        @Suppress("UNCHECKED_CAST")
        val likes = doc.get("likes") as? List<String> ?: emptyList()

        val ubicacionRaw = doc.getString("ubicacion") ?: doc.getString("ubicacionYTiempo") ?: "Comunidad"
        val ubicacionLimpia = ubicacionRaw.substringBefore("·").trim().ifBlank { "Comunidad" }
        val timestamp = doc.getLong("timestamp") ?: doc.getLong("fechaCreacion") ?: 0L
        val tiempoString = calcularTiempoTranscurrido(timestamp)
        val ubicacionYTiempo = "$ubicacionLimpia · $tiempoString"

        return Producto(
            id = numericId,
            titulo = doc.getString("titulo") ?: "",
            descripcion = doc.getString("descripcion") ?: "",
            precio = doc.getString("precio") ?: "",
            ubicacionYTiempo = ubicacionYTiempo,
            categoriaProducto = categoria,
            esParaComprar = doc.getBoolean("esParaComprar") ?: true,
            imagenRes = null,
            imagenUrl = fotos.firstOrNull() ?: doc.getString("imagenUrl"),
            firestoreId = doc.id,
            comunidadId = comunidadId,
            vendedorUid = doc.getString("vendedorUid") ?: autorUid,
            vendedorNombre = doc.getString("vendedorNombre") ?: "",
            timestamp = doc.getLong("timestamp") ?: doc.getLong("fechaCreacion") ?: 0L,
            autorUid = autorUid,
            compradorUid = doc.getString("compradorUid") ?: "",
            marca = doc.getString("marca") ?: "",
            color = doc.getString("color") ?: "",
            estadoUso = doc.getString("estadoUso") ?: "",
            fotos = fotos,
            estado = estado,
            plazoDiasEntrega = doc.getLong("plazoDiasEntrega")?.toInt() ?: 7,
            diasPrestamo = doc.getLong("diasPrestamo")?.toInt() ?: 7,
            fechaDevolucion = doc.getString("fechaDevolucion") ?: "",
            likes = likes
        )
    }

    /**
     * Observa en tiempo real los anuncios del mercadillo pertenecientes exclusivamente a [comunidadId].
     */
    fun observarProductosDeComunidad(
        comunidadId: String,
        onError: (String) -> Unit = {}
    ): Flow<List<Producto>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        launch {
            db.migrarDocumentosRaizAComunidad(comunidadId, COLECCION_PRODUCTOS)
        }

        var listener: ListenerRegistration? = null
        var emitioAlMenosUnaVez = false
        val subcoleccionRef = db.subcoleccionComunidad(comunidadId, COLECCION_PRODUCTOS)
        Log.i("MercadilloRepository", "Suscribiendo listener a ${subcoleccionRef.path}")

        try {
            listener = subcoleccionRef
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MercadilloRepository", "Error escuchando productos en ${subcoleccionRef.path}", error)
                        onError("Error de Firestore al cargar mercadillo: ${error.localizedMessage ?: error.code}")
                        if (!emitioAlMenosUnaVez) {
                            emitioAlMenosUnaVez = true
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.documents
                        ?.mapNotNull { documentToProducto(it) }
                        ?.filter { 
                            it.comunidadId == comunidadId &&
                            it.estado != EstadoProducto.VENDIDO &&
                            it.estado != EstadoProducto.DEVUELTO
                        }
                        ?.sortedByDescending { it.timestamp }
                        ?: emptyList()

                    Log.i(
                        "MercadilloRepository",
                        "Snapshot ${subcoleccionRef.path}: ${lista.size} docs (fromCache=${snapshot?.metadata?.isFromCache}, pending=${snapshot?.metadata?.hasPendingWrites()})"
                    )
                    emitioAlMenosUnaVez = true
                    trySend(lista)
                }
        } catch (e: Exception) {
            Log.e("MercadilloRepository", "Excepción escuchando productos en ${subcoleccionRef.path}", e)
            onError("Error al conectar mercadillo con Firestore: ${e.localizedMessage ?: "Error desconocido"}")
            if (!emitioAlMenosUnaVez) {
                emitioAlMenosUnaVez = true
                trySend(emptyList())
            }
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Publica un nuevo anuncio en `comunidades/{comunidadId}/productos`
     * asociándolo obligatoriamente a [comunidadId].
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
            val docRef = db.subcoleccionComunidad(comunidadId, COLECCION_PRODUCTOS).document()
            val timestamp = System.currentTimeMillis()
            val creado = producto.copy(
                id = abs(docRef.id.hashCode()),
                firestoreId = docRef.id,
                comunidadId = comunidadId,
                vendedorUid = vendedorUid,
                autorUid = producto.autorUid.ifBlank { vendedorUid },
                vendedorNombre = vendedorNombre,
                timestamp = timestamp
            )

            val ubicacionLimpia = creado.ubicacionYTiempo.substringBefore("·").trim().ifBlank { "Mi portal" }

            val payload = mapOf(
                "id" to creado.id,
                "firestoreId" to creado.firestoreId,
                "comunidadId" to creado.comunidadId,
                "titulo" to creado.titulo,
                "descripcion" to creado.descripcion,
                "precio" to creado.precio,
                "ubicacion" to ubicacionLimpia,
                "ubicacionYTiempo" to ubicacionLimpia,
                "categoria" to creado.categoriaProducto.name,
                "esParaComprar" to creado.esParaComprar,
                "vendedorUid" to creado.vendedorUid,
                "autorUid" to creado.autorUid,
                "vendedorNombre" to creado.vendedorNombre,
                "timestamp" to creado.timestamp,
                "fechaCreacion" to creado.timestamp,
                "marca" to creado.marca,
                "color" to creado.color,
                "estadoUso" to creado.estadoUso,
                "fotos" to creado.fotos,
                "plazoDiasEntrega" to creado.plazoDiasEntrega,
                "diasPrestamo" to creado.diasPrestamo,
                "estado" to creado.estado.name,
                "likes" to emptyList<String>()
            )

            docRef.set(payload).await()
            Log.i("MercadilloRepository", "Producto guardado en Firestore: ${docRef.path}")
            Result.success(creado)
        } catch (e: Exception) {
            Log.e("MercadilloRepository", "Error creando producto en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un anuncio de Firestore verificando que pertenezca a la comunidad activa.
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
            db.subcoleccionComunidad(comunidadIdActual, COLECCION_PRODUCTOS)
                .document(producto.firestoreId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MercadilloRepository", "Error eliminando producto en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Añade o quita el producto de los favoritos del usuario en la BD
     */
    suspend fun toggleMeGustaProducto(
        comunidadId: String,
        productoFirestoreId: String,
        usuarioUid: String
    ): Result<Boolean> {
        if (comunidadId.isBlank() || productoFirestoreId.isBlank() || usuarioUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Faltan datos para reaccionar al producto."))
        }

        return try {
            val docRef = db.subcoleccionComunidad(comunidadId, COLECCION_PRODUCTOS)
                .document(productoFirestoreId)

            val snapshot = docRef.get().await()

            val autorUid = snapshot.getString("autorUid") ?: snapshot.getString("vendedorUid") ?: ""
            val titulo = snapshot.getString("titulo") ?: "tu artículo"

            if (autorUid == usuarioUid) {
                return Result.success(false)
            }

            @Suppress("UNCHECKED_CAST")
            val likes = snapshot.get("likes") as? List<String> ?: emptyList()
            val yaEsFavorito = likes.contains(usuarioUid)

            if (yaEsFavorito) {
                // Quitar de favoritos
                docRef.update("likes", FieldValue.arrayRemove(usuarioUid)).await()
                Result.success(false) // Ya no es favorito
            } else {
                // Añadir a favoritos
                docRef.update("likes", FieldValue.arrayUnion(usuarioUid)).await()

                // Enviar notificacion
                if (autorUid.isNotBlank()) {
                    com.example.radiopatio.notificacion.NotificacionesHelper.enviarNotificacion(
                        destinatarioUid = autorUid,
                        titulo = "¡A alguien le gusta tu artículo!",
                        mensaje = "A un vecino le ha gustado tu artículo \"$titulo\".",
                        productoId = productoFirestoreId
                    )
                }

                Result.success(true) // Ahora es favorito
            }
        } catch (e: Exception) {
            Log.e("MercadilloRepository", "Error en toggleMeGustaProducto", e)
            Result.failure(e)
        }
    }

    fun calcularTiempoTranscurrido(tiempoMilisegundos: Long): String {
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - tiempoMilisegundos

        val segundos = diferencia / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        return when {
            segundos < 60 -> "Ahora mismo"
            minutos < 60 -> "Hace $minutos min"
            horas < 24 -> "Hace $horas h"
            dias == 1L -> "Ayer"
            else -> "Hace $dias d"
        }
    }

    /**
     * Mapea una entidad local de Room [ProductoEntidad] a [Producto].
     */
    fun entidadToProducto(entidad: com.example.radiopatio.data.producto.ProductoEntidad): Producto {
        val tiempoString = calcularTiempoTranscurrido(entidad.fechaCreacion)
        return Producto(
            id = entidad.id,
            titulo = entidad.titulo,
            precio = entidad.precio,
            ubicacionYTiempo = "${entidad.ubicacion} · $tiempoString",
            categoriaProducto = try {
                CategoriaProducto.valueOf(entidad.categoria)
            } catch (e: Exception) {
                CategoriaProducto.TODOS
            },
            esParaComprar = entidad.esParaComprar,
            imagenUrl = entidad.imagenUrl,
            firestoreId = entidad.firestoreId,
            autorUid = entidad.autorUid,
            vendedorUid = entidad.autorUid,
            descripcion = entidad.descripcion,
            timestamp = entidad.fechaCreacion,
            marca = entidad.marca,
            color = entidad.color,
            estadoUso = entidad.estadoUso,
            fotos = if (!entidad.imagenUrl.isNullOrBlank()) listOf(entidad.imagenUrl) else emptyList(),
            estado = try {
                EstadoProducto.valueOf(entidad.estado)
            } catch (e: Exception) {
                EstadoProducto.DISPONIBLE
            }
        )
    }

    companion object {
        const val COLECCION_PRODUCTOS = "productos"
    }
}
