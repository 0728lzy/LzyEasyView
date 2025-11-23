package com.lzylym.zymview.utils.wifi

import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.*
import java.text.DecimalFormat
import kotlin.math.max

class NetSpeedMonitor {

    private var monitorJob: Job? = null
    private val calculator = NetSpeedCalculator()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startMonitorSpeed(
        times: Int = -1,
        interval: Long = 1000L,
        onSpeedUpdate: (NetSpeedData) -> Unit
    ) {
        stop()
        calculator.reset()

        monitorJob = scope.launch {
            withContext(Dispatchers.IO) {
                calculator.initBaseline()
            }

            var remainingTimes = times

            while (isActive && (times == -1 || remainingTimes > 0)) {
                delay(interval)

                val speedData = withContext(Dispatchers.IO) {
                    calculator.calculate(interval)
                }

                onSpeedUpdate(speedData)

                if (times != -1) remainingTimes--
            }

            if (times != -1 && remainingTimes <= 0) {
                stop()
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

    private class NetSpeedCalculator {
        private var lastTotalRxBytes: Long = 0
        private var lastTotalTxBytes: Long = 0
        private var lastTime: Long = 0

        fun reset() {
            lastTotalRxBytes = 0
            lastTotalTxBytes = 0
            lastTime = 0
        }

        fun initBaseline() {
            lastTotalRxBytes = TrafficStats.getTotalRxBytes()
            lastTotalTxBytes = TrafficStats.getTotalTxBytes()
            lastTime = SystemClock.elapsedRealtime()
        }

        fun calculate(intervalMs: Long): NetSpeedData {
            val now = SystemClock.elapsedRealtime()
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()

            if (currentRx == TrafficStats.UNSUPPORTED.toLong() || lastTotalRxBytes == 0L) {
                initBaseline()
                return NetSpeedData(0f, 0f, "0 KB/s", "0 KB/s")
            }

            val timeDelta = now - lastTime
            if (timeDelta == 0L) return NetSpeedData(0f, 0f, "0 KB/s", "0 KB/s")

            val rxDelta = max(0L, currentRx - lastTotalRxBytes)
            val txDelta = max(0L, currentTx - lastTotalTxBytes)

            val downSpeedVal = (rxDelta * 1000f) / (1024f * timeDelta)
            val upSpeedVal = (txDelta * 1000f) / (1024f * timeDelta)

            lastTotalRxBytes = currentRx
            lastTotalTxBytes = currentTx
            lastTime = now

            return NetSpeedData(
                upSpeedVal = upSpeedVal,
                downSpeedVal = downSpeedVal,
                upSpeedStr = formatSpeed(upSpeedVal),
                downSpeedStr = formatSpeed(downSpeedVal)
            )
        }

        private fun formatSpeed(speedKb: Float): String {
            val df = DecimalFormat("#0.0")
            return if (speedKb >= 1024) {
                "${df.format(speedKb / 1024)} MB/s"
            } else {
                "${df.format(speedKb)} KB/s"
            }
        }
    }
}
