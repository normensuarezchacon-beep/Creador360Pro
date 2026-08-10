package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_records")
data class AudioRecordItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val filePath: String,
    val duracion: Long = 0,
    val fechaCreacion: Long = System.currentTimeMillis()
)
