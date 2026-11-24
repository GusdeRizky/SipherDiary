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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
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
            return when (info.currentSecurityType) {
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
