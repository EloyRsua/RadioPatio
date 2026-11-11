package com.example.radiopatio.eventos

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
    val esDestacado: Boolean = false,
    val comunidadId: String = "",
    val creadorUid: String = "",
    val asistentesUids: List<String> = emptyList(),
    val recordatoriosUids: List<String> = emptyList()
)

object DatosEjemploEventos {
    fun obtenerEventos(comunidadId: String = "com_preview"): List<Evento> {
        val hoy = LocalDate.now()
        return listOf(
            Evento(
                id = "evt-1",
                titulo = "Fiestas del Barrio 2026 🎉",
                descripcion = "¡Gran celebración para todos los vecinos! Habrá música en directo, barbacoa vecinal en el parque, castillos hinchables para los peques y concurso de tortillas con premios.",
                fecha = hoy.withDayOfMonth(minOf(17, hoy.lengthOfMonth())),
                hora = "18:00 - 23:30",
                ubicacion = "Parque del Oeste",
                categoria = CategoriaEvento.FIESTAS,
                organizador = "Comisión de Fiestas del Barrio",
                asistentes = 38,
                asistiendoUsuario = true,
                recordatorioActivado = true,
                esDestacado = true,
                comunidadId = comunidadId
            ),
            Evento(
                id = "evt-2",
                titulo = "Reunión Ordinaria de Comunidad 📋",
                descripcion = "Convocatoria para tratar la aprobación de cuentas, reforma del ascensor del portal 2 y nuevas normas para zonas comunes.",
                fecha = hoy.withDayOfMonth(minOf(20, hoy.lengthOfMonth())),
                hora = "19:30 - 21:00",
                ubicacion = "Sala de Juntas · Portal 4",
                categoria = CategoriaEvento.REUNION,
                organizador = "Presidencia y Administración",
                asistentes = 14,
                asistiendoUsuario = false,
                recordatorioActivado = false,
                esDestacado = false,
                comunidadId = comunidadId
            )
        )
    }
}
