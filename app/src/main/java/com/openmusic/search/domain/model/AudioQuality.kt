package com.openmusic.search.domain.model

/**
 * 音质筛选选项。不猜测来源未提供的音质。
 */
enum class AudioQualityFilter(val label: String, val minBitrate: Int?) {
    ANY("不限", null),
    KBPS_64("64 kbps+", 64),
    KBPS_96("96 kbps+", 96),
    KBPS_128("128 kbps+", 128),
    KBPS_192("192 kbps+", 192),
    KBPS_256("256 kbps+", 256),
    KBPS_320("320 kbps+", 320),
    LOSSLESS("Lossless", 800);

    fun matches(bitrate: Int?): Boolean {
        if (minBitrate == null) return true
        if (bitrate == null) return false
        return bitrate >= minBitrate
    }

    companion object {
        val DEFAULT = ANY
    }
}
