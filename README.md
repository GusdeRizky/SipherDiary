# SipherDiary 🔐

Aplikasi **diary rahasia** untuk Android. Semua catatan dienkripsi di HP sebelum disimpan, dan backup ke cloud juga terenkripsi. Server tidak pernah melihat isi catatan.

Project UAS **Keamanan Sistem Mobile**, Informatika Universitas Udayana, Kelompok 3: Dena, Jordan, Jofi, Kenny, Gusrisky.

---

## Fitur keamanan & materi kuliah

| Fitur | Implementasi | Materi |
|---|---|---|
| Persetujuan privasi | Disimpan versi & waktunya, dicatat ke Firestore | Kebijakan Keamanan (UU PDP No. 27/2022) |
| Login OTP SMS | Firebase Phone Auth | Otentikasi (OTP) |
| Login Google | Credential Manager + Firebase (OpenID Connect) | SSO / OAuth 2.0 / OIDC |
| Kunci aplikasi | PIN 6 digit di-hash **bcrypt (cost 12)**, biometrik `BIOMETRIC_STRONG` | MFA |
| PIN samaran | PIN kedua membuka diary kosong | Otentikasi |
| Deteksi penyusup | PIN salah 3× → foto kamera depan (CameraX), dienkripsi, kunci 5 menit | Otentikasi & deteksi |
| Enkripsi catatan | **AES-256-GCM**, kunci di **Android Keystore**, IV acak per enkripsi | Enkripsi & Keamanan Data |
| Anti-screenshot | `FLAG_SECURE` | Keamanan Data |
| Auto-lock | `ProcessLifecycleOwner`, batas waktu bisa diatur | Keamanan Data |
| Cek Wi-Fi sebelum backup | WPA3/WPA2/WPA/WEP/Open, sinyal dBm, IP lokal, uji TLS | Jaringan Mobile (logika NetworkMonitor) |
| HTTPS wajib | `network_security_config`: cleartext diblokir, hanya CA sistem | Jaringan Mobile |
| Backup terenkripsi | Kunci dari **kode pemulihan** (PBKDF2 310.000 iterasi), Firebase Storage + SHA-256 | Disaster Recovery (RPO 24 jam, RTO < 2 menit) |
| Backup otomatis | WorkManager setiap 24 jam, ditunda jika Wi-Fi tidak aman | Disaster Recovery |
| Hak data | Unduh data (JSON), hapus akun + backup + data lokal | UU PDP |
| Tanpa tracker | Tidak memakai Firebase Analytics / AdMob | Kebijakan Keamanan, skor MobSF |

---

## Menjalankan project

1. Buka folder ini di **Android Studio**, lalu tunggu *Gradle Sync* selesai.
2. Klik **Run ▶** (emulator atau HP dengan *USB debugging*).

Tanpa langkah Firebase di bawah, aplikasi berjalan dalam **mode demo**:
- OTP disimulasikan. Kode yang diterima adalah `123456`.
- Login Google disimulasikan.
- "Cloud" backup disimpan di folder internal aplikasi.

Semua fitur lain (enkripsi, PIN, biometrik, penyusup, Wi-Fi, kalender) berjalan normal.

---

## Mengaktifkan Firebase (mode asli)

1. Buka [Firebase Console](https://console.firebase.google.com), lalu buat project, misalnya `sipherdiary`. **Matikan Google Analytics** agar tidak ada tracker.
2. **Add app → Android**:
   - Package name: `com.example.sipherdiary`
   - SHA-1: jalankan di terminal Android Studio
     ```
     ./gradlew signingReport
     ```
     Salin nilai SHA-1 varian `debug`. SHA-1 ini dibutuhkan untuk OTP SMS dan Google Sign-In.
3. Unduh **`google-services.json`**, lalu taruh di folder **`app/`**. File ini sengaja ada di `.gitignore`, jadi bagikan lewat Drive kelompok.
4. **Authentication → Sign-in method**: aktifkan **Phone** dan **Google**.
   - Untuk demo tanpa SMS sungguhan: *Phone → Phone numbers for testing*, contoh `+62 812 0000 0000` dengan kode `123456`.
5. **Firestore Database**: buat database (mode production), lalu buka tab **Rules** dan tempel isi `firestore.rules`.
6. **Storage**: aktifkan, lalu buka tab **Rules** dan tempel isi `storage.rules`.
7. Setelah mengaktifkan Google, **unduh ulang** `google-services.json` agar `default_web_client_id` ikut ada.
8. Sync Gradle, lalu Run.

> Project ini **tidak** memakai plugin `com.google.gms.google-services`. `app/build.gradle.kts` membaca `google-services.json` sendiri dan mengubahnya menjadi string resource. Jika file tidak ada, aplikasi otomatis masuk mode demo.

### Struktur data cloud

```
Firestore
users/{uid}                 authProvider, consentVersion, consentAt, lastLoginAt
users/{uid}/devices/{id}    model, androidVersion, lastActiveAt
users/{uid}/backups/{id}    createdAt, storagePath, sizeBytes, sha256, network, formatVersion

Storage
backups/{uid}/{id}.enc      "SDB1" ‖ salt(16) ‖ IV(12) ‖ AES-256-GCM(JSON catatan)
```

Tidak ada judul, isi, maupun mood catatan yang tersimpan polos di cloud.

---

## Struktur kode

```
app/src/main/java/com/example/sipherdiary/
├── SipherApp.kt              Application + AppContainer (DI manual) + auto-lock
├── MainActivity.kt           Navigasi 13 layar + penjaga sesi
├── security/                 CryptoManager, PinManager, BiometricHelper, SessionManager,
│                             RecoveryKeyManager, NetworkSecurityChecker, IntruderCamera, SecurityScore
├── data/
│   ├── local/                Room (entries, intruder_logs) — kolom isi berupa BLOB terenkripsi
│   ├── prefs/SecurePrefs.kt  DataStore; nilai sensitif dienkripsi Keystore
│   ├── remote/               FirebaseGateway
│   └── repository/           Entry, Auth, Backup, Intruder
├── worker/BackupWorker.kt    Backup harian (WorkManager)
└── ui/                       consent, auth, setup, lock, home, editor, intruder,
                              security (Pusat Keamanan + Cek Wi-Fi), backup, calendar, profile
```

### Alur aplikasi

```
Persetujuan → Masuk (OTP/Google) → Buat PIN → Kode pemulihan (atau Pulihkan dari backup) → Beranda
Buka ulang → Kunci (PIN/sidik jari) ─ benar → Beranda
                                     ├ PIN samaran → Beranda kosong
                                     └ salah 3× → foto penyusup → Peringatan (kunci 5 menit)
Lupa PIN → masukkan kode pemulihan → buat PIN baru
```

---

## Evaluasi keamanan (UAS)

1. **Build → Generate Signed App Bundle / APK → APK → release**. Buat keystore `.jks`, tapi **jangan di-commit**.
2. Unggah APK ke **MobSF** (`mobsf.live` atau Docker lokal) untuk SAST, lalu jalankan Dynamic Analyzer di emulator untuk DAST.
3. Hal yang sudah diantisipasi:
   - `allowBackup=false`
   - cleartext diblokir
   - `debuggable=false`
   - hanya `MainActivity` yang di-export
   - AES/GCM dengan IV acak
   - tidak ada hardcoded secret
   - tidak ada tracker
   - log debug dihapus R8
   - `FLAG_SECURE`
4. Catatan: MobSF biasanya melaporkan API key Firebase di `google-services.json` sebagai *info*. Ini normal. Key Firebase bukan rahasia, dan keamanannya dijaga oleh **Security Rules** di atas.
