package com.example.sipherdiary.data.repository

import android.content.Context
import com.example.sipherdiary.data.prefs.SecurePrefs
import com.example.sipherdiary.data.remote.FirebaseGateway
import com.example.sipherdiary.data.remote.await
import com.example.sipherdiary.security.RecoveryKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.crypto.AEADBadTagException

data class BackupMeta(
    val id: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val sha256: String,
    val network: String
)

sealed class BackupResult {
    data class Success(val meta: BackupMeta, val entryCount: Int) : BackupResult()
    data class Failed(val message: String) : BackupResult()
}

/**
 * Backup & pemulihan (materi Disaster Recovery).
 *
 * - File backup = JSON catatan → AES-256-GCM dengan kunci dari KODE PEMULIHAN.
 * - Firebase hanya menerima blob terenkripsi + metadata (waktu, ukuran, SHA-256, jenis jaringan).
 * - RPO 24 jam (BackupWorker harian), target RTO < 2 menit (unduh 1 file + dekripsi).
 * - Mode demo (tanpa Firebase): "cloud" disimulasikan di folder internal aplikasi.
 */
class BackupRepository(
    private val context: Context,
    private val gateway: FirebaseGateway,
    private val prefs: SecurePrefs,
    private val entries: EntryRepository
) {
    companion object {
        const val MAX_BACKUPS = 5
        const val RPO_HOURS = 24
        private const val MAX_DOWNLOAD = 20L * 1024 * 1024
    }

    val isDemoMode: Boolean get() = !gateway.isConfigured

    private val simDir: File get() = File(context.filesDir, "cloud_sim").apply { mkdirs() }
    private val simIndex: File get() = File(simDir, "index.json")

    // ---------------------------------------------------------------- Kode pemulihan
    /** Buat kode pemulihan baru + kunci backup. Kode hanya ditampilkan sekali ke pengguna. */
    suspend fun createRecoveryCode(): String = withContext(Dispatchers.Default) {
        val code = RecoveryKeyManager.generateCode()
        val salt = RecoveryKeyManager.newSalt()
        prefs.setBackupKey(RecoveryKeyManager.deriveKey(code, salt), salt)
        code
    }

    // ---------------------------------------------------------------- Backup
    suspend fun backupNow(networkLabel: String): BackupResult = withContext(Dispatchers.IO) {
        val key = prefs.getBackupKey()
            ?: return@withContext BackupResult.Failed("Kode pemulihan belum dibuat")
        val salt = prefs.getBackupSalt()
            ?: return@withContext BackupResult.Failed("Salt backup tidak ditemukan")

        runCatching {
            val json = entries.exportJson()
            val count = json.getJSONArray("entries").length()
            val blob = RecoveryKeyManager.encryptBackup(json.toString().toByteArray(Charsets.UTF_8), key, salt)
            val meta = BackupMeta(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                sizeBytes = blob.size.toLong(),
                sha256 = RecoveryKeyManager.sha256Hex(blob),
                network = networkLabel
            )
            if (gateway.isConfigured) uploadFirebase(meta, blob) else uploadSim(meta, blob)
            pruneOld()
            prefs.setLastBackup(meta.createdAt, "SUCCESS")
            BackupResult.Success(meta, count)
        }.getOrElse {
            prefs.setLastBackup(System.currentTimeMillis(), "FAILED")
            BackupResult.Failed(it.localizedMessage ?: "Backup gagal")
        }
    }

    suspend fun markDeferred() = prefs.setLastBackup(System.currentTimeMillis(), "DEFERRED_UNSAFE_NET")

    suspend fun listBackups(): List<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            if (gateway.isConfigured) listFirebase() else listSim()
        }.getOrDefault(emptyList()).sortedByDescending { it.createdAt }
    }

    // ---------------------------------------------------------------- Restore
    /** @return jumlah catatan yang dipulihkan, atau pesan error. */
    suspend fun restoreLatest(recoveryCode: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val latest = listBackups().firstOrNull() ?: error("Belum ada backup di akun ini")
            val blob = if (gateway.isConfigured) downloadFirebase(latest) else File(simDir, "${latest.id}.enc").readBytes()

            check(RecoveryKeyManager.sha256Hex(blob) == latest.sha256) {
                "File backup rusak (SHA-256 tidak cocok)"
            }
            val salt = RecoveryKeyManager.saltOf(blob)
            val key = RecoveryKeyManager.deriveKey(recoveryCode, salt)
            val plain = try {
                RecoveryKeyManager.decryptBackup(blob, key)
            } catch (e: AEADBadTagException) {
                error("Kode pemulihan salah")
            }
            val count = entries.importJson(JSONObject(String(plain, Charsets.UTF_8)))
            // Simpan kunci yang sama agar backup berikutnya bisa dipulihkan dengan kode yang sama
            prefs.setBackupKey(key, salt)
            count
        }
    }

    // ---------------------------------------------------------------- Hapus semua (hak UU PDP)
    suspend fun deleteAllBackups() = withContext(Dispatchers.IO) {
        if (gateway.isConfigured) {
            val uid = gateway.currentUid ?: return@withContext
            listFirebase().forEach { meta ->
                runCatching { gateway.storage!!.reference.child(storagePath(uid, meta.id)).delete().await() }
            }
        } else {
            simDir.deleteRecursively()
        }
    }

    // ---------------------------------------------------------------- Firebase
    private fun storagePath(uid: String, id: String) = "backups/$uid/$id.enc"

    private suspend fun uploadFirebase(meta: BackupMeta, blob: ByteArray) {
        val uid = gateway.currentUid ?: error("Belum masuk ke akun")
        gateway.storage!!.reference.child(storagePath(uid, meta.id)).putBytes(blob).await()
        gateway.firestore!!.collection("users").document(uid).collection("backups").document(meta.id)
            .set(
                mapOf(
                    "createdAt" to meta.createdAt,
                    "storagePath" to storagePath(uid, meta.id),
                    "sizeBytes" to meta.sizeBytes,
                    "sha256" to meta.sha256,
                    "formatVersion" to 1,
                    "network" to meta.network
                )
            ).await()
    }

    private suspend fun listFirebase(): List<BackupMeta> {
        val uid = gateway.currentUid ?: return emptyList()
        return gateway.firestore!!.collection("users").document(uid).collection("backups").get().await()
            .documents.map {
                BackupMeta(
                    id = it.id,
                    createdAt = it.getLong("createdAt") ?: 0L,
                    sizeBytes = it.getLong("sizeBytes") ?: 0L,
                    sha256 = it.getString("sha256") ?: "",
                    network = it.getString("network") ?: "-"
                )
            }
    }

    private suspend fun downloadFirebase(meta: BackupMeta): ByteArray {
        val uid = gateway.currentUid ?: error("Belum masuk ke akun")
        return gateway.storage!!.reference.child(storagePath(uid, meta.id)).getBytes(MAX_DOWNLOAD).await()
    }

    private suspend fun pruneOld() {
        val all = listBackups()
        if (all.size <= MAX_BACKUPS) return
        val old = all.drop(MAX_BACKUPS)
        if (gateway.isConfigured) {
            val uid = gateway.currentUid ?: return
            old.forEach { meta ->
                runCatching {
                    gateway.storage!!.reference.child(storagePath(uid, meta.id)).delete().await()
                    gateway.firestore!!.collection("users").document(uid)
                        .collection("backups").document(meta.id).delete().await()
                }
            }
        } else {
            old.forEach { File(simDir, "${it.id}.enc").delete() }
            writeSimIndex(all.take(MAX_BACKUPS))
        }
    }

    // ---------------------------------------------------------------- Simulasi (mode demo)
    private fun uploadSim(meta: BackupMeta, blob: ByteArray) {
        File(simDir, "${meta.id}.enc").writeBytes(blob)
        writeSimIndex(listSim() + meta)
    }

    private fun listSim(): List<BackupMeta> {
        if (!simIndex.exists()) return emptyList()
        val array = JSONArray(simIndex.readText())
        return (0 until array.length()).map {
            val o = array.getJSONObject(it)
            BackupMeta(o.getString("id"), o.getLong("createdAt"), o.getLong("sizeBytes"), o.getString("sha256"), o.getString("network"))
        }
    }

    private fun writeSimIndex(list: List<BackupMeta>) {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject().put("id", it.id).put("createdAt", it.createdAt).put("sizeBytes", it.sizeBytes)
                    .put("sha256", it.sha256).put("network", it.network)
            )
        }
        simIndex.writeText(array.toString())
    }
}
