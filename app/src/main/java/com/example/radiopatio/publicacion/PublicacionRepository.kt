package com.example.radiopatio.publicacion

import android.util.Log
import com.example.radiopatio.comunidad.migrarDocumentosRaizAComunidad
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.example.radiopatio.data.PublicacionEntidad
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar las publicaciones del tablón en tiempo real con Cloud Firestore
 * dentro de la subcolección `comunidades/{comunidadId}/publicaciones`,
 * aplicando aislamiento obligatorio por `comunidadId`.
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
    fun observarPublicacionesDeComunidad(
        comunidadId: String,
        onError: (String) -> Unit = {}
    ): Flow<List<PublicacionEntidad>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        launch {
            db.migrarDocumentosRaizAComunidad(comunidadId, COLECCION_PUBLICACIONES)
        }

        var listener: ListenerRegistration? = null
        var emitioAlMenosUnaVez = false
        val subcoleccionRef = db.subcoleccionComunidad(comunidadId, COLECCION_PUBLICACIONES)
        Log.i("PublicacionRepository", "Suscribiendo listener a ${subcoleccionRef.path}")

        try {
            listener = subcoleccionRef
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PublicacionRepository", "Error escuchando publicaciones en ${subcoleccionRef.path}", error)
                        onError("Error de Firestore al cargar publicaciones: ${error.localizedMessage ?: error.code}")
                        if (!emitioAlMenosUnaVez) {
                            emitioAlMenosUnaVez = true
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }

                    val publicaciones = snapshot?.documents
                        ?.mapNotNull { documentToPublicacion(it) }
                        ?.filter { it.comunidadId == comunidadId }
                        ?.sortedByDescending { it.timestamp }
                        ?: emptyList()

                    Log.i(
                        "PublicacionRepository",
                        "Snapshot ${subcoleccionRef.path}: ${publicaciones.size} docs (fromCache=${snapshot?.metadata?.isFromCache}, pending=${snapshot?.metadata?.hasPendingWrites()})"
                    )
                    emitioAlMenosUnaVez = true
                    trySend(publicaciones)
                }
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Excepción escuchando publicaciones en ${subcoleccionRef.path}", e)
            onError("Error al conectar con Firestore: ${e.localizedMessage ?: "Error desconocido"}")
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
     * Crea una nueva publicación en la subcolección `comunidades/{comunidadId}/publicaciones`
     * asociada obligatoriamente a [comunidadId].
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
            val docRef = db.subcoleccionComunidad(comunidadId, COLECCION_PUBLICACIONES).document()
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
            Log.i("PublicacionRepository", "Publicación guardada en Firestore: ${docRef.path}")
            Result.success(entidad)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error creando publicación en Firestore", e)
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
                db.subcoleccionComunidad(comunidadIdActual, COLECCION_PUBLICACIONES)
                    .document(publicacion.firestoreId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error eliminando publicación en Firestore", e)
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_PUBLICACIONES = "publicaciones"
    }
}
