package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "design_projects")
data class DesignProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val fechaCreacion: Long,
    val fechaModificacion: Long,
    val jsonCapas: String,
    val anchoLienzo: Int,
    val altoLienzo: Int,
    val thumbnailPath: String? = null
)
