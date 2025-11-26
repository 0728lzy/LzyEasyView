package com.lzylym.zymview.utils.wifi

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.DecimalFormat

class PingMonitor {

    private var monitorJob: Job? = null
    private val executor = PingExecutor()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startMonitorPing(
        host: String,
        times: Int = -1,
        interval: Long = 1000L,
        onPingUpdate: (PingData) -> Unit
    ) {
        stop()

        monitorJob = scope.launch {
            var remainingTimes = times

            while (isActive && (times == -1 || remainingTimes > 0)) {
                val pingData = withContext(Dispatchers.IO) {
                    executor.executePing(host)
                }

                onPingUpdate(pingData)

                if (times != -1) remainingTimes--

                if (isActive && (times == -1 || remainingTimes > 0)) {
                    delay(interval)
                }
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

    private class PingExecutor {

        private val timePattern = "time=([\\d.]+)".toRegex()
        private val df = DecimalFormat("#0.0")

        fun executePing(host: String): PingData {
            var delay = -1f
            var isSuccess = false
            var process: Process? = null
            var reader: BufferedReader? = null

            try {
                val command = "ping -c 1 -w 2 $host"
                process = Runtime.getRuntime().exec(command)

                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line != null && line!!.contains("time=")) {
                            val matchResult = timePattern.find(line!!)
                            if (matchResult != null) {
                                val timeStr = matchResult.groupValues[1]
                                delay = timeStr.toFloatOrNull() ?: 0f
                                isSuccess = true
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isSuccess = false
            } finally {
                try {
                    reader?.close()
                    process?.destroy()
                } catch (e: Exception) {}
            }

            return PingData(
                delayMs = delay,
                isSuccess = isSuccess,
                host = host,
                displayStr = if (isSuccess) "${df.format(delay)} ms" else "Timeout"
            )
        }
    }
}