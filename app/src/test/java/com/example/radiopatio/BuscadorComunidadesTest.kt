package com.example.radiopatio

import com.example.radiopatio.comunidad.BuscadorComunidadesUiState
import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.comunidad.OrdenComunidad
import com.example.radiopatio.comunidad.filtrarYOrdenarComunidades
import com.example.radiopatio.informacionUsuario.ROL_ADMINISTRADOR_FINCAS
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.registroInicio.DatosRegistroComunidad
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
    fun ordenacionPorVecinosYNombre() {
        val porVecinos = filtrarYOrdenarComunidades(
            comunidades = comunidades,
            textoBusqueda = "",
            ordenSeleccionado = OrdenComunidad.MAS_VECINOS
        )
        assertEquals(listOf("c2", "c4", "c1", "c3"), porVecinos.map { it.id })

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

    @Test
    fun soloAdministradoresFincasPuedenCrearComunidades() {
        val vecino = Usuario("Lucía", "L", "2ºA", "l@rp.es", "", rol = ROL_VECINO, uid = "u_vec")
        val admin = Usuario(
            nombre = "Marta",
            inicial = "M",
            vivienda = "Administración",
            email = "m@rp.es",
            telefono = "",
            rol = ROL_ADMINISTRADOR_FINCAS,
            uid = "u_adm",
            tipoCuenta = ROL_ADMINISTRADOR_FINCAS
        )

        val stateVecino = BuscadorComunidadesUiState(usuarioActual = vecino)
        assertFalse(stateVecino.puedeCrearComunidad(modoRegistro = false, esAdministradorRegistro = false))
        assertFalse(stateVecino.puedeCrearComunidad(modoRegistro = true, esAdministradorRegistro = false))

        val stateAdmin = BuscadorComunidadesUiState(usuarioActual = admin)
        assertTrue(stateAdmin.puedeCrearComunidad(modoRegistro = false, esAdministradorRegistro = false))
        assertTrue(stateAdmin.puedeCrearComunidad(modoRegistro = true, esAdministradorRegistro = true))
    }

    @Test
    fun datosRegistroComunidad_validaCorrectamenteSegunTipoCuenta() {
        val comunidadExistente = comunidades.first()

        assertFalse(DatosRegistroComunidad().estaConfigurada(esAdministrador = false))
        assertTrue(DatosRegistroComunidad(comunidadExistente = comunidadExistente).estaConfigurada(esAdministrador = false))
        assertFalse(
            DatosRegistroComunidad(
                crearNueva = true,
                nuevaComunidadNombre = "Nueva",
                nuevaComunidadDireccion = "Calle 1"
            ).estaConfigurada(esAdministrador = false)
        )

        assertTrue(DatosRegistroComunidad().estaConfigurada(esAdministrador = true))
        assertTrue(
            DatosRegistroComunidad(
                crearNueva = true,
                nuevaComunidadNombre = "Nueva",
                nuevaComunidadDireccion = "Calle 1"
            ).estaConfigurada(esAdministrador = true)
        )
        assertTrue(
            DatosRegistroComunidad(
                solicitarTraspaso = true,
                comunidadExistente = comunidadExistente
            ).estaConfigurada(esAdministrador = true)
        )
    }
}

