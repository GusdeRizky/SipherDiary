package com.example.sipherdiary.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.URL
import javax.net.ssl.HttpsURLConnection

enum class NetVerdict { AMAN, AMAN_SINYAL_LEMAH, TIDAK_AMAN, SELULER, TIDAK_ADA_KONEKSI }

data class NetCheckResult(
    val verdict: NetVerdict,
    val title: String,
    val reason: String,
    val connectionType: String,     // "Wi-Fi" / "Data seluler" / "-"
    val securityType: String,       // WPA3 / WPA2 / WPA / WEP / OPEN / UNKNOWN / -
    val signalDbm: Int?,            // null untuk seluler
    val localIp: String,
    val tlsInfo: String,            // hasil uji TLS ke server Google
    val tlsOk: Boolean
) {
    /** Backup boleh jalan? */
    val allowsBackup: Boolean
        get() = verdict == NetVerdict.AMAN || verdict == NetVerdict.SELULER ||
                verdict == NetVerdict.AMAN_SINYAL_LEMAH

    /** Label singkat untuk riwayat backup. */
    val shortLabel: String
        get() = if (connectionType == "Data seluler") "SELULER" else securityType
}

/** Satu jaringan Wi-Fi hasil pemindaian di sekitar. */
data class NearbyWifi(
    val ssid: String,
    val bssid: String,
    val securityType: String,   // WPA3 / WPA2 / WPA2-Enterprise / WPA / OWE / WEP / OPEN
    val rssi: Int,
    val band: String,           // 2.4 GHz / 5 GHz / 6 GHz
    val isConnected: Boolean,
    val verdict: NetVerdict
)

sealed class ScanOutcome {
    data class Success(val networks: List<NearbyWifi>) : ScanOutcome()
    data class Unavailable(val reason: String) : ScanOutcome()
}

/**
 * Cek keamanan jaringan sebelum backup. Mengikuti logika aplikasi NetworkMonitor dosen:
 * - WPA3 → aman
 * - WPA2 + sinyal > -70 dBm → aman; ≤ -70 dBm → aman tapi sinyal lemah
 * - WPA + IP lokal 192.168.x.x → cukup aman
 * - Terbuka / WEP → TIDAK aman, backup ditunda
 * Materi kuliah: Jaringan Mobile (Wi-Fi WPA2/WPA3, 4G/5G).
 */
