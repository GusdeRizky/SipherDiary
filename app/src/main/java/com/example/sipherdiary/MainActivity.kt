package com.example.sipherdiary

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sipherdiary.data.repository.AccountInfo
import com.example.sipherdiary.data.repository.BackupMeta
import com.example.sipherdiary.data.repository.BackupResult
import com.example.sipherdiary.data.repository.DeviceInfo
import com.example.sipherdiary.data.repository.DiaryEntry
import com.example.sipherdiary.data.repository.OtpSendResult
import com.example.sipherdiary.security.BiometricHelper
import com.example.sipherdiary.security.IntruderCamera
import com.example.sipherdiary.security.NetCheckResult
import com.example.sipherdiary.security.PinResult
import com.example.sipherdiary.security.RecoveryKeyManager
import com.example.sipherdiary.security.SecurityState
import com.example.sipherdiary.security.SessionManager
import com.example.sipherdiary.security.SessionState
import com.example.sipherdiary.ui.auth.LoginScreen
import com.example.sipherdiary.ui.auth.OtpScreen
import com.example.sipherdiary.ui.backup.BackupScreen
import com.example.sipherdiary.ui.backup.RecoveryCodeScreen
import com.example.sipherdiary.ui.backup.RestoreDialog
import com.example.sipherdiary.ui.calendar.CalendarScreen
import com.example.sipherdiary.ui.consent.ConsentScreen
import com.example.sipherdiary.ui.editor.EditorScreen
import com.example.sipherdiary.ui.home.HomeScreen
import com.example.sipherdiary.ui.intruder.IntruderAlertScreen
import com.example.sipherdiary.ui.lock.LockScreen
import com.example.sipherdiary.ui.profile.ProfileScreen
import com.example.sipherdiary.ui.security.SecurityCenterScreen
import com.example.sipherdiary.ui.security.WifiCheckScreen
import com.example.sipherdiary.ui.setup.CreatePinScreen
import com.example.sipherdiary.ui.theme.SipherColor
import com.example.sipherdiary.ui.theme.SipherDiaryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Layar yang hanya boleh dibuka saat sesi TIDAK terkunci. */
private val PROTECTED_ROUTES = setOf(
    "home", "editor/{entryId}", "calendar", "security", "backup",
    "wifi_check/{forBackup}", "intruder_log", "profile"
)

class MainActivity : FragmentActivity() {

