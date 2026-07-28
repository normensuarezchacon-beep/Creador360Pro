package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val fechaCreacion: Long,
    val fechaModificacion: Long,
    val jsonClips: String,
    val jsonOverlays: String,
    val jsonAudioTracks: String,
    val duracionTotal: Long,
    val thumbnailPath: String? = null
)
