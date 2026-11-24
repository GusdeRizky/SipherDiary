package com.example.sipherdiary.security

data class SecurityState(
    val biometricEnabled: Boolean,
    val autoLockMinutes: Int,
    val antiScreenshot: Boolean,
    val intruderPhoto: Boolean,
    val cameraPermission: Boolean,
    val hasRecoveryKey: Boolean,
    val lastBackupAt: Long
)

data class ScoreItem(val label: String, val points: Int, val earned: Boolean)

/** Skor keamanan di Pusat Keamanan (layar 09). Basis 40 karena enkripsi selalu aktif. */
object SecurityScore {
    fun items(s: SecurityState, now: Long = System.currentTimeMillis()): List<ScoreItem> {
        val backupFresh = s.lastBackupAt > 0 && now - s.lastBackupAt <= 24 * 3_600_000L
        return listOf(
            ScoreItem("Enkripsi AES-256-GCM", 40, true),
            ScoreItem("Sidik jari aktif", 15, s.biometricEnabled),
            ScoreItem("Kunci otomatis ≤ 1 menit", 10, s.autoLockMinutes <= 1),
            ScoreItem("Blokir screenshot", 10, s.antiScreenshot),
            ScoreItem("Foto penyusup", 10, s.intruderPhoto && s.cameraPermission),
            ScoreItem("Backup ≤ 24 jam (RPO)", 15, s.hasRecoveryKey && backupFresh)
        )
    }

    fun score(s: SecurityState): Int = items(s).filter { it.earned }.sumOf { it.points }.coerceAtMost(100)

    fun grade(score: Int): String = when {
        score >= 85 -> "Sangat aman"
        score >= 65 -> "Aman"
        score >= 50 -> "Cukup"
        else -> "Perlu ditingkatkan"
    }
}