    private val container by lazy { (application as SipherApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Anti-screenshot aktif sejak awal, lalu mengikuti pengaturan di Pusat Keamanan
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.securePrefs.antiScreenshotFlow.collect { secure ->
                    if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            SipherDiaryTheme {
                SipherNavHost()
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /** Navigasi sambil membersihkan seluruh back stack (setelah login, unlock, logout). */
    private fun NavHostController.navigateClearing(route: String) {
        navigate(route) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    private fun NavHostController.navigateTab(route: String) {
        navigate(route) {
            popUpTo("home")
            launchSingleTop = true
        }
    }

    /** Menentukan layar pertama berdasarkan status: persetujuan → akun → PIN → kode pemulihan → kunci. */
    private suspend fun resolveStart(): String {
        val prefs = container.securePrefs
        return when {
            prefs.getConsentVersion() == null -> "consent"
            !container.authRepository.isSignedIn() -> "login"
            !container.pinManager.hasPin() -> "create_pin"
            !prefs.hasRecoveryKey() -> "recovery"
            else -> "lock"
        }
    }

    /** Setelah login berhasil (OTP/Google). */
    private suspend fun afterSignIn(nav: NavHostController) {
        val next = when {
            !container.pinManager.hasPin() -> "create_pin"
            !container.securePrefs.hasRecoveryKey() -> "recovery"
            else -> "lock"
        }
        nav.navigateClearing(next)
    }

    @Composable
    private fun SipherNavHost() {
        val nav = rememberNavController()
        val scope = rememberCoroutineScope()
        val prefs = container.securePrefs

        var startRoute by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) { startRoute = resolveStart() }

        val entries by container.entryRepository.observeEntries().collectAsState(initial = emptyList())
        val biometricOn by prefs.biometricFlow.collectAsState(initial = false)
        val session by SessionManager.state.collectAsState()
        val backStack by nav.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route

        // Penjaga sesi: jika terkunci (auto-lock) saat berada di layar sensitif → ke layar kunci
        LaunchedEffect(session, currentRoute) {
            if (session == SessionState.Locked && currentRoute in PROTECTED_ROUTES) {
                nav.navigateClearing("lock")
            }
        }

        val account by produceState<AccountInfo?>(initialValue = null, session, currentRoute) {
            value = container.authRepository.account()
        }

        val start = startRoute
        if (start == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SipherColor.Blue500)
            }
            return
        }

        var pendingPhone by remember { mutableStateOf("") }

        NavHost(navController = nav, startDestination = start) {

            // 01. Persetujuan Privasi (UU PDP)
            composable("consent") {
                ConsentScreen(onAgreeClicked = {
                    scope.launch {
                        prefs.saveConsent("1.0")
                        nav.navigateClearing("login")
                    }
                })
            }

            // 02. Masuk (OTP SMS / Google SSO)
            composable("login") {
                LoginScreen(
                    onSendOtpClicked = { phone ->
                        pendingPhone = phone
                        scope.launch {
                            if (container.authRepository.isDemoMode) {
                                toast("Mode demo: gunakan kode OTP 123456")
                            }
                            when (val r = container.authRepository.sendOtp(this@MainActivity, phone)) {
                                OtpSendResult.CodeSent -> nav.navigate("otp")
                                OtpSendResult.AutoVerified -> {
                                    container.authRepository.completeAutoVerified()
                                    afterSignIn(nav)
                                }
                                is OtpSendResult.Failed -> toast(r.message)
                            }
                        }
                    },
                    onGoogleSignInClicked = {
                        scope.launch {
                            val error = container.authRepository.signInWithGoogle(this@MainActivity)
                            if (error == null) afterSignIn(nav) else toast(error)
                        }
                    }
                )
            }

            // 03. Verifikasi OTP
            composable("otp") {
                OtpScreen(
                    phoneNumber = pendingPhone,
                    onBackClicked = { nav.popBackStack() },
                    onVerifyClicked = { code ->
                        scope.launch {
                            val error = container.authRepository.verifyOtp(code)
                            if (error == null) afterSignIn(nav) else toast(error)
                        }
                    },
                    onResendClicked = {
                        scope.launch {
                            val r = container.authRepository.sendOtp(this@MainActivity, pendingPhone, resend = true)
                            if (r is OtpSendResult.Failed) toast(r.message) else toast("Kode OTP dikirim ulang")
                        }
                    }
                )
            }

            // 04. Buat PIN & Biometrik
            composable("create_pin") {
                CreatePinScreen(
                    pinManager = container.pinManager,
                    canUseBiometric = BiometricHelper.canAuthenticate(this@MainActivity),
                    onPinSaved = { pin, biometricEnabled, decoyPin ->
                        scope.launch {
                            container.pinManager.setPin(pin)
                            container.pinManager.setDecoyPin(decoyPin)
                            prefs.setBiometricEnabled(biometricEnabled)
                            if (prefs.hasRecoveryKey()) {
                                // Reset PIN lewat "Lupa PIN": kode pemulihan sudah ada
                                SessionManager.unlock()
                                nav.navigateClearing("home")
                            } else {
                                nav.navigateClearing("recovery")
                            }
                        }
                    }
                )
            }

            // 04b. Simpan kode pemulihan / pulihkan dari backup (HP baru)
            composable("recovery") {
                var hasCloudBackup by remember { mutableStateOf<Boolean?>(null) }
                var code by remember { mutableStateOf<String?>(null) }
                var working by remember { mutableStateOf(false) }
                var restoreError by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    hasCloudBackup = container.backupRepository.listBackups().isNotEmpty() &&
                            container.entryRepository.count() == 0
                }

                val hasBackup = hasCloudBackup
                if (hasBackup == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SipherColor.Blue500)
                    }
                } else {
                    RecoveryCodeScreen(
                        hasCloudBackup = hasBackup,
                        generatedCode = code,
                        isWorking = working,
                        restoreError = restoreError,
                        onGenerate = { scope.launch { code = container.backupRepository.createRecoveryCode() } },
                        onRestore = { input ->
                            scope.launch {
                                working = true
                                restoreError = null
                                container.backupRepository.restoreLatest(input)
                                    .onSuccess { count ->
                                        toast("$count catatan berhasil dipulihkan")
                                        SessionManager.unlock()
                                        nav.navigateClearing("home")
                                    }
                                    .onFailure { restoreError = it.message ?: "Pemulihan gagal" }
                                working = false
                            }
                        },
                        onDone = {
                            SessionManager.unlock()
                            scope.launch { container.authRepository.registerDevice() }
                            nav.navigateClearing("home")
                        }
                    )
                }
            }

            // 05. Kunci Aplikasi
            composable("lock") {
                var showForgot by remember { mutableStateOf(false) }
                var forgotError by remember { mutableStateOf<String?>(null) }
                var checking by remember { mutableStateOf(false) }

                val unlock: (SessionState) -> Unit = { state ->
                    if (state == SessionState.DecoyUnlocked) SessionManager.unlockDecoy() else SessionManager.unlock()
                    scope.launch { container.authRepository.registerDevice() }
                    nav.navigateClearing("home")
                }

                LockScreen(
                    userName = account?.displayName?.substringBefore(" ") ?: "",
                    pinManager = container.pinManager,
                    biometricEnabled = biometricOn,
                    onBiometricClicked = {
                        BiometricHelper.showPrompt(
                            activity = this@MainActivity,
                            onSuccess = { unlock(SessionState.Unlocked) }
                        )
                    },
                    onUnlocked = { unlock(SessionState.Unlocked) },
                    onDecoyUnlocked = {
                        toast("Mode samaran aktif")
                        unlock(SessionState.DecoyUnlocked)
                    },
                    onIntruderDetected = { isNew ->
                        scope.launch {
                            if (isNew) {
                                val photo = if (prefs.isIntruderPhoto()) {
                                    IntruderCamera.capture(this@MainActivity, this@MainActivity)
                                } else null
                                container.intruderRepository.record(3, photo)
                            }
                            nav.navigate("intruder_alert") { launchSingleTop = true }
                        }
                    },
                    onForgotPinClicked = {
                        forgotError = null
                        showForgot = true
                    }
                )

                if (showForgot) {
                    // Reset PIN hanya dengan kode pemulihan, agar pencuri HP tidak bisa sekadar login OTP ulang
                    RestoreDialog(
                        isRestoring = checking,
                        error = forgotError,
                        onDismiss = { showForgot = false },
                        onConfirm = { input ->
                            scope.launch {
                                checking = true
                                val ok = withContext(Dispatchers.Default) {
                                    val salt = prefs.getBackupSalt()
                                    val stored = prefs.getBackupKey()
                                    salt != null && stored != null &&
                                            RecoveryKeyManager.deriveKey(input, salt).contentEquals(stored)
                                }
                                checking = false
                                if (ok) {
                                    showForgot = false
                                    toast("Kode benar. Silakan buat PIN baru.")
                                    nav.navigate("create_pin")
                                } else {
                                    forgotError = "Kode pemulihan salah"
                                }
                            }
                        }
                    )
                }
            }

            // 08. Peringatan Penyusup (setelah PIN salah 3×)
            composable("intruder_alert") {
                val logs by container.intruderRepository.observeLogs().collectAsState(initial = emptyList())
                var lockUntil by remember { mutableLongStateOf(0L) }
                LaunchedEffect(Unit) { lockUntil = prefs.getLockUntil() }
                IntruderAlertScreen(
                    logs = logs,
                    lockUntilMillis = lockUntil,
                    loadPhoto = { container.intruderRepository.loadPhoto(it) },
                    isHistoryMode = false,
                    onBack = { nav.popBackStack() },
                    onTryAgain = { nav.navigateClearing("lock") },
                    onClearLogs = {}
                )
            }

            // 08b. Riwayat penyusup (dibuka pemilik dari Pusat Keamanan)
            composable("intruder_log") {
                val logs by container.intruderRepository.observeLogs().collectAsState(initial = emptyList())
                DisposableEffect(Unit) {
                    onDispose { scope.launch { container.intruderRepository.markAllSeen() } }
                }
                IntruderAlertScreen(
                    logs = logs,
                    lockUntilMillis = 0L,
                    loadPhoto = { container.intruderRepository.loadPhoto(it) },
                    isHistoryMode = true,
                    onBack = { nav.popBackStack() },
                    onTryAgain = {},
                    onClearLogs = { scope.launch { container.intruderRepository.wipe() } }
                )
            }

            // 06. Beranda
            composable("home") {
                val unseen by container.database.intruderDao().observeUnseenCount().collectAsState(initial = 0)
                LaunchedEffect(unseen) {
                    if (unseen > 0 && session == SessionState.Unlocked) {
                        toast("⚠ Ada $unseen percobaan masuk gagal. Cek Pusat Keamanan.")
                    }
                }
                HomeScreen(
                    entries = entries,
                    userInitial = (account?.displayName?.firstOrNull { it.isLetter() } ?: 'S').uppercase(),
                    onOpenEditor = { id -> nav.navigate(if (id != null) "editor/$id" else "editor/new") },
                    onNavigateTab = { nav.navigateTab(it) }
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
                    if (entryId != null && entryId != "new") existing = container.entryRepository.getEntryById(entryId)
                }
                EditorScreen(
                    existingEntry = existing,
                    onCloseClicked = { nav.popBackStack() },
                    onSaveClicked = { title, body, mood ->
                        scope.launch {
                            if (session == SessionState.DecoyUnlocked) {
                                toast("Tersimpan") // mode samaran: tidak benar-benar disimpan
                            } else {
                                container.entryRepository.saveEntry(id = existing?.id, title = title, body = body, mood = mood)
                            }
                            nav.popBackStack()
                        }
                    }
                )
            }

            // 12. Kalender
            composable("calendar") {
                CalendarScreen(
                    entries = entries,
                    onOpenEntry = { nav.navigate("editor/$it") },
                    onWriteForToday = { nav.navigate("editor/new") },
                    onNavigateTab = { nav.navigateTab(it) }
                )
            }

            // 09. Pusat Keamanan
            composable("security") { SecurityRoute(nav) }

            // 11. Backup & Pemulihan
            composable("backup") {
                var backups by remember { mutableStateOf<List<BackupMeta>>(emptyList()) }
                var loading by remember { mutableStateOf(true) }
                var restoring by remember { mutableStateOf(false) }
                var restoreError by remember { mutableStateOf<String?>(null) }
                val lastAt by prefs.lastBackupAtFlow.collectAsState(initial = 0L)
                val lastStatus by prefs.lastBackupStatusFlow.collectAsState(initial = null)

                LaunchedEffect(Unit) {
                    backups = container.backupRepository.listBackups()
                    loading = false
                }
                BackupScreen(
                    backups = backups,
                    isLoading = loading,
                    lastBackupAt = lastAt,
                    lastStatus = lastStatus,
                    entryCount = entries.size,
                    isDemoMode = container.backupRepository.isDemoMode,
                    isRestoring = restoring,
                    restoreError = restoreError,
                    onBackupNow = { nav.navigate("wifi_check/true") },
                    onRestore = { code ->
                        scope.launch {
                            restoring = true
                            restoreError = null
                            container.backupRepository.restoreLatest(code)
                                .onSuccess { toast("$it catatan dipulihkan") }
                                .onFailure { restoreError = it.message ?: "Pemulihan gagal" }
                            restoring = false
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            // 10. Cek Wi-Fi sebelum backup
            composable(
                route = "wifi_check/{forBackup}",
                arguments = listOf(navArgument("forBackup") { type = NavType.BoolType })
            ) { backStackEntry ->
                val forBackup = backStackEntry.arguments?.getBoolean("forBackup") ?: false
                var result by remember { mutableStateOf<NetCheckResult?>(null) }
                var checking by remember { mutableStateOf(true) }
                var backingUp by remember { mutableStateOf(false) }
                var runKey by remember { mutableIntStateOf(0) }

                LaunchedEffect(runKey) {
                    checking = true
                    result = container.networkChecker.check()
                    checking = false
                    if (forBackup && result?.allowsBackup == false) container.backupRepository.markDeferred()
                }
                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { runKey++ }

                val needsLocation = !container.networkChecker.hasLocationPermission() &&
                        (Build.VERSION.SDK_INT < 31 || result?.securityType == "UNKNOWN")

                WifiCheckScreen(
                    result = result,
                    isChecking = checking,
                    forBackup = forBackup,
                    isBackingUp = backingUp,
                    needsLocationPermission = needsLocation,
                    onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    onRecheck = { runKey++ },
                    onProceedBackup = {
                        val net = result ?: return@WifiCheckScreen
                        scope.launch {
                            backingUp = true
                            when (val r = container.backupRepository.backupNow(net.shortLabel)) {
                                is BackupResult.Success -> {
                                    toast("Backup berhasil: ${r.entryCount} catatan terenkripsi")
                                    nav.popBackStack()
                                }
                                is BackupResult.Failed -> toast(r.message)
                            }
                            backingUp = false
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            // 13. Profil
            composable("profile") { ProfileRoute(nav, entries.size) }
        }
    }

    @Composable
    private fun SecurityRoute(nav: NavHostController) {
        val prefs = container.securePrefs
        val scope = rememberCoroutineScope()
        val biometric by prefs.biometricFlow.collectAsState(initial = false)
        val antiScreenshot by prefs.antiScreenshotFlow.collectAsState(initial = true)
        val intruderPhoto by prefs.intruderPhotoFlow.collectAsState(initial = true)
        val autoLock by prefs.autoLockFlow.collectAsState(initial = 1)
        val lastBackupAt by prefs.lastBackupAtFlow.collectAsState(initial = 0L)
        val unseen by container.database.intruderDao().observeUnseenCount().collectAsState(initial = 0)
        var hasRecovery by remember { mutableStateOf(false) }
        var cameraGranted by remember { mutableStateOf(IntruderCamera.hasPermission(this)) }
        LaunchedEffect(Unit) { hasRecovery = prefs.hasRecoveryKey() }

        // Perbarui status izin kamera saat kembali ke layar
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) cameraGranted = IntruderCamera.hasPermission(this@MainActivity)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraGranted = granted
            scope.launch { prefs.setIntruderPhoto(granted) }
            if (!granted) toast("Izin kamera ditolak, foto penyusup nonaktif")
        }

        val lastBackupText = if (lastBackupAt > 0) {
            val hours = (System.currentTimeMillis() - lastBackupAt) / 3_600_000
            "Terakhir " + (if (hours < 1) "kurang dari 1 jam lalu" else "$hours jam lalu") +
                    " · " + SimpleDateFormat("d MMM HH.mm", Locale.forLanguageTag("id-ID")).format(Date(lastBackupAt))
        } else "Belum pernah backup"

        SecurityCenterScreen(
            state = SecurityState(
                biometricEnabled = biometric,
                autoLockMinutes = autoLock,
                antiScreenshot = antiScreenshot,
                intruderPhoto = intruderPhoto,
                cameraPermission = cameraGranted,
                hasRecoveryKey = hasRecovery,
                lastBackupAt = lastBackupAt
            ),
            canUseBiometric = BiometricHelper.canAuthenticate(this),
            unseenIntruders = unseen,
            lastBackupText = lastBackupText,
            onBiometricChange = { enable ->
                if (enable) {
                    // Konfirmasi dengan sidik jari sebelum diaktifkan
                    BiometricHelper.showPrompt(this, onSuccess = { scope.launch { prefs.setBiometricEnabled(true) } })
                } else scope.launch { prefs.setBiometricEnabled(false) }
            },
            onAntiScreenshotChange = { scope.launch { prefs.setAntiScreenshot(it) } },
            onIntruderPhotoChange = { enable ->
                if (enable && !cameraGranted) cameraLauncher.launch(Manifest.permission.CAMERA)
                else scope.launch { prefs.setIntruderPhoto(enable) }
            },
            onAutoLockChange = { scope.launch { prefs.setAutoLockMinutes(it) } },
            onOpenBackup = { nav.navigate("backup") },
            onOpenWifiCheck = { nav.navigate("wifi_check/false") },
            onOpenIntruderLog = { nav.navigate("intruder_log") },
            onNavigateTab = { nav.navigateTab(it) }
        )
    }

    @Composable
    private fun ProfileRoute(nav: NavHostController, entryCount: Int) {
        val scope = rememberCoroutineScope()
        var account by remember { mutableStateOf<AccountInfo?>(null) }
        var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
        var busy by remember { mutableStateOf<String?>(null) }
        var pinError by remember { mutableStateOf<String?>(null) }
        var reloadKey by remember { mutableIntStateOf(0) }

        LaunchedEffect(reloadKey) {
            account = container.authRepository.account()
            devices = container.authRepository.devices()
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    val json = container.entryRepository.exportJson().toString(2)
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    }
                }.onSuccess { toast("Data diekspor. File ini TIDAK terenkripsi, simpan baik-baik.") }
                    .onFailure { toast("Gagal mengekspor: ${it.message}") }
            }
        }

        ProfileScreen(
            account = account,
            devices = devices,
            entryCount = entryCount,
            busyMessage = busy,
            pinError = pinError,
            onChangePin = { old, new ->
                scope.launch {
                    pinError = null
                    when (val r = container.pinManager.verify(old)) {
                        PinResult.Ok -> {
                            if (container.pinManager.isWeakPin(new)) {
                                pinError = "PIN baru terlalu mudah ditebak"
                            } else {
                                container.pinManager.setPin(new)
                                pinError = "" // sukses
                                toast("PIN berhasil diganti")
                            }
                        }
                        is PinResult.Wrong -> pinError = "PIN lama salah (sisa ${r.remainingAttempts} percobaan)"
                        else -> {
                            pinError = "Terlalu banyak percobaan"
                            SessionManager.lock()
                        }
                    }
                }
            },
            onExportData = {
                val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportLauncher.launch("sipherdiary-export-$date.json")
            },
            onRemoveDevice = { id ->
                scope.launch {
                    container.authRepository.removeDevice(id)
                    reloadKey++
                }
            },
            onSignOut = {
                scope.launch {
                    container.authRepository.signOut()
                    SessionManager.lock()
                    nav.navigateClearing("login")
                }
            },
            onDeleteAccount = { pin ->
                scope.launch {
                    pinError = null
                    if (container.pinManager.verify(pin) != PinResult.Ok) {
                        pinError = "PIN salah"
                        return@launch
                    }
                    busy = "Menghapus backup cloud…"
                    container.backupRepository.deleteAllBackups()
                    busy = "Menghapus akun…"
                    val cloudError = container.authRepository.deleteCloudAccount()
                    if (cloudError != null) {
                        busy = null
                        pinError = cloudError
                        return@launch
                    }
                    busy = "Menghapus data di HP ini…"
                    container.authRepository.signOut()
                    (application as SipherApp).wipeLocal()
                    busy = null
                    toast("Akun dan semua data telah dihapus")
                    nav.navigateClearing("consent")
                }
            },
            onNavigateTab = { nav.navigateTab(it) }
        )
    }
}
