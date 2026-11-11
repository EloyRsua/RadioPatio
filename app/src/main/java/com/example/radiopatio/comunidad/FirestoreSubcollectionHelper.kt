package com.example.radiopatio.comunidad

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Devuelve la referencia a la subcolección [nombreSubcoleccion] dentro del documento
 * `comunidades/{comunidadId}` en Cloud Firestore.
 */
fun FirebaseFirestore.subcoleccionComunidad(
    comunidadId: String,
    nombreSubcoleccion: String
): CollectionReference = collection(ComunidadRepository.COLECCION_COMUNIDADES)
    .document(comunidadId)
    .collection(nombreSubcoleccion)

/**
 * Migra de forma idempotente documentos que pudieran existir en la colección raíz
 * [nombreColeccion] con `comunidadId == comunidadId` hacia la subcolección
 * `comunidades/{comunidadId}/{nombreColeccion}`.
 */
suspend fun FirebaseFirestore.migrarDocumentosRaizAComunidad(
    comunidadId: String,
    nombreColeccion: String
) {
    if (comunidadId.isBlank()) return
    try {
        val raizSnap = collection(nombreColeccion)
            .whereEqualTo("comunidadId", comunidadId)
            .get()
            .await()

        if (raizSnap.isEmpty) return

        val destinoSubcoleccion = subcoleccionComunidad(comunidadId, nombreColeccion)
        for (doc in raizSnap.documents) {
            val data = doc.data?.toMutableMap() ?: continue
            data["comunidadId"] = comunidadId
            destinoSubcoleccion.document(doc.id).set(data).await()
            try {
                doc.reference.delete().await()
            } catch (deleteError: Exception) {
                Log.w(
                    "RadioPatioFirestore",
                    "No se pudo eliminar documento migrado en raíz $nombreColeccion/${doc.id}: ${deleteError.message}"
                )
            }
        }
    } catch (e: Exception) {
        Log.w(
            "RadioPatioFirestore",
            "Migración opcional desde raíz omitida para $nombreColeccion: ${e.message}"
        )
    }
}
