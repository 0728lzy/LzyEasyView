package com.lzylym.zymview.utils.device

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import com.lzylym.zymview.utils.device.DeviceInfoHelper.getAppInfo
import com.lzylym.zymview.utils.device.DeviceInfoHelper.getDeviceInfo
import com.lzylym.zymview.utils.device.DeviceInfoHelper.getScreenInfo
import com.lzylym.zymview.utils.device.DeviceInfoHelper.getStorageInfo
import com.lzylym.zymview.utils.device.DeviceInfoHelper.getSystemInfo
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import javax.microedition.khronos.egl.EGL10

object DeviceInfoUtils {

    fun getCpuDetailInfo(context: Context): CpuDetailInfo {
        val info = CpuDetailInfo()

        info.cpuAbi = Build.CPU_ABI
        info.supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
        info.coreCount = Runtime.getRuntime().availableProcessors()
        info.cpuArchType = if (isCpu64Bit()) "64 Bit" else "32 Bit"
        if (!isValidCpuName(info.hardwareName)) {
            info.hardwareName = getSoCModelFromProps()
        }

        if (!isValidCpuName(info.productSeries)) {
            val soc = getSoCModelFromProps()
            if (isValidCpuName(soc)) {
                info.productSeries = soc
            } else {
                info.productSeries = Build.HARDWARE
            }
        }

        if (!isValidCpuName(info.hardwareName)) {
            if (Build.HARDWARE != "unknown" && Build.HARDWARE != "qcom") {
                info.hardwareName = Build.HARDWARE
            }
        }
        parseProcCpuInfo(info)
        readSysCpuInfo(info)
        info.isVulkanSupported = if (hasVulkanSupport(context)) "支持" else "不支持"

        return info
    }

    private fun getSoCModelFromProps(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (isValidCpuName(soc)) return soc
        }

