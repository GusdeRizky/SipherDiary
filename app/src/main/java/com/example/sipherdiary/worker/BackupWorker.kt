package com.example.sipherdiary.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sipherdiary.SipherApp
import com.example.sipherdiary.data.repository.BackupRepository
import com.example.sipherdiary.data.repository.BackupResult
import java.util.concurrent.TimeUnit

/**
 * Backup otomatis setiap 24 jam (RPO = 24 jam).
 * Sebelum upload, jaringan dicek dulu: Wi-Fi terbuka/WEP → backup DITUNDA lalu dicoba lagi.
 */
class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SipherApp).container
        if (!container.authRepository.isSignedIn() || !container.securePrefs.hasRecoveryKey()) {
            return Result.success() // belum siap backup, tidak perlu dicoba ulang
        }

        val net = container.networkChecker.check()
        if (!net.allowsBackup) {
            container.backupRepository.markDeferred()
            return Result.retry()
        }

        return when (container.backupRepository.backupNow(net.shortLabel)) {
            is BackupResult.Success -> Result.success()
            is BackupResult.Failed -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val NAME = "daily_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(BackupRepository.RPO_HOURS.toLong(), TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
