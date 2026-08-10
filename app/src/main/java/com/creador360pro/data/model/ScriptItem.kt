package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaModificacion: Long = System.currentTimeMillis()
)
