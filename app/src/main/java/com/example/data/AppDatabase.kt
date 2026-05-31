package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProjectFile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
}
