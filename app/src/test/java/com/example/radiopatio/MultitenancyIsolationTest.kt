package com.example.radiopatio

import com.example.radiopatio.comunidad.Comunidad
import com.example.radiopatio.data.PublicacionEntidad
import com.example.radiopatio.informacionUsuario.Usuario
import com.example.radiopatio.marketplace.CategoriaProducto
import com.example.radiopatio.marketplace.MercadilloUiState
import com.example.radiopatio.marketplace.Producto
import com.example.radiopatio.vecinos.VecinosUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MultitenancyIsolationTest {

    private val comA = "com_a"
    private val comB = "com_b"

    @Test
    fun vecinosUiState_filtraEstrictamentePorComunidadId() {
        val vecinos = listOf(
            Usuario("Carlos Ruiz", "C", "Portal 1, 2ºA", "c@rp.es", "", rol = "Presidente", uid = "u1", comunidadId = comA),
            Usuario("Lucía Fernández", "L", "Portal 2, 4ºC", "l@rp.es", "", rol = "Vecino/a", uid = "u2", comunidadId = comA),
            Usuario("Elena Morales", "E", "Bloque B, 3ºD", "e@rp.es", "", rol = "Vocal", uid = "u3", comunidadId = comB)
        )

        val stateA = VecinosUiState(comunidadIdActual = comA, vecinos = vecinos)
        assertEquals(listOf("u1", "u2"), stateA.vecinosFiltrados.map { it.uid })
        assertEquals(listOf("Todos", "Presidente", "Vecino/a"), stateA.rolesDisponibles)

        val stateBusqueda = stateA.copy(textoBusqueda = "Elena")
        assertTrue(stateBusqueda.vecinosFiltrados.isEmpty())

        val stateBusquedaPorPiso = stateA.copy(textoBusqueda = "Portal 2")
        assertEquals(listOf("u2"), stateBusquedaPorPiso.vecinosFiltrados.map { it.uid })
    }

    @Test
    fun vecinosUiState_conComunidadVacia_noExponeVecinos() {
        val vecinos = listOf(
            Usuario("Carlos Ruiz", "C", "Portal 1", "c@rp.es", "", uid = "u1", comunidadId = comA),
            Usuario("Sin Comunidad", "S", "Portal 1", "s@rp.es", "", uid = "u2", comunidadId = "")
        )
        val state = VecinosUiState(comunidadIdActual = "", vecinos = vecinos)
        assertTrue(state.vecinosFiltrados.isEmpty())
        assertEquals(listOf("Todos"), state.rolesDisponibles)
    }

    @Test
    fun tablonUiState_filtraEstrictamentePorComunidadId() {
        val publicaciones = listOf(
            PublicacionEntidad(1, "p1", comA, "u1", "Carlos", "C", "Hoy · 2ºA", "Reunión en el portal", 2, 1, 100L),
            PublicacionEntidad(2, "p2", comB, "u3", "Elena", "E", "Hoy · Bloque B", "Reunión piscina", 5, 0, 200L)
        )

        val stateA = TablonUiState(comunidadIdActual = comA, publicacionesTotales = publicaciones)
        assertEquals(listOf("p1"), stateA.publicacionesFiltradas.map { it.firestoreId })

        val stateSinComunidad = TablonUiState(comunidadIdActual = "", publicacionesTotales = publicaciones)
        assertTrue(stateSinComunidad.publicacionesFiltradas.isEmpty())
    }

    @Test
    fun mercadilloUiState_filtraEstrictamentePorComunidadIdYModo() {
        val productos = listOf(
            Producto(1, "Mesa auxiliar", "15€", "1ºA", CategoriaProducto.MUEBLES, true, comunidadId = comA),
            Producto(2, "Taladro Bosch", "10€", "2ºB", CategoriaProducto.HERRAMIENTAS, false, comunidadId = comA),
            Producto(3, "Mesa jardín", "45€", "Bloque B", CategoriaProducto.MUEBLES, true, comunidadId = comB)
        )

        val stateComprarA = MercadilloUiState(
            comunidadActualId = comA,
            esModoComprar = true,
            productosTotales = productos
        )
        assertEquals(listOf(1), stateComprarA.productosFiltrados.map { it.id })

        val statePrestarA = stateComprarA.copy(esModoComprar = false)
        assertEquals(listOf(2), statePrestarA.productosFiltrados.map { it.id })

        val stateSinComunidad = stateComprarA.copy(comunidadActualId = "")
        assertTrue(stateSinComunidad.productosFiltrados.isEmpty())
    }

    @Test
    fun calendarioUiState_filtraEventosYIndicadoresPorComunidadId() {
        val mes = YearMonth.of(2026, 11)
        val dia15 = LocalDate.of(2026, 11, 15)
        val dia20 = LocalDate.of(2026, 11, 20)

        val eventos = listOf(
            Evento("e1", "Junta", "Desc", dia15, "19:00", "Portal 1", CategoriaEvento.REUNION, "Carlos", 5, comunidadId = comA),
            Evento("e2", "Fiesta", "Desc", dia15, "20:00", "Patio B", CategoriaEvento.FIESTAS, "Elena", 10, comunidadId = comB),
            Evento("e3", "Limpieza", "Desc", dia20, "10:00", "Jardín", CategoriaEvento.SOLIDARIO, "Lucía", 3, comunidadId = comA)
        )

        val stateA = CalendarioUiState(
            mesActual = mes,
            fechaHoy = dia15,
            comunidadActualId = comA,
            listaEventos = eventos
        )

        assertEquals(listOf("e1", "e3"), stateA.eventosFiltrados.map { it.id })
        assertEquals(setOf(15, 20), stateA.diasConEventos.keys)
        assertEquals(listOf("e1"), stateA.diasConEventos[15]?.map { it.id })

        val stateDia15 = stateA.copy(diaSeleccionado = dia15)
        assertEquals(listOf("e1"), stateDia15.eventosFiltrados.map { it.id })

        val stateSinComunidad = stateA.copy(comunidadActualId = "")
        assertTrue(stateSinComunidad.eventosFiltrados.isEmpty())
        assertTrue(stateSinComunidad.diasConEventos.isEmpty())
    }

    @Test
    fun comunidad_direccionCompleta_formateaCorrectamente() {
        val completa = Comunidad(
            id = "c1",
            nombre = "Residencial Montecerrao",
            direccion = "Calle Mayor, 12",
            codigoPostal = "33006",
            ciudad = "Oviedo"
        )
        assertEquals("Calle Mayor, 12 · 33006 · Oviedo", completa.direccionCompleta())

        val vacia = Comunidad(id = "c2", nombre = "Sin Dirección")
        assertEquals("Dirección no especificada", vacia.direccionCompleta())
    }
}

