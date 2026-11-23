package com.example.sipherdiary.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.ui.theme.SipherColor
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun OtpScreen(
    phoneNumber: String,
    onBackClicked: () -> Unit,
    onVerifyClicked: (String) -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(60) }
    val focusRequester = remember { FocusRequester() }

    // Hitung mundur 60 detik
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    // Fokus otomatis ke kotak OTP saat layar dibuka
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val formattedTime = String.format(Locale.US, "%02d:%02d", secondsLeft / 60, secondsLeft % 60)

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Tombol Kembali
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackClicked() },
                shape = RoundedCornerShape(12.dp),
                color = SipherColor.Bg,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Kembali",
                        tint = SipherColor.Ink900
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Masukkan kode 6 digit",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dikirim lewat SMS ke $phoneNumber.",
                fontSize = 14.sp,
                color = SipherColor.Ink600
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ubah nomor",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SipherColor.Blue500,
                modifier = Modifier.clickable { onBackClicked() }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6 Kotak Input OTP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = otpCode,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 6) {
                            otpCode = digits
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (index in 0 until 6) {
                        val char = otpCode.getOrNull(index)?.toString() ?: ""
                        val isCurrentSlot = index == otpCode.length
                        val isFilled = char.isNotEmpty()

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFilled) SipherColor.Tint else SipherColor.Bg,
                            border = BorderStroke(
                                width = if (isCurrentSlot) 1.8.dp else 1.dp,
                                color = if (isCurrentSlot) SipherColor.Blue500 else SipherColor.Line
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isFilled) {
                                    Text(
                                        text = char,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SipherColor.Ink900
                                    )
                                } else if (isCurrentSlot) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(22.dp)
                                            .background(SipherColor.Blue500)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Timer Kirim Ulang Kode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = SipherColor.Ink600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (secondsLeft > 0) {
                    Text(
                        text = "Kirim ulang kode dalam ",
                        fontSize = 13.sp,
                        color = SipherColor.Ink600
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SipherColor.Ink900
                    )
                } else {
                    Text(
                        text = "Kirim ulang kode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SipherColor.Blue500,
                        modifier = Modifier.clickable { secondsLeft = 60 }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Kartu Edukasi Anti-Phishing
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SipherColor.Subtle
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = SipherColor.Blue500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SipherDiary tidak pernah meminta kode ini lewat telepon atau chat. Jangan bagikan ke siapa pun.",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = SipherColor.Ink600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Verifikasi
            Button(
                onClick = { onVerifyClicked(otpCode) },
                enabled = otpCode.length == 6,
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
                    text = "Verifikasi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}