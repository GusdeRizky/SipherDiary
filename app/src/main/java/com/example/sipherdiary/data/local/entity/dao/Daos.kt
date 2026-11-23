package com.example.sipherdiary.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.sipherdiary.data.local.entity.EntryEntity
import com.example.sipherdiary.data.local.entity.IntruderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE is_draft = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EntryEntity?

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM entries")
    suspend fun wipe()
}

@Dao
interface IntruderDao {
    @Query("SELECT * FROM intruder_logs ORDER BY occurred_at DESC")
    fun observeAll(): Flow<List<IntruderLogEntity>>

    @Upsert
    suspend fun insert(log: IntruderLogEntity)

    @Query("DELETE FROM intruder_logs")
    suspend fun wipe()
}