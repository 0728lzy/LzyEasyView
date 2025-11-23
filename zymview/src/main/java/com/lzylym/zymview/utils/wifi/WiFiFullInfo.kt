package com.lzylym.zymview.utils.wifi

data class WiFiFullInfo(
    val ssid: String,
    val bssid: String,
    val interfaceName: String?,
    val rssi: Int,
    val level: Int,
    val frequency: Int,
    val frequencyBand: String,
    val channel: Int,
    val linkSpeed: Int,
    val distanceMeters: Double,
    val bandwidth: Int,
    val frequencyRange: String,
    val wifiStandard: String,
    val ipAddress: String,
    val ipv6Address: String,
    val gateway: String,
    val subnetMask: String,
    val dns1: String,
    val dns2: String,
    val leaseDuration: Int,
    val capabilities: String,
    val securityType: String
)