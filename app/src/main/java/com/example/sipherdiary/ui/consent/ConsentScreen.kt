package com.example.sipherdiary.ui.consent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.ui.theme.SipherColor

@Composable
fun ConsentScreen(
    onAgreeClicked: () -> Unit
) {
    var isAgreed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Logo SipherDiary
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SipherColor.Blue500),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Logo SipherDiary",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SipherDiary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SipherColor.Ink900
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Judul Utama
                Text(
                    text = "Sebelum mulai, ini janji kami soal datamu.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    color = SipherColor.Ink900
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subjudul UU PDP
                Text(
                    text = "SipherDiary menyimpan hal paling pribadi milikmu. Sesuai UU No. 27/2022 tentang Pelindungan Data Pribadi, kami minta persetujuanmu dengan jelas.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = SipherColor.Ink600
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3 Poin Janji Privasi
                PrivacyPromiseItem(
                    icon = Icons.Outlined.Description,
                    title = "Dienkripsi di HP-mu",
                    description = "Isi catatan dikunci AES-256 sebelum disimpan. Kami pun tidak bisa membacanya."
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrivacyPromiseItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Tanpa iklan & pelacak",
                    description = "Tidak ada data yang dijual atau dibagikan ke pihak ketiga."
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrivacyPromiseItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Datamu, hakmu",
                    description = "Unduh, ubah, atau hapus semua catatanmu kapan saja."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kotak Centang Persetujuan
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { isAgreed = !isAgreed },
                shape = RoundedCornerShape(14.dp),
                color = SipherColor.Subtle,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { isAgreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SipherColor.Blue500,
                            uncheckedColor = SipherColor.Ink600,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saya sudah membaca dan menyetujui Kebijakan Privasi SipherDiary.",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 19.sp,
                        color = SipherColor.Ink900
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Setuju & Lanjutkan
            Button(
                onClick = onAgreeClicked,
                enabled = isAgreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SipherColor.Blue500,
                    contentColor = Color.White,
                    disabledContainerColor = SipherColor.Blue100,
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "Setuju & lanjutkan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PrivacyPromiseItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SipherColor.Tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SipherColor.Blue500,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SipherColor.Ink900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = SipherColor.Ink600
            )
        }
    }
}