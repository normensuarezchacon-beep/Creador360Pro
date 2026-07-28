package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.IdeaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY id DESC")
    fun getAllIdeas(): Flow<List<IdeaItem>>

    @Query("SELECT * FROM ideas WHERE categoria = :categoria")
    fun getIdeasByCategory(categoria: String): Flow<List<IdeaItem>>

    @Query("SELECT * FROM ideas WHERE estadoUso = 'favorita'")
    fun getFavorites(): Flow<List<IdeaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaItem): Long

    @Update
    suspend fun updateIdea(idea: IdeaItem)

    @Delete
    suspend fun deleteIdea(idea: IdeaItem)

    @Query("UPDATE ideas SET estadoUso = :estado WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: String)
}
