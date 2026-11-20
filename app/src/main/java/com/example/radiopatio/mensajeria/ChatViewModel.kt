package com.example.radiopatio.mensajeria

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatUiState(
    val cargando: Boolean = false,
    val comunidadId: String = "",
    val usuarioActual: Usuario? = null,
    val conversacionActiva: Conversacion? = null,
    val mensajesActivos: List<Mensaje> = emptyList(),
    val errorMensaje: String? = null
)

class ChatViewModelFactory(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(chatRepository, userRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    init {
        observarUsuario()
    }

    private fun observarUsuario() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual().collect { usuario ->
                val comId = usuario?.comunidadId.orEmpty()
                uiState = uiState.copy(
                    usuarioActual = usuario,
                    comunidadId = comId
                )
            }
        }
    }

    /**
     * Abre o crea la conversación directa entre el usuario actual (comprador/prestatario)
     * y el vendedor/prestador de un artículo, y comienza a escuchar los mensajes.
     */
    fun abrirChatConProducto(
        productoId: String,
        productoTitulo: String,
        productoPrecio: String,
        productoImagenUrl: String,
        vendedorUid: String,
        vendedorNombre: String,
        onChatListo: (Conversacion) -> Unit = {}
    ) {
        val usuario = uiState.usuarioActual
        val comId = uiState.comunidadId.ifBlank { usuario?.comunidadId.orEmpty() }
        val compradorUid = usuario?.uid.orEmpty()
        val compradorNombre = usuario?.nombre.orEmpty().ifBlank { "Vecino" }

        if (comId.isBlank() || compradorUid.isBlank()) {
            uiState = uiState.copy(
                errorMensaje = "Debes iniciar sesión y pertenecer a una comunidad para chatear."
            )
            return
        }

        if (compradorUid == vendedorUid) {
            uiState = uiState.copy(
                errorMensaje = "No puedes iniciar un chat contigo mismo sobre tu propio artículo."
            )
            return
        }

        uiState = uiState.copy(cargando = true, errorMensaje = null)

        viewModelScope.launch {
            val resultado = chatRepository.abrirOCrearConversacion(
                comunidadId = comId,
                productoId = productoId,
                productoTitulo = productoTitulo,
                productoPrecio = productoPrecio,
                productoImagenUrl = productoImagenUrl,
                vendedorUid = vendedorUid,
                vendedorNombre = vendedorNombre,
                compradorUid = compradorUid,
                compradorNombre = compradorNombre
            )

            resultado.onSuccess { conversacion ->
                seleccionarConversacion(conversacion, comId)
                uiState = uiState.copy(cargando = false)
                onChatListo(conversacion)
            }.onFailure { e ->
                uiState = uiState.copy(
                    cargando = false,
                    errorMensaje = e.localizedMessage ?: "Error al abrir el chat."
                )
            }
        }
    }

    /**
     * Asigna la conversación activa, marca sus mensajes como leídos y escucha cambios en tiempo real.
     */
    fun seleccionarConversacion(
        conversacion: Conversacion,
        comunidadIdOverride: String = ""
    ) {
        val comId = comunidadIdOverride.ifBlank {
            conversacion.comunidadId.ifBlank { uiState.comunidadId }
        }
        val miUid = uiState.usuarioActual?.uid.orEmpty()

        uiState = uiState.copy(
            comunidadId = comId,
            conversacionActiva = conversacion,
            mensajesActivos = emptyList()
        )

        viewModelScope.launch {
            chatRepository.marcarConversacionComoLeida(comId, conversacion, miUid)
            chatRepository.observarMensajes(comId, conversacion.id).collectLatest { mensajes ->
                uiState = uiState.copy(mensajesActivos = mensajes)
            }
        }
    }

    /**
     * Envía un mensaje dentro de la conversación activa.
     */
    fun enviarMensaje(texto: String) {
        val conv = uiState.conversacionActiva ?: return
        val comId = uiState.comunidadId.ifBlank {
            conv.comunidadId
        }
        val usuario = uiState.usuarioActual ?: return
        val remitenteUid = usuario.uid
        val remitenteNombre = usuario.nombre.ifBlank {
            "Vecino"
        }

        viewModelScope.launch {
            val resultado = chatRepository.enviarMensaje(
                comunidadId = comId,
                conversacion = conv,
                remitenteUid = remitenteUid,
                remitenteNombre = remitenteNombre,
                texto = texto
            )
            resultado.onFailure { e ->
                uiState = uiState.copy(
                    errorMensaje = e.localizedMessage ?: "No se pudo enviar el mensaje."
                )
            }
        }
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }
}