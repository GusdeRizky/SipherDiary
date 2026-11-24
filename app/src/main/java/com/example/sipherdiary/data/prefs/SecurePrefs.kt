package com.example.sipherdiary.data.prefs

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.sipherdiary.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "sipher_secure_prefs")

class SecurePrefs(private val context: Context) {

    companion object {
        private val KEY_CONSENT_VERSION = stringPreferencesKey("consent_version")
        private val KEY_CONSENT_AT = longPreferencesKey("consent_at")
        private val KEY_PIN_HASH_ENC = stringPreferencesKey("pin_hash_enc")
        private val KEY_DECOY_HASH_ENC = stringPreferencesKey("decoy_hash_enc")
        private val KEY_FAILED_ATTEMPTS_ENC = stringPreferencesKey("failed_attempts_enc")
        private val KEY_LOCK_UNTIL_ENC = stringPreferencesKey("lock_until_enc")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_ANTI_SCREENSHOT = booleanPreferencesKey("anti_screenshot")
        private val KEY_INTRUDER_PHOTO = booleanPreferencesKey("intruder_photo")
        private val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")

        // Akun
        private val KEY_AUTH_PROVIDER = stringPreferencesKey("auth_provider")      // phone | google
        private val KEY_ACCOUNT_LABEL = stringPreferencesKey("account_label")      // no. HP / email
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_DEMO_SIGNED_IN = booleanPreferencesKey("demo_signed_in")  // mode demo tanpa Firebase
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")

        // Backup & pemulihan
        private val KEY_BACKUP_KEY_ENC = stringPreferencesKey("backup_key_enc")    // dibungkus kunci Keystore
        private val KEY_BACKUP_SALT = stringPreferencesKey("backup_salt")
        private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        private val KEY_LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
    }

    private fun encryptText(plain: String): String {
        val encrypted = CryptoManager.encrypt(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptText(cipherBase64: String?): String? {
        if (cipherBase64.isNullOrBlank()) return null
        return runCatching {
            val raw = Base64.decode(cipherBase64, Base64.NO_WRAP)
            String(CryptoManager.decrypt(raw), Charsets.UTF_8)
        }.getOrNull()
    }

    // --- Persetujuan Privasi (UU PDP) ---
    suspend fun saveConsent(version: String = "1.0") {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONSENT_VERSION] = version
            prefs[KEY_CONSENT_AT] = System.currentTimeMillis()
        }
    }

    suspend fun getConsentVersion(): String? =
        context.dataStore.data.map { it[KEY_CONSENT_VERSION] }.first()

