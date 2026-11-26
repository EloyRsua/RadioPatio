package com.example.radiopatio.mensajeria

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.usuarios.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatUiState(
    val cargando: Boolean = false,
    val enviando: Boolean = false,
    val usuarioActualUid: String = "",
    val usuarioActualNombre: String = "Vecino",
    val comunidadId: String = "",
    val conversacion: Conversacion? = null,
    val mensajes: List<Mensaje> = emptyList(),
    val errorMensaje: String? = null
)

class ChatViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(userRepository) as T
    }
}

class ChatViewModel(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var mensajesListener: ListenerRegistration? = null

    init {
        cargarUsuarioActual()
    }

    private fun cargarUsuarioActual() {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return

        viewModelScope.launch {
            val nombreReal = resolverNombreUsuario(uid)
            val comId = try {
                db.collection("users").document(uid).get().await()
                    .getString("comunidadId").orEmpty()
            } catch (_: Exception) {
                ""
            }

            uiState = uiState.copy(
                usuarioActualUid = uid,
                usuarioActualNombre = nombreReal,
                comunidadId = uiState.comunidadId.ifBlank { comId }
            )
        }
    }

    private suspend fun resolverNombreUsuario(uid: String): String {
        if (uid.isBlank()) return "Vecino"
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val nombre = doc.getString("nombre").orEmpty().trim()
            val apellidos = doc.getString("apellidos").orEmpty().trim()
            val nombreCompleto = "$nombre $apellidos".trim()
            when {
                nombreCompleto.isNotBlank() -> nombreCompleto
                !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName!!.trim()
                !auth.currentUser?.email.isNullOrBlank() -> auth.currentUser!!.email!!.substringBefore("@")
                else -> "Vecino"
            }
        } catch (_: Exception) {
            auth.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email?.substringBefore("@")
                ?: "Vecino"
        }
    }

    fun abrirChatConProducto(
        productoId: String,
        productoTitulo: String,
        productoPrecio: String,
        productoImagenUrl: String,
        vendedorUid: String,
        vendedorNombre: String,
        compradorUid: String = "",
        compradorNombre: String = "",
        comunidadId: String = "",
        onChatListo: () -> Unit = {}
    ) {
        val miUid = auth.currentUser?.uid.orEmpty().ifBlank { uiState.usuarioActualUid }
        if (miUid.isBlank() || productoId.isBlank()) {
            uiState = uiState.copy(errorMensaje = "No se ha podido abrir el chat.")
            return
        }

        mensajesListener?.remove()
        uiState = uiState.copy(
            cargando = true,
            usuarioActualUid = miUid,
            mensajes = emptyList(),
            errorMensaje = null
        )

        viewModelScope.launch {
            try {
                val miNombreReal = resolverNombreUsuario(miUid)
                val soyVendedor = miUid == vendedorUid
                val compradorFinalUid = if (soyVendedor) compradorUid else miUid
                val compradorFinalNombre = if (soyVendedor) {
                    compradorNombre.ifBlank { "Vecino" }
                } else {
                    miNombreReal
                }

                if (compradorFinalUid.isBlank() || vendedorUid.isBlank()) {
                    uiState = uiState.copy(
                        cargando = false,
                        errorMensaje = "Faltan datos del participante para iniciar el chat."
                    )
                    return@launch
                }

                val comIdResuelta = resolverComunidadId(
                    comunidadParam = comunidadId,
                    uid = miUid,
                    productoId = productoId
                )

                val conversacion = chatRepository.obtenerOCrearConversacion(
                    comunidadId = comIdResuelta,
                    productoId = productoId,
                    productoTitulo = productoTitulo,
                    productoPrecio = productoPrecio,
                    productoImagenUrl = productoImagenUrl,
                    vendedorUid = vendedorUid,
                    vendedorNombre = if (soyVendedor) miNombreReal else vendedorNombre.ifBlank { "Vendedor" },
                    compradorUid = compradorFinalUid,
                    compradorNombre = compradorFinalNombre
                )

                uiState = uiState.copy(
                    cargando = false,
                    comunidadId = comIdResuelta,
                    usuarioActualUid = miUid,
                    usuarioActualNombre = miNombreReal,
                    conversacion = conversacion
                )

                mensajesListener = chatRepository.observarMensajes(
                    comunidadId = comIdResuelta,
                    conversacionId = conversacion.id,
                    onMensajes = { lista ->
                        uiState = uiState.copy(mensajes = lista)
                    },
                    onError = { msg ->
                        uiState = uiState.copy(errorMensaje = msg)
                    }
                )

                onChatListo()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    errorMensaje = e.message ?: "No se pudo abrir la conversación."
                )
            }
        }
    }

    fun enviarMensaje(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return

        val conv = uiState.conversacion ?: return
        val miUid = auth.currentUser?.uid.orEmpty().ifBlank { uiState.usuarioActualUid }
        if (miUid.isBlank() || conv.comunidadId.isBlank()) return

        val destinatarioUid = if (miUid == conv.vendedorUid) conv.compradorUid else conv.vendedorUid

        uiState = uiState.copy(enviando = true, errorMensaje = null)

        viewModelScope.launch {
            try {
                val miNombreReal = resolverNombreUsuario(miUid)
                uiState = uiState.copy(
                    usuarioActualUid = miUid,
                    usuarioActualNombre = miNombreReal
                )

                chatRepository.enviarMensaje(
                    comunidadId = conv.comunidadId,
                    conversacionId = conv.id,
                    productoId = conv.productoId,
                    productoTitulo = conv.productoTitulo,
                    remitenteUid = miUid,
                    remitenteNombre = miNombreReal,
                    destinatarioUid = destinatarioUid,
                    texto = limpio
                )
                uiState = uiState.copy(enviando = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    enviando = false,
                    errorMensaje = e.message ?: "No se pudo enviar el mensaje."
                )
            }
        }
    }

    fun limpiarConversacion() {
        mensajesListener?.remove()
        mensajesListener = null
        uiState = uiState.copy(
            conversacion = null,
            mensajes = emptyList(),
            errorMensaje = null
        )
    }

    fun limpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    private suspend fun resolverComunidadId(
        comunidadParam: String,
        uid: String,
        productoId: String
    ): String {
        if (comunidadParam.isNotBlank()) return comunidadParam
        if (uiState.comunidadId.isNotBlank()) return uiState.comunidadId

        try {
            val userDoc = db.collection("users").document(uid).get().await()
            val userCom = userDoc.getString("comunidadId").orEmpty()
            if (userCom.isNotBlank()) return userCom
        } catch (_: Exception) {
        }

        try {
            val prodSnap = db.collectionGroup("productos")
                .whereEqualTo("firestoreId", productoId)
                .limit(1)
                .get()
                .await()
            val prodCom = prodSnap.documents.firstOrNull()?.getString("comunidadId").orEmpty()
            if (prodCom.isNotBlank()) return prodCom
        } catch (_: Exception) {
        }

        return "general"
    }

    override fun onCleared() {
        mensajesListener?.remove()
        super.onCleared()
    }
}