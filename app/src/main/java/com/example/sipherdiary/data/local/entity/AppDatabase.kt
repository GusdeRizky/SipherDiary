package com.example.sipherdiary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sipherdiary.data.local.dao.EntryDao
import com.example.sipherdiary.data.local.dao.IntruderDao
import com.example.sipherdiary.data.local.entity.EntryEntity
import com.example.sipherdiary.data.local.entity.IntruderLogEntity

@Database(
    entities = [EntryEntity::class, IntruderLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun intruderDao(): IntruderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sipher_diary.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}