package com.example.sipherdiary.ui.backup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor

/**
 * Layar tambahan setelah 04 Buat PIN: "Simpan kode pemulihan".
 * - Jika akun sudah punya backup (HP baru) → tawarkan pulihkan dengan kode lama.
 * - Jika tidak → tampilkan kode baru SEKALI, pengguna wajib menyimpannya.
 */
@Composable
fun RecoveryCodeScreen(
    hasCloudBackup: Boolean,
    generatedCode: String?,
    isWorking: Boolean,
    restoreError: String?,
    onGenerate: () -> Unit,
    onRestore: (String) -> Unit,
    onDone: () -> Unit
) {
    var restoreMode by remember(hasCloudBackup) { mutableStateOf(hasCloudBackup) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(restoreMode, generatedCode) {
        if (!restoreMode && generatedCode == null) onGenerate()
    }

    Scaffold(containerColor = SipherColor.Bg) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            IconBadge(if (restoreMode) Icons.Outlined.Restore else Icons.Outlined.Key)
            Spacer(Modifier.height(16.dp))

            if (restoreMode) {
                var code by remember { mutableStateOf("") }
                Text("Pulihkan diary-mu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Akun ini punya backup terenkripsi. Masukkan kode pemulihan untuk memindahkan catatan ke HP ini.",
                    fontSize = 14.sp, color = SipherColor.Ink600, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(14) },
                    placeholder = { Text("XXXX-XXXX-XXXX", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                    shape = RoundedCornerShape(14.dp),
                    isError = restoreError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (restoreError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(restoreError, fontSize = 13.sp, color = SipherColor.Danger)
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    "Pulihkan catatan",
                    onClick = { onRestore(code) },
                    enabled = code.count { it.isLetterOrDigit() } >= 12,
                    loading = isWorking
                )
                Spacer(Modifier.height(10.dp))
                OutlineButton(
                    "Lewati, mulai diary baru",
                    onClick = { restoreMode = false },
                    enabled = !isWorking
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Jika dilewati, backup lama tidak bisa dibuka dengan kode baru.",
                    fontSize = 12.sp, color = SipherColor.Ink600, textAlign = TextAlign.Center
                )
            } else {
                Text("Simpan kode pemulihan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Kode ini satu-satunya cara membuka backup di HP baru. Tulis di kertas atau simpan di password manager. " +
                            "Kode hanya ditampilkan sekali.",
                    fontSize = 14.sp, color = SipherColor.Ink600, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SipherColor.Tint,
                    border = BorderStroke(1.dp, SipherColor.Blue100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
                        if (generatedCode == null) {
                            CircularProgressIndicator(color = SipherColor.Blue500, modifier = Modifier.size(28.dp))
                        } else {
                            Text(
                                generatedCode,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = SipherColor.Blue700
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "PBKDF2-HMAC-SHA256 · 310.000 iterasi → kunci AES-256",
                    fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = SipherColor.Ink600
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { confirmed = !confirmed },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = { confirmed = it },
                        colors = CheckboxDefaults.colors(checkedColor = SipherColor.Blue500)
                    )
                    Text("Saya sudah menyimpan kode ini di tempat aman", fontSize = 14.sp, color = SipherColor.Ink900)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    "Lanjut ke diary",
                    onClick = onDone,
                    enabled = confirmed && generatedCode != null,
                    loading = isWorking
                )
            }
        }
    }
}
