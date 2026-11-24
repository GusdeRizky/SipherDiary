package com.example.sipherdiary.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import com.example.sipherdiary.security.NearbyWifi
import com.example.sipherdiary.security.NetCheckResult
import com.example.sipherdiary.security.NetVerdict
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor

/**
 * Layar 10 — Cek Wi-Fi sebelum backup (logika NetworkMonitor dosen).
 * [forBackup] = true jika dibuka dari tombol "Backup sekarang".
 */
@Composable
fun WifiCheckScreen(
    result: NetCheckResult?,
    isChecking: Boolean,
    forBackup: Boolean,
    isBackingUp: Boolean,
    needsLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
    onRecheck: () -> Unit,
    onProceedBackup: () -> Unit,
    onBack: () -> Unit,
    nearby: List<NearbyWifi>? = null,
    isScanning: Boolean = false,
    scanMessage: String? = null,
    onScanNearby: () -> Unit = {}
) {
    Scaffold(containerColor = SipherColor.Bg) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            ScreenHeader(title = "Cek Keamanan Jaringan", overline = if (forBackup) "Sebelum backup" else "Jaringan mobile", onBack = onBack)

            if (isChecking || result == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SipherColor.Blue500)
                        Spacer(Modifier.height(12.dp))
                        Text("Memeriksa jaringan…", color = SipherColor.Ink600)
                    }
                }
                return@Column
            }

            val (color, icon) = verdictStyle(result.verdict)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = color.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(result.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                    Spacer(Modifier.height(4.dp))
                    Text(result.reason, fontSize = 13.5.sp, color = SipherColor.Ink600, textAlign = TextAlign.Center)
                    if (forBackup) {
                        Spacer(Modifier.height(10.dp))
                        StatusChip(
                            if (result.allowsBackup) "Backup diizinkan" else "Backup ditunda",
                            if (result.allowsBackup) SipherColor.Success else SipherColor.Danger
                        )
                    }
                }
            }

            SectionLabel("Hasil pemeriksaan")
            SipherCard {
                InfoRow("Koneksi", result.connectionType)
                HorizontalDivider(color = SipherColor.Line)
                InfoRow(
                    "Tipe keamanan", result.securityType, mono = true,
                    valueColor = when (result.securityType) {
                        "WPA3", "WPA2", "4G/5G" -> SipherColor.Success
                        "OPEN", "WEP" -> SipherColor.Danger
                        else -> SipherColor.Warning
                    }
                )
                HorizontalDivider(color = SipherColor.Line)
                InfoRow(
                    "Kekuatan sinyal",
                    result.signalDbm?.let { "$it dBm" } ?: "-",
                    mono = true,
                    valueColor = when {
                        result.signalDbm == null -> SipherColor.Ink900
                        result.signalDbm > -70 -> SipherColor.Success
                        else -> SipherColor.Warning
                    }
                )
                HorizontalDivider(color = SipherColor.Line)
                InfoRow("IP lokal", result.localIp, mono = true)
                HorizontalDivider(color = SipherColor.Line)
                InfoRow(
                    "Koneksi server", result.tlsInfo, mono = true,
                    valueColor = if (result.tlsOk) SipherColor.Success else SipherColor.Danger
                )
            }

            SipherCard(color = SipherColor.Subtle) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.SignalCellularAlt, null, tint = SipherColor.Ink600, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Aturan: WPA3 aman · WPA2 aman jika sinyal > -70 dBm · Wi-Fi terbuka/WEP ditolak · " +
                                "data seluler diizinkan. Semua lalu lintas wajib HTTPS (cleartext diblokir).",
                        fontSize = 12.5.sp, color = SipherColor.Ink600
                    )
                }
            }

            // ---------------- Wi-Fi di sekitar
            SectionLabel(
                if (nearby == null) "Wi-Fi di sekitar"
                else "Wi-Fi di sekitar (${nearby.size})"
            )
            if (nearby != null && nearby.isNotEmpty()) {
                val safe = nearby.count { it.verdict == NetVerdict.AMAN }
                val weak = nearby.count { it.verdict == NetVerdict.AMAN_SINYAL_LEMAH }
                val unsafe = nearby.count { it.verdict == NetVerdict.TIDAK_AMAN }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("$safe aman", SipherColor.Success)
                    StatusChip("$weak lemah", SipherColor.Warning)
                    StatusChip("$unsafe tidak aman", SipherColor.Danger)
                }
                SipherCard {
                    nearby.forEachIndexed { index, wifi ->
                        if (index > 0) HorizontalDivider(color = SipherColor.Line)
                        NearbyWifiRow(wifi)
                    }
                }
            } else if (nearby != null && !isScanning) {
                SipherCard(color = SipherColor.Subtle) {
                    Text(scanMessage ?: "Tidak ada Wi-Fi terdeteksi.", fontSize = 13.sp, color = SipherColor.Ink600)
                }
            } else if (scanMessage != null && !isScanning) {
                SipherCard(color = SipherColor.Subtle) {
                    Text(scanMessage, fontSize = 13.sp, color = SipherColor.Warning)
                }
            }
            OutlineButton(
                text = if (isScanning) "Memindai…" else if (nearby == null) "Pindai Wi-Fi di sekitar" else "Pindai ulang",
                onClick = onScanNearby,
                enabled = !isScanning
            )

            if (needsLocationPermission) {
                OutlineButton("Izinkan akses lokasi (baca jenis Wi-Fi)", onRequestLocation)
            }

            Spacer(Modifier.height(4.dp))
            if (forBackup) {
                PrimaryButton(
                    text = if (result.allowsBackup) "Lanjutkan backup" else "Backup ditunda",
                    enabled = result.allowsBackup,
                    loading = isBackingUp,
                    onClick = onProceedBackup
                )
            }
            OutlineButton("Periksa ulang", onRecheck)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NearbyWifiRow(wifi: NearbyWifi) {
    val (color, _) = verdictStyle(wifi.verdict)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = if (wifi.isConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiFind,
            tint = color,
            background = color.copy(alpha = 0.10f)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                wifi.ssid + if (wifi.isConnected) "  · tersambung" else "",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SipherColor.Ink900,
                maxLines = 1
            )
            Text(
                "${wifi.securityType} · ${wifi.rssi} dBm · ${wifi.band}",
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = SipherColor.Ink600
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusChip(
            when (wifi.verdict) {
                NetVerdict.AMAN -> "Aman"
                NetVerdict.AMAN_SINYAL_LEMAH -> "Lemah"
                else -> "Tidak aman"
            },
            color
        )
    }
}

private fun verdictStyle(v: NetVerdict): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> = when (v) {
    NetVerdict.AMAN, NetVerdict.SELULER -> SipherColor.Success to Icons.Outlined.GppGood
    NetVerdict.AMAN_SINYAL_LEMAH -> SipherColor.Warning to Icons.Outlined.GppMaybe
    NetVerdict.TIDAK_AMAN, NetVerdict.TIDAK_ADA_KONEKSI -> SipherColor.Danger to Icons.Outlined.GppBad
}
