package com.example.radiopatio.usuarios

import android.net.Uri
import com.example.radiopatio.informacionUsuario.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    /**
     * Obtiene los datos del usuario actual autenticado desde Firestore.
     */
    suspend fun getCurrentUserData(): Result<Usuario?> {
        return try {
            val currentUser = auth.currentUser ?: return Result.success(null)
            val uid = currentUser.uid
            val documentSnapshot = db.collection("users").document(uid).get().await()

            if (documentSnapshot.exists()) {
                val nombre = documentSnapshot.getString("nombre") ?: ""
                val apellidos = documentSnapshot.getString("apellidos") ?: ""

                val nombreCompleto = "$nombre $apellidos".trim().ifEmpty {
                    currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Vecino"
                }

                val inicial = nombreCompleto.firstOrNull()?.uppercase() ?: "V"

                val usuario = Usuario(
                    nombre = nombreCompleto,
                    inicial = inicial,
                    vivienda = documentSnapshot.getString("piso")
                        ?: documentSnapshot.getString("vivienda")
                        ?: "Portal 1, 2ºA",
                    email = documentSnapshot.getString("email") ?: currentUser.email ?: "",
                    telefono = documentSnapshot.getString("telefono") ?: "",
                    rol = documentSnapshot.getString("role") ?: "Vecino/a"
                )
                Result.success(usuario)
            } else {
                // Si el documento Firestore no se creó a tiempo, usamos la información de Firebase Auth
                val emailAuth = currentUser.email ?: "vecino@radiopatio.com"
                val nombreAuth = currentUser.displayName?.ifBlank { null }
                    ?: emailAuth.substringBefore("@").replaceFirstChar { it.uppercase() }

                Result.success(
                    Usuario(
                        nombre = nombreAuth,
                        inicial = nombreAuth.firstOrNull()?.uppercase() ?: "V",
                        vivienda = "Portal 1, 2ºA",
                        email = emailAuth,
                        telefono = ""
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserta en Cloud Firestore los usuarios registrados que aparecen en PaginaPrincipal
     * junto con sus publicaciones iniciales si la colección está vacía.
     */
    suspend fun inicializarDatosPrueba(): Result<Unit> {
        return try {
            val usersCollection = db.collection("users")
            val snapshot = usersCollection.get().await()

            // Solo insertamos si no existen documentos de usuarios
            if (snapshot.isEmpty) {

                // 1. USUARIOS EXTRAÍDOS DE PAGINA PRINCIPAL
                val usuariosRegistrados = listOf(
                    mapOf(
                        "uid" to "user_manolo",
                        "nombre" to "Manolo",
                        "apellidos" to "García Pérez",
                        "email" to "manolo@radiopatio.com",
                        "photoUrl" to "https://i.pravatar.cc/150?img=11",
                        "isRegistered" to true,
                        "role" to "VECINO"
                    ),
                    mapOf(
                        "uid" to "user_lucia",
                        "nombre" to "Lucía",
                        "apellidos" to "Fernández Ramos",
                        "email" to "lucia@radiopatio.com",
                        "photoUrl" to "https://i.pravatar.cc/150?img=5",
                        "isRegistered" to true,
                        "role" to "VECINO"
                    ),
                    mapOf(
                        "uid" to "user_anonimo",
                        "nombre" to "Vecino",
                        "apellidos" to "Anónimo",
                        "email" to "anonimo@radiopatio.com",
                        "photoUrl" to "",
                        "isRegistered" to true,
                        "role" to "VECINO"
                    )
                )

                // Guardar cada vecino registrado
                for (usuario in usuariosRegistrados) {
                    val id = usuario["uid"] as String
                    usersCollection.document(id).set(usuario).await()
                }

                // 2. PUBLICACIONES CORRESPONDIENTES DEL FEED
                val publicacionesIniciales = listOf(
                    mapOf(
                        "autor" to "Manolo García",
                        "inicial" to "M",
                        "tiempoYUbica" to "Hace 10 min · Portal 3",
                        "contenido" to "¡Ojo vecinos! Han dejado una caja abierta en la entrada del ascensor.",
                        "meGusta" to 4,
                        "comentarios" to 2
                    ),
                    mapOf(
                        "autor" to "Lucía Fernández",
                        "inicial" to "L",
                        "tiempoYUbica" to "Hace 1 hora · Garaje",
                        "contenido" to "¿Alguien sabe de quién es el coche blanco aparcado en la plaza 12?",
                        "meGusta" to 8,
                        "comentarios" to 5
                    ),
                    mapOf(
                        "autor" to "Vecino Anónimo",
                        "inicial" to "V",
                        "tiempoYUbica" to "Ahora mismo · Comunidad",
                        "contenido" to "Recordatorio: La reunión de vecinos sobre las obras de la fachada es este jueves.",
                        "meGusta" to 1,
                        "comentarios" to 0
                    )
                )

                val postsCollection = db.collection("publicaciones")
                for (post in publicacionesIniciales) {
                    postsCollection.add(post).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}