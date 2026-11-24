package com.example.sipherdiary.data.repository

import android.app.Activity
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.sipherdiary.data.prefs.SecurePrefs
import com.example.sipherdiary.data.remote.FirebaseGateway
import com.example.sipherdiary.data.remote.await
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class OtpSendResult {
    /** Kode SMS sudah dikirim, pengguna perlu memasukkan 6 digit. */
    data object CodeSent : OtpSendResult()
    /** Android memverifikasi nomor otomatis (tanpa input kode). */
    data object AutoVerified : OtpSendResult()
    data class Failed(val message: String) : OtpSendResult()
}

data class AccountInfo(
    val provider: String,      // "phone" | "google"
    val label: String,         // no. HP atau email
    val displayName: String,
    val isDemo: Boolean
)

data class DeviceInfo(
    val id: String,
    val model: String,
    val androidVersion: String,
    val lastActiveAt: Long,
    val isThisDevice: Boolean
)

/**
 * Otentikasi akun: OTP SMS (Firebase Phone Auth) dan Google SSO (OpenID Connect via
 * Credential Manager). Materi kuliah: Manajemen Identitas & Otentikasi.
 */
class AuthRepository(
    private val gateway: FirebaseGateway,
    private val prefs: SecurePrefs
) {
    companion object {
        /** OTP yang diterima di mode demo (tanpa Firebase). */
        const val DEMO_OTP = "123456"
    }

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var pendingPhone: String = ""

    val isDemoMode: Boolean get() = !gateway.isConfigured

    suspend fun isSignedIn(): Boolean =
        if (gateway.isConfigured) gateway.auth?.currentUser != null else prefs.isDemoSignedIn()

    // ---------------------------------------------------------------- OTP SMS
    suspend fun sendOtp(activity: Activity, phone: String, resend: Boolean = false): OtpSendResult {
        pendingPhone = phone
        val auth = gateway.auth ?: return OtpSendResult.CodeSent   // mode demo: kode = 123456

        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (cont.isActive) cont.resume(
                            if (task.isSuccessful) OtpSendResult.AutoVerified
                            else OtpSendResult.Failed(task.exception?.localizedMessage ?: "Verifikasi gagal")
                        )
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val msg = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Nomor telepon tidak valid"
                        is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi nanti."
                        else -> e.localizedMessage ?: "Gagal mengirim OTP"
                    }
                    if (cont.isActive) cont.resume(OtpSendResult.Failed(msg))
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    if (cont.isActive) cont.resume(OtpSendResult.CodeSent)
                }
            }

            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            val token = resendToken
            if (resend && token != null) builder.setForceResendingToken(token)
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    /** @return null jika sukses, atau pesan error. */
    suspend fun verifyOtp(code: String): String? {
        if (code.length != 6) return "Kode OTP harus 6 digit"
        val auth = gateway.auth
        if (auth == null) {
            if (code != DEMO_OTP) return "Kode salah. Mode demo: gunakan $DEMO_OTP"
            prefs.setDemoSignedIn(true)
            prefs.saveAccount("phone", pendingPhone, pendingPhone)
            return null
        }
        val id = verificationId ?: return "Minta kode OTP terlebih dahulu"
        return runCatching {
            auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)).await()
            onSignedIn("phone", pendingPhone, pendingPhone)
        }.exceptionOrNull()?.let {
            if (it is FirebaseAuthInvalidCredentialsException) "Kode OTP salah atau kedaluwarsa"
            else it.localizedMessage ?: "Verifikasi gagal"
        }
    }

    /** Dipanggil setelah verifikasi otomatis (AutoVerified). */
    suspend fun completeAutoVerified() = onSignedIn("phone", pendingPhone, pendingPhone)

    // ---------------------------------------------------------------- Google SSO
    /** @return null jika sukses, atau pesan error. */
    suspend fun signInWithGoogle(activity: Activity): String? {
        val auth = gateway.auth
        if (auth == null) {
            prefs.setDemoSignedIn(true)
            prefs.saveAccount("google", "demo@gmail.com", "Pengguna Demo")
            return null
        }
        val webClientIdRes = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (webClientIdRes == 0) return "Google Sign-In belum diaktifkan di Firebase (default_web_client_id kosong)"
        val webClientId = activity.getString(webClientIdRes)
        if (webClientId.isBlank()) return "Aktifkan provider Google di Firebase, lalu unduh ulang google-services.json"

        return runCatching {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(activity).getCredential(activity, request)
            val credential = result.credential
            require(
                credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "Kredensial Google tidak dikenali" }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
            onSignedIn("google", google.id, google.displayName ?: google.id)
        }.exceptionOrNull()?.let { it.localizedMessage ?: "Login Google gagal" }
    }

    // ---------------------------------------------------------------- Akun & perangkat
    private suspend fun onSignedIn(provider: String, label: String, displayName: String) {
        prefs.saveAccount(provider, label, displayName)
        val uid = gateway.currentUid ?: return
        // Catat persetujuan privasi (UU PDP) & profil minimal. Tidak ada isi catatan di sini.
        runCatching {
            gateway.firestore?.collection("users")?.document(uid)?.set(
                mapOf(
                    "authProvider" to provider,
                    "consentVersion" to (prefs.getConsentVersion() ?: "1.0"),
                    "consentAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )?.await()
        }
        registerDevice()
    }

    suspend fun account(): AccountInfo = AccountInfo(
        provider = prefs.getAuthProvider() ?: "phone",
        label = prefs.getAccountLabel() ?: "-",
        displayName = prefs.getDisplayName() ?: "Pengguna",
        isDemo = isDemoMode
    )

    suspend fun registerDevice() {
        val uid = gateway.currentUid ?: return
        val deviceId = prefs.getDeviceId()
        runCatching {
            gateway.firestore?.collection("users")?.document(uid)
                ?.collection("devices")?.document(deviceId)
                ?.set(
                    mapOf(
                        "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                        "androidVersion" to Build.VERSION.RELEASE,
                        "lastActiveAt" to System.currentTimeMillis()
                    )
                )?.await()
        }
    }

    suspend fun devices(): List<DeviceInfo> {
        val thisId = prefs.getDeviceId()
        val local = DeviceInfo(
            thisId, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.RELEASE,
            System.currentTimeMillis(), true
        )
        val uid = gateway.currentUid ?: return listOf(local)
        return runCatching {
            gateway.firestore!!.collection("users").document(uid).collection("devices").get().await()
                .documents.map {
                    DeviceInfo(
                        id = it.id,
                        model = it.getString("model") ?: "Perangkat",
                        androidVersion = it.getString("androidVersion") ?: "-",
                        lastActiveAt = it.getLong("lastActiveAt") ?: 0L,
                        isThisDevice = it.id == thisId
                    )
                }.sortedByDescending { it.isThisDevice }
        }.getOrDefault(listOf(local))
    }

    suspend fun removeDevice(deviceId: String) {
        val uid = gateway.currentUid ?: return
        runCatching {
            gateway.firestore?.collection("users")?.document(uid)
                ?.collection("devices")?.document(deviceId)?.delete()?.await()
        }
    }

    suspend fun signOut() {
        removeDevice(prefs.getDeviceId())
        gateway.auth?.signOut()
        prefs.setDemoSignedIn(false)
    }

    /** Hapus data akun di Firestore & akun Firebase Auth. @return null jika sukses. */
    suspend fun deleteCloudAccount(): String? {
        val auth = gateway.auth ?: return null
        val user = auth.currentUser ?: return null
        val db = gateway.firestore!!
        return runCatching {
            val userDoc = db.collection("users").document(user.uid)
            for (sub in listOf("devices", "backups")) {
                userDoc.collection(sub).get().await().documents.forEach { it.reference.delete().await() }
            }
            userDoc.delete().await()
            user.delete().await()
        }.exceptionOrNull()?.let {
            "Gagal menghapus akun cloud: ${it.localizedMessage}. Coba keluar lalu masuk lagi, kemudian ulangi."
        }
    }
}
