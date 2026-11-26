package com.example.radiopatio.marketplace

import android.util.Log
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.example.radiopatio.data.producto.ProductoDao
import com.example.radiopatio.data.producto.ProductoEntidad
import com.example.radiopatio.notificacion.NotificacionesHelper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio de Cloud Firestore para el módulo de Mercadillo dentro de la subcolección
 * `comunidades/{comunidadId}/productos`, con sincronización en tiempo real
 * y aislamiento obligatorio por `comunidadId`.
 */
class MercadilloRepository(
    private val productoDao: ProductoDao,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // 1. Observacion desde room

    fun observarProductos(): Flow<List<Producto>> {
        return productoDao.observarTodos().map { entidades ->
            entidades.map { entidadToProducto(it) }
        }
    }

    fun observarProductoPorId(id: String): Flow<Producto?> {
        return productoDao.observarPorId(id).map { entidad ->
            entidad?.let { entidadToProducto(it) }
        }
    }

    // 2. Actualizacion desde firestore hacia room
    suspend fun actualizarProductos(comunidadId: String){
        if(comunidadId.isBlank()){
            productoDao.eliminarTodos()
            return
        }

        val snapshot = db.subcoleccionComunidad(comunidadId, COLECCION_PRODUCTOS)
            .get().await()

        val entidades = snapshot.documents.mapNotNull { doc ->
            documentToEntidad(doc, comunidadId)
        }

        productoDao.sustituirTodos(entidades)

    }

    // 3. Operaciones de escritura
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
            val fechaCreacion = System.currentTimeMillis()
            val ubicacionLimpia = producto.ubicacion.trim().ifBlank { "Mi portal" }

            val creado = producto.copy(
                id = docRef.id,
                comunidadId = comunidadId,
                vendedorUid = vendedorUid,
                vendedorNombre = vendedorNombre,
                ubicacion = ubicacionLimpia,
                fechaCreacion = fechaCreacion,
                estado = EstadoProducto.DISPONIBLE,
                likes = emptyList()
            )

            val payload = mapOf(
                "id" to creado.id,
                "comunidadId" to creado.comunidadId,
                "vendedorUid" to creado.vendedorUid,
                "vendedorNombre" to creado.vendedorNombre,
                "compradorUid" to "",
                "titulo" to creado.titulo,
                "descripcion" to creado.descripcion,
                "precio" to creado.precio,
                "ubicacion" to creado.ubicacion,
                "fechaCreacion" to creado.fechaCreacion,
                "categoria" to creado.categoriaProducto.name,
                "esParaComprar" to creado.esParaComprar,
                "marca" to creado.marca,
                "color" to creado.color,
                "estadoUso" to creado.estadoUso,
                "fotos" to creado.fotos,
                "estado" to creado.estado.name,
                "plazoDiasEntrega" to creado.plazoDiasEntrega,
                "diasPrestamo" to creado.diasPrestamo,
                "likes" to emptyList<String>()
            )

            docRef.set(payload).await()
            actualizarProductos(comunidadId)
            Log.i(TAG, "Producto guardado en Firestore: ${docRef.path}")
            Result.success(creado)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando producto en Firestore", e)
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
        if (producto.id.isBlank()) {
            return Result.failure(IllegalArgumentException("El anuncio no tiene identificador remoto."))
        }

        return try {
            db.subcoleccionComunidad(comunidadIdActual, COLECCION_PRODUCTOS)
                .document(producto.id)
                .delete()
                .await()
            actualizarProductos(comunidadIdActual)
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
        productoId: String,
        usuarioUid: String
    ): Result<Boolean> {
        if (comunidadId.isBlank() || productoId.isBlank() || usuarioUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Faltan datos para reaccionar al producto."))
        }

        return try {
            val docRef = db.subcoleccionComunidad(comunidadId, COLECCION_PRODUCTOS)
                .document(productoId)

            val snapshot = docRef.get().await()

            val vendedorUid = snapshot.getString("vendedorUid") ?: ""
            val titulo = snapshot.getString("titulo") ?: "tu artículo"

            if (vendedorUid == usuarioUid) {
                return Result.success(false)
            }

            @Suppress("UNCHECKED_CAST")
            val likes = snapshot.get("likes") as? List<String> ?: emptyList()
            val yaEsFavorito = likes.contains(usuarioUid)

            if (yaEsFavorito) {
                // Quitar de favoritos
                docRef.update("likes", FieldValue.arrayRemove(usuarioUid)).await()
            } else {
                // Añadir a favoritos
                docRef.update("likes", FieldValue.arrayUnion(usuarioUid)).await()

                // Enviar notificacion
                if (vendedorUid.isNotBlank()) {
                    NotificacionesHelper.enviarNotificacion(
                        destinatarioUid = vendedorUid,
                        titulo = "¡A alguien le gusta tu artículo!",
                        mensaje = "A un vecino le ha gustado tu artículo \"$titulo\".",
                        productoId = productoId
                    )
                }
            }
            actualizarProductos(comunidadId)
            Result.success(!yaEsFavorito)
        } catch (e: Exception) {
            Log.e(TAG, "Error en toggleMeGustaProducto", e)
            Result.failure(e)
        }
    }

    suspend fun comprarOPrestar(
        producto: Producto,
        compradorUid: String,
        compradorNombre: String,
        compradorPiso: String
    ): Result<Unit> {
        if (producto.comunidadId.isBlank() || producto.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Producto sin comunidad o identificador."))
        }

        return try {
            val plazoDias = producto.plazoDiasEntrega
            val fechaActual = System.currentTimeMillis()
            val fechaLimite = fechaActual + (plazoDias * MILIS_POR_DIA)

            val updates = mapOf<String, Any>(
                "estado" to EstadoProducto.EN_ENTREGA.name,
                "compradorUid" to compradorUid,
                "compradorNombre" to compradorNombre,
                "compradorPiso" to compradorPiso,
                "fechaCompra" to fechaActual,
                "fechaLimiteEntrega" to fechaLimite,
                "vendedorConfirmoEntrega" to false,
                "compradorConfirmoRecepcion" to false
            )

            db.subcoleccionComunidad(producto.comunidadId, COLECCION_PRODUCTOS)
                .document(producto.id)
                .update(updates)
                .await()

            val datosVecino = if (compradorPiso.isNotBlank()) "$compradorNombre ($compradorPiso)" else compradorNombre
            val titulo = if (producto.esParaComprar) "¡Artículo vendido!" else "¡Préstamo solicitado!"
            val mensaje = if (producto.esParaComprar) {
                "El vecino $datosVecino ha comprado \"${producto.titulo}\". Tienes $plazoDias días para llevárselo."
            } else {
                "El vecino $datosVecino ha solicitado el préstamo de \"${producto.titulo}\"."
            }

            NotificacionesHelper.enviarNotificacion(
                destinatarioUid = producto.vendedorUid,
                titulo = titulo,
                mensaje = mensaje,
                productoId = producto.id
            )

            actualizarProductos(producto.comunidadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en comprarOPrestar", e)
            Result.failure(e)
        }
    }

    suspend fun confirmarEntregaVendedor(producto: Producto): Result<Unit> {
        return try {
            val estadoFinal = if (producto.esParaComprar) EstadoProducto.VENDIDO.name else EstadoProducto.PRESTADO.name
            val nuevoEstado = if (producto.compradorConfirmoRecepcion) estadoFinal else EstadoProducto.EN_ENTREGA.name

            val updates = mapOf<String, Any>(
                "vendedorConfirmoEntrega" to true,
                "estado" to nuevoEstado
            )

            db.subcoleccionComunidad(producto.comunidadId, COLECCION_PRODUCTOS)
                .document(producto.id)
                .update(updates)
                .await()

            NotificacionesHelper.enviarNotificacion(
                destinatarioUid = producto.compradorUid,
                titulo = "Producto entregado por el vendedor",
                mensaje = "El vendedor ha marcado \"${producto.titulo}\" como entregado. Por favor, confirma su recepción cuando lo tengas en mano.",
                productoId = producto.id
            )

            actualizarProductos(producto.comunidadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en confirmarEntregaVendedor", e)
            Result.failure(e)
        }
    }

    suspend fun confirmarRecepcionComprador(producto: Producto): Result<Unit> {
        return try {
            val estadoFinal = if (producto.esParaComprar) EstadoProducto.VENDIDO.name else EstadoProducto.PRESTADO.name
            val updates = hashMapOf<String, Any>(
                "compradorConfirmoRecepcion" to true,
                "estado" to estadoFinal
            )

            if (!producto.esParaComprar) {
                val fechaLimiteDevolucion = System.currentTimeMillis() + (producto.diasPrestamo * MILIS_POR_DIA)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                updates["fechaDevolucion"] = sdf.format(Date(fechaLimiteDevolucion))
            }

            db.subcoleccionComunidad(producto.comunidadId, COLECCION_PRODUCTOS)
                .document(producto.id)
                .update(updates)
                .await()

            NotificacionesHelper.enviarNotificacion(
                destinatarioUid = producto.vendedorUid,
                titulo = "Entrega completada",
                mensaje = "El comprador ha confirmado la recepción de \"${producto.titulo}\". ¡Trato finalizado con éxito!",
                productoId = producto.id
            )

            actualizarProductos(producto.comunidadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en confirmarRecepcionComprador", e)
            Result.failure(e)
        }
    }

    suspend fun reportarConflicto(producto: Producto, motivo: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any>(
                "estado" to EstadoProducto.EN_CONFLICTO.name,
                "motivoConflicto" to motivo
            )

            db.subcoleccionComunidad(producto.comunidadId, COLECCION_PRODUCTOS)
                .document(producto.id)
                .update(updates)
                .await()

            actualizarProductos(producto.comunidadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en reportarConflicto", e)
            Result.failure(e)
        }
    }

    suspend fun autocompletarEntregaSiVencida(producto: Producto) {
        val ahora = System.currentTimeMillis()
        if (producto.estado == EstadoProducto.EN_ENTREGA &&
            producto.fechaLimiteEntrega > 0 &&
            ahora > producto.fechaLimiteEntrega &&
            producto.motivoConflicto.isBlank()
        ) {
            try {
                val nuevoEstado = if (producto.esParaComprar) EstadoProducto.VENDIDO else EstadoProducto.PRESTADO
                db.subcoleccionComunidad(producto.comunidadId, COLECCION_PRODUCTOS)
                    .document(producto.id)
                    .update("estado", nuevoEstado.name)
                    .await()
                actualizarProductos(producto.comunidadId)
            } catch (e: Exception) {
                Log.e(TAG, "Error autocompletando entrega vencida", e)
            }
        }
    }

    // 4. Mappers
    private fun documentToEntidad(doc: DocumentSnapshot, comunidadId: String): ProductoEntidad? {
        val titulo = doc.getString("titulo") ?: return null
        val plazoDias = doc.getLong("plazoDiasEntrega")?.toInt() ?: 7
        val fechaCompra = doc.getLong("fechaCompra") ?: 0L
        val fechaLimite = doc.getLong("fechaLimiteEntrega")
            ?: if (fechaCompra > 0L) fechaCompra + (plazoDias * MILIS_POR_DIA) else 0L

        @Suppress("UNCHECKED_CAST")
        val fotos = doc.get("fotos") as? List<String> ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val likes = doc.get("likes") as? List<String> ?: emptyList()

        return ProductoEntidad(
            id = doc.id,
            comunidadId = doc.getString("comunidadId") ?: comunidadId,
            vendedorUid = doc.getString("vendedorUid") ?: "",
            vendedorNombre = doc.getString("vendedorNombre") ?: "",
            compradorUid = doc.getString("compradorUid") ?: "",
            titulo = titulo,
            descripcion = doc.getString("descripcion") ?: "",
            precio = doc.getString("precio") ?: "",
            ubicacion = doc.getString("ubicacion") ?: "Comunidad",
            fechaCreacion = doc.getLong("fechaCreacion") ?: 0L,
            categoria = doc.getString("categoria") ?: CategoriaProducto.MUEBLES.name,
            esParaComprar = doc.getBoolean("esParaComprar") ?: true,
            marca = doc.getString("marca") ?: "",
            color = doc.getString("color") ?: "",
            estadoUso = doc.getString("estadoUso") ?: "",
            fotosSerializadas = fotos.joinToString(SEPARADOR_LISTA),
            estado = doc.getString("estado") ?: EstadoProducto.DISPONIBLE.name,
            compradorNombre = doc.getString("compradorNombre") ?: "",
            compradorPiso = doc.getString("compradorPiso") ?: "",
            fechaCompra = fechaCompra,
            plazoDiasEntrega = plazoDias,
            diasPrestamo = doc.getLong("diasPrestamo")?.toInt() ?: 7,
            fechaLimiteEntrega = fechaLimite,
            vendedorConfirmoEntrega = doc.getBoolean("vendedorConfirmoEntrega") ?: false,
            compradorConfirmoRecepcion = doc.getBoolean("compradorConfirmoRecepcion") ?: false,
            motivoConflicto = doc.getString("motivoConflicto") ?: "",
            fechaDevolucion = doc.getString("fechaDevolucion") ?: "",
            likesSerializados = likes.joinToString(SEPARADOR_LISTA)
        )
    }

    private fun entidadToProducto(entidad: ProductoEntidad): Producto {
        val categoria = CategoriaProducto.entries.find {
            it.name.equals(entidad.categoria, ignoreCase = true) ||
                    it.nombre.equals(entidad.categoria, ignoreCase = true)
        } ?: CategoriaProducto.MUEBLES

        val estado = try {
            EstadoProducto.valueOf(entidad.estado)
        } catch (e: Exception) {
            EstadoProducto.DISPONIBLE
        }

        val fotos = entidad.fotosSerializadas
            .split(SEPARADOR_LISTA)
            .filter { it.isNotBlank() }

        val likes = entidad.likesSerializados
            .split(SEPARADOR_LISTA)
            .filter { it.isNotBlank() }

        return Producto(
            id = entidad.id,
            comunidadId = entidad.comunidadId,
            vendedorUid = entidad.vendedorUid,
            vendedorNombre = entidad.vendedorNombre,
            compradorUid = entidad.compradorUid,
            titulo = entidad.titulo,
            descripcion = entidad.descripcion,
            precio = entidad.precio,
            ubicacion = entidad.ubicacion,
            fechaCreacion = entidad.fechaCreacion,
            categoriaProducto = categoria,
            esParaComprar = entidad.esParaComprar,
            marca = entidad.marca,
            color = entidad.color,
            estadoUso = entidad.estadoUso,
            fotos = fotos,
            estado = estado,
            compradorNombre = entidad.compradorNombre,
            compradorPiso = entidad.compradorPiso,
            fechaCompra = entidad.fechaCompra,
            plazoDiasEntrega = entidad.plazoDiasEntrega,
            diasPrestamo = entidad.diasPrestamo,
            fechaLimiteEntrega = entidad.fechaLimiteEntrega,
            vendedorConfirmoEntrega = entidad.vendedorConfirmoEntrega,
            compradorConfirmoRecepcion = entidad.compradorConfirmoRecepcion,
            motivoConflicto = entidad.motivoConflicto,
            fechaDevolucion = entidad.fechaDevolucion,
            likes = likes
        )
    }

    companion object {
        private const val TAG = "MercadilloRepository"
        const val COLECCION_PRODUCTOS = "productos"
        private const val SEPARADOR_LISTA = "\n"
        private const val MILIS_POR_DIA = 24 * 60 * 60 * 1000L
    }
}
