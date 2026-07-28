package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collaboration_history")
data class CollaborationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val fecha: Long,
    val tipo: String,
    val resultado: String? = null,
    val valorEstimado: Double = 0.0,
    val descripcion: String? = null
)
