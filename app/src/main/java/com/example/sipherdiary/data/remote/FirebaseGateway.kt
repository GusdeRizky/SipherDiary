package com.example.sipherdiary.data.remote

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Pintu masuk ke Firebase.
 *
 * Firebase hanya aktif jika `app/google-services.json` dipasang (lihat README).
 * Tanpa file itu, [isConfigured] = false dan seluruh aplikasi berjalan di "mode demo":
 * login & backup disimulasikan secara lokal sehingga aplikasi tetap bisa didemokan.
 */
class FirebaseGateway(context: Context) {

    val isConfigured: Boolean = runCatching {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }.getOrDefault(false)

    val auth: FirebaseAuth? get() = if (isConfigured) FirebaseAuth.getInstance() else null
    val firestore: FirebaseFirestore? get() = if (isConfigured) FirebaseFirestore.getInstance() else null
    val storage: FirebaseStorage? get() = if (isConfigured) FirebaseStorage.getInstance() else null

    val currentUid: String? get() = auth?.currentUser?.uid
}

/** Mengubah Task Google Play Services menjadi fungsi suspend (tanpa library tambahan). */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> cont.resumeWithException(error)
            task.isCanceled -> cont.cancel()
            else -> cont.resume(task.result)
        }
    }
}
