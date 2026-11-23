package com.example.sipherdiary.data.prefs

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.sipherdiary.security.CryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}