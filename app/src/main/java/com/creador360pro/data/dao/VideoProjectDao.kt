package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.VideoProject
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY fechaModificacion DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects ORDER BY fechaModificacion DESC LIMIT 4")
    fun getRecentProjects(): Flow<List<VideoProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProject?
}
