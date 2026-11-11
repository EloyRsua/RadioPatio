package com.example.radiopatio.usuarios

import android.net.Uri
import android.util.Log
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repositorio encargado de la gestión del usuario autenticado, su comunidad asignada
 * y la consulta en tiempo real de los vecinos pertenecientes a una comunidad en Firestore.
 */
class UserRepository(
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    /**
     * Convierte un [DocumentSnapshot] de la colección `users` en un modelo [Usuario].
     */
    fun documentToUsuario(
        doc: DocumentSnapshot,
        fallbackUser: FirebaseUser? = null
    ): Usuario {
        val nombre = doc.getString("nombre") ?: ""
        val apellidos = doc.getString("apellidos") ?: ""
        val email = doc.getString("email") ?: fallbackUser?.email ?: ""

        val nombreCompleto = "$nombre $apellidos".trim().ifEmpty {
            fallbackUser?.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Vecino" }
        }

        val inicial = nombreCompleto.firstOrNull()?.uppercase() ?: "V"
        val vivienda = doc.getString("piso")?.takeIf { it.isNotBlank() }
            ?: doc.getString("vivienda")?.takeIf { it.isNotBlank() }
            ?: "Sin vivienda asignada"

        val rol = doc.getString("role")?.takeIf { it.isNotBlank() }
            ?: doc.getString("rol")?.takeIf { it.isNotBlank() }
            ?: "Vecino/a"

        return Usuario(
            nombre = nombreCompleto,
            inicial = inicial,
            vivienda = vivienda,
            email = email,
            telefono = doc.getString("telefono") ?: "",
            rol = rol,
            fechaAlta = doc.getString("fechaAlta") ?: "Enero 2026",
            totalPublicaciones = (doc.getLong("totalPublicaciones") ?: 0L).toInt(),
            uid = doc.getString("uid")?.takeIf { it.isNotBlank() } ?: doc.id,
            comunidadId = doc.getString("comunidadId") ?: "",
            comunidadNombre = doc.getString("comunidadNombre") ?: "",
            photoUrl = doc.getString("photoUrl") ?: ""
        )
    }

    private fun buildFallbackUsuario(user: FirebaseUser): Usuario {
        val emailAuth = user.email ?: "vecino@radiopatio.com"
        val nombreAuth = user.displayName?.takeIf { it.isNotBlank() }
            ?: emailAuth.substringBefore("@").replaceFirstChar { it.uppercase() }
        return Usuario(
            nombre = nombreAuth,
            inicial = nombreAuth.firstOrNull()?.uppercase() ?: "V",
            vivienda = "Sin vivienda asignada",
            email = emailAuth,
            telefono = "",
            uid = user.uid,
            comunidadId = "",
            comunidadNombre = "",
            photoUrl = user.photoUrl?.toString() ?: ""
        )
    }

    /**
     * Observa en tiempo real al usuario autenticado actual y su documento en Firestore,
     * obteniendo dinámicamente su `comunidadId` ante cualquier cambio de sesión o perfil.
     */
    fun observarUsuarioActual(): Flow<Usuario?> = callbackFlow {
        var userListener: ListenerRegistration? = null

        fun suscribirDocumentoUsuario(firebaseUser: FirebaseUser?) {
            userListener?.remove()
            userListener = null

            if (firebaseUser == null) {
                trySend(null)
                return
            }

            try {
                userListener = db.collection(COLECCION_USERS)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("UserRepository", "Error leyendo users/${firebaseUser.uid}", error)
                            trySend(buildFallbackUsuario(firebaseUser))
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val usuario = documentToUsuario(snapshot, firebaseUser)
                            Log.i(
                                "UserRepository",
                                "Usuario activo uid=${usuario.uid}, comunidadId='${usuario.comunidadId}', nombre='${usuario.nombre}'"
                            )
                            trySend(usuario)
                        } else {
                            Log.w(
                                "UserRepository",
                                "Documento users/${firebaseUser.uid} no existe en Firestore; usando fallback sin comunidadId"
                            )
                            trySend(buildFallbackUsuario(firebaseUser))
                        }
                    }
            } catch (e: Exception) {
                Log.e("UserRepository", "Excepción suscribiendo users/${firebaseUser.uid}", e)
                trySend(buildFallbackUsuario(firebaseUser))
            }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            suscribirDocumentoUsuario(firebaseAuth.currentUser)
        }

        auth.addAuthStateListener(authListener)
        suscribirDocumentoUsuario(auth.currentUser)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            userListener?.remove()
        }
    }

    /**
     * Flujo reactivo con el `comunidadId` del usuario autenticado actual.
     */
    fun observarComunidadIdActual(): Flow<String> =
        observarUsuarioActual()
            .map { it?.comunidadId.orEmpty() }
            .distinctUntilChanged()

    /**
     * Obtiene los datos del usuario actual autenticado desde Firestore.
     */
    suspend fun getCurrentUserData(): Result<Usuario?> {
        return try {
            val currentUser = auth.currentUser ?: return Result.success(null)
            val documentSnapshot = db.collection(COLECCION_USERS)
                .document(currentUser.uid)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                Result.success(documentToUsuario(documentSnapshot, currentUser))
            } else {
                Result.success(buildFallbackUsuario(currentUser))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real los vecinos que pertenecen exclusivamente a [comunidadId].
     * Aplica filtro obligatorio en la consulta de Firestore y validación adicional en memoria.
     */
    fun observarVecinosPorComunidad(comunidadId: String): Flow<List<Usuario>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_USERS)
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val vecinos = snapshot?.documents
                        ?.map { documentToUsuario(it) }
                        ?.filter { it.comunidadId == comunidadId }
                        ?.sortedBy { it.nombre.lowercase() }
                        ?: emptyList()

                    trySend(vecinos)
                }
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Intenta subir la foto de perfil a Firebase Storage y devuelve la URL pública.
     * Si no hay imagen o Storage falla, devuelve cadena vacía para usar avatar por inicial.
     */
    suspend fun subirAvatarSiExiste(uid: String, photoUri: Uri?): String {
        if (photoUri == null || uid.isBlank()) return ""
        return try {
            val ref = storage.reference.child("avatars/$uid.jpg")
            ref.putFile(photoUri).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Registra o actualiza el perfil del usuario en Firestore asociándolo a su comunidad.
     */
    suspend fun registrarUsuarioEnFirestore(
        uid: String,
        nombre: String,
        apellidos: String,
        vivienda: String,
        email: String,
        comunidadId: String,
        comunidadNombre: String,
        rol: String = "Vecino/a",
        photoUrl: String = ""
    ): Result<Unit> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("Usuario no autenticado."))
        }

        return try {
            val nuevoUsuario = mapOf(
                "uid" to uid,
                "nombre" to nombre.trim(),
                "apellidos" to apellidos.trim(),
                "piso" to vivienda.trim(),
                "email" to email.trim(),
                "role" to rol,
                "photoUrl" to photoUrl,
                "comunidadId" to comunidadId,
                "comunidadNombre" to comunidadNombre,
                "isRegistered" to true
            )

            db.collection(COLECCION_USERS).document(uid).set(nuevoUsuario).await()

            if (comunidadId.isNotBlank()) {
                comunidadRepository.ajustarContadorVecinos(comunidadId, 1L)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Asigna o cambia la comunidad del usuario autenticado actual en Firestore.
     */
    suspend fun asignarComunidadUsuario(
        comunidadId: String,
        comunidadNombre: String,
        nuevoRol: String? = null
    ): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Debes iniciar sesión para unirte a una comunidad."))

        if (comunidadId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador de la comunidad es obligatorio."))
        }

        return try {
            val userRef = db.collection(COLECCION_USERS).document(currentUser.uid)
            val snapshotAnterior = userRef.get().await()
            val comunidadAnteriorId = snapshotAnterior.getString("comunidadId").orEmpty()
            val rolAnterior = snapshotAnterior.getString("role")
                ?: snapshotAnterior.getString("rol")
                ?: ""

            if (comunidadAnteriorId.isNotBlank() && comunidadAnteriorId != comunidadId) {
                val comAnteriorSnap = db.collection(ComunidadRepository.COLECCION_COMUNIDADES)
                    .document(comunidadAnteriorId)
                    .get()
                    .await()
                val presAnteriorUid = comAnteriorSnap.getString("presidenteUid").orEmpty()
                val esPresidenteSaliente = (presAnteriorUid == currentUser.uid) ||
                    (presAnteriorUid.isBlank() && rolAnterior.equals("Presidente", ignoreCase = true))

                if (esPresidenteSaliente) {
                    val miembrosAnteriores = db.collection(COLECCION_USERS)
                        .whereEqualTo("comunidadId", comunidadAnteriorId)
                        .get()
                        .await()
                    val hayOtrosMiembros = miembrosAnteriores.documents.any { it.id != currentUser.uid }
                    if (hayOtrosMiembros) {
                        return Result.failure(
                            IllegalStateException(
                                "Eres el presidente de tu comunidad actual. Transfiere la presidencia a otro miembro antes de cambiarte."
                            )
                        )
                    } else if (comAnteriorSnap.exists()) {
                        db.collection(ComunidadRepository.COLECCION_COMUNIDADES)
                            .document(comunidadAnteriorId)
                            .update("presidenteUid", "")
                            .await()
                    }
                }
            }

            val cambios = mutableMapOf<String, Any>(
                "uid" to currentUser.uid,
                "email" to (snapshotAnterior.getString("email") ?: currentUser.email ?: ""),
                "comunidadId" to comunidadId,
                "comunidadNombre" to comunidadNombre
            )
            if (!snapshotAnterior.exists()) {
                val nombreBase = currentUser.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "Vecino"
                cambios["nombre"] = nombreBase
                cambios["apellidos"] = ""
                cambios["piso"] = "Portal 1"
                cambios["role"] = nuevoRol ?: "Vecino/a"
                cambios["photoUrl"] = ""
            } else if (!nuevoRol.isNullOrBlank()) {
                cambios["role"] = nuevoRol
            } else if (comunidadAnteriorId != comunidadId) {
                // Al cambiar a una comunidad ajena nunca se conserva el rol de Presidente anterior
                cambios["role"] = "Vecino/a"
            }

            userRef.set(cambios, SetOptions.merge()).await()

            if (comunidadAnteriorId != comunidadId) {
                if (comunidadAnteriorId.isNotBlank()) {
                    comunidadRepository.ajustarContadorVecinos(comunidadAnteriorId, -1L)
                }
                comunidadRepository.ajustarContadorVecinos(comunidadId, 1L)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_USERS = "users"
    }
}
