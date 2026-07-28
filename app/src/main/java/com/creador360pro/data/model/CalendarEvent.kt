package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val plataforma: String,
    val tipo: String,
    val estado: String,
    val prioridad: String,
    val fechaHora: Long,
    val notas: String? = null
)
