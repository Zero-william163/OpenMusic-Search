package com.openmusic.search.data.provider

import android.util.Log
import com.openmusic.search.data.remote.api.JamendoApi
import com.openmusic.search.domain.model.MediaType
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.PlayableUrl
import com.openmusic.search.domain.provider.SearchProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Jamendo 搜索源（CC 授权免费音乐）。
 * - 搜索同时返回 MP3 直链（audio 字段），所以 streamUrl 搜索阶段就有
 * - resolveStreamUrl 也直接返回缓存的直链
 * - 直链可直接播放/下载，完全合法（CC / 自由授权）
 *
 * 这个源在国内网络通常也能访问（CDN 友好）。
 */
@Singleton
class JamendoProvider @Inject constructor(
    private val api: JamendoApi,
    @Named("JAMENDO_CLIENT_ID") private val clientId: String
) : SearchProvider {

    companion object { private const val TAG = "JamendoProvider" }

    override val id: String = Source.JAMENDO.name
    override val displayName: String = Source.JAMENDO.displayName
    override val requiresApiKey: Boolean = false
    override val enabled: Boolean get() = clientId.isNotBlank()

    private val urlCache = LinkedHashMap<String, PlayableUrl>(64, 0.75f, true)
    private val cacheLock = Mutex()

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val offset = (page - 1) * pageSize
        val resp = try {
            api.search(
                clientId = clientId,
                search = query,
                limit = pageSize,
                offset = offset
            )
        } catch (e: Exception) {
            Log.w(TAG, "search failed: ${e.message}")
            return emptyList()
        }

        Log.d(TAG, "返回 ${resp.results.size} 条")
        return resp.results.mapNotNull { track ->
            val audioUrl = track.audio ?: track.audiodownload
            if (audioUrl == null) return@mapNotNull null

            val trackId = track.id ?: return@mapNotNull null
            val title = track.name ?: "Jamendo Track $trackId"

            // 预缓存 PlayableUrl
            val playableUrl = PlayableUrl(
                streamUrl = audioUrl,
                downloadUrl = track.audiodownload ?: audioUrl,
                fileExtension = "mp3",
                durationSeconds = track.duration?.toLong(),
                bitrate = 128,
                fileSizeBytes = null,
                mime = "audio/mpeg"
            )
            cacheLock.withLock {
                if (urlCache.size > 128) urlCache.remove(urlCache.keys.first())
                urlCache[trackId] = playableUrl
            }

            SearchResult(
                id = "$id:$trackId",
                sourceId = trackId,
                source = Source.JAMENDO,
                title = title,
                author = track.artist_name,
                thumbnailUrl = track.image_small ?: track.image,
                durationSeconds = track.duration?.toLong(),
                bitrate = 128,
                mediaType = MediaType.AUDIO,
                streamUrl = audioUrl,   // Jamendo 搜索阶段就有直链！
                downloadUrl = track.audiodownload ?: audioUrl,
                originalUrl = "https://www.jamendo.com/track/$trackId",
                publishedAt = null,
                playable = true,
                downloadable = track.audiodownload_allowed ?: true
            )
        }
    }

    override suspend fun resolveStreamUrl(sourceId: String): PlayableUrl? {
        cacheLock.withLock { urlCache[sourceId]?.let { return it } }
        return null  // 搜索阶段已经缓存了
    }
}
