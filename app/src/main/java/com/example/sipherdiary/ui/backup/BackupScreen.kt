package com.example.sipherdiary.ui.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.repository.BackupMeta
import com.example.sipherdiary.data.repository.BackupRepository
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Layar 11 — Backup & Pemulihan (materi Disaster Recovery). */
@Composable
fun BackupScreen(
    backups: List<BackupMeta>,
    isLoading: Boolean,
    lastBackupAt: Long,
    lastStatus: String?,
    entryCount: Int,
    isDemoMode: Boolean,
    isRestoring: Boolean,
    restoreError: String?,
    onBackupNow: () -> Unit,
    onRestore: (code: String) -> Unit,
    onBack: () -> Unit
) {
    var showRestoreDialog by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy · HH.mm", Locale.forLanguageTag("id-ID")) }
    val hoursAgo = if (lastBackupAt > 0) (System.currentTimeMillis() - lastBackupAt) / 3_600_000 else -1
    val withinRpo = hoursAgo in 0..BackupRepository.RPO_HOURS.toLong()

    Scaffold(containerColor = SipherColor.Bg) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader(title = "Backup & Pemulihan", overline = "Disaster recovery", onBack = onBack) }
            if (isDemoMode) item { DemoModeBanner() }

            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SipherColor.Subtle, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Outlined.CloudDone)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Backup terakhir", fontSize = 12.5.sp, color = SipherColor.Ink600)
                                Text(
                                    if (lastBackupAt > 0) fmt.format(Date(lastBackupAt)) else "Belum pernah",
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900
                                )
                            }
                            StatusChip(
                                if (withinRpo) "Sesuai RPO" else "Lewat RPO",
                                if (withinRpo) SipherColor.Success else SipherColor.Warning
                            )
                        }
                        if (lastStatus == "DEFERRED_UNSAFE_NET") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Backup otomatis terakhir ditunda karena Wi-Fi tidak aman.",
                                fontSize = 12.5.sp, color = SipherColor.Warning
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricBox("RPO", "≤ 24 jam", Modifier.weight(1f))
                            MetricBox("RTO", "< 2 menit", Modifier.weight(1f))
                            MetricBox("Catatan", "$entryCount", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                PrimaryButton("Backup sekarang", onBackupNow)
            }
            item {
                OutlineButton(
                    "Pulihkan ke HP ini",
                    onClick = { showRestoreDialog = true },
                    enabled = backups.isNotEmpty() && !isRestoring
                )
            }

            item {
                SipherCard(color = SipherColor.Subtle) {
                    Text(
                        "Isi catatan dienkripsi AES-256-GCM dengan kunci dari kode pemulihanmu sebelum diunggah. " +
                                "Server hanya menyimpan file terenkripsi + SHA-256 untuk cek integritas.",
                        fontSize = 12.5.sp, color = SipherColor.Ink600
                    )
                }
            }

            item { SectionLabel("Riwayat backup (maks. ${BackupRepository.MAX_BACKUPS})") }
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SipherColor.Blue500, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (backups.isEmpty()) {
                item {
                    SipherCard { Text("Belum ada backup.", fontSize = 13.5.sp, color = SipherColor.Ink600) }
                }
            } else {
                items(backups, key = { it.id }) { meta ->
                    SipherCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Outlined.Description)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(fmt.format(Date(meta.createdAt)), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink900)
                                Text(
                                    "${"%.1f".format(meta.sizeBytes / 1024f)} KB · via ${meta.network}",
                                    fontSize = 12.5.sp, color = SipherColor.Ink600
                                )
                                Text(
                                    "sha256 ${meta.sha256.take(16)}…",
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SipherColor.Ink600
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        RestoreDialog(
            isRestoring = isRestoring,
            error = restoreError,
            onDismiss = { showRestoreDialog = false },
            onConfirm = { onRestore(it) }
        )
    }
    LaunchedEffect(isRestoring, restoreError) {
        // Tutup dialog otomatis setelah restore sukses
        if (!isRestoring && restoreError == null && showRestoreDialog) showRestoreDialog = false
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = SipherColor.Bg, modifier = modifier) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 10.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink600)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
        }
    }
}

@Composable
fun RestoreDialog(
    isRestoring: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        title = { Text("Masukkan kode pemulihan") },
        text = {
            Column {
                Text(
                    "Kode 12 karakter yang ditampilkan saat pertama kali membuat PIN.",
                    fontSize = 13.sp, color = SipherColor.Ink600
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(14) },
                    placeholder = { Text("XXXX-XXXX-XXXX", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(12.dp),
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, fontSize = 12.5.sp, color = SipherColor.Danger)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(code) },
                enabled = !isRestoring && code.count { it.isLetterOrDigit() } >= 12
            ) {
                if (isRestoring) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Pulihkan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isRestoring) { Text("Batal") } }
    )
}
