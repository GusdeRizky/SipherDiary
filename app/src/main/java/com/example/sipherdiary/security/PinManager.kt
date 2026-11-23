package com.example.sipherdiary.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.sipherdiary.data.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PinResult {
    data object Ok : PinResult()
    data object Decoy : PinResult()
    data class Wrong(val remainingAttempts: Int) : PinResult()
    data object Intruder : PinResult()
    data class LockedOut(val lockUntilMillis: Long) : PinResult()
}

class PinManager(private val prefs: SecurePrefs) {

    fun isWeakPin(pin: String): Boolean {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return true
        // Tolak angka kembar semua (misal 000000, 111111)
        if (pin.toSet().size == 1) return true
        // Tolak urutan naik atau turun (misal 123456, 654321)
        val ascending = "0123456789"
        val descending = "9876543210"
        return ascending.contains(pin) || descending.contains(pin)
    }

    suspend fun setPin(pin: String) = withContext(Dispatchers.Default) {
        val hash = BCrypt.withDefaults().hashToString(12, pin.toCharArray())
        prefs.setPinHash(hash)
        prefs.setFailedAttempts(0)
        prefs.setLockUntil(0L)
    }

    suspend fun setDecoyPin(pin: String?) = withContext(Dispatchers.Default) {
        if (pin.isNullOrBlank()) {
            prefs.setDecoyHash(null)
        } else {
            val hash = BCrypt.withDefaults().hashToString(12, pin.toCharArray())
            prefs.setDecoyHash(hash)
        }
    }

    suspend fun hasPin(): Boolean = !prefs.getPinHash().isNullOrBlank()

    suspend fun verify(pin: String): PinResult = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.getLockUntil()
        if (now < lockUntil) {
            return@withContext PinResult.LockedOut(lockUntil)
        }

        val storedHash = prefs.getPinHash() ?: return@withContext PinResult.Wrong(3)
        if (BCrypt.verifyer().verify(pin.toCharArray(), storedHash).verified) {
            prefs.setFailedAttempts(0)
            prefs.setLockUntil(0L)
            return@withContext PinResult.Ok
        }

        val decoyHash = prefs.getDecoyHash()
        if (!decoyHash.isNullOrBlank() &&
            BCrypt.verifyer().verify(pin.toCharArray(), decoyHash).verified
        ) {
            prefs.setFailedAttempts(0)
            prefs.setLockUntil(0L)
            return@withContext PinResult.Decoy
        }

        val failed = prefs.getFailedAttempts() + 1
        prefs.setFailedAttempts(failed)

        return@withContext if (failed >= 3) {
            prefs.setLockUntil(now + 5 * 60_000L) // Kunci 5 menit
            PinResult.Intruder
        } else {
            PinResult.Wrong(3 - failed)
        }
    }
}