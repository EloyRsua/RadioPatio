package com.example.radiopatio.mensajeria

import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Crea una conversación nueva si no existe, o devuelve la que ya estaba abierta
     * entre ese comprador y ese vendedor para el mismo producto.
     */
    suspend fun abrirOCrearConversacion(
        comunidadId: String,
        productoId: String,
        productoTitulo: String,
        productoPrecio: String,
        productoImagenUrl: String,
        vendedorUid: String,
        vendedorNombre: String,
        compradorUid: String,
        compradorNombre: String
    ): Result<Conversacion> {
        if (comunidadId.isBlank() || productoId.isBlank() || vendedorUid.isBlank() || compradorUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Faltan datos obligatorios para iniciar el chat."))
        }
        if (vendedorUid == compradorUid) {
            return Result.failure(IllegalStateException("No puedes iniciar un chat contigo mismo."))
        }

        return try {
            val conversacionId = generarIdConversacion(productoId, compradorUid, vendedorUid)
            val docRef = db.subcoleccionComunidad(comunidadId, "conversaciones").document(conversacionId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val existente = snapshotToConversacion(snapshot)
                    ?: Conversacion(
                        id = conversacionId,
                        comunidadId = comunidadId,
                        productoId = productoId,
                        productoTitulo = productoTitulo,
                        productoPrecio = productoPrecio,
                        productoImagenUrl = productoImagenUrl,
                        vendedorUid = vendedorUid,
                        vendedorNombre = vendedorNombre,
                        compradorUid = compradorUid,
                        compradorNombre = compradorNombre,
                        participantesUids = listOf(compradorUid, vendedorUid)
                    )
                Result.success(existente)
            } else {
                val nueva = Conversacion(
                    id = conversacionId,
                    comunidadId = comunidadId,
                    productoId = productoId,
                    productoTitulo = productoTitulo,
                    productoPrecio = productoPrecio,
                    productoImagenUrl = productoImagenUrl,
                    vendedorUid = vendedorUid,
                    vendedorNombre = vendedorNombre,
                    compradorUid = compradorUid,
                    compradorNombre = compradorNombre,
                    participantesUids = listOf(compradorUid, vendedorUid),
                    ultimoMensaje = "",
                    ultimoMensajeTimestamp = System.currentTimeMillis(),
                    ultimoRemitenteUid = "",
                    mensajesNoLeidosVendedor = 0,
                    mensajesNoLeidosComprador = 0
                )

                val datos = mapOf(
                    "id" to nueva.id,
                    "comunidadId" to nueva.comunidadId,
                    "productoId" to nueva.productoId,
                    "productoTitulo" to nueva.productoTitulo,
                    "productoPrecio" to nueva.productoPrecio,
                    "productoImagenUrl" to nueva.productoImagenUrl,
                    "vendedorUid" to nueva.vendedorUid,
                    "vendedorNombre" to nueva.vendedorNombre,
                    "compradorUid" to nueva.compradorUid,
                    "compradorNombre" to nueva.compradorNombre,
                    "participantesUids" to nueva.participantesUids,
                    "ultimoMensaje" to nueva.ultimoMensaje,
                    "ultimoMensajeTimestamp" to nueva.ultimoMensajeTimestamp,
                    "ultimoRemitenteUid" to nueva.ultimoRemitenteUid,
                    "mensajesNoLeidosVendedor" to 0,
                    "mensajesNoLeidosComprador" to 0
                )

                docRef.set(datos).await()
                Result.success(nueva)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escucha en tiempo real todas las conversaciones en las que participa [miUid]
     * dentro de [comunidadId], ordenadas por el mensaje más reciente.
     */
    fun observarMisConversaciones(
        comunidadId: String,
        miUid: String
    ): Flow<List<Conversacion>> = callbackFlow {
        if (comunidadId.isBlank() || miUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.subcoleccionComunidad(comunidadId, "conversaciones")
            .whereArrayContains("participantesUids", miUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot.documents
                    .mapNotNull { snapshotToConversacion(it) }
                    .sortedByDescending { it.ultimoMensajeTimestamp }

                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Escucha en tiempo real los mensajes de una conversación concreta ordenados de más antiguo a más nuevo.
     */
    fun observarMensajes(
        comunidadId: String,
        conversacionId: String
    ): Flow<List<Mensaje>> = callbackFlow {
        if (comunidadId.isBlank() || conversacionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.subcoleccionComunidad(comunidadId, "conversaciones")
            .document(conversacionId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val mensajes = snapshot.documents.map { doc ->
                    Mensaje(
                        id = doc.id,
                        conversacionId = conversacionId,
                        remitenteUid = doc.getString("remitenteUid") ?: "",
                        remitenteNombre = doc.getString("remitenteNombre") ?: "",
                        texto = doc.getString("texto") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        leido = doc.getBoolean("leido") ?: false
                    )
                }

                trySend(mensajes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Envía un mensaje nuevo a la subcolección `mensajes` y actualiza la cabecera de la conversación.
     */
    suspend fun enviarMensaje(
        comunidadId: String,
        conversacion: Conversacion,
        remitenteUid: String,
        remitenteNombre: String,
        texto: String
    ): Result<Unit> {
        val textoLimpio = texto.trim()
        if (comunidadId.isBlank() || conversacion.id.isBlank() || textoLimpio.isBlank()) {
            return Result.failure(IllegalArgumentException("El mensaje no puede estar vacío."))
        }

        return try {
            val convRef = db.subcoleccionComunidad(comunidadId, "conversaciones").document(conversacion.id)
            val msgRef = convRef.collection("mensajes").document()
            val ahora = System.currentTimeMillis()

            val mensajeMap = mapOf(
                "id" to msgRef.id,
                "conversacionId" to conversacion.id,
                "remitenteUid" to remitenteUid,
                "remitenteNombre" to remitenteNombre,
                "texto" to textoLimpio,
                "timestamp" to ahora,
                "leido" to false
            )

            msgRef.set(mensajeMap).await()

            // Incrementamos los no leídos del destinatario
            val campoNoLeidosDestinatario = if (remitenteUid == conversacion.vendedorUid) {
                "mensajesNoLeidosComprador"
            } else {
                "mensajesNoLeidosVendedor"
            }

            convRef.update(
                mapOf(
                    "ultimoMensaje" to textoLimpio,
                    "ultimoMensajeTimestamp" to ahora,
                    "ultimoRemitenteUid" to remitenteUid,
                    campoNoLeidosDestinatario to FieldValue.increment(1)
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pone a 0 el contador de mensajes no leídos del usuario actual al entrar en el chat.
     */
    suspend fun marcarConversacionComoLeida(
        comunidadId: String,
        conversacion: Conversacion,
        miUid: String
    ) {
        if (comunidadId.isBlank() || conversacion.id.isBlank() || miUid.isBlank()) return
        try {
            val campoPropiosNoLeidos = if (miUid == conversacion.vendedorUid) {
                "mensajesNoLeidosVendedor"
            } else {
                "mensajesNoLeidosComprador"
            }

            db.subcoleccionComunidad(comunidadId, "conversaciones")
                .document(conversacion.id)
                .update(campoPropiosNoLeidos, 0)
                .await()
        } catch (_: Exception) {
            // Ignorar si el documento aún no se ha inicializado
        }
    }

    private fun snapshotToConversacion(doc: DocumentSnapshot): Conversacion? {
        if (!doc.exists()) return null
        @Suppress("UNCHECKED_CAST")
        val participantes = doc.get("participantesUids") as? List<String> ?: emptyList()

        return Conversacion(
            id = doc.id,
            comunidadId = doc.getString("comunidadId") ?: "",
            productoId = doc.getString("productoId") ?: "",
            productoTitulo = doc.getString("productoTitulo") ?: "",
            productoPrecio = doc.getString("productoPrecio") ?: "",
            productoImagenUrl = doc.getString("productoImagenUrl") ?: "",
            vendedorUid = doc.getString("vendedorUid") ?: "",
            vendedorNombre = doc.getString("vendedorNombre") ?: "",
            compradorUid = doc.getString("compradorUid") ?: "",
            compradorNombre = doc.getString("compradorNombre") ?: "",
            participantesUids = participantes,
            ultimoMensaje = doc.getString("ultimoMensaje") ?: "",
            ultimoMensajeTimestamp = doc.getLong("ultimoMensajeTimestamp") ?: 0L,
            ultimoRemitenteUid = doc.getString("ultimoRemitenteUid") ?: "",
            mensajesNoLeidosVendedor = (doc.getLong("mensajesNoLeidosVendedor") ?: 0L).toInt(),
            mensajesNoLeidosComprador = (doc.getLong("mensajesNoLeidosComprador") ?: 0L).toInt()
        )
    }
}