package com.creador360pro.ui.planificar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creador360pro.data.dao.CalendarDao
import com.creador360pro.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

class CalendarViewModel(private val calendarDao: CalendarDao) : ViewModel() {

    private var currentWeekStart: Long = getWeekStart(System.currentTimeMillis())

    fun getEventsForCurrentWeek(): Flow<List<CalendarEvent>> {
        val weekEnd = currentWeekStart + 7 * 24 * 60 * 60 * 1000
        return calendarDao.getEventsInRange(currentWeekStart, weekEnd)
    }

    fun addEvent(titulo: String, diaOffset: Int) {
        val eventDate = currentWeekStart + diaOffset * 24 * 60 * 60 * 1000
        val event = CalendarEvent(
            titulo = titulo,
            plataforma = "General",
            tipo = "Publicación",
            estado = "Pendiente",
            prioridad = "Media",
            fechaHora = eventDate
        )
        viewModelScope.launch {
            calendarDao.insertEvent(event)
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            calendarDao.deleteEvent(event)
        }
    }

    fun goToPreviousWeek() {
        currentWeekStart -= 7 * 24 * 60 * 60 * 1000
    }

    fun goToNextWeek() {
        currentWeekStart += 7 * 24 * 60 * 60 * 1000
    }

    fun getCurrentWeekLabel(): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentWeekStart
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "Semana del $day/$month"
    }

    fun getDayLabel(diaOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentWeekStart + diaOffset * 24 * 60 * 60 * 1000
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val diasSemana = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val diaSemana = diasSemana[cal.get(Calendar.DAY_OF_WEEK) - 1]
        return "$diaSemana $day/$month"
    }

    private fun getWeekStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
