package com.example.sipherdiary

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sipherdiary.data.local.AppDatabase
import com.example.sipherdiary.data.prefs.SecurePrefs
import com.example.sipherdiary.data.remote.FirebaseGateway
import com.example.sipherdiary.data.repository.AuthRepository
import com.example.sipherdiary.data.repository.BackupRepository
import com.example.sipherdiary.data.repository.EntryRepository
import com.example.sipherdiary.data.repository.IntruderRepository
import com.example.sipherdiary.security.CryptoManager
import com.example.sipherdiary.security.NetworkSecurityChecker
import com.example.sipherdiary.security.PinManager
import com.example.sipherdiary.security.SessionManager
import com.example.sipherdiary.worker.BackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Semua dependensi aplikasi (DI manual, tanpa Hilt). */
class AppContainer(context: Context) {
    val securePrefs = SecurePrefs(context)
    val pinManager = PinManager(securePrefs)
    val database = AppDatabase.getInstance(context)
    val firebase = FirebaseGateway(context)
    val entryRepository = EntryRepository(database.entryDao())
    val intruderRepository = IntruderRepository(context, database.intruderDao())
    val authRepository = AuthRepository(firebase, securePrefs)
    val backupRepository = BackupRepository(context, firebase, securePrefs, entryRepository)
    val networkChecker = NetworkSecurityChecker(context)
}

class SipherApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var backgroundAt: Long = 0L
    private var autoLockMinutes: Int = 1

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BackupWorker.schedule(this)

        appScope.launch {
            container.securePrefs.autoLockFlow.collect { autoLockMinutes = it }
        }

        // Auto-lock: jika aplikasi di background lebih lama dari batas waktu → kunci lagi
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundAt = SystemClock.elapsedRealtime()
            }

            override fun onStart(owner: LifecycleOwner) {
                if (backgroundAt == 0L) return
                val idleMs = SystemClock.elapsedRealtime() - backgroundAt
                if (idleMs >= autoLockMinutes * 60_000L) SessionManager.lock()
            }
        })
    }

    /** Dipakai setelah hapus akun: hapus semua data lokal. */
    suspend fun wipeLocal() {
        container.entryRepository.wipe()
        container.intruderRepository.wipe()
        container.securePrefs.clearAll()
        filesDir.listFiles()
            ?.filter { it.name != "datastore" }
            ?.forEach { it.deleteRecursively() }
        CryptoManager.deleteKey() // kunci lama dibuang, data lama tidak bisa didekripsi lagi
        BackupWorker.cancel(this)
        SessionManager.lock()
    }
}
