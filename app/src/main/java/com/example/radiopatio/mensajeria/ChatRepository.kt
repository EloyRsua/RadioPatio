package com.example.radiopatio.mensajeria

import com.example.radiopatio.notificacion.NotificacionesHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun conversacionesCollection(comunidadId: String) =
        db.collection("comunidades")
            .document(comunidadId)
            .collection("conversaciones")

    suspend fun obtenerOCrearConversacion(
        comunidadId: String,
        productoId: String,
        productoTitulo: String,
        productoPrecio: String,
        productoImagenUrl: String,
        vendedorUid: String,
        vendedorNombre: String,
        compradorUid: String,
        compradorNombre: String
    ): Conversacion {
        val convId = generarIdConversacion(productoId, compradorUid, vendedorUid)
        val docRef = conversacionesCollection(comunidadId).document(convId)
        val snap = docRef.get().await()

        if (snap.exists()) {
            return snap.toObject(Conversacion::class.java)?.copy(id = docRef.id)
                ?: Conversacion(
                    id = docRef.id,
                    comunidadId = comunidadId,
                    productoId = productoId,
                    productoTitulo = productoTitulo,
                    productoPrecio = productoPrecio,
                    productoImagenUrl = productoImagenUrl,
                    vendedorUid = vendedorUid,
                    vendedorNombre = vendedorNombre,
                    compradorUid = compradorUid,
                    compradorNombre = compradorNombre,
                    participantesUids = listOfNotNull(
                        vendedorUid.takeIf { it.isNotBlank() },
                        compradorUid.takeIf { it.isNotBlank() }
                    ).distinct()
                )
        }

        val nueva = Conversacion(
            id = convId,
            comunidadId = comunidadId,
            productoId = productoId,
            productoTitulo = productoTitulo,
            productoPrecio = productoPrecio,
            productoImagenUrl = productoImagenUrl,
            vendedorUid = vendedorUid,
            vendedorNombre = vendedorNombre.ifBlank { "Vendedor" },
            compradorUid = compradorUid,
            compradorNombre = compradorNombre.ifBlank { "Vecino" },
            participantesUids = listOfNotNull(
                vendedorUid.takeIf { it.isNotBlank() },
                compradorUid.takeIf { it.isNotBlank() }
            ).distinct()
        )

        docRef.set(nueva).await()
        return nueva
    }

    fun observarMensajes(
        comunidadId: String,
        conversacionId: String,
        onMensajes: (List<Mensaje>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return conversacionesCollection(comunidadId)
            .document(conversacionId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error al cargar los mensajes.")
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Mensaje::class.java)?.copy(id = doc.id)
                }.orEmpty()
                onMensajes(lista)
            }
    }

    suspend fun enviarMensaje(
        comunidadId: String,
        conversacionId: String,
        productoId: String,
        productoTitulo: String,
        remitenteUid: String,
        remitenteNombre: String,
        destinatarioUid: String,
        texto: String
    ) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return

        val ahora = System.currentTimeMillis()
        val convRef = conversacionesCollection(comunidadId).document(conversacionId)
        val msgRef = convRef.collection("mensajes").document()

        val mensaje = Mensaje(
            id = msgRef.id,
            conversacionId = conversacionId,
            remitenteUid = remitenteUid,
            remitenteNombre = remitenteNombre.ifBlank { "Vecino" },
            texto = limpio,
            timestamp = ahora,
            leido = false
        )

        db.runBatch { batch ->
            batch.set(msgRef, mensaje)
            batch.set(
                convRef,
                mapOf(
                    "ultimoMensaje" to limpio,
                    "ultimoMensajeTimestamp" to ahora,
                    "ultimoRemitenteUid" to remitenteUid
                ),
                SetOptions.merge()
            )
        }.await()

        if (destinatarioUid.isNotBlank() && destinatarioUid != remitenteUid) {
            val remitenteCorto = remitenteNombre.ifBlank { "Un vecino" }
            val tituloArticulo = productoTitulo.ifBlank { "Artículo" }
            val resumen = if (limpio.length > 70) "${limpio.take(67)}..." else limpio
            NotificacionesHelper.enviarNotificacion(
                destinatarioUid = destinatarioUid,
                titulo = "Nuevo mensaje · $tituloArticulo",
                mensaje = "$remitenteCorto: \"$resumen\"",
                productoId = productoId
            )
        }
    }
}