package com.example.radiopatio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.radiopatio.data.PublicacionDao
import com.example.radiopatio.data.PublicacionEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioPatioViewModelFactory(
    private val publicacionDao: PublicacionDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RadioPatioViewModel::class.java)) {
            return RadioPatioViewModel(publicacionDao) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}

class RadioPatioViewModel(
    private val publicacionDao: PublicacionDao
) : ViewModel() {

    var publicaciones by mutableStateOf<List<PublicacionEntidad>>(emptyList())
        private set

    fun cargarPublicaciones() {
        viewModelScope.launch { // Inicia corrutina en el ámbito del ViewModel[cite: 12]
            val lista = withContext(Dispatchers.IO) { // Cambia el contexto al hilo de E/S[cite: 15]
                publicacionDao.obtenerPublicaciones()
            }
            publicaciones = lista
        }
    }

    fun crearPublicacion(publicacion: PublicacionEntidad) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                publicacionDao.insertar(publicacion)
            }
            cargarPublicaciones() // Recarga la lista tras insertar
        }
    }

    /**
     * Elimina una publicación de la base de datos Room y recarga la lista[cite: 22, 35].
     */
    fun eliminarPublicacion(publicacion: PublicacionEntidad) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                publicacionDao.eliminar(publicacion)
            }
            cargarPublicaciones() // Recarga la lista tras eliminar
        }
    }

    fun insertarDatosIniciales() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (publicacionDao.obtenerPublicaciones().isEmpty()) {
                    val datosEjemplo = listOf(
                        PublicacionEntidad(
                            autor = "Lucía Fernández",
                            inicial = "L",
                            tiempoYUbica = "Hace 2 h · Calle Mayor",
                            contenido = "¡Buenas vecin@s! ☀️ Este sábado organizamos una quedada en el parque para tomar algo. ¡Os esperamos!",
                            meGusta = 24,
                            comentarios = 6
                        ),
                        PublicacionEntidad(
                            autor = "María García",
                            inicial = "M",
                            tiempoYUbica = "Hace 3 h · Calle Mayor",
                            contenido = "Se ha encontrado un peluche en el portal, se ha dejado en la garita del conserje.",
                            meGusta = 0,
                            comentarios = 0
                        ),
                        PublicacionEntidad(
                            autor = "Carlos Ruiz",
                            inicial = "C",
                            tiempoYUbica = "Hace 5 h · Portal 4",
                            contenido = "¿Alguien sabe si mañana cortan el agua por obras en la caldera?",
                            meGusta = 4,
                            comentarios = 8
                        ),
                        PublicacionEntidad(
                            autor = "Ana Belén",
                            inicial = "A",
                            tiempoYUbica = "Hace 6 h · 2º B",
                            contenido = "Tengo cajas de cartón limpias para mudanza por si a alguien le hacen falta antes de tirarlas al contenedor azul.",
                            meGusta = 9,
                            comentarios = 3
                        ),
                        PublicacionEntidad(
                            autor = "Javier Gómez",
                            inicial = "J",
                            tiempoYUbica = "Ayer · Garaje",
                            contenido = "Recordad aparcar dentro de las líneas en la planta -1 para no dificultar la maniobra de salida.",
                            meGusta = 15,
                            comentarios = 2
                        )
                    )
                    datosEjemplo.forEach { publicacionDao.insertar(it) }
                }
            }
            cargarPublicaciones()
        }
    }
}