    // --- PIN & PIN Samaran (Terenkripsi) ---
    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[KEY_PIN_HASH_ENC] = encryptText(hash) }
    }

    suspend fun getPinHash(): String? =
        decryptText(context.dataStore.data.map { it[KEY_PIN_HASH_ENC] }.first())

    suspend fun setDecoyHash(hash: String?) {
        context.dataStore.edit { prefs ->
            if (hash == null) prefs.remove(KEY_DECOY_HASH_ENC)
            else prefs[KEY_DECOY_HASH_ENC] = encryptText(hash)
        }
    }

    suspend fun getDecoyHash(): String? =
        decryptText(context.dataStore.data.map { it[KEY_DECOY_HASH_ENC] }.first())

    // --- Percobaan Gagal & Waktu Lockout (Terenkripsi) ---
    suspend fun setFailedAttempts(count: Int) {
        context.dataStore.edit { it[KEY_FAILED_ATTEMPTS_ENC] = encryptText(count.toString()) }
    }

    suspend fun getFailedAttempts(): Int =
        decryptText(context.dataStore.data.map { it[KEY_FAILED_ATTEMPTS_ENC] }.first())?.toIntOrNull() ?: 0

    suspend fun setLockUntil(timestampMillis: Long) {
        context.dataStore.edit { it[KEY_LOCK_UNTIL_ENC] = encryptText(timestampMillis.toString()) }
    }

    suspend fun getLockUntil(): Long =
        decryptText(context.dataStore.data.map { it[KEY_LOCK_UNTIL_ENC] }.first())?.toLongOrNull() ?: 0L

    // --- Pengaturan Keamanan ---
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun isBiometricEnabled(): Boolean =
        context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }.first()

    suspend fun setAntiScreenshot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ANTI_SCREENSHOT] = enabled }
    }

    suspend fun isAntiScreenshot(): Boolean =
        context.dataStore.data.map { it[KEY_ANTI_SCREENSHOT] ?: true }.first()

    suspend fun setIntruderPhoto(enabled: Boolean) {
        context.dataStore.edit { it[KEY_INTRUDER_PHOTO] = enabled }
    }

    suspend fun isIntruderPhoto(): Boolean =
        context.dataStore.data.map { it[KEY_INTRUDER_PHOTO] ?: true }.first()

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun getAutoLockMinutes(): Int =
        context.dataStore.data.map { it[KEY_AUTO_LOCK_MINUTES] ?: 1 }.first()

    // --- Flow untuk UI (reaktif) ---
    val antiScreenshotFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ANTI_SCREENSHOT] ?: true }
    val biometricFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val intruderPhotoFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_INTRUDER_PHOTO] ?: true }
    val autoLockFlow: Flow<Int> = context.dataStore.data.map { it[KEY_AUTO_LOCK_MINUTES] ?: 1 }
    val lastBackupAtFlow: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_BACKUP_AT] ?: 0L }
    val lastBackupStatusFlow: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_BACKUP_STATUS] }

    // --- Akun ---
    suspend fun saveAccount(provider: String, label: String, displayName: String) {
        context.dataStore.edit {
            it[KEY_AUTH_PROVIDER] = provider
            it[KEY_ACCOUNT_LABEL] = label
            it[KEY_DISPLAY_NAME] = displayName
        }
    }

    suspend fun getAuthProvider(): String? = context.dataStore.data.map { it[KEY_AUTH_PROVIDER] }.first()
    suspend fun getAccountLabel(): String? = context.dataStore.data.map { it[KEY_ACCOUNT_LABEL] }.first()
    suspend fun getDisplayName(): String? = context.dataStore.data.map { it[KEY_DISPLAY_NAME] }.first()

    suspend fun setDemoSignedIn(signedIn: Boolean) {
        context.dataStore.edit { it[KEY_DEMO_SIGNED_IN] = signedIn }
    }

    suspend fun isDemoSignedIn(): Boolean =
        context.dataStore.data.map { it[KEY_DEMO_SIGNED_IN] ?: false }.first()

    /** ID acak per instalasi, dipakai untuk daftar "Perangkat aktif". Bukan IMEI/ANDROID_ID. */
    suspend fun getDeviceId(): String {
        val existing = context.dataStore.data.map { it[KEY_DEVICE_ID] }.first()
        if (existing != null) return existing
        val fresh = UUID.randomUUID().toString()
        context.dataStore.edit { it[KEY_DEVICE_ID] = fresh }
        return fresh
    }

    // --- Kunci backup (hasil PBKDF2 dari kode pemulihan, dibungkus Keystore) ---
    suspend fun setBackupKey(rawKey: ByteArray, salt: ByteArray) {
        context.dataStore.edit {
            it[KEY_BACKUP_KEY_ENC] = Base64.encodeToString(CryptoManager.encrypt(rawKey), Base64.NO_WRAP)
            it[KEY_BACKUP_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
        }
    }

    suspend fun getBackupKey(): ByteArray? {
        val enc = context.dataStore.data.map { it[KEY_BACKUP_KEY_ENC] }.first() ?: return null
        return runCatching { CryptoManager.decrypt(Base64.decode(enc, Base64.NO_WRAP)) }.getOrNull()
    }

    suspend fun getBackupSalt(): ByteArray? =
        context.dataStore.data.map { it[KEY_BACKUP_SALT] }.first()?.let { Base64.decode(it, Base64.NO_WRAP) }

    suspend fun hasRecoveryKey(): Boolean = getBackupKey() != null

    suspend fun setLastBackup(timestamp: Long, status: String) {
        context.dataStore.edit {
            if (status == "SUCCESS") it[KEY_LAST_BACKUP_AT] = timestamp
            it[KEY_LAST_BACKUP_STATUS] = status
        }
    }

    suspend fun getLastBackupAt(): Long = lastBackupAtFlow.first()

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}