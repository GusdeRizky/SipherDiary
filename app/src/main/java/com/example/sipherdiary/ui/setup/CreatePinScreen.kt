package com.example.sipherdiary.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.security.PinManager
import com.example.sipherdiary.ui.theme.SipherColor

@Composable
fun CreatePinScreen(
    pinManager: PinManager,
    canUseBiometric: Boolean,
    onPinSaved: (pin: String, biometricEnabled: Boolean, decoyPin: String?) -> Unit
) {
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var biometricEnabled by remember { mutableStateOf(canUseBiometric) }
    var decoyPin by remember { mutableStateOf<String?>(null) }
    var showDecoyDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val activePin = if (isConfirmStage) confirmPin else firstPin

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = SipherColor.Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Indikator Langkah 2 dari 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(SipherColor.Blue500)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(SipherColor.Blue500)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LANGKAH 2 DARI 2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = SipherColor.Blue500
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isConfirmStage) "Konfirmasi PIN barumu" else "Buat PIN untuk membuka diary",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 33.sp,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isConfirmStage)
                    "Masukkan ulang 6 digit PIN yang sama untuk memastikan tidak salah ketik."
                else
                    "PIN jadi lapisan kedua setelah OTP. Disimpan sebagai hash bcrypt, bukan angka aslinya.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = SipherColor.Ink600
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Kartu 6 Titik PIN (klik untuk memunculkan keyboard angka)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() },
                shape = RoundedCornerShape(16.dp),
                color = SipherColor.Subtle
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = activePin,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 6) {
                                errorMessage = null
                                if (!isConfirmStage) {
                                    firstPin = digits
                                    if (digits.length == 6) {
                                        if (pinManager.isWeakPin(digits)) {
                                            errorMessage = "PIN terlalu mudah ditebak. Hindari urutan atau angka kembar."
                                        } else {
                                            isConfirmStage = true
                                        }
                                    }
                                } else {
                                    confirmPin = digits
                                    if (digits.length == 6 && digits != firstPin) {
                                        errorMessage = "PIN konfirmasi tidak cocok. Coba lagi."
                                        confirmPin = ""
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusRequester)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (i in 0 until 6) {
                            val filled = i < activePin.length
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Baris Peringatan / Error
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (errorMessage != null) SipherColor.Danger else SipherColor.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorMessage ?: "Hindari 123456 atau tanggal lahirmu.",
                        fontSize = 12.5.sp,
                        color = if (errorMessage != null) SipherColor.Danger else SipherColor.Warning
                    )
                }

                if (isConfirmStage) {
                    Text(
                        text = "Ulangi",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SipherColor.Blue500,
                        modifier = Modifier.clickable {
                            firstPin = ""
                            confirmPin = ""
                            isConfirmStage = false
                            errorMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Pengamanan tambahan",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SipherColor.Ink900
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kartu 1: Buka dengan sidik jari
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SipherColor.Bg,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SipherColor.Tint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = SipherColor.Blue500,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buka dengan sidik jari",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SipherColor.Ink900
                        )
                        Text(
                            text = "Kunci biometrik disimpan di Android Keystore.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = SipherColor.Ink600
                        )
                    }

                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SipherColor.Blue500
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kartu 2: PIN samaran
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDecoyDialog = true },
                shape = RoundedCornerShape(14.dp),
                color = SipherColor.Bg,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SipherColor.Tint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = SipherColor.Blue500,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PIN samaran",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SipherColor.Ink900
                        )
                        Text(
                            text = if (decoyPin != null)
                                "Aktif — menampilkan diary kosong saat dipakai."
                            else
                                "Saat dipaksa membuka, tampilkan diary kosong.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (decoyPin != null) SipherColor.Success else SipherColor.Ink600
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = SipherColor.Ink600
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tombol Simpan & Lanjutkan
            val isReadyToSave = isConfirmStage && confirmPin.length == 6 && confirmPin == firstPin
            Button(
                onClick = { onPinSaved(firstPin, biometricEnabled, decoyPin) },
                enabled = isReadyToSave,
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
                    text = "Simpan & lanjutkan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Dialog Atur PIN Samaran
    if (showDecoyDialog) {
        var tempDecoy by remember { mutableStateOf(decoyPin ?: "") }
        AlertDialog(
            onDismissRequest = { showDecoyDialog = false },
            title = { Text("Atur PIN Samaran (6 Digit)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Masukkan 6 digit berbeda dari PIN utama. Jika dimasukkan di layar kunci, aplikasi akan membuka ruang kosong.",
                        fontSize = 13.sp,
                        color = SipherColor.Ink600
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempDecoy,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) tempDecoy = it },
                        singleLine = true,
                        label = { Text("PIN Samaran") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        decoyPin = if (tempDecoy.length == 6 && tempDecoy != firstPin) tempDecoy else null
                        showDecoyDialog = false
                    }
                ) {
                    Text("Simpan", color = SipherColor.Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyDialog = false }) {
                    Text("Batal", color = SipherColor.Ink600)
                }
            }
        )
    }
}