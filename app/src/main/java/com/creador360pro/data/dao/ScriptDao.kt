package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.ScriptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY fechaModificacion DESC")
    fun getAllScripts(): Flow<List<ScriptItem>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: Long): ScriptItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptItem): Long

    @Update
    suspend fun updateScript(script: ScriptItem)

    @Delete
    suspend fun deleteScript(script: ScriptItem)
}
