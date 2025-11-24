package com.lzylym.zymview.utils.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.DhcpInfo
import android.net.LinkProperties
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.net.Inet6Address
import java.net.NetworkInterface
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object WiFiUtils {

    private val EMPTY_INFO = WiFiFullInfo(
        ssid = "",
        bssid = "",
        interfaceName = "",
        rssi = 0,
        level = 0,
        frequency = 0,
        frequencyBand = "",
        channel = 0,
        linkSpeed = 0,
        distanceMeters = 0.0,
        bandwidth = 0,
        frequencyRange = "",
        wifiStandard = "",
        ipAddress = "",
        ipv6Address = "",
        gateway = "",
        subnetMask = "",
        dns1 = "",
        dns2 = "",
        leaseDuration = 0,
        capabilities = "",
        securityType = ""
    )

    @RequiresApi(Build.VERSION_CODES.M)
    fun getCurrentWiFiInfo(context: Context): WiFiFullInfo {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return EMPTY_INFO

        val hasLocationPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val wifiInfo: WifiInfo? = try { wifiManager.connectionInfo } catch (_: Exception) { null }
        val dhcpInfo: DhcpInfo? = try { wifiManager.dhcpInfo } catch (_: Exception) { null }

        if (wifiInfo == null && dhcpInfo == null) return EMPTY_INFO

        val rawSsid = try { wifiInfo?.ssid } catch (_: Exception) { null }
        val ssid = when {
            rawSsid == null -> ""
            rawSsid == "<unknown ssid>" -> ""
            else -> rawSsid.replace("\"", "")
        }

        val bssid = try { wifiInfo?.bssid ?: "" } catch (_: Exception) { "" }
        val interfaceName = getInterfaceName() ?: ""
        val rssi = try { wifiInfo?.rssi ?: 0 } catch (_: Exception) { 0 }
        val frequency = try { wifiInfo?.frequency ?: 0 } catch (_: Exception) { 0 }
        val frequencyBand = if (frequency > 0) getFrequencyBand(frequency) else ""
        val channel = if (frequency > 0) getChannelFromFrequency(frequency) else 0
        val linkSpeed = try { wifiInfo?.linkSpeed ?: 0 } catch (_: Exception) { 0 }

        val level = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
                try { wifiManager.calculateSignalLevel(rssi) } catch (_: Exception) { WifiManager.calculateSignalLevel(rssi, 5) }
            } else {
                WifiManager.calculateSignalLevel(rssi, 5)
            }
        } catch (_: Exception) { 0 }

        val distance = calculateDistance(rssi, frequency)

        var capabilities = ""
        var bandwidth = 20
        try {
            val scanResults = wifiManager.scanResults
            val match = scanResults?.find { it.BSSID.equals(bssid, ignoreCase = true) }
            if (match != null) {
                capabilities = match.capabilities ?: ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bandwidth = when (match.channelWidth) {
                        ScanResult.CHANNEL_WIDTH_20MHZ -> 20
                        ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                        ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                        ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                        5 -> 320
                        else -> 20
                    }
                }
            }
        } catch (_: Exception) { capabilities = "" }
        val securityType = getSecurityType(context)
        val halfWidth = if (bandwidth > 0 && frequency > 0) bandwidth / 2 else 0
        val frequencyRange = if (halfWidth > 0 && frequency > 0) "${frequency - halfWidth} - ${frequency + halfWidth}" else ""

        var standard = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
            try {
                standard = when (wifiInfo.wifiStandard) {
                    ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
                    ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                    ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                    ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
                    ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7 (802.11be)"
                    else -> ""
                }
            } catch (_: Exception) { standard = "" }
        }

        val ipAddr = safeIp(intToIp(dhcpInfo?.ipAddress ?: 0))
        val ipv6Addr = safeIp(getIPv6Address(interfaceName))
        val gateway = safeIp(intToIp(dhcpInfo?.gateway ?: 0))
        val dns1 = safeIp(intToIp(dhcpInfo?.dns1 ?: 0))
        val dns2 = safeIp(intToIp(dhcpInfo?.dns2 ?: 0))
        val subnetMask = safeIp(getRealSubnetMask(context))
        val leaseTime = try { (dhcpInfo?.leaseDuration ?: 0) / 60 } catch (_: Exception) { 0 }

        return WiFiFullInfo(
            ssid = ssid,
            bssid = bssid,
            interfaceName = interfaceName,
            rssi = rssi,
            level = level,
            frequency = frequency,
            frequencyBand = frequencyBand,
            channel = channel,
            linkSpeed = linkSpeed,
            distanceMeters = distance,
            bandwidth = bandwidth,
            frequencyRange = frequencyRange,
            wifiStandard = standard,
            ipAddress = ipAddr,
            ipv6Address = ipv6Addr,
            gateway = gateway,
            subnetMask = subnetMask,
            dns1 = dns1,
            dns2 = dns2,
            leaseDuration = leaseTime,
            capabilities = capabilities,
            securityType = securityType
        )
    }

    private fun safeIp(ip: String?): String {
        return if (ip == null || ip == "0.0.0.0" || ip == "Unavailable") "0.0.0.0" else ip
    }

    @SuppressLint("MissingPermission")
    fun getSecurityType(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (wifiInfo.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_OPEN -> "Open"
                WifiInfo.SECURITY_TYPE_WEP -> "WEP"
                WifiInfo.SECURITY_TYPE_PSK -> "WPA/WPA2-PSK"
                WifiInfo.SECURITY_TYPE_EAP -> "EAP"
                WifiInfo.SECURITY_TYPE_SAE -> "WPA3-SAE"
                WifiInfo.SECURITY_TYPE_OWE -> "OWE"
                WifiInfo.SECURITY_TYPE_WAPI_PSK -> "WAPI-PSK"
                WifiInfo.SECURITY_TYPE_WAPI_CERT -> "WAPI-CERT"
                else -> "Unknown"
            }
        } else {
            try {
                val scanResults = wifiManager.scanResults
                val bssid = wifiInfo.bssid ?: return "Unknown"
                val result = scanResults.find { it.BSSID == bssid } ?: return "Unknown"
                val caps = result.capabilities.uppercase()
                when {
                    caps.contains("WEP") -> "WEP"
                    caps.contains("PSK") -> "WPA/WPA2-PSK"
                    caps.contains("SAE") -> "WPA3-SAE"
                    caps.contains("EAP") -> "EAP"
                    caps.contains("OWE") -> "OWE"
                    else -> "Open"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRealSubnetMask(context: Context): String? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return null
            val lp: LinkProperties = cm.getLinkProperties(nw) ?: return null
            for (la in lp.linkAddresses) return prefixToMask(la.prefixLength)
            null
        } catch (e: Exception) { null }
    }

    private fun prefixToMask(prefix: Int): String {
        return try {
            val mask = (-1 shl (32 - prefix))
            "${mask shr 24 and 0xff}.${mask shr 16 and 0xff}.${mask shr 8 and 0xff}.${mask and 0xff}"
        } catch (_: Exception) { "" }
    }

    private fun getFrequencyBand(freq: Int): String {
        return when {
            freq in 2400..2500 -> "2.4 GHz"
            freq in 4900..5925 -> "5 GHz"
            freq in 5925..7125 -> "6 GHz"
            freq > 50000 -> "60 GHz"
            else -> ""
        }
    }

    private fun intToIp(i: Int): String {
        return if (i == 0) "0.0.0.0" else "${i and 0xFF}.${i shr 8 and 0xFF}.${i shr 16 and 0xFF}.${i shr 24 and 0xFF}"
    }

    private fun getIPv6Address(interfaceName: String?): String {
        if (interfaceName == null) return ""
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.equals(interfaceName, ignoreCase = true)) {
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet6Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress
                            val i = host.indexOf('%')
                            return if (i < 0) host else host.substring(0, i)
                        }
                    }
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    private fun getInterfaceName(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.contains("wlan")) return intf.name
            }
            null
        } catch (e: Exception) { null }
    }

    private fun calculateDistance(rssi: Int, freqMHz: Int): Double {
        if (rssi == 0 || freqMHz == 0) return 0.0
        val exp = (27.55 - (20 * log10(freqMHz.toDouble())) + abs(rssi)) / 20.0
        return String.format("%.1f", 10.0.pow(exp)).toDouble()
    }

    private fun getChannelFromFrequency(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2407) / 5
            freq in 4910..4980 -> (freq - 4000) / 5
            freq in 5000..5935 -> (freq - 5000) / 5
            freq >= 5935 -> (freq - 5940) / 5
            else -> 0
        }
    }

    @SuppressLint("MissingPermission")
    fun getNearbyWiFiList(context: Context): List<WiFiFullInfo> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return emptyList()

        val hasLocationPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val scanResults: List<ScanResult> = try {
            wifiManager.scanResults ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val list = mutableListOf<WiFiFullInfo>()
        for (result in scanResults) {
            val ssid = try { result.SSID ?: "" } catch (_: Exception) { "" }
            val bssid = try { result.BSSID ?: "" } catch (_: Exception) { "" }
            val frequency = try { result.frequency } catch (_: Exception) { 0 }
            val channel = if (frequency > 0) getChannelFromFrequency(frequency) else 0
            val frequencyBand = if (frequency > 0) getFrequencyBand(frequency) else ""
            val capabilities = try { result.capabilities ?: "" } catch (_: Exception) { "" }
            val securityType = parseSecurityTypeFromCapabilities(capabilities)

            val level = try { WifiManager.calculateSignalLevel(result.level, 5) } catch (_: Exception) { 0 }
            val distance = calculateDistance(result.level, frequency)
            val bandwidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    when (result.channelWidth) {
                        ScanResult.CHANNEL_WIDTH_20MHZ -> 20
                        ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                        ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                        ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                        5 -> 320
                        else -> 20
                    }
                } catch (_: Exception) { 20 }
            } else 20

            val frequencyRange = if (frequency > 0) "${frequency - bandwidth / 2} - ${frequency + bandwidth / 2}" else ""
            val wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    when (result.wifiStandard) {
                        ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
                        ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                        ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                        ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
                        ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7 (802.11be)"
                        else -> ""
                    }
                } catch (_: Exception) { "" }
            } else ""

            list.add(
                WiFiFullInfo(
                    ssid = ssid,
                    bssid = bssid,
                    interfaceName = "",
                    rssi = result.level,
                    level = level,
                    frequency = frequency,
                    frequencyBand = frequencyBand,
                    channel = channel,
                    linkSpeed = 0,
                    distanceMeters = distance,
                    bandwidth = bandwidth,
                    frequencyRange = frequencyRange,
                    wifiStandard = wifiStandard,
                    ipAddress = "",
                    ipv6Address = "",
                    gateway = "",
                    subnetMask = "",
                    dns1 = "",
                    dns2 = "",
                    leaseDuration = 0,
                    capabilities = capabilities,
                    securityType = securityType
                )
            )
        }

        return list
    }

    private fun parseSecurityTypeFromCapabilities(caps: String): String {
        val upper = caps.uppercase()
        return when {
            upper.contains("WEP") -> "WEP"
            upper.contains("PSK") -> "WPA/WPA2-PSK"
            upper.contains("SAE") -> "WPA3-SAE"
            upper.contains("EAP") -> "EAP"
            upper.contains("OWE") -> "OWE"
            else -> "Open"
        }
    }

    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var networkType = "NONE"
        if (connectivityManager != null) {
            networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectivityManager.activeNetwork
                val actNw = connectivityManager.getNetworkCapabilities(nw)
                when {
                    actNw == null -> "NONE"
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                    else -> "UNKNOWN"
                }
            } else {
                val nwInfo = connectivityManager.activeNetworkInfo
                when {
                    nwInfo == null || !nwInfo.isConnected -> "NONE"
                    nwInfo.type == ConnectivityManager.TYPE_WIFI -> "WIFI"
                    nwInfo.type == ConnectivityManager.TYPE_MOBILE -> "MOBILE"
                    else -> "UNKNOWN"
                }
            }
        }
        return networkType
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }

    fun isAirplaneModeOn(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            } else {
                Settings.System.getInt(context.contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0
            }
        } catch (_: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isWifiConnectedWifi(context: Context, ssid:String, bssid:String):Boolean{
        val current= getCurrentWiFiInfo(context)
        return ssid==current.ssid&&bssid==current.bssid
    }
}