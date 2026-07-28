package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ideas")
data class IdeaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val gancho: String,
    val hashtags: String,
    val categoria: String,
    val efemerideOpcional: String? = null,
    val estadoUso: String = "disponible" // disponible, usada, favorita, no_gustada
)
