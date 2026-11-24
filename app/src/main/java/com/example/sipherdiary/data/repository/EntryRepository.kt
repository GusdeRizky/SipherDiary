package com.example.sipherdiary.data.repository

import com.example.sipherdiary.data.local.dao.EntryDao
import com.example.sipherdiary.data.local.entity.EntryEntity
import com.example.sipherdiary.security.CryptoManager
import com.example.sipherdiary.security.SessionManager
import com.example.sipherdiary.security.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class MoodType(val code: Int, val label: String, val colorHex: Long) {
    SENANG(1, "Senang", 0xFF1E9E6A),
    TENANG(2, "Tenang", 0xFF2A74D6),
    CEMAS(3, "Cemas", 0xFFC77A0E),
    SEDIH(4, "Sedih", 0xFF64748B),
    LELAH(5, "Lelah", 0xFF6366F1);

    companion object {
        fun fromCode(code: Int): MoodType = entries.firstOrNull { it.code == code } ?: TENANG
    }
}

data class DiaryEntry(
    val id: String,
    val date: LocalDate,
    val createdAt: Long,
    val title: String,
    val body: String,
    val mood: MoodType
)

class EntryRepository(private val dao: EntryDao) {

    fun observeEntries(): Flow<List<DiaryEntry>> {
        return combine(dao.observeAll(), SessionManager.state) { entities, session ->
            if (session == SessionState.DecoyUnlocked) {
                emptyList() // PIN samaran menampilkan diary kosong
            } else {
                entities.mapNotNull { decryptEntity(it) }
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getEntryById(id: String): DiaryEntry? = withContext(Dispatchers.Default) {
        if (SessionManager.state.value == SessionState.DecoyUnlocked) return@withContext null
        dao.getById(id)?.let { decryptEntity(it) }
    }

    suspend fun saveEntry(
        id: String?,
        title: String,
        body: String,
        mood: MoodType,
        date: LocalDate = LocalDate.now()
    ) = withContext(Dispatchers.Default) {
        if (SessionManager.state.value == SessionState.DecoyUnlocked) return@withContext

        val now = System.currentTimeMillis()
        val existing = id?.let { dao.getById(it) }

        val entity = EntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            dateKey = existing?.dateKey ?: date.toString(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            titleEnc = CryptoManager.encrypt(title.trim().toByteArray(Charsets.UTF_8)),
            bodyEnc = CryptoManager.encrypt(body.trim().toByteArray(Charsets.UTF_8)),
            moodEnc = CryptoManager.encrypt(byteArrayOf(mood.code.toByte())),
            isDraft = false
        )
        dao.upsert(entity)
    }

    suspend fun deleteEntry(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun count(): Int = dao.count()

    /**
     * Semua catatan dalam bentuk JSON polos (hanya di memori).
     * Dipakai untuk backup (lalu dienkripsi ulang dengan kunci backup) dan fitur "Unduh data saya".
     */
    suspend fun exportJson(): JSONObject = withContext(Dispatchers.Default) {
        val array = JSONArray()
        dao.getAll().forEach { entity ->
            val entry = decryptEntity(entity) ?: return@forEach
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("date", entity.dateKey)
                    .put("createdAt", entity.createdAt)
                    .put("updatedAt", entity.updatedAt)
                    .put("title", entry.title)
                    .put("body", entry.body)
                    .put("mood", entry.mood.code)
            )
        }
        JSONObject()
            .put("app", "SipherDiary")
            .put("formatVersion", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("entries", array)
    }

    /** Pulihkan dari JSON backup: tiap entri dienkripsi ulang dengan kunci Keystore HP ini. */
    suspend fun importJson(json: JSONObject): Int = withContext(Dispatchers.Default) {
        val array = json.optJSONArray("entries") ?: return@withContext 0
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            dao.upsert(
                EntryEntity(
                    id = o.getString("id"),
                    dateKey = o.getString("date"),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.optLong("updatedAt", o.getLong("createdAt")),
                    titleEnc = CryptoManager.encrypt(o.optString("title").toByteArray(Charsets.UTF_8)),
                    bodyEnc = CryptoManager.encrypt(o.optString("body").toByteArray(Charsets.UTF_8)),
                    moodEnc = CryptoManager.encrypt(byteArrayOf(o.optInt("mood", 2).toByte())),
                    isDraft = false
                )
            )
        }
        array.length()
    }

    suspend fun wipe() = withContext(Dispatchers.IO) { dao.wipe() }

    private fun decryptEntity(entity: EntryEntity): DiaryEntry? {
        return runCatching {
            val title = String(CryptoManager.decrypt(entity.titleEnc), Charsets.UTF_8)
            val body = String(CryptoManager.decrypt(entity.bodyEnc), Charsets.UTF_8)
            val moodCode = CryptoManager.decrypt(entity.moodEnc).firstOrNull()?.toInt() ?: 2
            DiaryEntry(
                id = entity.id,
                date = LocalDate.parse(entity.dateKey),
                createdAt = entity.createdAt,
                title = title,
                body = body,
                mood = MoodType.fromCode(moodCode)
            )
        }.getOrNull()
    }
}