        val propKeys = arrayOf(
            "ro.soc.model",
            "ro.board.platform",
            "ro.mediatek.platform",
            "ro.chipname",
            "ro.hardware.chipname",
            "ro.product.board"
        )
        for (key in propKeys) {
            val value = getSystemProperty(key)
            if (isValidCpuName(value)) {
                return value
            }
        }
        return "Unknown"
    }

    private fun isValidCpuName(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val n = name.trim().lowercase()
        return n != "unknown" &&
                n != "null" &&
                n != "qcom" &&
                n != "0"
    }

    private fun parseProcCpuInfo(info: CpuDetailInfo) {
        try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                val parts = l.split(":")
                if (parts.size < 2) continue

                val key = parts[0].trim()
                val value = parts[1].trim()

                when (key) {
                    "Processor" -> {
                        if (isValidCpuName(value)) info.productSeries = value
                    }
                    "Hardware" -> {
                        if (isValidCpuName(value)) info.hardwareName = value
                    }
                    "Features" -> info.features = value
                    "BogoMIPS" -> info.bogoMips = value
                    "CPU architecture" -> {
                        info.chipArch = if (value == "8") "ARMv64" else "ARMv$value"
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSystemProperty(key: String): String {
        var line = ""
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $key")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine() ?: ""
            p.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return line.trim()
    }

    private fun readSysCpuInfo(info: CpuDetailInfo) {
        val cpu0Path = "/sys/devices/system/cpu/cpu0/cpufreq"

        val minFreq = readFileContent("$cpu0Path/cpuinfo_min_freq").toLongOrNull()
        val maxFreq = readFileContent("$cpu0Path/cpuinfo_max_freq").toLongOrNull()
        if (minFreq != null && maxFreq != null) {
            info.freqRange = "${minFreq / 1000}MHz~${maxFreq / 1000}MHz"
        }

        val availFreqStr = readFileContent("$cpu0Path/scaling_available_frequencies")
        if (availFreqStr.isNotEmpty()) {
            val freqs = availFreqStr.split("\\s+".toRegex())
            val formattedFreqs = StringBuilder()
            for (f in freqs) {
                val k = f.toLongOrNull()
                if (k != null) {
                    if (formattedFreqs.isNotEmpty()) formattedFreqs.append(", ")
                    formattedFreqs.append("${k / 1000.0} MHz")
                }
            }
            info.availableFreqs = formattedFreqs.toString()
        }

        val gov = readFileContent("$cpu0Path/scaling_governor")
        if (gov.isNotEmpty()) info.currentGovernor = gov

        val availGov = readFileContent("$cpu0Path/scaling_available_governors")
        if (availGov.isNotEmpty()) info.supportedGovernors = availGov

        val driver = readFileContent("$cpu0Path/scaling_driver")
        if (driver.isNotEmpty()) info.machineType = driver
    }

    private fun hasVulkanSupport(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val pm = context.packageManager
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } else {
            false
        }
    }

    private fun isCpu64Bit(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.Process.is64Bit()
        } else {
            Build.CPU_ABI.contains("64")
        }
    }

    private fun readFileContent(path: String): String {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return ""
        return try {
            BufferedReader(FileReader(file)).use { br ->
                br.readLine() ?: ""
            }
        } catch (e: IOException) {
            ""
        }
    }

    fun CpuDetailInfo.printToLog() {
        val TAG = "CpuDetailLog"
        Log.d(TAG, "================= CPU 信息开始 =================")
        Log.d(TAG, "CPU硬件名称: $hardwareName")
        Log.d(TAG, "产品系列代号: $productSeries")
        Log.d(TAG, "核心数: $coreCount")
        Log.d(TAG, "频率范围: $freqRange")
        Log.d(TAG, "CPU 可靠平频率单位: $availableFreqs")
        Log.d(TAG, "CPU指令集(ABI): $cpuAbi")
        Log.d(TAG, "支持的ABI: $supportedAbis")
        Log.d(TAG, "芯片架构: $chipArch")
        Log.d(TAG, "CPU支持的功能: $features")
        Log.d(TAG, "当前调频策略: $currentGovernor")
        Log.d(TAG, "CPU支持的调频模型: $supportedGovernors")
        Log.d(TAG, "支持机器类型: $machineType")
        Log.d(TAG, "CPU 架型: $cpuArchType")
        Log.d(TAG, "是否支持Vulkan: $isVulkanSupported")
        Log.d(TAG, "BogoMips: $bogoMips")
        Log.d(TAG, "================= CPU 信息结束 =================")
    }

    fun getGpuDetailInfo(callback: (GpuDetailInfo) -> Unit) {
        Thread {
            val info = extractGpuInfoFromEGL()
            callback(info)
        }.start()
    }

    private fun extractGpuInfoFromEGL(): GpuDetailInfo {
        val TAG = "GpuUtils"
        val info = GpuDetailInfo()

        val egl = javax.microedition.khronos.egl.EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        if (display === EGL10.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed")
            return info
        }

        val version = IntArray(2)
        if (!egl.eglInitialize(display, version)) {
            Log.e(TAG, "eglInitialize failed")
            return info
        }

        val configAttribs = intArrayOf(
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_NONE
        )

        val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        egl.eglChooseConfig(display, configAttribs, configs, 1, numConfigs)

        if (numConfigs[0] == 0) {
            Log.e(TAG, "No config chosen")
            return info
        }
        val config = configs[0]

        val contextAttribs = intArrayOf(0x3098, 2, EGL10.EGL_NONE)
        val context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs)

        val surfaceAttribs = intArrayOf(EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE)
        val surface = egl.eglCreatePbufferSurface(display, config, surfaceAttribs)

        egl.eglMakeCurrent(display, surface, surface, context)

        try {
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION)
            val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)

            val maxTextureSize = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)

            info.renderer = renderer ?: "Unknown"
            info.vendor = vendor ?: "Unknown"
            info.version = glVersion ?: "Unknown"
            info.maxTextureSize = maxTextureSize[0]

            if (!extensions.isNullOrEmpty()) {
                info.extensions = extensions.replace(" ", "\n")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, surface)
            egl.eglDestroyContext(display, context)
            egl.eglTerminate(display)
        }

        return info
    }

    fun GpuDetailInfo.printToLog() {
        val TAG = "GpuUtils"
        Log.d(TAG, "================= GPU 信息开始 =================")
        Log.d(TAG, "GPU渲染器: $renderer")
        Log.d(TAG, "GPU供应商: $vendor")
        Log.d(TAG, "GPU版本: $version")
        Log.d(TAG, "最大纹理尺寸: $maxTextureSize")
        Log.d(TAG, "GPU扩展: $extensions")
        Log.d(TAG, "================= GPU 信息结束 =================")
    }

    /** 获取所有信息 */
    fun getGlobalDeviceInfo(context: Context): AllInfoBean {
        val device = getDeviceInfo()
        val system = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getSystemInfo() else null
        val screen = getScreenInfo(context)
        val storage = getStorageInfo()
        val app = getAppInfo(context)
        return AllInfoBean(deviceInfo = device, systemInfo = system, screenInfo = screen, storageInfo = storage, appInfo = app)
    }
}