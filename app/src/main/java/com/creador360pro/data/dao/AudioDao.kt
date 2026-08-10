package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.AudioRecordItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_records ORDER BY fechaCreacion DESC")
    fun getAllRecords(): Flow<List<AudioRecordItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AudioRecordItem): Long

    @Delete
    suspend fun deleteRecord(record: AudioRecordItem)
}
