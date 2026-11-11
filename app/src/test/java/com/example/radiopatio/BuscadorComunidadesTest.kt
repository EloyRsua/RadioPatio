package com.example.radiopatio

import com.example.radiopatio.comunidad.BuscadorComunidadesUiState
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.OrdenComunidad
import com.example.radiopatio.comunidad.filtrarYOrdenarComunidades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuscadorComunidadesTest {

    private val comunidades = listOf(
        Comunidad(
            id = "c1",
            nombre = "Residencial Montecerrao",
            direccion = "Calle Fuertes Acevedo, 42",
            ciudad = "Oviedo",
            codigoPostal = "33006",
            tipo = "Residencial",
            totalPortales = 3,
            totalVecinos = 28,
            verificada = true
        ),
        Comunidad(
            id = "c2",
            nombre = "Urbanización La Florida",
            direccion = "Paseo de la Florida, 15",
            ciudad = "Oviedo",
            codigoPostal = "33012",
            tipo = "Urbanización",
            totalPortales = 5,
            totalVecinos = 45,
            verificada = false
        ),
        Comunidad(
            id = "c3",
            nombre = "Edificio Uría Centro",
            direccion = "Calle Uría, 20",
            ciudad = "Oviedo",
            codigoPostal = "33003",
            tipo = "Edificio",
            totalPortales = 1,
            totalVecinos = 12,
            verificada = true
        ),
        Comunidad(
            id = "c4",
            nombre = "Residencial Playa San Lorenzo",
            direccion = "Avenida Rufo Rendueles, 8",
            ciudad = "Gijón",
            codigoPostal = "33203",
            tipo = "Residencial",
            totalPortales = 4,
            totalVecinos = 30,
            verificada = false
        )
    )

    @Test
    fun busquedaInsensibleATildesYMultiparámetro() {
        val sinTilde = filtrarYOrdenarComunidades(comunidades, "uria")
        assertEquals(listOf("c3"), sinTilde.map { it.id })

        val multipalabra = filtrarYOrdenarComunidades(comunidades, "oviedo 33006")
        assertEquals(listOf("c1"), multipalabra.map { it.id })

        val gijonSinTilde = filtrarYOrdenarComunidades(comunidades, "gijon residencial")
        assertEquals(listOf("c4"), gijonSinTilde.map { it.id })
    }

    @Test
    fun filtrosPorTipoCiudadYVerificadasSeCombinanCorrectamente() {
        val soloOviedoVerificadas = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            ciudadSeleccionada = "Oviedo",
            soloVerificadas = true
        )
        assertEquals(listOf("c1", "c3"), soloOviedoVerificadas.map { it.id })

        val porTipo = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            tipoSeleccionado = "Urbanización"
        )
        assertEquals(listOf("c2"), porTipo.map { it.id })
    }

    @Test
    fun ordenacionPorVecinosNombreYPortales() {
        val porVecinos = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            ordenSeleccionado = OrdenComunidad.MAS_VECINOS
        )
        assertEquals(listOf("c2", "c4", "c1", "c3"), porVecinos.map { it.id })

        val porPortales = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            ordenSeleccionado = OrdenComunidad.MAS_PORTALES
        )
        assertEquals(listOf("c2", "c4", "c1", "c3"), porPortales.map { it.id })

        val porNombre = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            ordenSeleccionado = OrdenComunidad.NOMBRE_AZ
        )
        assertEquals(listOf("c3", "c1", "c4", "c2"), porNombre.map { it.id })
    }

    @Test
    fun uiState_calculaCiudadesTiposYContadorDeFiltros() {
        val stateVacio = BuscadorComunidadesUiState(comunidadesTotales = comunidades)
        assertFalse(stateVacio.hayFiltrosActivos)
        assertEquals(0, stateVacio.cantidadFiltrosActivos)
        assertEquals(listOf("Todas", "Gijón", "Oviedo"), stateVacio.ciudadesDisponibles)

        val stateFiltrado = stateVacio.copy(
            ciudadSeleccionada = "Gijón",
            soloVerificadas = true,
            ordenSeleccionado = OrdenComunidad.NOMBRE_AZ
        )
        assertTrue(stateFiltrado.hayFiltrosActivos)
        assertEquals(3, stateFiltrado.cantidadFiltrosActivos)
    }
}

