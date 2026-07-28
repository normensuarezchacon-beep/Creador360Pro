package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.CollaborationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface CollaborationDao {
    @Query("SELECT * FROM collaboration_history WHERE contactId = :contactId ORDER BY fecha DESC")
    fun getHistoryForContact(contactId: Long): Flow<List<CollaborationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CollaborationHistory): Long

    @Delete
    suspend fun deleteHistory(history: CollaborationHistory)
}
