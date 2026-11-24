package com.example.sipherdiary.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.repository.AccountInfo
import com.example.sipherdiary.data.repository.DeviceInfo
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Layar 13 — Profil (Manajemen Identitas + hak data pengguna UU PDP). */
@Composable
fun ProfileScreen(
    account: AccountInfo?,
    devices: List<DeviceInfo>,
    entryCount: Int,
    busyMessage: String?,
    pinError: String?,
    onChangePin: (old: String, new: String) -> Unit,
    onExportData: () -> Unit,
    onRemoveDevice: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: (pin: String) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRightsDialog by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy · HH.mm", Locale.forLanguageTag("id-ID")) }

    // Tutup dialog PIN setelah berhasil (pinError == "" menandakan sukses)
    LaunchedEffect(pinError) { if (pinError == "") showPinDialog = false }

    Scaffold(
        containerColor = SipherColor.Bg,
        bottomBar = { SipherNavBar(currentRoute = "profile", onNavigate = onNavigateTab) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader(title = "Profil", overline = "Akun & identitas") }
            if (account?.isDemo == true) item { DemoModeBanner() }

            item {
                SipherCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SipherColor.Tint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (account?.displayName?.firstOrNull { it.isLetter() } ?: 'S').uppercase(),
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SipherColor.Blue500
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account?.displayName ?: "-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900)
                            Text(account?.label ?: "-", fontSize = 13.sp, color = SipherColor.Ink600)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = SipherColor.Line)
                    InfoRow(
                        "Metode masuk",
                        when (account?.provider) {
                            "google" -> "Google SSO (OpenID Connect)"
                            else -> "OTP SMS"
                        }
                    )
                    InfoRow("Kunci lokal", "PIN 6 digit (bcrypt)")
                    InfoRow("Catatan tersimpan", "$entryCount")
                }
            }

            item { SectionLabel("Perangkat aktif") }
            items(devices, key = { it.id }) { device ->
                SipherCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Smartphone)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(device.model, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink900)
                            Text(
                                "Android ${device.androidVersion} · " +
                                        if (device.isThisDevice) "Perangkat ini" else "Aktif ${fmt.format(Date(device.lastActiveAt))}",
                                fontSize = 12.5.sp, color = SipherColor.Ink600
                            )
                        }
                        if (device.isThisDevice) {
                            StatusChip("Aktif", SipherColor.Success)
                        } else {
                            TextButton(onClick = { onRemoveDevice(device.id) }) {
                                Text("Keluarkan", color = SipherColor.Danger)
                            }
                        }
                    }
                }
            }

            item { SectionLabel("Keamanan & data") }
            item {
                SipherCard {
                    NavRow(Icons.Outlined.Password, "Ganti PIN", "Wajib memasukkan PIN lama") { showPinDialog = true }
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(Icons.Outlined.Download, "Unduh data saya", "Ekspor catatan ke file JSON", onClick = onExportData)
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(
                        Icons.Outlined.Policy, "Hak data (UU PDP No. 27/2022)",
                        "Akses, unduh, dan hapus data kapan saja"
                    ) { showRightsDialog = true }
                }
            }

            item {
                SipherCard {
                    NavRow(
                        Icons.AutoMirrored.Outlined.Logout, "Keluar",
                        "Catatan tetap terenkripsi di HP ini"
                    ) { showSignOutDialog = true }
                    HorizontalDivider(color = SipherColor.Line)
                    NavRow(
                        Icons.Outlined.DeleteForever, "Hapus akun",
                        "Hapus semua catatan, backup, dan akun",
                        tint = SipherColor.Danger,
                        badgeBackground = SipherColor.Danger.copy(alpha = 0.1f),
                        titleColor = SipherColor.Danger
                    ) { showDeleteDialog = true }
                }
            }

            item {
                Text(
                    "SipherDiary v1.0 · Kelompok 3 Keamanan Sistem Mobile",
                    fontSize = 12.sp, color = SipherColor.Ink600,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (busyMessage != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(busyMessage)
                }
            }
        )
    }

    if (showRightsDialog) {
        AlertDialog(
            onDismissRequest = { showRightsDialog = false },
            title = { Text("Hak atas data pribadimu") },
            text = {
                Text(
                    "Sesuai UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi:\n\n" +
                            "• Akses: semua catatan bisa kamu lihat di aplikasi.\n" +
                            "• Portabilitas: \"Unduh data saya\" mengekspor catatan ke JSON.\n" +
                            "• Penghapusan: \"Hapus akun\" menghapus data lokal, backup cloud, dan akun.\n" +
                            "• Persetujuan: dicatat saat pertama kali membuka aplikasi.\n\n" +
                            "SipherDiary tidak memakai iklan, analytics, atau pelacak pihak ketiga.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = { TextButton(onClick = { showRightsDialog = false }) { Text("Mengerti") } }
        )
    }

    if (showPinDialog) {
        ChangePinDialog(
            error = pinError?.takeIf { it.isNotEmpty() },
            onDismiss = { showPinDialog = false },
            onConfirm = onChangePin
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Keluar dari akun?") },
            text = { Text("Catatan tetap tersimpan terenkripsi di HP ini. Kamu perlu masuk lagi dengan OTP/Google.") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) { Text("Keluar") }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Batal") } }
        )
    }

    if (showDeleteDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus akun permanen?") },
            text = {
                Column {
                    Text(
                        "Semua catatan, foto penyusup, backup di cloud, dan akun akan dihapus. Tindakan ini tidak bisa dibatalkan.",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    PinField("Masukkan PIN untuk konfirmasi", pin) { pin = it }
                    if (pinError != null && pinError.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(pinError, color = SipherColor.Danger, fontSize = 12.5.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDeleteAccount(pin) }, enabled = pin.length == 6) {
                    Text("Hapus semuanya", color = SipherColor.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChangePinDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.length == 6 && new != confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField("PIN lama", old) { old = it }
                PinField("PIN baru", new) { new = it }
                PinField("Ulangi PIN baru", confirm) { confirm = it }
                if (mismatch) Text("PIN baru tidak sama", color = SipherColor.Danger, fontSize = 12.5.sp)
                if (error != null) Text(error, color = SipherColor.Danger, fontSize = 12.5.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(old, new) },
                enabled = old.length == 6 && new.length == 6 && new == confirm
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
