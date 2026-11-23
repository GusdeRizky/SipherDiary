package com.example.sipherdiary

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sipherdiary.data.local.AppDatabase
import com.example.sipherdiary.data.prefs.SecurePrefs
import com.example.sipherdiary.data.repository.DiaryEntry
import com.example.sipherdiary.data.repository.EntryRepository
import com.example.sipherdiary.security.BiometricHelper
import com.example.sipherdiary.security.PinManager
import com.example.sipherdiary.security.SessionManager
import com.example.sipherdiary.ui.auth.LoginScreen
import com.example.sipherdiary.ui.auth.OtpScreen
import com.example.sipherdiary.ui.consent.ConsentScreen
import com.example.sipherdiary.ui.editor.EditorScreen
import com.example.sipherdiary.ui.home.HomeScreen
import com.example.sipherdiary.ui.lock.LockScreen
import com.example.sipherdiary.ui.setup.CreatePinScreen
import com.example.sipherdiary.ui.theme.SipherDiaryTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val securePrefs = SecurePrefs(applicationContext)
        val pinManager = PinManager(securePrefs)
        val database = AppDatabase.getInstance(applicationContext)
        val entryRepository = EntryRepository(database.entryDao())

        enableEdgeToEdge()
        setContent {
            SipherDiaryTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                var pendingPhone by remember { mutableStateOf("+62 812-3456-7890") }
                var isBiometricOn by remember { mutableStateOf(false) }
                val entries by entryRepository.observeEntries().collectAsState(initial = emptyList())

                NavHost(
                    navController = navController,
                    startDestination = "consent"
                ) {
                    // 01. Persetujuan Privasi
                    composable("consent") {
                        ConsentScreen(
                            onAgreeClicked = {
                                scope.launch {
                                    securePrefs.saveConsent("1.0")
                                    navController.navigate("login") {
                                        popUpTo("consent") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // 02. Masuk
                    composable("login") {
                        LoginScreen(
                            onSendOtpClicked = { phone ->
                                pendingPhone = phone
                                navController.navigate("otp")
                            },
                            onGoogleSignInClicked = {
                                navController.navigate("create_pin")
                            }
                        )
                    }

                    // 03. Verifikasi OTP
                    composable("otp") {
                        OtpScreen(
                            phoneNumber = pendingPhone,
                            onBackClicked = { navController.popBackStack() },
                            onVerifyClicked = {
                                navController.navigate("create_pin")
                            }
                        )
                    }

                    // 04. Buat PIN & Biometrik
                    composable("create_pin") {
                        CreatePinScreen(
                            pinManager = pinManager,
                            canUseBiometric = BiometricHelper.canAuthenticate(this@MainActivity),
                            onPinSaved = { pin, biometricEnabled, decoyPin ->
                                scope.launch {
                                    pinManager.setPin(pin)
                                    pinManager.setDecoyPin(decoyPin)
                                    securePrefs.setBiometricEnabled(biometricEnabled)
                                    isBiometricOn = biometricEnabled
                                    navController.navigate("lock") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // 05. Kunci Aplikasi
                    composable("lock") {
                        LockScreen(
                            userName = "Jordan",
                            pinManager = pinManager,
                            biometricEnabled = isBiometricOn,
                            onBiometricClicked = {
                                BiometricHelper.showPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        SessionManager.unlock()
                                        navController.navigate("home") {
                                            popUpTo("lock") { inclusive = true }
                                        }
                                    }
                                )
                            },
                            onUnlocked = {
                                SessionManager.unlock()
                                navController.navigate("home") {
                                    popUpTo("lock") { inclusive = true }
                                }
                            },
                            onDecoyUnlocked = {
                                SessionManager.unlockDecoy()
                                Toast.makeText(
                                    this@MainActivity,
                                    "Mode Samaran aktif: menampilkan diary kosong",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("home") {
                                    popUpTo("lock") { inclusive = true }
                                }
                            },
                            onIntruderDetected = {
                                navController.navigate("intruder_alert")
                            },
                            onForgotPinClicked = {
                                navController.navigate("login") {
                                    popUpTo("lock") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 06. Beranda (Catatanmu)
                    composable("home") {
                        HomeScreen(
                            entries = entries,
                            userInitial = "J",
                            onOpenEditor = { entryId ->
                                val route = if (entryId != null) "editor/$entryId" else "editor/new"
                                navController.navigate(route)
                            },
                            onNavigateTab = { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // 07. Tulis / Edit Catatan
                    composable(
                        route = "editor/{entryId}",
                        arguments = listOf(navArgument("entryId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val entryId = backStackEntry.arguments?.getString("entryId")
                        var existing by remember { mutableStateOf<DiaryEntry?>(null) }

                        LaunchedEffect(entryId) {
                            if (entryId != null && entryId != "new") {
                                existing = entryRepository.getEntryById(entryId)
                            }
                        }

                        EditorScreen(
                            existingEntry = existing,
                            onCloseClicked = { navController.popBackStack() },
                            onSaveClicked = { title, body, mood ->
                                scope.launch {
                                    entryRepository.saveEntry(
                                        id = existing?.id,
                                        title = title,
                                        body = body,
                                        mood = mood
                                    )
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    // Placeholder untuk Fase 3B & Fase 4
                    composable("intruder_alert") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Layar 08: Peringatan Penyusup (Fase 3B)")
                        }
                    }
                    composable("calendar") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Layar 12: Kalender Mood (Fase 3B)")
                        }
                    }
                    composable("security") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Layar 09: Pusat Keamanan (Fase 4)")
                        }
                    }
                    composable("profile") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Layar 13: Profil (Fase 4)")
                        }
                    }
                }
            }
        }
    }
}