package com.openmusic.search.domain.model

/**
 * 统一搜索结果模型。
 *
 * 核心原则：区分 "搜索结果存在" / "可直接播放" / "可合法下载"。
 * - [streamUrl] 非 null 且 [playable] 为 true 时才允许 App 内播放。
 * - [downloadUrl] 非 null 且 [downloadable] 为 true 时才显示下载按钮。
 * - 否则仅展示并提供 [originalUrl] 打开原网站。
 */
data class SearchResult(
    val id: String,
    val sourceId: String,
    val source: Source,
    val title: String,
    val author: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val bitrate: Int?,          // kbps，null 表示来源未提供（音质未知，不猜测）
    val mediaType: MediaType,
    val streamUrl: String?,     // 可直接播放的直链
    val downloadUrl: String?,   // 可合法下载的直链
    val originalUrl: String,    // 原网站链接
    val publishedAt: String?,
    val playable: Boolean,
    val downloadable: Boolean,
    val fileExtension: String? = null, // 下载时的真实扩展名
    val fileSizeBytes: Long? = null
)

enum class MediaType { AUDIO, VIDEO }

enum class Source(val displayName: String) {
    INTERNET_ARCHIVE("Internet Archive"),
    WIKIMEDIA_COMMONS("Wikimedia Commons"),
    FREESOUND("Freesound"),
    JAMENDO("Jamendo"),
    YOUTUBE("YouTube"),
    SOUNDCLOUD("SoundCloud"),
    LASTFM("Last.fm");

    companion object {
        fun fromId(id: String): Source = entries.firstOrNull { it.name == id } ?: INTERNET_ARCHIVE
    }
}
