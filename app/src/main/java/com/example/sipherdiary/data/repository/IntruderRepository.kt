package com.example.sipherdiary.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.sipherdiary.data.local.dao.IntruderDao
import com.example.sipherdiary.data.local.entity.IntruderLogEntity
import com.example.sipherdiary.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Log percobaan masuk & foto penyusup (foto dienkripsi AES-256-GCM di penyimpanan internal). */
class IntruderRepository(
    private val context: Context,
    private val dao: IntruderDao
) {
    private val dir: File get() = File(context.filesDir, "intruder").apply { mkdirs() }

    fun observeLogs(): Flow<List<IntruderLogEntity>> = dao.observeAll()

    suspend fun record(attempts: Int, jpeg: ByteArray?) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val path = jpeg?.let {
            val file = File(dir, "$id.enc")
            file.writeBytes(CryptoManager.encrypt(it))
            file.absolutePath
        }
        dao.insert(
            IntruderLogEntity(
                id = id,
                occurredAt = System.currentTimeMillis(),
                attempts = attempts,
                photoPath = path,
                note = if (path != null) "PIN salah $attempts× · foto diambil" else "PIN salah $attempts× · tanpa foto"
            )
        )
    }

    suspend fun loadPhoto(path: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (path == null) return@withContext null
        runCatching {
            val bytes = CryptoManager.decrypt(File(path).readBytes())
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    suspend fun markAllSeen() = withContext(Dispatchers.IO) { dao.markAllSeen() }

    suspend fun wipe() = withContext(Dispatchers.IO) {
        dao.wipe()
        dir.listFiles()?.forEach { it.delete() }
    }
}
