package com.lzylym.zymview.utils.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.DhcpInfo
import android.net.LinkProperties
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.net.ConnectivityManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.net.Inet6Address
import java.net.NetworkInterface
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object WiFiUtils {

    @RequiresApi(Build.VERSION_CODES.M)
    fun getCurrentWiFiInfo(context: Context): WiFiFullInfo? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val wifiInfo: WifiInfo = wifiManager.connectionInfo ?: return null
        val dhcpInfo: DhcpInfo = wifiManager.dhcpInfo

        if (wifiInfo.bssid == null || wifiInfo.networkId == -1) {
            return null
        }

        val ssid = wifiInfo.ssid.replace("\"", "")
        val bssid = wifiInfo.bssid
        val interfaceName = getInterfaceName()

        val rssi = wifiInfo.rssi
        val frequency = wifiInfo.frequency
        val frequencyBand = getFrequencyBand(frequency)
        val channel = getChannelFromFrequency(frequency)
        val linkSpeed = wifiInfo.linkSpeed

        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wifiManager.calculateSignalLevel(rssi)
        } else {
            WifiManager.calculateSignalLevel(rssi, 5)
        }
        val distance = calculateDistance(rssi, frequency)

        var capabilities = "Unknown"
        var bandwidth = 20

        try {
            val scanResults = wifiManager.scanResults
            val match = scanResults.find { it.BSSID == bssid }
            if (match != null) {
                capabilities = match.capabilities
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val securityType = getSecurityType(capabilities)

        val halfWidth = bandwidth / 2
        val rangeStart = frequency - halfWidth
        val rangeEnd = frequency + halfWidth
        val frequencyRange = "$rangeStart - $rangeEnd"

        var standard = "Unknown/Legacy"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            standard = when (wifiInfo.wifiStandard) {
                ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
                ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
                ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7 (802.11be)"
                else -> "Unknown"
            }
        }

        val ipAddr = intToIp(dhcpInfo.ipAddress)
        val ipv6Addr = getIPv6Address(interfaceName)
        val gateway = intToIp(dhcpInfo.gateway)

        val subnetMask = getRealSubnetMask(context) ?: "0.0.0.0"

        val dns1 = intToIp(dhcpInfo.dns1)
        val dns2 = intToIp(dhcpInfo.dns2)
        val leaseTime = dhcpInfo.leaseDuration / 60

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

    private fun getSecurityType(caps: String): String {
        val upper = caps.uppercase()
        return when {
            upper.contains("EAP") -> "EAP"
            upper.contains("PSK") || upper.contains("SAE") -> "WPA/WPA2-PSK"
            upper.contains("WEP") -> "WEP"
            upper.contains("WPA") || upper.contains("RSN") -> "WPA/WPA2"
            else -> "Open"
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRealSubnetMask(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return null
        val lp: LinkProperties = cm.getLinkProperties(nw) ?: return null

        for (la in lp.linkAddresses) {
            val prefix = la.prefixLength
            return prefixToMask(prefix)
        }
        return null
    }

    private fun prefixToMask(prefix: Int): String {
        val mask = (-1 shl (32 - prefix))
        return "${mask shr 24 and 0xff}.${mask shr 16 and 0xff}.${mask shr 8 and 0xff}.${mask and 0xff}"
    }

    private fun getFrequencyBand(freq: Int): String {
        return when {
            freq in 2400..2500 -> "2.4 GHz"
            freq in 4900..5925 -> "5 GHz"
            freq in 5925..7125 -> "6 GHz"
            freq > 50000 -> "60 GHz"
            else -> "Unknown"
        }
    }

    private fun intToIp(i: Int): String {
        return if (i == 0) "0.0.0.0"
        else "${i and 0xFF}.${i shr 8 and 0xFF}.${i shr 16 and 0xFF}.${i shr 24 and 0xFF}"
    }

    private fun getIPv6Address(interfaceName: String?): String {
        if (interfaceName == null) return "Unavailable"
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
            "Unavailable"
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getChannelFromFrequency(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq < 2484 -> (freq - 2407) / 5
            freq in 4910..4980 -> (freq - 4000) / 5
            freq < 5935 -> (freq - 5000) / 5
            freq >= 5935 -> (freq - 5940) / 5
            else -> 0
        }
    }

    private fun getInterfaceName(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.contains("wlan")) return intf.name
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateDistance(rssi: Int, freqMHz: Int): Double {
        val exp = (27.55 - (20 * log10(freqMHz.toDouble())) + abs(rssi)) / 20.0
        return String.format("%.1f", 10.0.pow(exp)).toDouble()
    }
}