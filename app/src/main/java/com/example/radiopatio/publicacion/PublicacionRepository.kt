package com.example.radiopatio.publicacion

import com.example.radiopatio.data.PublicacionEntidad
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar las publicaciones del tablón en tiempo real con Cloud Firestore,
 * aplicando aislamiento multi-inquilino obligatorio por `comunidadId`.
 */
class PublicacionRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Mapea un [DocumentSnapshot] de Firestore a [PublicacionEntidad].
     */
    fun documentToPublicacion(doc: DocumentSnapshot): PublicacionEntidad? {
        val comunidadId = doc.getString("comunidadId") ?: return null
        if (comunidadId.isBlank()) return null

        val autor = doc.getString("autor") ?: "Vecino"
        val inicial = doc.getString("inicial")?.takeIf { it.isNotBlank() }
            ?: autor.trim().firstOrNull()?.uppercase()
            ?: "V"

        return PublicacionEntidad(
            firestoreId = doc.id,
            comunidadId = comunidadId,
            autorUid = doc.getString("autorUid") ?: "",
            autor = autor,
            inicial = inicial,
            tiempoYUbica = doc.getString("tiempoYUbica") ?: "Comunidad",
            contenido = doc.getString("contenido") ?: "",
            meGusta = (doc.getLong("meGusta") ?: 0L).toInt(),
            comentarios = (doc.getLong("comentarios") ?: 0L).toInt(),
            timestamp = doc.getLong("timestamp") ?: 0L
        )
    }

    /**
     * Escucha en tiempo real únicamente las publicaciones pertenecientes a [comunidadId].
     */
    fun observarPublicacionesDeComunidad(comunidadId: String): Flow<List<PublicacionEntidad>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_PUBLICACIONES)
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val publicaciones = snapshot?.documents
                        ?.mapNotNull { documentToPublicacion(it) }
                        ?.filter { it.comunidadId == comunidadId }
                        ?.sortedByDescending { it.timestamp }
                        ?: emptyList()

                    trySend(publicaciones)
                }
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Crea una nueva publicación en Firestore asociada obligatoriamente a [comunidadId].
     */
    suspend fun crearPublicacion(
        comunidadId: String,
        autorUid: String,
        autor: String,
        inicial: String,
        tiempoYUbica: String,
        contenido: String
    ): Result<PublicacionEntidad> {
        if (comunidadId.isBlank()) {
            return Result.failure(IllegalStateException("Debes pertenecer a una comunidad para publicar."))
        }
        if (contenido.isBlank()) {
            return Result.failure(IllegalArgumentException("El contenido de la publicación no puede estar vacío."))
        }

        return try {
            val docRef = db.collection(COLECCION_PUBLICACIONES).document()
            val timestamp = System.currentTimeMillis()
            val entidad = PublicacionEntidad(
                firestoreId = docRef.id,
                comunidadId = comunidadId,
                autorUid = autorUid,
                autor = autor,
                inicial = inicial,
                tiempoYUbica = tiempoYUbica,
                contenido = contenido,
                meGusta = 0,
                comentarios = 0,
                timestamp = timestamp
            )

            val payload = mapOf(
                "firestoreId" to entidad.firestoreId,
                "comunidadId" to entidad.comunidadId,
                "autorUid" to entidad.autorUid,
                "autor" to entidad.autor,
                "inicial" to entidad.inicial,
                "tiempoYUbica" to entidad.tiempoYUbica,
                "contenido" to entidad.contenido,
                "meGusta" to entidad.meGusta,
                "comentarios" to entidad.comentarios,
                "timestamp" to entidad.timestamp
            )

            docRef.set(payload).await()
            Result.success(entidad)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una publicación de Firestore verificando que pertenezca a la comunidad activa.
     */
    suspend fun eliminarPublicacion(
        publicacion: PublicacionEntidad,
        comunidadIdActual: String
    ): Result<Unit> {
        if (comunidadIdActual.isBlank() || publicacion.comunidadId != comunidadIdActual) {
            return Result.failure(SecurityException("No puedes eliminar una publicación de otra comunidad."))
        }

        return try {
            if (publicacion.firestoreId.isNotBlank()) {
                db.collection(COLECCION_PUBLICACIONES)
                    .document(publicacion.firestoreId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_PUBLICACIONES = "publicaciones"
    }
}
