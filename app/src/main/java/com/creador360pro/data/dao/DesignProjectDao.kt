package com.creador360pro.data.dao

import androidx.room.*
import com.creador360pro.data.model.DesignProject
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignProjectDao {
    @Query("SELECT * FROM design_projects ORDER BY fechaModificacion DESC")
    fun getAllProjects(): Flow<List<DesignProject>>

    @Query("SELECT * FROM design_projects ORDER BY fechaModificacion DESC LIMIT 4")
    fun getRecentProjects(): Flow<List<DesignProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: DesignProject): Long

    @Update
    suspend fun updateProject(project: DesignProject)

    @Delete
    suspend fun deleteProject(project: DesignProject)

    @Query("SELECT * FROM design_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): DesignProject?
}
