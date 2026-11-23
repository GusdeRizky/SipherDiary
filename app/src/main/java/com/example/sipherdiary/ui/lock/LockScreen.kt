package com.example.sipherdiary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.security.PinManager
import com.example.sipherdiary.security.PinResult
import com.example.sipherdiary.ui.theme.SipherColor
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    userName: String = "Jordan",
    pinManager: PinManager,
    biometricEnabled: Boolean,
    onBiometricClicked: () -> Unit,
    onUnlocked: () -> Unit,
    onDecoyUnlocked: () -> Unit,
    onIntruderDetected: () -> Unit,
    onForgotPinClicked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var remainingAttempts by remember { mutableStateOf<Int?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onDigitPressed(digit: String) {
        if (isVerifying || enteredPin.length >= 6) return
        val nextPin = enteredPin + digit
        enteredPin = nextPin

        if (nextPin.length == 6) {
            isVerifying = true
            scope.launch {
                when (val result = pinManager.verify(nextPin)) {
                    is PinResult.Ok -> {
                        onUnlocked()
                    }
                    is PinResult.Decoy -> {
                        onDecoyUnlocked()
                    }
                    is PinResult.Wrong -> {
                        remainingAttempts = result.remainingAttempts
                        enteredPin = ""
                        isVerifying = false
                    }
                    is PinResult.Intruder, is PinResult.LockedOut -> {
                        enteredPin = ""
                        isVerifying = false
                        onIntruderDetected()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo Ikon Gembok Tengah
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SipherColor.Blue500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Terkunci",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Halo lagi, $userName",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Masukkan PIN atau sentuh sensor sidik jari",
                fontSize = 14.sp,
                color = SipherColor.Ink600
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6 Titik PIN
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (i in 0 until 6) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (filled) SipherColor.Blue500 else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (filled) SipherColor.Blue500 else SipherColor.Blue500.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pill Peringatan Sisa Percobaan
            if (remainingAttempts != null) {
                Surface(
                    shape = CircleShape,
                    color = SipherColor.Warning.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = SipherColor.Warning,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sisa $remainingAttempts percobaan sebelum kamera aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SipherColor.Warning
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(31.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Keypad Angka 3x4
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowDigits ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowDigits.forEach { digit ->
                            KeypadDigitButton(
                                label = digit,
                                onClick = { onDigitPressed(digit) }
                            )
                        }
                    }
                }

                // Baris 4: Sidik Jari | 0 | Hapus
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Tombol Sidik Jari
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (biometricEnabled) SipherColor.Tint else Color.Transparent)
                            .clickable(enabled = biometricEnabled) { onBiometricClicked() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (biometricEnabled) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = "Sidik Jari",
                                tint = SipherColor.Blue500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Tombol 0
                    KeypadDigitButton(
                        label = "0",
                        onClick = { onDigitPressed("0") }
                    )

                    // Tombol Hapus (Backspace)
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = enteredPin.isNotEmpty() && !isVerifying) {
                                enteredPin = enteredPin.dropLast(1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "Hapus",
                            tint = SipherColor.Ink600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Lupa PIN
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lupa PIN? ",
                    fontSize = 13.sp,
                    color = SipherColor.Ink600
                )
                Text(
                    text = "Masuk ulang dengan OTP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SipherColor.Blue500,
                    modifier = Modifier.clickable { onForgotPinClicked() }
                )
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SipherColor.Subtle)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = SipherColor.Ink900
        )
    }
}