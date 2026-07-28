package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.IncomeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_records ORDER BY fecha DESC")
    fun getAllIncomes(): Flow<List<IncomeRecord>>

    @Query("SELECT * FROM income_records WHERE fecha BETWEEN :start AND :end")
    fun getIncomesInRange(start: Long, end: Long): Flow<List<IncomeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeRecord): Long

    @Update
    suspend fun updateIncome(income: IncomeRecord)

    @Delete
    suspend fun deleteIncome(income: IncomeRecord)
}
