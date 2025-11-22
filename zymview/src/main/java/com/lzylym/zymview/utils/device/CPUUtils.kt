package com.lzylym.zymview.utils.device

import android.content.Context

object CPUUtils {

    private var tempMonitor: CpuTempMonitor? = null
    private var usageMonitor: CpuUsageRateMonitor? = null

    /** ---------------- CPU 温度 ---------------- **/

    fun startCpuTemperatureMonitor(
        context: Context,
        times: Int = 1,
        intervalMs: Long = 1000,
        callback: (Float?) -> Unit
    ) {
        if (tempMonitor == null) tempMonitor = CpuTempMonitor(context)
        tempMonitor?.startMonitoring(times, intervalMs, callback)
    }

    fun stopCpuTemperatureMonitor() {
        tempMonitor?.stopMonitoring()
    }

    fun releaseCpuTemperatureMonitor() {
        tempMonitor?.release()
        tempMonitor = null
    }

    /** ---------------- CPU 使用率 ---------------- **/

    fun startCpuUsageRateMonitor(
        context: Context,
        intervalMs: Long = 1000,
        callback: (Float) -> Unit
    ) {
        if (usageMonitor == null) usageMonitor = CpuUsageRateMonitor(context)
        usageMonitor?.startMonitoring(intervalMs, callback)
    }

    fun stopCpuUsageRateMonitor() {
        usageMonitor?.stopMonitoring()
    }

    fun releaseCpuUsageRateMonitor() {
        usageMonitor?.release()
        usageMonitor = null
    }
}