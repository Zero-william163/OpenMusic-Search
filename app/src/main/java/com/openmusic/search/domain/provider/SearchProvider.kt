package com.openmusic.search.domain.provider

import com.openmusic.search.domain.model.SearchResult

/**
 * 搜索 Provider 抽象。
 *
 * 核心设计：search() 只做"秒回"的基础搜索，不查慢速 metadata。
 * 播放时再通过 resolveStreamUrl() 拿真实直链（带缓存 + 超时保护）。
 * 这样：搜索不阻塞 → 结果立即可见 → 点击播放才去解析真实 URL。
 */
interface SearchProvider {
    val id: String
    val displayName: String
    val enabled: Boolean get() = true
    val requiresApiKey: Boolean get() = false

    /** 搜索，秒返回基础数据（标题/作者/缩略图/来源/可播放标识），**不查直链**。 */
    suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult>

    /** 根据 sourceId 异步解析真实播放直链。返回 null 表示该 item 无可播放 URL。 */
    suspend fun resolveStreamUrl(sourceId: String): PlayableUrl? = null
}

/** 解析结果：真实直链 + 可选扩展名/时长 */
data class PlayableUrl(
    val streamUrl: String,
    val downloadUrl: String? = null,
    val fileExtension: String? = null,
    val durationSeconds: Long? = null,
    val bitrate: Int? = null,
    val fileSizeBytes: Long? = null,
    val mime: String? = null
)
