package com.example.radiopatio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.comunidad.ComunidadRepository
import com.example.radiopatio.data.ComunidadDao
import com.example.radiopatio.data.ComunidadEntidad
import com.example.radiopatio.data.PublicacionDao
import com.example.radiopatio.data.PublicacionEntidad
import com.example.radiopatio.data.toEntidad
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.publicacion.PublicacionRepository
import com.example.radiopatio.usuarios.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Estado inmutable del Tablón principal con aislamiento multi-inquilino por `comunidadIdActual`.
 */
data class TablonUiState(
    val comunidadIdActual: String = "",
    val comunidadNombre: String = "",
    val usuarioActual: Usuario? = null,
    val publicacionesTotales: List<PublicacionEntidad> = emptyList(),
    val textoBusqueda: String = "",
    val cargando: Boolean = true,
    val errorMensaje: String? = null
) {
    val publicacionesFiltradas: List<PublicacionEntidad>
        get() = publicacionesTotales
            .filter { pub ->
                val coincideComunidad = comunidadIdActual.isNotBlank() && pub.comunidadId == comunidadIdActual
                val consulta = textoBusqueda.trim()
                val coincideTexto = consulta.isBlank() ||
                    pub.contenido.contains(consulta, ignoreCase = true) ||
                    pub.autor.contains(consulta, ignoreCase = true) ||
                    pub.tiempoYUbica.contains(consulta, ignoreCase = true)
                coincideComunidad && coincideTexto
            }
            .distinctBy { it.firestoreId.ifBlank { "local-${it.id}" } }
}

