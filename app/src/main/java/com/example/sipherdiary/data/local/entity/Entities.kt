package com.example.sipherdiary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "entries", indices = [Index("date_key")])
data class EntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "title_enc", typeAffinity = ColumnInfo.BLOB) val titleEnc: ByteArray,
    @ColumnInfo(name = "body_enc", typeAffinity = ColumnInfo.BLOB) val bodyEnc: ByteArray,
    @ColumnInfo(name = "mood_enc", typeAffinity = ColumnInfo.BLOB) val moodEnc: ByteArray,
    @ColumnInfo(name = "is_draft") val isDraft: Boolean = false
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "attempts") val attempts: Int,
    @ColumnInfo(name = "photo_path") val photoPath: String? = null,
    @ColumnInfo(name = "note") val note: String = "PIN salah · foto diambil",
    @ColumnInfo(name = "seen") val seen: Boolean = false
)