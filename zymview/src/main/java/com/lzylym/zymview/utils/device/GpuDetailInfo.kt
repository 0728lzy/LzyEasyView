package com.lzylym.zymview.utils.device

/**
 * GPU 详细信息实体类
 */
data class GpuDetailInfo(
    var renderer: String = "Unknown",      // GPU渲染器 (e.g., Mali-G610 MC6)
    var vendor: String = "Unknown",        // GPU供应商 (e.g., ARM)
    var version: String = "Unknown",       // GPU版本 (e.g., OpenGL ES 3.2 v1 r38p1)
    var extensions: String = "Unknown",    // GPU扩展 (支持的功能列表)
    var maxTextureSize: Int = 0            // 最大纹理尺寸
)