class NetworkSecurityChecker(private val context: Context) {

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    suspend fun check(testTls: Boolean = true): NetCheckResult = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val link = network?.let { cm.getLinkProperties(it) }
        val ip = localIpv4(link)

        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return@withContext NetCheckResult(
                NetVerdict.TIDAK_ADA_KONEKSI, "Tidak ada koneksi", "Sambungkan ke internet terlebih dahulu",
                "-", "-", null, ip, "Tidak diuji", false
            )
        }

        val (tlsInfo, tlsOk) = if (testTls) testTls() else "Tidak diuji" to true

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return@withContext NetCheckResult(
                NetVerdict.SELULER, "Data seluler",
                "Jaringan 4G/5G dienkripsi oleh operator (EPS-AKA / 5G-AKA)",
                "Data seluler", "4G/5G", null, ip, tlsInfo, tlsOk
            )
        }

        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return@withContext NetCheckResult(
                NetVerdict.AMAN, "Jaringan lain", "Ethernet/VPN, lalu lintas tetap memakai TLS",
                "Lainnya", "-", null, ip, tlsInfo, tlsOk
            )
        }

        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val info: WifiInfo? = wifiInfo(caps, wifiManager)
        val rssi = info?.rssi ?: -100
        val security = securityType(wifiManager, info)

        val (verdict, title, reason) = when {
            security == "OPEN" || security == "WEP" ->
                Triple(NetVerdict.TIDAK_AMAN, "Wi-Fi tidak aman", "Jaringan terbuka/WEP mudah disadap. Backup ditunda.")
            security == "WPA3" ->
                Triple(NetVerdict.AMAN, "Wi-Fi aman", "Dilindungi WPA3 (SAE)")
            security == "WPA2" && rssi > -70 ->
                Triple(NetVerdict.AMAN, "Wi-Fi aman", "Dilindungi WPA2, sinyal kuat")
            security == "WPA2" ->
                Triple(NetVerdict.AMAN_SINYAL_LEMAH, "Aman, sinyal lemah", "WPA2 dengan sinyal $rssi dBm, koneksi bisa terputus")
            security == "WPA" && ip.startsWith("192.168.") ->
                Triple(NetVerdict.AMAN_SINYAL_LEMAH, "Cukup aman", "WPA lama di jaringan lokal")
            security == "UNKNOWN" && !hasLocationPermission() && Build.VERSION.SDK_INT < 31 ->
                Triple(NetVerdict.TIDAK_AMAN, "Tidak dapat diverifikasi", "Izinkan akses lokasi agar jenis keamanan Wi-Fi bisa dibaca")
            else ->
                Triple(NetVerdict.TIDAK_AMAN, "Keamanan tidak dikenali", "Jenis keamanan Wi-Fi tidak dapat dipastikan")
        }

        NetCheckResult(
            verdict, title, if (tlsOk) reason else "$reason · Uji TLS gagal",
            "Wi-Fi", security, rssi, ip, tlsInfo, tlsOk
        ).let { if (!tlsOk && it.verdict != NetVerdict.TIDAK_AMAN) it.copy(verdict = NetVerdict.TIDAK_AMAN) else it }
    }

    @Suppress("DEPRECATION")
    private fun wifiInfo(caps: NetworkCapabilities, wifiManager: WifiManager): WifiInfo? {
        if (Build.VERSION.SDK_INT >= 31) {
            (caps.transportInfo as? WifiInfo)?.let { return it }
        }
        return wifiManager.connectionInfo
    }

    private fun securityType(wifiManager: WifiManager, info: WifiInfo?): String {
        if (info == null) return "UNKNOWN"
        if (Build.VERSION.SDK_INT >= 31) {
            val fromInfo = when (info.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_SAE,
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> "WPA3"
                WifiInfo.SECURITY_TYPE_PSK,
                WifiInfo.SECURITY_TYPE_EAP -> "WPA2"
                WifiInfo.SECURITY_TYPE_WEP -> "WEP"
                WifiInfo.SECURITY_TYPE_OPEN,
                WifiInfo.SECURITY_TYPE_OWE -> "OPEN"
                else -> "UNKNOWN"
            }
            if (fromInfo != "UNKNOWN") return fromInfo
        }
        // Android 8–11: cocokkan BSSID dengan hasil scan (butuh izin lokasi)
        if (!hasLocationPermission()) return "UNKNOWN"
        @Suppress("DEPRECATION")
        val bssid = info.bssid ?: return "UNKNOWN"
        val caps = runCatching {
            wifiManager.scanResults.firstOrNull { it.BSSID.equals(bssid, ignoreCase = true) }?.capabilities
        }.getOrNull() ?: return "UNKNOWN"
        return when {
            "SAE" in caps || "WPA3" in caps -> "WPA3"
            "RSN" in caps || "WPA2" in caps -> "WPA2"
            "WPA" in caps -> "WPA"
            "WEP" in caps -> "WEP"
            else -> "OPEN"
        }
    }

    // ------------------------------------------------------------------ Wi-Fi di sekitar
    fun isLocationEnabled(): Boolean = runCatching {
        LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))
    }.getOrDefault(false)

    /**
     * Memindai semua Wi-Fi di sekitar dan menilai keamanannya dengan aturan yang sama.
     * Android mensyaratkan izin lokasi + Lokasi (GPS) aktif untuk membaca hasil scan,
     * dan membatasi scan baru maks. 4× per 2 menit (jika dibatasi, hasil terakhir dipakai).
     */
    @Suppress("DEPRECATION")
    suspend fun scanNearby(): ScanOutcome = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext ScanOutcome.Unavailable("Izin lokasi diperlukan untuk memindai Wi-Fi")
        if (!isLocationEnabled()) return@withContext ScanOutcome.Unavailable("Nyalakan Lokasi (GPS) di HP agar Wi-Fi sekitar bisa dibaca")
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        if (!wifiManager.isWifiEnabled) return@withContext ScanOutcome.Unavailable("Wi-Fi HP sedang mati")

        runCatching { wifiManager.startScan() }
        delay(3000)

        val connectedBssid = runCatching { wifiManager.connectionInfo?.bssid }.getOrNull()
        val results = runCatching { wifiManager.scanResults }.getOrNull().orEmpty()

        val networks = results
            .map { r ->
                val ssid = (if (Build.VERSION.SDK_INT >= 33) r.wifiSsid?.toString() else r.SSID)
                    ?.removeSurrounding("\"")?.takeIf { it.isNotBlank() } ?: "(SSID tersembunyi)"
                val security = securityFromCapabilities(r.capabilities.orEmpty())
                NearbyWifi(
                    ssid = ssid,
                    bssid = r.BSSID ?: "",
                    securityType = security,
                    rssi = r.level,
                    band = when {
                        r.frequency >= 5925 -> "6 GHz"
                        r.frequency >= 4900 -> "5 GHz"
                        else -> "2.4 GHz"
                    },
                    isConnected = connectedBssid != null && r.BSSID.equals(connectedBssid, ignoreCase = true),
                    verdict = classify(security, r.level)
                )
            }
            // Satu SSID bisa punya banyak access point: ambil yang tersambung / sinyal terkuat
            .groupBy { it.ssid }
            .map { (_, aps) -> aps.firstOrNull { it.isConnected } ?: aps.maxBy { it.rssi } }
            .sortedWith(compareByDescending<NearbyWifi> { it.isConnected }.thenByDescending { it.rssi })

        ScanOutcome.Success(networks)
    }

    private fun securityFromCapabilities(caps: String): String = when {
        "SAE" in caps || "WPA3" in caps -> "WPA3"
        "EAP" in caps && ("RSN" in caps || "WPA2" in caps) -> "WPA2-Enterprise"
        "RSN" in caps || "WPA2" in caps -> "WPA2"
        "WPA" in caps -> "WPA"
        "OWE" in caps -> "OWE"
        "WEP" in caps -> "WEP"
        else -> "OPEN"
    }

    /** Aturan penilaian yang sama dengan jaringan yang tersambung. */
    private fun classify(security: String, rssi: Int): NetVerdict = when (security) {
        "OPEN", "WEP" -> NetVerdict.TIDAK_AMAN
        "WPA3", "WPA2-Enterprise" -> NetVerdict.AMAN
        "WPA2" -> if (rssi > -70) NetVerdict.AMAN else NetVerdict.AMAN_SINYAL_LEMAH
        else -> NetVerdict.AMAN_SINYAL_LEMAH // WPA lama / OWE: terenkripsi tapi lemah
    }

    private fun localIpv4(link: LinkProperties?): String =
        link?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address }?.hostAddress ?: "-"

    /** Uji handshake TLS ke server Google, tampilkan cipher suite yang dipakai. */
    private fun testTls(): Pair<String, Boolean> = runCatching {
        val conn = URL("https://www.gstatic.com/generate_204").openConnection() as HttpsURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.instanceFollowRedirects = false
        try {
            conn.responseCode
            val suite = conn.cipherSuite ?: "TLS"
            "TLS OK · ${suite.removePrefix("TLS_")}" to true
        } finally {
            conn.disconnect()
        }
    }.getOrElse { "TLS gagal: ${it.javaClass.simpleName}" to false }
}
