package com.example.sipherdiary.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Memotret wajah orang yang salah memasukkan PIN 3x dengan kamera depan (tanpa preview).
 * Foto TIDAK disimpan ke galeri; hasilnya berupa JPEG di memori yang lalu dienkripsi.
 */
object IntruderCamera {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    suspend fun capture(context: Context, owner: LifecycleOwner): ByteArray? {
        if (!hasPermission(context)) return null
        return withTimeoutOrNull(8_000) {
            withContext(Dispatchers.Main) {
                runCatching { captureInternal(context, owner) }.getOrNull()
            }
        }
    }

    private suspend fun captureInternal(context: Context, owner: LifecycleOwner): ByteArray? {
        val provider = suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }
        if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) return null

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)

        return try {
            suspendCancellableCoroutine { cont ->
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bytes = runCatching { image.toJpeg() }.getOrNull()
                            image.close()
                            if (cont.isActive) cont.resume(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } finally {
            provider.unbind(imageCapture)
        }
    }

    private fun ImageProxy.toJpeg(): ByteArray {
        val source = toBitmap()
        val matrix = Matrix().apply {
            postRotate(imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f) // kamera depan → cermin agar terlihat natural
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val maxSide = 720
        val scale = minOf(1f, maxSide.toFloat() / maxOf(rotated.width, rotated.height))
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true)
        } else rotated
        return ByteArrayOutputStream().use {
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, it)
            it.toByteArray()
        }
    }
}