class RadioPatioViewModelFactory(
    private val publicacionDao: PublicacionDao,
    private val comunidadDao: ComunidadDao? = null,
    private val userRepository: UserRepository = UserRepository(),
    private val publicacionRepository: PublicacionRepository = PublicacionRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RadioPatioViewModel::class.java)) {
            return RadioPatioViewModel(
                publicacionDao = publicacionDao,
                comunidadDao = comunidadDao,
                userRepository = userRepository,
                publicacionRepository = publicacionRepository,
                comunidadRepository = comunidadRepository
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RadioPatioViewModel(
    private val publicacionDao: PublicacionDao,
    private val comunidadDao: ComunidadDao? = null,
    private val userRepository: UserRepository = UserRepository(),
    private val publicacionRepository: PublicacionRepository = PublicacionRepository(),
    private val comunidadRepository: ComunidadRepository = ComunidadRepository()
) : ViewModel() {

    var uiState by mutableStateOf(TablonUiState())
        private set

    val publicaciones: List<PublicacionEntidad>
        get() = uiState.publicacionesFiltradas

    private var syncFirestoreJob: Job? = null
    private var syncComunidadRoomJob: Job? = null
    private var comunidadSincronizada: String? = null
    private val roomSyncMutex = Mutex()

    init {
        observarTablonEnTiempoReal()
    }

    private fun observarTablonEnTiempoReal() {
        viewModelScope.launch {
            userRepository.observarUsuarioActual()
                .flatMapLatest { usuario ->
                    val comId = usuario?.comunidadId.orEmpty()
                    sincronizarFirestoreConRoom(comId)

                    if (comId.isBlank()) {
                        flowOf(Triple<Usuario?, String, List<PublicacionEntidad>>(usuario, "", emptyList()))
                    } else {
                        combine(
                            comunidadRepository.observarComunidadPorId(comId),
                            publicacionDao.observarPublicacionesPorComunidad(comId)
                        ) { comunidad, listaRoom ->
                            val nombreCom = comunidad?.nombre?.takeIf { it.isNotBlank() }
                                ?: usuario?.comunidadNombre
                                ?: ""
                            Triple(usuario, nombreCom, listaRoom)
                        }
                    }
                }
                .collect { (usuario, nombreCom, lista) ->
                    val comId = usuario?.comunidadId.orEmpty()
                    uiState = uiState.copy(
                        comunidadIdActual = comId,
                        comunidadNombre = nombreCom,
                        usuarioActual = usuario,
                        publicacionesTotales = lista.filter { comId.isNotBlank() && it.comunidadId == comId },
                        cargando = false
                    )
                }
        }
    }

    private fun sincronizarFirestoreConRoom(comunidadId: String) {
        if (comunidadSincronizada == comunidadId) return
        comunidadSincronizada = comunidadId
        syncFirestoreJob?.cancel()
        syncComunidadRoomJob?.cancel()
        if (comunidadId.isBlank()) return

        syncComunidadRoomJob = viewModelScope.launch {
            comunidadRepository.observarComunidadPorId(comunidadId).collect { comunidad ->
                if (comunidad != null) {
                    roomSyncMutex.withLock {
                        withContext(Dispatchers.IO) {
                            comunidadDao?.upsert(comunidad.toEntidad())
                        }
                    }
                }
            }
        }

        syncFirestoreJob = viewModelScope.launch {
            publicacionRepository.observarPublicacionesDeComunidad(
                comunidadId = comunidadId,
                onError = { mensaje ->
                    uiState = uiState.copy(errorMensaje = mensaje)
                }
            )
                .collect { remotas ->
                    val filtradas = remotas
                        .filter { it.comunidadId == comunidadId }
                        .distinctBy { it.firestoreId.ifBlank { "local-${it.id}" } }
                    roomSyncMutex.withLock {
                        withContext(Dispatchers.IO) {
                            asegurarComunidadPadreEnRoom(comunidadId)
                            publicacionDao.reemplazarPublicacionesComunidad(comunidadId, filtradas)
                        }
                    }
                }
        }
    }

    private suspend fun asegurarComunidadPadreEnRoom(comunidadId: String) {
        if (comunidadDao == null || comunidadId.isBlank()) return
        comunidadDao.insertarSiNoExiste(
            ComunidadEntidad(
                id = comunidadId,
                nombre = uiState.comunidadNombre.ifBlank { comunidadId },
                direccion = ""
            )
        )
    }

    fun onTextoBusquedaChange(nuevoTexto: String) {
        uiState = uiState.copy(textoBusqueda = nuevoTexto)
    }

    fun onLimpiarError() {
        uiState = uiState.copy(errorMensaje = null)
    }

    fun crearPublicacion(
        autor: String,
        ubicacion: String,
        contenido: String,
        onResultado: (Boolean) -> Unit = {}
    ) {
        val comId = uiState.comunidadIdActual
        if (comId.isBlank()) {
            uiState = uiState.copy(errorMensaje = "Únete primero a una comunidad para publicar.")
            onResultado(false)
            return
        }

        val autorLimpio = autor.trim().ifBlank {
            uiState.usuarioActual?.nombre?.ifBlank { "Vecino Anónimo" } ?: "Vecino Anónimo"
        }
        val ubicacionLimpia = ubicacion.trim().ifBlank {
            uiState.usuarioActual?.vivienda?.ifBlank { "Comunidad" } ?: "Comunidad"
        }
        val inicial = autorLimpio.firstOrNull()?.uppercase() ?: "V"
        val uid = uiState.usuarioActual?.uid.orEmpty()

        viewModelScope.launch {
            val resultado = publicacionRepository.crearPublicacion(
                comunidadId = comId,
                autorUid = uid,
                autor = autorLimpio,
                inicial = inicial,
                tiempoYUbica = "Ahora mismo · $ubicacionLimpia",
                contenido = contenido.trim()
            )
            resultado.onSuccess { entidad ->
                roomSyncMutex.withLock {
                    withContext(Dispatchers.IO) {
                        asegurarComunidadPadreEnRoom(comId)
                        if (entidad.firestoreId.isNotBlank()) {
                            publicacionDao.eliminarPorFirestoreId(entidad.firestoreId, comId)
                        }
                        publicacionDao.insertar(entidad)
                    }
                }
                onResultado(true)
            }.onFailure { error ->
                uiState = uiState.copy(
                    errorMensaje = error.localizedMessage ?: "Error al crear la publicación."
                )
                onResultado(false)
            }
        }
    }

    /**
     * Elimina una publicación de Firestore y Room validando el aislamiento por comunidad.
     */
    fun eliminarPublicacion(publicacion: PublicacionEntidad) {
        val comId = uiState.comunidadIdActual
        if (comId.isBlank() || publicacion.comunidadId != comId) {
            uiState = uiState.copy(errorMensaje = "No puedes eliminar publicaciones de otra comunidad.")
            return
        }

        viewModelScope.launch {
            val resultado = publicacionRepository.eliminarPublicacion(publicacion, comId)
            resultado.onSuccess {
                roomSyncMutex.withLock {
                    withContext(Dispatchers.IO) {
                        if (publicacion.firestoreId.isNotBlank()) {
                            publicacionDao.eliminarPorFirestoreId(publicacion.firestoreId, comId)
                        } else {
                            publicacionDao.eliminar(publicacion)
                        }
                    }
                }
            }.onFailure { error ->
                uiState = uiState.copy(
                    errorMensaje = error.localizedMessage ?: "No se pudo eliminar la publicación."
                )
            }
        }
    }
}
