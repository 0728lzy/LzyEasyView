package com.lzylym.zymview.utils.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

class CpuTempMonitor(context: Context) {

    private val appContext: Context = context.applicationContext
    private var cachedFilePath: File? = null
    private var cachedMethod: TempMethod? = null
    private val isMonitoring = AtomicBoolean(false)
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var callback: ((Float?) -> Unit)? = null

    fun startMonitoring(times: Int = -1, intervalMs: Long = 1000, callback: (Float?) -> Unit) {
        if (isMonitoring.get()) return
        this.callback = callback
        isMonitoring.set(true)
        monitorJob = scope.launch {
            var left = times
            while (isActive && isMonitoring.get() && (times == -1 || left > 0)) {
                val temp = withContext(Dispatchers.IO) { detectTemp() }
                callback(temp)
                if (times != -1) left--
                if (left == 0) break
                delay(intervalMs)
            }
            stopMonitoring()
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

    enum class TempMethod { FILE_PATH, SHELL, BATTERY }

    private fun detectTemp(): Float? {
        cachedFilePath?.let {
            val t = readFileValue(it)
            if (isValidTemp(t)) return t
            cachedFilePath = null
        }

        if (cachedMethod == TempMethod.SHELL) {
            val t = tryExecShell()
            if (isValidTemp(t)) return t
            cachedMethod = null
        }

        val zones = File("/sys/class/thermal").listFiles()

        // 1.精准CPU
        zones?.forEach { z ->
            val type = readFileContent(File(z, "type")).lowercase()
            if (type.contains("cpu")) {
                val tempFile = File(z, "temp")
                val t = readFileValue(tempFile)
                if (isValidTemp(t)) {
                    cachedFilePath = tempFile
                    cachedMethod = TempMethod.FILE_PATH
                    return t
                }
            }
        }

        // 2.精准SOC/AP
        zones?.forEach { z ->
            val type = readFileContent(File(z, "type")).lowercase()
            if (type.contains("soc") || type.contains("mtktscpu") || type.contains("ap")) {
                val tempFile = File(z, "temp")
                val t = readFileValue(tempFile)
                if (isValidTemp(t)) {
                    cachedFilePath = tempFile
                    cachedMethod = TempMethod.FILE_PATH
                    return t
                }
            }
        }

        // 3.精准Hwmon
        val mons = File("/sys/class/hwmon").listFiles()
        mons?.forEach { m ->
            val inputs = m.listFiles { _, name -> name.startsWith("temp") && name.endsWith("_input") }
            inputs?.forEach { f ->
                val t = readFileValue(f)
                if (isValidTemp(t)) {
                    cachedFilePath = f
                    cachedMethod = TempMethod.FILE_PATH
                    return t
                }
            }
        }

        // 4.模糊Thermal-zone最大值
        var best = Float.NaN
        zones?.forEach { z ->
            val t = readFileValue(File(z, "temp"))
            if (isValidTemp(t) && (best.isNaN() || t > best)) best = t
        }
        if (!best.isNaN()) return best

        // 5.不可靠Shell
        val shellTemp = tryExecShell()
        if (isValidTemp(shellTemp)) {
            cachedMethod = TempMethod.SHELL
            return shellTemp
        }

        // 6.保底电池温度
        return readBatteryTemp()
    }

    private fun readFileContent(file: File): String = try {
        if (file.exists()) file.readText().trim() else ""
    } catch (e: Exception) { "" }

    private fun readFileValue(file: File): Float {
        return try {
            val s = readFileContent(file)
            if (s.isEmpty()) return Float.NaN
            var v = s.toFloatOrNull() ?: return Float.NaN
            if (v > 1000) v /= 1000f
            v
        } catch (e: Exception) { Float.NaN }
    }

    private fun isValidTemp(t: Float?): Boolean = t != null && !t.isNaN() && t > 0 && t < 150

    private fun tryExecShell(): Float? {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat /sys/class/thermal/thermal_zone*/temp"))
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var best = Float.NaN
            var line: String? = br.readLine()
            while (line != null) {
                val t = line.trim().toFloatOrNull()
                if (t != null) {
                    val v = if (t > 1000) t / 1000f else t
                    if (isValidTemp(v) && (best.isNaN() || v > best)) best = v
                }
                line = br.readLine()
            }
            br.close()
            if (isValidTemp(best)) best else null
        } catch (e: Exception) { null } finally { process?.destroy() }
    }

    private fun readBatteryTemp(): Float? {
        return try {
            val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: return null
            if (temp > 0) temp / 10f else null
        } catch (e: Exception) { null }
    }
}