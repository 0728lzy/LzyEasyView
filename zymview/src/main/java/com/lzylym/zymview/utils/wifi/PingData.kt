package com.lzylym.zymview.utils.wifi

data class PingData(
    val delayMs: Float,      // 延时毫秒数，-1f 表示超时或失败
    val isSuccess: Boolean,  // 是否 ping 通
    val host: String,        // 目标主机
    val displayStr: String   // 格式化后的显示字符串，例如 "45.2 ms" 或 "Timeout"
)