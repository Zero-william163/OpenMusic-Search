package com.openmusic.search.data.provider

import com.openmusic.search.data.remote.api.YoutubeApi
import com.openmusic.search.domain.model.MediaType
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.SearchProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube 搜索源。
 * - 需要 API Key（YouTube Data API v3）
 * - 仅展示搜索结果，不提供播放/下载直链（遵守平台政策）
 * - 仅提供"打开原网站"按钮
 */
@Singleton
class YoutubeProvider @Inject constructor(
    private val api: YoutubeApi,
    @javax.inject.Named("YOUTUBE_API_KEY") private val apiKey: String
) : SearchProvider {

    override val id: String = Source.YOUTUBE.name
    override val displayName: String = Source.YOUTUBE.displayName
    override val requiresApiKey: Boolean = true
    override val enabled: Boolean = apiKey.isNotBlank()

    private val pageTokens = mutableMapOf<Int, String>()

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        if (!enabled) return emptyList()
        val pageToken = pageTokens[page - 1]
        val response = try {
            api.search(query = query, maxResults = pageSize, pageToken = pageToken, key = apiKey)
        } catch (e: Exception) {
            return emptyList()
        }
        if (page > 1) pageTokens.remove(page - 1)
        response.nextPageToken?.let { pageTokens[page] = it }

        return response.items.mapNotNull { item ->
            val videoId = item.id?.videoId ?: return@mapNotNull null
            val snippet = item.snippet
            SearchResult(
                id = "$id:$videoId",
                sourceId = videoId,
                source = Source.YOUTUBE,
                title = snippet?.title ?: videoId,
                author = snippet?.channelTitle,
                thumbnailUrl = snippet?.thumbnails?.high?.url ?: snippet?.thumbnails?.medium?.url,
                durationSeconds = null,
                bitrate = null,
                mediaType = MediaType.VIDEO,
                streamUrl = null,        // 不伪造播放地址
                downloadUrl = null,      // 不伪造下载地址
                originalUrl = "https://www.youtube.com/watch?v=$videoId",
                publishedAt = snippet?.publishedAt,
                playable = false,
                downloadable = false
            )
        }
    }
}
