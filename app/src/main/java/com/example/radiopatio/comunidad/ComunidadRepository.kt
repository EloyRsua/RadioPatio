package com.example.radiopatio.comunidad

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar la consulta, creación y sincronización en tiempo real
 * de comunidades de vecinos en Cloud Firestore.
 */
class ComunidadRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Convierte un [DocumentSnapshot] de Firestore en un objeto [Comunidad].
     */
    fun documentToComunidad(doc: DocumentSnapshot): Comunidad {
        return Comunidad(
            id = doc.getString("id")?.takeIf { it.isNotBlank() } ?: doc.id,
            nombre = doc.getString("nombre") ?: "",
            direccion = doc.getString("direccion") ?: "",
            ciudad = doc.getString("ciudad") ?: "",
            codigoPostal = doc.getString("codigoPostal") ?: "",
            descripcion = doc.getString("descripcion") ?: "",
            tipo = doc.getString("tipo") ?: "Residencial",
            totalVecinos = (doc.getLong("totalVecinos") ?: 0L).toInt().coerceAtLeast(0),
            photoUrl = doc.getString("photoUrl") ?: "",
            verificada = doc.getBoolean("verificada") ?: false,
            createdByUid = doc.getString("createdByUid") ?: ""
        )
    }

    /**
     * Convierte una instancia de [Comunidad] en un mapa serializable para Firestore.
     */
    fun comunidadToMap(comunidad: Comunidad): Map<String, Any?> {
        return mapOf(
            "id" to comunidad.id,
            "nombre" to comunidad.nombre,
            "direccion" to comunidad.direccion,
            "ciudad" to comunidad.ciudad,
            "codigoPostal" to comunidad.codigoPostal,
            "descripcion" to comunidad.descripcion,
            "tipo" to comunidad.tipo,
            "totalVecinos" to comunidad.totalVecinos,
            "photoUrl" to comunidad.photoUrl,
            "verificada" to comunidad.verificada,
            "createdByUid" to comunidad.createdByUid
        )
    }

    /**
     * Observa en tiempo real los datos de una comunidad concreta por su [comunidadId].
     * Si [comunidadId] está vacío, emite `null` inmediatamente sin consultar Firestore.
     */
    fun observarComunidadPorId(comunidadId: String): Flow<Comunidad?> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_COMUNIDADES)
                .document(comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(documentToComunidad(snapshot))
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Observa en tiempo real el catálogo de comunidades disponibles en Firestore.
     */
    fun observarComunidades(): Flow<List<Comunidad>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_COMUNIDADES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val lista = snapshot?.documents
                        ?.map { documentToComunidad(it) }
                        ?.sortedBy { it.nombre.lowercase() }
                        ?: emptyList()
                    trySend(lista)
                }
        } catch (e: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Consulta puntual de una comunidad por su identificador.
     */
    suspend fun obtenerComunidadPorId(comunidadId: String): Result<Comunidad?> {
        if (comunidadId.isBlank()) return Result.success(null)
        return try {
            val doc = db.collection(COLECCION_COMUNIDADES).document(comunidadId).get().await()
            if (doc.exists()) {
                Result.success(documentToComunidad(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva comunidad en Firestore con ID autogenerado por la base de datos.
     */
    suspend fun crearComunidad(
        nombre: String,
        direccion: String,
        ciudad: String = "",
        codigoPostal: String = "",
        descripcion: String = "",
        tipo: String = "Residencial",
        createdByUid: String = ""
    ): Result<Comunidad> {
        val nombreLimpio = nombre.trim()
        val direccionLimpia = direccion.trim()
        if (nombreLimpio.isBlank() || direccionLimpia.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre y la dirección de la comunidad son obligatorios."))
        }

        return try {
            val docRef = db.collection(COLECCION_COMUNIDADES).document()
            val nuevaComunidad = Comunidad(
                id = docRef.id,
                nombre = nombreLimpio,
                direccion = direccionLimpia,
                ciudad = ciudad.trim(),
                codigoPostal = codigoPostal.trim(),
                descripcion = descripcion.trim().ifBlank { "Comunidad de vecinos de $nombreLimpio" },
                tipo = tipo.ifBlank { "Residencial" },
                totalVecinos = 0,
                photoUrl = "",
                verificada = false,
                createdByUid = createdByUid
            )
            docRef.set(comunidadToMap(nuevaComunidad)).await()
            Result.success(nuevaComunidad)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ajusta de forma atómica el contador de vecinos de la comunidad indicada.
     */
    suspend fun ajustarContadorVecinos(comunidadId: String, delta: Long) {
        if (comunidadId.isBlank()) return
        try {
            db.collection(COLECCION_COMUNIDADES)
                .document(comunidadId)
                .update("totalVecinos", FieldValue.increment(delta))
                .await()
        } catch (_: Exception) {
            // Si el documento aún no existe o no permite incremento, se ignora sin bloquear el flujo principal
        }
    }

    companion object {
        const val COLECCION_COMUNIDADES = "comunidades"
    }
}
