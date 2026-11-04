package com.example.radiopatio.usuarios

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun registerUser(
        nombre: String,
        apellidos: String,
        email: String,
        pass: String,
        photoUri: Uri?
    ): Result<Unit> {
        return try {
            // 1. Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al obtener el UID")

            // 2. Subir foto a Firebase Storage si se ha proporcionado una
            var photoUrl: String? = null
            photoUri?.let { uri ->
                val storageRef = storage.reference.child("profile_images/$uid.jpg")
                storageRef.putFile(uri).await()
                photoUrl = storageRef.downloadUrl.await().toString()
            }

            // 3. Guardar todos los datos en Cloud Firestore
            val userMap = hashMapOf(
                "nombre" to nombre,
                "apellidos" to apellidos,
                "email" to email,
                "photoUrl" to (photoUrl ?: ""),
                "isRegistered" to true,
                "role" to "VECINO"
            )

            db.collection("users").document(uid).set(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}