package com.example.radiopatio

import android.util.Log
import com.example.radiopatio.comunidad.migrarDocumentosRaizAComunidad
import com.example.radiopatio.comunidad.subcoleccionComunidad
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Repositorio de Cloud Firestore para el Calendario de Eventos vecinales dentro de la
 * subcolección `comunidades/{comunidadId}/eventos`, con sincronización en tiempo real
 * y aislamiento obligatorio por `comunidadId`.
 */
class CalendarioRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Mapea un [DocumentSnapshot] de Firestore a [Evento].
     */
    fun documentToEvento(doc: DocumentSnapshot, currentUid: String): Evento? {
        val comunidadId = doc.getString("comunidadId") ?: return null
        if (comunidadId.isBlank()) return null

        val fechaStr = doc.getString("fecha") ?: ""
        val fecha = try {
            LocalDate.parse(fechaStr)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val catRaw = doc.getString("categoria") ?: CategoriaEvento.FIESTAS.name
        val categoria = CategoriaEvento.entries.find {
            it.name.equals(catRaw, ignoreCase = true) ||
                it.nombre.equals(catRaw, ignoreCase = true)
        } ?: CategoriaEvento.FIESTAS

        val asistentesUids = (doc.get("asistentesUids") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val recordatoriosUids = (doc.get("recordatoriosUids") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()

        val totalAsistentes = if (doc.contains("asistentesUids")) {
            asistentesUids.size
        } else {
            (doc.getLong("asistentes") ?: 0L).toInt()
        }

        return Evento(
            id = doc.id,
            titulo = doc.getString("titulo") ?: "",
            descripcion = doc.getString("descripcion") ?: "",
            fecha = fecha,
            hora = doc.getString("hora") ?: "Por determinar",
            ubicacion = doc.getString("ubicacion") ?: "",
            categoria = categoria,
            organizador = doc.getString("organizador") ?: "Vecino RadioPatio",
            asistentes = totalAsistentes,
            asistiendoUsuario = currentUid.isNotBlank() && currentUid in asistentesUids,
            recordatorioActivado = currentUid.isNotBlank() && currentUid in recordatoriosUids,
            esDestacado = doc.getBoolean("esDestacado") ?: false,
            comunidadId = comunidadId,
            creadorUid = doc.getString("creadorUid") ?: "",
            asistentesUids = asistentesUids,
            recordatoriosUids = recordatoriosUids
        )
    }

    /**
     * Observa en tiempo real los eventos pertenecientes exclusivamente a [comunidadId].
     */
    fun observarEventosDeComunidad(
        comunidadId: String,
        currentUid: String,
        onError: (String) -> Unit = {}
    ): Flow<List<Evento>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        launch {
            db.migrarDocumentosRaizAComunidad(comunidadId, COLECCION_EVENTOS)
        }

        var listener: ListenerRegistration? = null
        var emitioAlMenosUnaVez = false
        val subcoleccionRef = db.subcoleccionComunidad(comunidadId, COLECCION_EVENTOS)
        Log.i("CalendarioRepository", "Suscribiendo listener a ${subcoleccionRef.path}")

        try {
            listener = subcoleccionRef
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CalendarioRepository", "Error escuchando eventos en ${subcoleccionRef.path}", error)
                        onError("Error de Firestore al cargar eventos: ${error.localizedMessage ?: error.code}")
                        if (!emitioAlMenosUnaVez) {
                            emitioAlMenosUnaVez = true
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.documents
                        ?.mapNotNull { documentToEvento(it, currentUid) }
                        ?.filter { it.comunidadId == comunidadId }
                        ?.sortedBy { it.fecha }
                        ?: emptyList()

                    Log.i(
                        "CalendarioRepository",
                        "Snapshot ${subcoleccionRef.path}: ${lista.size} docs (fromCache=${snapshot?.metadata?.isFromCache}, pending=${snapshot?.metadata?.hasPendingWrites()})"
                    )
                    emitioAlMenosUnaVez = true
                    trySend(lista)
                }
        } catch (e: Exception) {
            Log.e("CalendarioRepository", "Excepción escuchando eventos en ${subcoleccionRef.path}", e)
            onError("Error al conectar eventos con Firestore: ${e.localizedMessage ?: "Error desconocido"}")
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
     * Crea un evento en `comunidades/{comunidadId}/eventos` asociado obligatoriamente a [comunidadId].
     */
    suspend fun crearEvento(
        evento: Evento,
        comunidadId: String,
        creadorUid: String
    ): Result<Evento> {
        if (comunidadId.isBlank()) {
            return Result.failure(IllegalStateException("Debes pertenecer a una comunidad para proponer eventos."))
        }
        if (evento.titulo.isBlank() || evento.ubicacion.isBlank()) {
            return Result.failure(IllegalArgumentException("Título y ubicación son obligatorios."))
        }

        return try {
            val docRef = db.subcoleccionComunidad(comunidadId, COLECCION_EVENTOS).document()
            val asistentesIniciales = if (creadorUid.isNotBlank()) listOf(creadorUid) else emptyList()
            val eventoCreado = evento.copy(
                id = docRef.id,
                comunidadId = comunidadId,
                creadorUid = creadorUid,
                asistentesUids = asistentesIniciales,
                asistentes = asistentesIniciales.size,
                asistiendoUsuario = creadorUid.isNotBlank()
            )

            val payload = mapOf(
                "id" to eventoCreado.id,
                "comunidadId" to eventoCreado.comunidadId,
                "creadorUid" to eventoCreado.creadorUid,
                "titulo" to eventoCreado.titulo,
                "descripcion" to eventoCreado.descripcion,
                "fecha" to eventoCreado.fecha.toString(),
                "hora" to eventoCreado.hora,
                "ubicacion" to eventoCreado.ubicacion,
                "categoria" to eventoCreado.categoria.name,
                "organizador" to eventoCreado.organizador,
                "asistentes" to eventoCreado.asistentes,
                "asistentesUids" to eventoCreado.asistentesUids,
                "recordatoriosUids" to emptyList<String>(),
                "esDestacado" to eventoCreado.esDestacado,
                "timestamp" to System.currentTimeMillis()
            )

            docRef.set(payload).await()
            Log.i("CalendarioRepository", "Evento guardado en Firestore: ${docRef.path}")
            Result.success(eventoCreado)
        } catch (e: Exception) {
            Log.e("CalendarioRepository", "Error creando evento en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza la asistencia del usuario en Firestore validando el aislamiento por comunidad.
     */
    suspend fun toggleAsistencia(
        evento: Evento,
        uid: String,
        comunidadIdActual: String
    ): Result<Unit> {
        if (comunidadIdActual.isBlank() || evento.comunidadId != comunidadIdActual) {
            return Result.failure(SecurityException("No puedes modificar eventos de otra comunidad."))
        }
        if (uid.isBlank() || evento.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Sesión o evento inválido."))
        }

        return try {
            val docRef = db.subcoleccionComunidad(comunidadIdActual, COLECCION_EVENTOS).document(evento.id)
            val op = if (evento.asistiendoUsuario) {
                FieldValue.arrayRemove(uid)
            } else {
                FieldValue.arrayUnion(uid)
            }
            docRef.update("asistentesUids", op).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarioRepository", "Error actualizando asistencia en Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el recordatorio del usuario en Firestore validando el aislamiento por comunidad.
     */
    suspend fun toggleRecordatorio(
        evento: Evento,
        uid: String,
        comunidadIdActual: String
    ): Result<Unit> {
        if (comunidadIdActual.isBlank() || evento.comunidadId != comunidadIdActual) {
            return Result.failure(SecurityException("No puedes modificar eventos de otra comunidad."))
        }
        if (uid.isBlank() || evento.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Sesión o evento inválido."))
        }

        return try {
            val docRef = db.subcoleccionComunidad(comunidadIdActual, COLECCION_EVENTOS).document(evento.id)
            val op = if (evento.recordatorioActivado) {
                FieldValue.arrayRemove(uid)
            } else {
                FieldValue.arrayUnion(uid)
            }
            docRef.update("recordatoriosUids", op).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarioRepository", "Error actualizando recordatorio en Firestore", e)
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_EVENTOS = "eventos"
    }
}
