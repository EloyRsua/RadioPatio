package com.example.radiopatio.usuarios

import android.net.Uri
import android.util.Log
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.informacionUsuario.ROL_ADMINISTRADOR_FINCAS
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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
            ?: ROL_VECINO
        val tipoCuenta = doc.getString("tipoCuenta")?.takeIf { it.isNotBlank() } ?: rol
        val comunidadActivaId = doc.getString("comunidadId") ?: ""
        val comunidadesIds = ((doc.get("comunidadesIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?: emptyList())
            .let { lista ->
                if (comunidadActivaId.isNotBlank() && comunidadActivaId !in lista) {
                    lista + comunidadActivaId
                } else {
                    lista
                }
            }
            .distinct()
        val solicitudesPendientes = (doc.get("solicitudesPendientesComunidades") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

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
            comunidadId = comunidadActivaId,
            comunidadNombre = doc.getString("comunidadNombre") ?: "",
            photoUrl = doc.getString("photoUrl") ?: "",
            tipoCuenta = tipoCuenta,
            comunidadesIds = comunidadesIds,
            solicitudesPendientesComunidades = solicitudesPendientes
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
     * Observa en tiempo real los vecinos y administradores pertenecientes a [comunidadId].
     * Combina consulta directa por `comunidadId` activo y por `comunidadesIds` (para administradores multi-comunidad).
     */
    fun observarVecinosPorComunidad(comunidadId: String): Flow<List<Usuario>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listenerActivos: ListenerRegistration? = null
        var listenerMultiComunidad: ListenerRegistration? = null
        var listaActivos = emptyList<Usuario>()
        var listaMulti = emptyList<Usuario>()

        fun reemitirCombinados() {
            val combinados = (listaActivos + listaMulti)
                .filter { it.perteneceAComunidad(comunidadId) }
                .distinctBy { it.uid.ifBlank { it.email } }
                .sortedBy { it.nombre.lowercase() }
            trySend(combinados)
        }

        try {
            listenerActivos = db.collection(COLECCION_USERS)
                .whereEqualTo("comunidadId", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        reemitirCombinados()
                        return@addSnapshotListener
                    }
                    listaActivos = snapshot?.documents?.map { documentToUsuario(it) }.orEmpty()
                    reemitirCombinados()
                }

            listenerMultiComunidad = db.collection(COLECCION_USERS)
                .whereArrayContains("comunidadesIds", comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        reemitirCombinados()
                        return@addSnapshotListener
                    }
                    listaMulti = snapshot?.documents?.map { documentToUsuario(it) }.orEmpty()
                    reemitirCombinados()
                }
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listenerActivos?.remove()
            listenerMultiComunidad?.remove()
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
     * Registra o actualiza el perfil del usuario en Firestore asociándolo a su comunidad y tipo de cuenta.
     */
    suspend fun registrarUsuarioEnFirestore(
        uid: String,
        nombre: String,
        apellidos: String,
        vivienda: String,
        email: String,
        comunidadId: String,
        comunidadNombre: String,
        rol: String = ROL_VECINO,
        photoUrl: String = "",
        tipoCuenta: String = rol,
        comunidadesIds: List<String> = if (comunidadId.isNotBlank()) listOf(comunidadId) else emptyList(),
        solicitudesPendientesComunidades: List<String> = emptyList()
    ): Result<Unit> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("Usuario no autenticado."))
        }

        return try {
            val listaComunidadesLimpia = comunidadesIds.filter { it.isNotBlank() }.distinct()
            val nuevoUsuario = mapOf(
                "uid" to uid,
                "nombre" to nombre.trim(),
                "apellidos" to apellidos.trim(),
                "piso" to vivienda.trim(),
                "email" to email.trim(),
                "role" to rol,
                "tipoCuenta" to tipoCuenta,
                "photoUrl" to photoUrl,
                "comunidadId" to comunidadId,
                "comunidadNombre" to comunidadNombre,
                "comunidadesIds" to listaComunidadesLimpia,
                "solicitudesPendientesComunidades" to solicitudesPendientesComunidades.filter { it.isNotBlank() }.distinct(),
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
     * Si el usuario es Administrador de fincas conserva su lista de comunidades administradas.
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
            val rolAnterior = snapshotAnterior.getString("role")
                ?: snapshotAnterior.getString("rol")
                ?: ""
            val tipoCuentaAnterior = snapshotAnterior.getString("tipoCuenta") ?: rolAnterior

            if (rolAnterior.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true) ||
                tipoCuentaAnterior.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true)
            ) {
                val yaPertenece = snapshotAnterior.getString("comunidadId") == comunidadId ||
                    comunidadId in ((snapshotAnterior.get("comunidadesIds") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList())
                return if (yaPertenece) {
                    cambiarComunidadActivaAdministrador(
                        Comunidad(id = comunidadId, nombre = comunidadNombre)
                    )
                } else {
                    unirseComoAdministradorAComunidadSinAdmin(comunidadId)
                }
            }

            return Result.failure(
                SecurityException(
                    "La unión a una comunidad requiere solicitud y verificación previa de su administrador."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Añade una comunidad recién creada por el administrador autenticado a su lista de comunidades
     * administradas y la marca como su comunidad activa sin perder las anteriores.
     */
    suspend fun asignarComunidadCreadaAdministrador(comunidad: Comunidad): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Debes iniciar sesión."))

        return try {
            val userRef = db.collection(COLECCION_USERS).document(currentUser.uid)
            val snap = userRef.get().await()
            val rol = snap.getString("role") ?: ""
            val tipo = snap.getString("tipoCuenta") ?: ""
            if (!rol.equals(ROL_ADMINISTRADOR_FINCAS, true) && !tipo.equals(ROL_ADMINISTRADOR_FINCAS, true)) {
                return Result.failure(SecurityException("Solo los administradores de fincas pueden administrar múltiples comunidades."))
            }

            val existentes = (snap.get("comunidadesIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()
            val comActivaAnterior = snap.getString("comunidadId").orEmpty()
            if (comActivaAnterior.isNotBlank() && comActivaAnterior !in existentes) {
                existentes.add(comActivaAnterior)
            }
            val yaEstaba = comunidad.id in existentes
            if (!yaEstaba) {
                existentes.add(comunidad.id)
            }

            userRef.set(
                mapOf(
                    "comunidadId" to comunidad.id,
                    "comunidadNombre" to comunidad.nombre,
                    "comunidadesIds" to existentes.distinct(),
                    "role" to ROL_ADMINISTRADOR_FINCAS,
                    "tipoCuenta" to ROL_ADMINISTRADOR_FINCAS
                ),
                SetOptions.merge()
            ).await()

            if (!yaEstaba) {
                comunidadRepository.ajustarContadorVecinos(comunidad.id, 1L)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cambia la comunidad activa visualizada por un administrador entre las comunidades que ya administra.
     */
    suspend fun cambiarComunidadActivaAdministrador(comunidad: Comunidad): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Debes iniciar sesión."))
        if (comunidad.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Comunidad inválida."))
        }

        return try {
            val userRef = db.collection(COLECCION_USERS).document(currentUser.uid)
            val comRef = db.collection(ComunidadRepository.COLECCION_COMUNIDADES).document(comunidad.id)
            val snap = userRef.get().await()
            val comSnap = comRef.get().await()
            val comunidades = (snap.get("comunidadesIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()
            val adminEnCom = comSnap.getString("administradorUid")?.trim().orEmpty()
            val pertenece = comunidad.id in comunidades ||
                snap.getString("comunidadId") == comunidad.id ||
                adminEnCom == currentUser.uid

            if (!pertenece) {
                return Result.failure(SecurityException("No administras esta comunidad."))
            }

            val listaActualizada = (comunidades + comunidad.id).filter { it.isNotBlank() }.distinct()
            val nombreFinal = comSnap.getString("nombre")?.takeIf { it.isNotBlank() } ?: comunidad.nombre

            userRef.set(
                mapOf(
                    "comunidadId" to comunidad.id,
                    "comunidadNombre" to nombreFinal,
                    "comunidadesIds" to listaActualizada
                ),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Asigna al administrador autenticado como único administrador de una comunidad que aún no tiene uno.
     * Si la comunidad ya tiene administrador, falla indicando que debe enviar solicitud de traspaso.
     */
    suspend fun unirseComoAdministradorAComunidadSinAdmin(comunidadId: String): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Debes iniciar sesión."))
        if (comunidadId.isBlank()) {
            return Result.failure(IllegalArgumentException("Comunidad inválida."))
        }

        return try {
            val comRef = db.collection(ComunidadRepository.COLECCION_COMUNIDADES).document(comunidadId)
            val userRef = db.collection(COLECCION_USERS).document(currentUser.uid)

            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                val userSnap = transaction.get(userRef)

                if (!comSnap.exists()) {
                    throw IllegalStateException("La comunidad no existe.")
                }
                if (!userSnap.exists()) {
                    throw IllegalStateException("Perfil de usuario no encontrado.")
                }

                val rol = userSnap.getString("role") ?: ""
                val tipo = userSnap.getString("tipoCuenta") ?: ""
                if (!rol.equals(ROL_ADMINISTRADOR_FINCAS, true) && !tipo.equals(ROL_ADMINISTRADOR_FINCAS, true)) {
                    throw SecurityException("Solo los administradores de fincas pueden administrar comunidades.")
                }

                val adminActualUid = comSnap.getString("administradorUid")?.trim().orEmpty()
                if (adminActualUid.isNotBlank() && adminActualUid != currentUser.uid) {
                    throw IllegalStateException(
                        "Esta comunidad ya tiene un administrador de fincas. Solicita el traspaso de administración."
                    )
                }

                val existentes = (userSnap.get("comunidadesIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() }
                    ?.toMutableList()
                    ?: mutableListOf()
                val comActivaAnterior = userSnap.getString("comunidadId").orEmpty()
                if (comActivaAnterior.isNotBlank() && comActivaAnterior !in existentes) {
                    existentes.add(comActivaAnterior)
                }
                val yaEstaba = comunidadId in existentes
                if (!yaEstaba) {
                    existentes.add(comunidadId)
                }

                val pendientes = (userSnap.get("solicitudesPendientesComunidades") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() && it != comunidadId }
                    ?: emptyList()

                transaction.update(comRef, "administradorUid", currentUser.uid)
                if (!yaEstaba) {
                    transaction.update(comRef, "totalVecinos", FieldValue.increment(1L))
                }
                transaction.update(
                    userRef,
                    mapOf(
                        "comunidadId" to comunidadId,
                        "comunidadNombre" to (comSnap.getString("nombre") ?: ""),
                        "comunidadesIds" to existentes.distinct(),
                        "solicitudesPendientesComunidades" to pendientes,
                        "role" to ROL_ADMINISTRADOR_FINCAS,
                        "tipoCuenta" to ROL_ADMINISTRADOR_FINCAS
                    )
                )
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLECCION_USERS = "users"
    }
}
