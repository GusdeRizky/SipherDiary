package com.example.sipherdiary.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.security.ScoreItem
import com.example.sipherdiary.security.SecurityScore
import com.example.sipherdiary.security.SecurityState
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor

/** Layar 09 — Pusat Keamanan. */
@Composable
fun SecurityCenterScreen(
    state: SecurityState,
    canUseBiometric: Boolean,
    unseenIntruders: Int,
    lastBackupText: String,
    onBiometricChange: (Boolean) -> Unit,
    onAntiScreenshotChange: (Boolean) -> Unit,
    onIntruderPhotoChange: (Boolean) -> Unit,
    onAutoLockChange: (Int) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenWifiCheck: () -> Unit,
    onOpenIntruderLog: () -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val score = SecurityScore.score(state)
    val items = SecurityScore.items(state)
    var showAutoLockDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SipherColor.Bg,
        bottomBar = { SipherNavBar(currentRoute = "security", onNavigate = onNavigateTab) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader(title = "Pusat Keamanan", overline = "Perlindungan diary") }

            item { ScoreCard(score, items) }

            item { SectionLabel("Kunci aplikasi") }
            item {
                SipherCard {
                    SettingToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Sidik jari",
                        subtitle = if (canUseBiometric) "Buka diary dengan biometrik" else "HP ini tidak mendukung biometrik kuat",
                        checked = state.biometricEnabled && canUseBiometric,
                        enabled = canUseBiometric,
                        onCheckedChange = onBiometricChange
                    )
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(
                        icon = Icons.Outlined.Timer,
                        title = "Kunci otomatis",
                        subtitle = autoLockLabel(state.autoLockMinutes),
                        onClick = { showAutoLockDialog = true }
                    )
                    HorizontalDivider(color = SipherColor.Line)
                    SettingToggleRow(
                        icon = Icons.Outlined.ScreenLockPortrait,
                        title = "Blokir screenshot",
                        subtitle = "FLAG_SECURE: layar tidak bisa direkam",
                        checked = state.antiScreenshot,
                        onCheckedChange = onAntiScreenshotChange
                    )
                }
            }

            item { SectionLabel("Deteksi penyusup") }
            item {
                SipherCard {
                    SettingToggleRow(
                        icon = Icons.Outlined.CameraAlt,
                        title = "Foto penyusup",
                        subtitle = if (state.intruderPhoto && !state.cameraPermission)
                            "Izin kamera belum diberikan" else "Potret wajah setelah PIN salah 3×",
                        checked = state.intruderPhoto && state.cameraPermission,
                        onCheckedChange = onIntruderPhotoChange
                    )
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(
                        icon = Icons.Outlined.ReportProblem,
                        title = "Log percobaan masuk",
                        subtitle = if (unseenIntruders > 0) "$unseenIntruders percobaan baru!" else "Tidak ada percobaan baru",
                        tint = if (unseenIntruders > 0) SipherColor.Danger else SipherColor.Blue500,
                        badgeBackground = if (unseenIntruders > 0) SipherColor.Danger.copy(alpha = 0.1f) else SipherColor.Tint,
                        onClick = onOpenIntruderLog
                    )
                }
            }

            item { SectionLabel("Data & jaringan") }
            item {
                SipherCard {
                    NavRow(
                        icon = Icons.Outlined.CloudUpload,
                        title = "Backup & pemulihan",
                        subtitle = lastBackupText,
                        onClick = onOpenBackup
                    )
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(
                        icon = Icons.Outlined.NetworkCheck,
                        title = "Cek keamanan Wi-Fi",
                        subtitle = "WPA2/WPA3, sinyal, IP lokal, TLS",
                        onClick = onOpenWifiCheck
                    )
                }
            }

            item {
                SipherCard(color = SipherColor.Subtle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = SipherColor.Ink600, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AES-256-GCM · Android Keystore · PIN bcrypt (cost 12)",
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = SipherColor.Ink600
                        )
                    }
                }
            }
        }
    }

    if (showAutoLockDialog) {
        val options = listOf(0, 1, 5, 15)
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("Kunci otomatis") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAutoLockChange(minutes)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.autoLockMinutes == minutes, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Text(autoLockLabel(minutes))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAutoLockDialog = false }) { Text("Tutup") } }
        )
    }
}

private fun autoLockLabel(minutes: Int) = when (minutes) {
    0 -> "Segera setelah keluar aplikasi"
    else -> "Setelah $minutes menit di latar belakang"
}

@Composable
private fun ScoreCard(score: Int, items: List<ScoreItem>) {
    val color: Color = when {
        score >= 85 -> SipherColor.Success
        score >= 65 -> SipherColor.Blue500
        score >= 50 -> SipherColor.Warning
        else -> SipherColor.Danger
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SipherColor.Subtle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.size(76.dp),
                        color = color,
                        strokeWidth = 7.dp,
                        trackColor = SipherColor.Line
                    )
                    Text("$score", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Skor keamanan", fontSize = 13.sp, color = SipherColor.Ink600)
                    Text(SecurityScore.grade(score), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                    Text("dari 100 poin", fontSize = 12.sp, color = SipherColor.Ink600)
                }
            }
            Spacer(Modifier.height(12.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (item.earned) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null,
                        tint = if (item.earned) SipherColor.Success else SipherColor.Ink600,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, fontSize = 13.sp, color = SipherColor.Ink900, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (item.earned) SipherColor.Success.copy(alpha = 0.12f) else SipherColor.Line)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "+${item.points}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                            color = if (item.earned) SipherColor.Success else SipherColor.Ink600
                        )
                    }
                }
            }
        }
    }
}
