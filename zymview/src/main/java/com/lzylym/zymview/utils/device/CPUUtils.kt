package com.lzylym.zymview.utils.device

import android.content.Context

object CPUUtils {

    private var monitor: CpuTempMonitor? = null

    fun startCpuTemperatureMonitor(
        context: Context,
        times: Int = 1,
        intervalMs: Long = 1000,
        callback: (Float?) -> Unit
    ) {
        if (monitor == null) monitor = CpuTempMonitor(context)
        monitor?.startMonitoring(times, intervalMs, callback)
    }

    fun stopCpuTemperatureMonitor() {
        monitor?.stopMonitoring()
    }

    fun releaseCpuTemperatureMonitor() {
        monitor?.release()
        monitor = null
    }
}