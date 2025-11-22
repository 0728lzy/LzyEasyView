package com.lzylym.zymview.utils.device

/**
 * CPU 详细信息实体类
 */
data class CpuDetailInfo(
    var hardwareName: String = "Unknown",          // CPU硬件名称 (优先取 Hardware 或 ro.board.platform, 如 mt6895)
    var productSeries: String = "Unknown",         // 产品系列代号 (优先取 Processor, 如 AArch64 processor...)
    var coreCount: Int = 0,                        // 核心数
    var freqRange: String = "Unknown",             // 频率范围
    var availableFreqs: String = "Unknown",        // CPU 可靠平频率单位
    var cpuAbi: String = "Unknown",                // CPU指令集(ABI)
    var supportedAbis: String = "Unknown",         // 支持的ABI
    var chipArch: String = "Unknown",              // 芯片架构
    var features: String = "Unknown",              // CPU支持的功能
    var currentGovernor: String = "Unknown",       // 当前调频策略
    var supportedGovernors: String = "Unknown",    // CPU支持的调频模型
    var machineType: String = "Unknown",           // 支持机器类型
    var cpuArchType: String = "Unknown",           // CPU 架型
    var isVulkanSupported: String = "No",          // 是否支持Vulkan
    var bogoMips: String = "Unknown"               // BogoMips
)