package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM project_files ORDER BY path ASC")
    fun getAllFiles(): Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): ProjectFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFile)

    @Delete
    suspend fun deleteFile(file: ProjectFile)

    @Query("DELETE FROM project_files WHERE path = :path")
    suspend fun deleteFileByPath(path: String)

    @Query("DELETE FROM project_files WHERE path LIKE :prefix || '%'")
    suspend fun deleteFilesWithPrefix(prefix: String)
}
