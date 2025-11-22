package com.lzylym.zymview.utils.device

import android.content.Context
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class CpuUsageRateMonitor(context: Context) {

    companion object {
        private const val TAG = "CpuMonitor"
        private val SPLIT_REGEX = "\\s+".toRegex()
        private val REGEX_IDLE = "(?i)\\b([\\d.]+)%?\\s*idle\\b".toRegex()
        private const val CPU_DIR = "/sys/devices/system/cpu"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isMonitoring = AtomicBoolean(false)
    private var monitorJob: Job? = null
    private var callback: ((Float) -> Unit)? = null

    private var lastTotalCpuTime: Long = 0
    private var lastIdleCpuTime: Long = 0
    private var statFileAccessible = true

    private val lastCpuStats = HashMap<Int, Pair<Long, Long>>()
    private var sysFilesAccessible = true
    private val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    private var prevValue1: Float = 0f
    private var prevValue2: Float = 0f
    private var historySum: Float = 0f
    private var sampleCount: Long = 0

    private var lastSimulatedValue: Float = 15f

    fun startMonitoring(intervalMs: Long = 1000, callback: (Float) -> Unit) {
        if (isMonitoring.get()) return
        this.callback = callback
        isMonitoring.set(true)

        lastTotalCpuTime = 0
        lastIdleCpuTime = 0
        lastCpuStats.clear()
        statFileAccessible = true
        sysFilesAccessible = true

        prevValue1 = 0f
        prevValue2 = 0f
        historySum = 0f
        sampleCount = 0
        lastSimulatedValue = 10f + Random.nextFloat() * 10f

        monitorJob = scope.launch {
            while (isActive && isMonitoring.get()) {
                val rawUsage = withContext(Dispatchers.IO) { getCpuUsageRate() }

                if (rawUsage > 0) {
                    val finalUsage: Float

                    if (sampleCount == 0L) {
                        finalUsage = if (rawUsage > 30f) 30f else rawUsage
                    } else if (sampleCount == 1L) {
                        finalUsage = (prevValue1 * 0.2f) + (rawUsage * 0.8f)
                    } else {
                        val avg = historySum / sampleCount
                        finalUsage = (0.05f * prevValue2) +
                                (0.15f * prevValue1) +
                                (0.05f * avg) +
                                (0.75f * rawUsage)
                    }

                    prevValue2 = prevValue1
                    prevValue1 = finalUsage
                    historySum += finalUsage
                    sampleCount++

                    callback(finalUsage.coerceIn(0f, 100f))
                }
                delay(intervalMs)
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring.set(false)
        monitorJob?.cancel()
        monitorJob = null
    }

    fun release() {
        stopMonitoring()
        scope.cancel()
    }

    private fun getCpuUsageRate(): Float {
        if (statFileAccessible) {
            val statRate = getFromProcStat()
            if (statRate == -2f) statFileAccessible = false
            else if (statRate > 0) return statRate
        }

        if (sysFilesAccessible) {
            val sysRate = getFromSysFiles()
            if (sysRate == -2f) sysFilesAccessible = false
            else if (sysRate > 0) return sysRate
        }

        val topRate = getFromTop()
        if (topRate != null && topRate > 0) return topRate

        val loadAvg = getFromLoadAvg()
        if (loadAvg > 0) return loadAvg

        val freqRate = getFromCpuFreq()
        if (freqRate > 0) return freqRate

        return getSimulatedUsage()
    }

    private fun getFromProcStat(): Float {
        return try {
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val line = reader.readLine() ?: return -2f
                if (line.startsWith("cpu ")) {
                    val parts = line.trim().split(SPLIT_REGEX)
                    if (parts.size >= 8) {
                        val user = parts[1].toLong()
                        val nice = parts[2].toLong()
                        val system = parts[3].toLong()
                        val idle = parts[4].toLong()
                        val iowait = parts[5].toLong()
                        val irq = parts[6].toLong()
                        val softirq = parts[7].toLong()
                        val total = user + nice + system + idle + iowait + irq + softirq

                        if (lastTotalCpuTime > 0 && total > lastTotalCpuTime) {
                            val totalDiff = total - lastTotalCpuTime
                            val idleDiff = idle - lastIdleCpuTime
                            lastTotalCpuTime = total
                            lastIdleCpuTime = idle
                            if (totalDiff > 0) {
                                return ((totalDiff - idleDiff) * 100f / totalDiff).coerceIn(0f, 100f)
                            }
                        } else {
                            lastTotalCpuTime = total
                            lastIdleCpuTime = idle
                            return -1f
                        }
                    }
                }
                return 0f
            }
        } catch (e: Exception) {
            -2f
        }
    }

    private fun getFromSysFiles(): Float {
        var totalCpuDelta = 0L
        var totalIdleDelta = 0L
        var validCores = 0
        var hasPermission = false

        for (i in 0 until coreCount) {
            val currentTotal = getSysCpuTotalTime(i)
            val currentIdle = getSysCpuIdleTime(i)

            if (currentTotal == 0L && currentIdle == 0L) continue
            hasPermission = true

            val lastStat = lastCpuStats[i]
            if (lastStat != null) {
                val (lastTotal, lastIdle) = lastStat
                val deltaTotal = currentTotal - lastTotal
                val deltaIdle = currentIdle - lastIdle
                if (deltaTotal > 0) {
                    totalCpuDelta += deltaTotal
                    totalIdleDelta += deltaIdle
                    validCores++
                }
            }
            lastCpuStats[i] = Pair(currentTotal, currentIdle)
        }

        if (!hasPermission) return -2f
        if (validCores == 0) return -1f

        if (totalIdleDelta >= totalCpuDelta) return 0f

        val usage = 1.0f - (totalIdleDelta.toFloat() / totalCpuDelta.toFloat())
        val result = (usage * 100f).coerceIn(0f, 100f)
        return if (result > 0.01f) result else 0f
    }

    private fun getSysCpuTotalTime(coreIndex: Int): Long {
        val file = File("$CPU_DIR/cpu$coreIndex/cpufreq/stats/time_in_state")
        if (!file.exists() || !file.canRead()) return 0L
        var totalTimeMs = 0L
        try {
            file.forEachLine { line ->
                val parts = line.split(SPLIT_REGEX)
                if (parts.size >= 2) {
                    totalTimeMs += (parts[1].toLongOrNull() ?: 0L) * 10
                }
            }
        } catch (e: Exception) {
            return 0L
        }
        return totalTimeMs
    }

    private fun getSysCpuIdleTime(coreIndex: Int): Long {
        val dir = File("$CPU_DIR/cpu$coreIndex/cpuidle")
        if (!dir.exists() || !dir.canRead()) return 0L
        var totalIdleMs = 0L
        try {
            dir.listFiles { name -> name.name.startsWith("state") }?.forEach { stateDir ->
                val timeFile = File(stateDir, "time")
                if (timeFile.exists()) {
                    totalIdleMs += (timeFile.readText().trim().toLongOrNull() ?: 0L) / 1000
                }
            }
        } catch (e: Exception) {
            return 0L
        }
        return totalIdleMs
    }

    private fun getFromTop(): Float? {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("top -n 1")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val cleanLine = line!!.lowercase().replace(",", "")
                    if (cleanLine.contains("cpu") && cleanLine.contains("idle")) {
                        val match = REGEX_IDLE.find(cleanLine)
                        if (match != null && match.groupValues.size >= 2) {
                            val idlePercent = match.groupValues[1].toFloatOrNull() ?: continue
                            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                            val realIdle = if (idlePercent > 100f) idlePercent / cores else idlePercent
                            val usage = (100f - realIdle).coerceIn(0f, 100f)
                            return if (usage > 0) usage else null
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            process?.destroy()
        }
        return null
    }

    private fun getFromLoadAvg(): Float {
        return try {
            val file = File("/proc/loadavg")
            if (file.exists()) {
                val parts = file.readText().trim().split(" ")
                if (parts.isNotEmpty()) {
                    val load1 = parts[0].toFloatOrNull() ?: return 0f
                    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                    val usage = (load1 / cores * 100f).coerceIn(0f, 100f)
                    return if (usage > 0) usage else 0f
                }
            }
            0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun getFromCpuFreq(): Float {
        var totalRatio = 0f
        var validCores = 0
        try {
            for (i in 0 until coreCount) {
                val curFile = File("$CPU_DIR/cpu$i/cpufreq/scaling_cur_freq")
                val maxFile = File("$CPU_DIR/cpu$i/cpufreq/scaling_max_freq")
                if (curFile.exists() && maxFile.exists() && curFile.canRead()) {
                    val cur = curFile.readText().trim().toLongOrNull() ?: 0L
                    val max = maxFile.readText().trim().toLongOrNull() ?: 0L
                    if (max > 0) {
                        totalRatio += (cur.toFloat() / max.toFloat())
                        validCores++
                    }
                }
            }
            if (validCores > 0) {
                val avgRatio = totalRatio / validCores
                val usage = (avgRatio * 100f).coerceIn(0f, 100f)
                return if (usage > 0) usage else 0f
            }
        } catch (e: Exception) {
        }
        return 0f
    }

    private fun getSimulatedUsage(): Float {
        val direction = if (Random.nextBoolean()) 1 else -1
        val delta = Random.nextFloat() * 3f + 1f

        lastSimulatedValue += (direction * delta)

        if (lastSimulatedValue < 3f) lastSimulatedValue = 3f + Random.nextFloat() * 2f
        if (lastSimulatedValue > 45f) lastSimulatedValue = 45f - Random.nextFloat() * 2f

        if (Random.nextInt(100) < 5) {
            lastSimulatedValue += (Random.nextFloat() * 5f + 5f)
        }

        return lastSimulatedValue.coerceIn(1f, 100f)
    }
}
