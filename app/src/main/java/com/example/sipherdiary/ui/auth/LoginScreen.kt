package com.example.sipherdiary.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.ui.theme.SipherColor

@Composable
fun LoginScreen(
    onSendOtpClicked: (String) -> Unit,
    onGoogleSignInClicked: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    // Bersihkan nol di depan jika user mengetik 0812...
    val cleanDigits = phoneNumber.filter { it.isDigit() }.removePrefix("0")
    val isPhoneValid = cleanDigits.length >= 9

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo Ikon Gembok
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SipherColor.Blue500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Logo SipherDiary",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Judul & Subjudul
            Text(
                text = "Masuk ke SipherDiary",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Kami kirim kode sekali pakai (OTP) ke nomormu. Tidak perlu kata sandi.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = SipherColor.Ink600
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Label Nomor HP
            Text(
                text = "Nomor HP",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kotak Input Nomor HP (+62 | 812 3456 7890)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = SipherColor.Bg,
                border = BorderStroke(
                    width = if (isFocused) 1.8.dp else 1.2.dp,
                    color = if (isFocused) SipherColor.Blue500 else SipherColor.Line
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+62",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SipherColor.Ink900
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(SipherColor.Line)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.length <= 15) phoneNumber = input
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SipherColor.Ink900
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            if (phoneNumber.isEmpty()) {
                                Text(
                                    text = "812 3456 7890",
                                    fontSize = 16.sp,
                                    color = SipherColor.Ink600.copy(alpha = 0.55f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kode berlaku 5 menit dan hanya bisa dipakai sekali.",
                fontSize = 12.sp,
                color = SipherColor.Ink600
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Kirim Kode OTP
            Button(
                onClick = { onSendOtpClicked("+62$cleanDigits") },
                enabled = isPhoneValid,
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
                    text = "Kirim kode OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pembatas "atau"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SipherColor.Line)
                Text(
                    text = "atau",
                    fontSize = 12.5.sp,
                    color = SipherColor.Ink600,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = SipherColor.Line)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Lanjut dengan Google
            OutlinedButton(
                onClick = onGoogleSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, SipherColor.Line),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SipherColor.Bg,
                    contentColor = SipherColor.Ink900
                )
            ) {
                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Lanjut dengan Google",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SipherColor.Ink900
                )
            }
        }
    }
}

@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.18f
        val radius = (size.minDimension - strokeWidth) / 2f
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Merah (atas)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -150f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Kuning (kiri)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 155f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Hijau (bawah)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Biru (kanan & palang tengah)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -10f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.5f),
            end = Offset(size.width * 0.92f, size.height * 0.5f),
            strokeWidth = strokeWidth
        )
    }
}