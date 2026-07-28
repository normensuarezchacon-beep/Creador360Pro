package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val plataforma: String,
    val seguidores: Int = 0,
    val telefono: String? = null,
    val email: String? = null,
    val notas: String? = null
)
