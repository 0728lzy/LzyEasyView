package com.lzylym.zymview.utils.wifi

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*

class SignalStrengthMonitor {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var executor: SignalExecutor

    fun startMonitorSignal(
        context: Context,
        interval: Long = 1000L,
        onSignalUpdate: (SignalData) -> Unit
    ) {
        stop()
        executor = SignalExecutor(context.applicationContext)
        monitorJob = scope.launch {
            while (isActive) {
                val signalData = withContext(Dispatchers.IO) {
                    executor.getSignalStrength()
                }
                onSignalUpdate(signalData)
                delay(interval)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    data class SignalData(
        val rssi: Int,
        val level: Int,
        val isConnected: Boolean,
        val displayStr: String
    )

    private class SignalExecutor(private val context: Context) {

        fun getSignalStrength(): SignalData {
            var rssi = -127
            var level = 0
            var isConnected = false

            try {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null && wifiManager.isWifiEnabled) {
                    val info = wifiManager.connectionInfo
                    rssi = info.rssi
                    if (rssi > -127 && info.bssid != null && info.networkId != -1) {
                        isConnected = true
                        level = WifiManager.calculateSignalLevel(rssi, 5)
                    } else {
                        rssi = -127
                        isConnected = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return SignalData(
                rssi = rssi,
                level = level,
                isConnected = isConnected,
                displayStr = if (isConnected) "$rssi dBm" else "No Signal"
            )
        }
    }
}
