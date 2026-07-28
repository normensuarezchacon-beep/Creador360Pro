package com.creador360pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_records")
data class IncomeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monto: Double,
    val moneda: String,
    val fuente: String,
    val fecha: Long,
    val notas: String? = null
)
