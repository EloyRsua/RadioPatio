package com.example.radiopatio

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

enum class CategoriaEvento(
    val nombre: String,
    val emoji: String,
    val colorTexto: Color,
    val colorFondo: Color
) {
    TODOS("Todos", "📌", Color(0xFF133E54), Color(0xFFEAEFF2)),
    FIESTAS("Fiestas", "🎉", Color(0xFFD9531E), Color(0xFFFFEDE6)),
    REUNION("Reunión", "👥", Color(0xFF0F8A5F), Color(0xFFEAF5EC)),
    DEPORTES("Deporte", "⚽", Color(0xFF1976D2), Color(0xFFE3F2FD)),
    AVISOS("Mantenimiento", "🛠️", Color(0xFFC2410C), Color(0xFFFFF7ED)),
    SOLIDARIO("Solidario", "🤝", Color(0xFF7B1FA2), Color(0xFFF3E5F5))
}

data class Evento(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fecha: LocalDate,
    val hora: String,
    val ubicacion: String,
    val categoria: CategoriaEvento,
    val organizador: String,
    val asistentes: Int,
    val asistiendoUsuario: Boolean = false,
    val recordatorioActivado: Boolean = false,
    val esDestacado: Boolean = false
)

object DatosEjemploEventos {
    fun obtenerEventos(): List<Evento> {
        val hoy = LocalDate.of(2026, 10, 15)
        return listOf(
            Evento(
                id = "evt-1",
                titulo = "Fiestas del Barrio 2026 🎉",
                descripcion = "¡Gran celebración para todos los vecinos! Habrá música en directo, barbacoa vecinal en el parque, castillos hinchables para los peques y concurso de tortillas con premios.",
                fecha = LocalDate.of(2026, 10, 17),
                hora = "18:00 - 23:30",
                ubicacion = "Parque del Oeste",
                categoria = CategoriaEvento.FIESTAS,
                organizador = "Comisión de Fiestas del Barrio",
                asistentes = 38,
                asistiendoUsuario = true,
                recordatorioActivado = true,
                esDestacado = true
            ),
            Evento(
                id = "evt-2",
                titulo = "Reunión Ordinaria de Comunidad 📋",
                descripcion = "Convocatoria para tratar la aprobación de cuentas, reforma del ascensor del portal 2 y nuevas normas para zonas comunes.",
                fecha = LocalDate.of(2026, 10, 20),
                hora = "19:30 - 21:00",
                ubicacion = "Sala de Juntas · Portal 4",
                categoria = CategoriaEvento.REUNION,
                organizador = "Presidencia y Administración",
                asistentes = 14,
                asistiendoUsuario = false,
                recordatorioActivado = false,
                esDestacado = false
            ),
            Evento(
                id = "evt-3",
                titulo = "Corte Programado de Agua (Caldera) ⚠️",
                descripcion = "Corte de suministro de agua caliente por revisión anual reglamentaria y purga del circuito general.",
                fecha = LocalDate.of(2026, 10, 22),
                hora = "09:00 - 13:00",
                ubicacion = "Portales 1 al 6 (General)",
                categoria = CategoriaEvento.AVISOS,
                organizador = "Mantenimiento Técnico HidroCal",
                asistentes = 29,
                asistiendoUsuario = false,
                recordatorioActivado = true,
                esDestacado = false
            ),
            Evento(
                id = "evt-4",
                titulo = "Torneo de Pádel Vecinal 🎾",
                descripcion = "Torneo express por parejas para todos los niveles de vecinos del barrio. Al terminar compartiremos un aperitivo en el bar del club.",
                fecha = LocalDate.of(2026, 10, 24),
                hora = "10:30 - 14:00",
                ubicacion = "Pistas Municipales La Florida",
                categoria = CategoriaEvento.DEPORTES,
                organizador = "Carlos Ruiz (Portal 4)",
                asistentes = 16,
                asistiendoUsuario = false,
                recordatorioActivado = false,
                esDestacado = false
            ),
            Evento(
                id = "evt-5",
                titulo = "Quedada Solidaria: Limpieza del Parque 🌿",
                descripcion = "Jornada de voluntariado para recoger residuos y plantar flores en las zonas verdes del parque infantil. ¡Traed guantes y ganas de colaborar!",
                fecha = LocalDate.of(2026, 10, 25),
                hora = "11:00 - 13:00",
                ubicacion = "Entrada Principal · Parque del Oeste",
                categoria = CategoriaEvento.SOLIDARIO,
                organizador = "Asociación Vecinal Sostenible",
                asistentes = 22,
                asistiendoUsuario = true,
                recordatorioActivado = false,
                esDestacado = false
            ),
            Evento(
                id = "evt-6",
                titulo = "Taller de Intercambio y Reparación 🔧",
                descripcion = "Banco de herramientas comunitarias y reparación guiada de pequeños aparatos y bicicletas. Trae lo que necesite arreglo.",
                fecha = LocalDate.of(2026, 10, 28),
                hora = "17:00 - 19:30",
                ubicacion = "Local Comunitario (Bajo C)",
                categoria = CategoriaEvento.SOLIDARIO,
                organizador = "Red Prestes RadioPatio",
                asistentes = 11,
                asistiendoUsuario = false,
                recordatorioActivado = false,
                esDestacado = false
            )
        )
    }
}
