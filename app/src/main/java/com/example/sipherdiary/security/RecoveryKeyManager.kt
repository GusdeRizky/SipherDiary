package com.example.sipherdiary.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kunci backup yang bisa dipindah antar-HP.
 *
 * Kunci Android Keystore terikat ke satu HP, jadi file backup dienkripsi dengan kunci lain
 * yang diturunkan dari KODE PEMULIHAN (12 karakter) memakai PBKDF2-HMAC-SHA256.
 * PIN 6 digit sengaja TIDAK dipakai: hanya 1 juta kemungkinan, mudah di-brute-force offline.
 *
 * Format file backup:  "SDB1" (4) ‖ salt (16) ‖ IV (12) ‖ ciphertext+tag GCM
 */
object RecoveryKeyManager {
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private val MAGIC = "SDB1".toByteArray(Charsets.US_ASCII)

    // Tanpa huruf/angka yang mirip (0/O, 1/I/L) agar mudah disalin
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    /** Contoh: "K7QM-2XTR-9HPA" (12 karakter acak ≈ 59 bit entropi). */
    fun generateCode(): String = (1..12)
        .map { ALPHABET[random.nextInt(ALPHABET.length)] }
        .chunked(4) { it.joinToString("") }
        .joinToString("-")

    fun normalize(code: String): String = code.uppercase().filter { it in ALPHABET }

    fun newSalt(): ByteArray = ByteArray(16).also { random.nextBytes(it) }

    fun deriveKey(code: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(normalize(code).toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encryptBackup(plain: ByteArray, key: ByteArray, salt: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { random.nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    /** Ambil salt dari header file backup (dibutuhkan untuk menurunkan kunci saat restore). */
    fun saltOf(blob: ByteArray): ByteArray {
        require(blob.size > 32 && blob.copyOfRange(0, 4).contentEquals(MAGIC)) { "Bukan file backup SipherDiary" }
        return blob.copyOfRange(4, 20)
    }

    /** Melempar AEADBadTagException jika kode salah atau file diubah. */
    fun decryptBackup(blob: ByteArray, key: ByteArray): ByteArray {
        saltOf(blob)
        val iv = blob.copyOfRange(20, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        return cipher.doFinal(blob.copyOfRange(32, blob.size))
    }

    fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
