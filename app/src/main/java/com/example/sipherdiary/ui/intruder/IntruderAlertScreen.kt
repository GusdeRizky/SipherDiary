package com.example.sipherdiary.ui.intruder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.local.entity.IntruderLogEntity
import com.example.sipherdiary.ui.components.OutlineButton
import com.example.sipherdiary.ui.components.PrimaryButton
import com.example.sipherdiary.ui.components.ScreenHeader
import com.example.sipherdiary.ui.components.SectionLabel
import com.example.sipherdiary.ui.components.SipherCard
import com.example.sipherdiary.ui.theme.SipherColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar 08 — Peringatan Penyusup.
 * Mode "terkunci": muncul setelah PIN salah 3×, menampilkan hitung mundur 5 menit.
 * Mode "riwayat": dibuka dari Pusat Keamanan untuk melihat log & foto penyusup.
 */
@Composable
fun IntruderAlertScreen(
    logs: List<IntruderLogEntity>,
    lockUntilMillis: Long,
    loadPhoto: suspend (String?) -> ImageBitmap?,
    isHistoryMode: Boolean,
    onBack: () -> Unit,
    onTryAgain: () -> Unit,
    onClearLogs: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockUntilMillis) {
        while (System.currentTimeMillis() < lockUntilMillis) {
            delay(1000)
            now = System.currentTimeMillis()
        }
        now = System.currentTimeMillis()
    }
    val remainingSec = ((lockUntilMillis - now) / 1000).coerceAtLeast(0)
    val latest = logs.firstOrNull()

    Scaffold(containerColor = SipherColor.Bg) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = if (isHistoryMode) "Log Penyusup" else "Peringatan Penyusup",
                    overline = "Deteksi percobaan masuk",
                    onBack = if (isHistoryMode) onBack else null
                )
            }

            if (!isHistoryMode) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SipherColor.Danger.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.ReportProblem, null,
                                tint = SipherColor.Danger, modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "PIN salah 3 kali",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (latest?.photoPath != null)
                                    "Foto orang yang mencoba membuka diary sudah diambil dan disimpan terenkripsi."
                                else "Percobaan masuk sudah dicatat.",
                                fontSize = 13.5.sp, color = SipherColor.Ink600, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                if (remainingSec > 0) "%02d:%02d".format(remainingSec / 60, remainingSec % 60) else "00:00",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace, color = SipherColor.Danger
                            )
                            Text(
                                if (remainingSec > 0) "Aplikasi terkunci sementara" else "Kamu bisa mencoba lagi",
                                fontSize = 12.5.sp, color = SipherColor.Ink600
                            )
                        }
                    }
                }
            }

            item { SectionLabel("Riwayat percobaan (${logs.size})") }

            if (logs.isEmpty()) {
                item {
                    SipherCard(color = SipherColor.Subtle) {
                        Text("Belum ada percobaan masuk yang gagal.", fontSize = 13.5.sp, color = SipherColor.Ink600)
                    }
                }
            } else {
                items(logs, key = { it.id }) { log -> IntruderLogCard(log, loadPhoto) }
            }

            item {
                Spacer(Modifier.height(4.dp))
                if (isHistoryMode) {
                    if (logs.isNotEmpty()) OutlineButton("Hapus semua log", onClearLogs, color = SipherColor.Danger)
                } else {
                    PrimaryButton(
                        text = if (remainingSec > 0) "Tunggu ${remainingSec} detik" else "Coba lagi",
                        enabled = remainingSec == 0L,
                        onClick = onTryAgain
                    )
                }
            }
        }
    }
}

@Composable
private fun IntruderLogCard(log: IntruderLogEntity, loadPhoto: suspend (String?) -> ImageBitmap?) {
    var photo by remember(log.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(log.id) { photo = loadPhoto(log.photoPath) }
    val time = remember(log.occurredAt) {
        SimpleDateFormat("EEEE, d MMM yyyy · HH.mm.ss", Locale.forLanguageTag("id-ID")).format(Date(log.occurredAt))
    }

    SipherCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SipherColor.Subtle),
                contentAlignment = Alignment.Center
            ) {
                val bmp = photo
                if (bmp != null) {
                    Image(bmp, "Foto penyusup", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Outlined.NoPhotography, null, tint = SipherColor.Ink600)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(log.note, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink900)
                Text(time, fontSize = 12.5.sp, color = SipherColor.Ink600)
                if (!log.seen) {
                    Spacer(Modifier.height(4.dp))
                    Text("Baru", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SipherColor.Danger)
                }
            }
        }
    }
}
