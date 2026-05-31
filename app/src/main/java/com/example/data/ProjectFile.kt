package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_files")
data class ProjectFile(
    @PrimaryKey val path: String, // e.g., "index.html", "assets/style.css"
    val isDirectory: Boolean,
    val content: String = "",
    val lastModified: Long = System.currentTimeMillis()
) {
    val name: String
        get() = path.substringAfterLast('/')

    val parentPath: String
        get() = if (path.contains('/')) path.substringBeforeLast('/') else ""

    val extension: String
        get() = if (isDirectory) "" else path.substringAfterLast('.', "")
}
