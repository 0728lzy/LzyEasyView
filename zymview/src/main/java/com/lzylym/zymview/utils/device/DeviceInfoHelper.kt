package com.lzylym.zymview.utils.device

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*

/** 用于显示的条目 */
data class InfoItem(
    val name: String,  // 中文描述
    val value: String  // 值
)

/** 设备信息 */
data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val product: String,
    val hardware: String,
    val board: String,
    val deviceId: String,
    val androidVersion: String,
    val sdkInt: Int
) {
    fun toInfoItems(): List<InfoItem> = listOf(
        InfoItem("设备制造商", manufacturer),
        InfoItem("品牌", brand),
        InfoItem("型号", model),
        InfoItem("产品名", product),
        InfoItem("硬件", hardware),
        InfoItem("主板", board),
        InfoItem("设备ID", deviceId),
        InfoItem("安卓版本", androidVersion),
        InfoItem("SDK版本", sdkInt.toString())
    )
}

/** 系统信息 */
data class SystemInfo(
    val systemStartTime: String,
    val timeZone: String,
    val locale: String,
    val securityPatch: String,
    val cpuCores: Int,
    val cpuAbi: String,
    val supportedAbis: String
) {
    fun toInfoItems(): List<InfoItem> = listOf(
        InfoItem("系统启动时间", systemStartTime),
        InfoItem("时区", timeZone),
        InfoItem("语言和地区", locale),
        InfoItem("安全补丁级别", securityPatch),
        InfoItem("处理器核心数", cpuCores.toString()),
        InfoItem("CPU 架构", cpuAbi),
        InfoItem("支持的 ABI 列表", supportedAbis)
    )
}

/** 屏幕信息 */
data class ScreenInfo(
    val resolution: String,
    val densityDpi: Int
) {
    fun toInfoItems(): List<InfoItem> = listOf(
        InfoItem("分辨率", resolution),
        InfoItem("屏幕密度 (dpi)", densityDpi.toString())
    )
}

/** 存储信息 */
data class StorageInfo(
    val totalInternal: String,
    val availableInternal: String
) {
    fun toInfoItems(): List<InfoItem> = listOf(
        InfoItem("内部存储总空间", totalInternal),
        InfoItem("内部存储可用空间", availableInternal)
    )
}

/** 应用信息 */
data class AppInfo(
    val versionName: String,
    val versionCode: Int
) {
    fun toInfoItems(): List<InfoItem> = listOf(
        InfoItem("版本名称", versionName),
        InfoItem("版本号", versionCode.toString())
    )
}

/** 所有信息统一 Bean */
data class AllInfoBean(
    val deviceInfo: DeviceInfo,
    val systemInfo: SystemInfo?,
    val screenInfo: ScreenInfo,
    val storageInfo: StorageInfo,
    val appInfo: AppInfo
) {
    /** 转成 InfoItem 列表 */
    fun toInfoItemList(): List<InfoItem> {
        val list = mutableListOf<InfoItem>()
        list.addAll(deviceInfo.toInfoItems())
        systemInfo?.let { list.addAll(it.toInfoItems()) }
        list.addAll(screenInfo.toInfoItems())
        list.addAll(storageInfo.toInfoItems())
        list.addAll(appInfo.toInfoItems())
        return list
    }
}

/** -------------------- 获取信息 Helper -------------------- */
object DeviceInfoHelper {

    /** 获取设备信息 */
    fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        product = Build.PRODUCT,
        hardware = Build.HARDWARE,
        board = Build.BOARD,
        deviceId = Build.ID,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT
    )

    /** 获取系统信息 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getSystemInfo(): SystemInfo {
        val systemStartTime = Date(System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime())
        val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
        val formattedStartTime = formatter.format(systemStartTime)
        return SystemInfo(
            systemStartTime = formattedStartTime,
            timeZone = TimeZone.getDefault().id,
            locale = Locale.getDefault().toString(),
            securityPatch = Build.VERSION.SECURITY_PATCH,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuAbi = Build.CPU_ABI,
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
        )
    }

    /** 获取屏幕信息 */
    fun getScreenInfo(context: Context): ScreenInfo {
        val dm = context.resources.displayMetrics
        return ScreenInfo(
            resolution = "${dm.widthPixels} x ${dm.heightPixels}",
            densityDpi = dm.densityDpi
        )
    }

    /** 获取存储信息 */
    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        return StorageInfo(
            totalInternal = formatSize(totalBlocks * blockSize),
            availableInternal = formatSize(availableBlocks * blockSize)
        )
    }

    private fun formatSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            size >= gb -> "${size / gb} GB"
            size >= mb -> "${size / mb} MB"
            size >= kb -> "${size / kb} KB"
            else -> "$size B"
        }
    }

    /** 获取应用信息 */
    fun getAppInfo(context: Context): AppInfo {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            AppInfo(versionName = pi.versionName, versionCode = pi.versionCode)
        } catch (e: Exception) {
            AppInfo(versionName = "未知", versionCode = -1)
        }
    }
}
