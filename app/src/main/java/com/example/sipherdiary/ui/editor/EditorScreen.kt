package com.example.sipherdiary.ui.editor

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.repository.DiaryEntry
import com.example.sipherdiary.data.repository.MoodType
import com.example.sipherdiary.security.CryptoManager
import com.example.sipherdiary.ui.theme.SipherColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    existingEntry: DiaryEntry?,
    onCloseClicked: () -> Unit,
    onSaveClicked: (title: String, body: String, mood: MoodType) -> Unit
) {
    var title by remember(existingEntry) { mutableStateOf(existingEntry?.title ?: "") }
    var body by remember(existingEntry) { mutableStateOf(existingEntry?.body ?: "") }
    var selectedMood by remember(existingEntry) { mutableStateOf(existingEntry?.mood ?: MoodType.TENANG) }

    val localeId = Locale.forLanguageTag("id-ID")
    val timestampText = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy · HH.mm", localeId))
    }

    // Simulasi live ciphertext AES-256-GCM dari Android Keystore
    val liveCipherPreview = remember(title, body) {
        val sample = "$title|$body".ifBlank { "SipherDiary-AES256" }
        runCatching {
            val encrypted = CryptoManager.encrypt(sample.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }.getOrDefault("U2FsdGVkX19vK3pQ8mWc1T0aZr7hJ2nYe4bXq9LkVd0sPf6GmR3tNw8yUiA5cOe1==")
    }

    val canSave = title.isNotBlank() || body.isNotBlank()

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Top Bar: Tutup (X) | Draf terenkripsi | Simpan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCloseClicked() },
                    shape = RoundedCornerShape(12.dp),
                    color = SipherColor.Bg,
                    border = BorderStroke(1.dp, SipherColor.Line)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = SipherColor.Ink900,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SipherColor.Success,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Draf terenkripsi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SipherColor.Ink600
                    )
                }

                Button(
                    onClick = { onSaveClicked(title, body, selectedMood) },
                    enabled = canSave,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SipherColor.Blue500,
                        contentColor = Color.White,
                        disabledContainerColor = SipherColor.Blue100,
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Simpan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Konten Editor yang Bisa Di-scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Pill Tanggal & Jam
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SipherColor.Subtle
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = SipherColor.Blue500,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timestampText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SipherColor.Ink900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Bagaimana perasaanmu?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SipherColor.Ink600
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5 Pilihan Mood
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoodType.entries.forEach { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedMood = mood },
                            shape = CircleShape,
                            color = if (isSelected) SipherColor.Tint else SipherColor.Bg,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SipherColor.Blue500 else SipherColor.Line
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(mood.colorHex))
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = mood.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) SipherColor.Blue500 else SipherColor.Ink900
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Input Judul Catatan
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = SipherColor.Ink900
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "Judul catatan...",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = SipherColor.Ink600.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Isi Catatan
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = SipherColor.Ink900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp),
                    decorationBox = { innerTextField ->
                        if (body.isEmpty()) {
                            Text(
                                text = "Ceritakan harimu di sini. Semua teks otomatis dienkripsi sebelum disimpan...",
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                color = SipherColor.Ink600.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kartu Live Ciphertext AES-256-GCM
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SipherColor.Subtle,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = SipherColor.Blue500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yang tersimpan di perangkat",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SipherColor.Blue500
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = liveCipherPreview,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = SipherColor.Ink900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AES-256-GCM  ·  kunci di Android Keystore",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SipherColor.Ink600
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Anti-Screenshot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = SipherColor.Ink600,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Screenshot & rekam layar diblokir di halaman ini",
                    fontSize = 11.5.sp,
                    color = SipherColor.Ink600
                )
            }
        }
    }
}