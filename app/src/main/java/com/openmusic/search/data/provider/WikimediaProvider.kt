package com.openmusic.search.data.provider

import com.openmusic.search.data.remote.api.WikimediaApi
import com.openmusic.search.domain.model.MediaType
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.PlayableUrl
import com.openmusic.search.domain.provider.SearchProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wikimedia Commons 搜索源。
 * - search() 秒返回，不查 fileInfo
 * - resolveStreamUrl() 异步拿真实直链 + 缓存
 *
 * 真实直链规则（Wikimedia 的 Canonical File URL）：
 *   https://upload.wikimedia.org/wikipedia/commons/<md5_prefix>/<md5_hash>/<filename>
 *  通过 API imageinfo 拿 url 字段最可靠。
 */
@Singleton
class WikimediaProvider @Inject constructor(
    private val api: WikimediaApi
) : SearchProvider {

    override val id: String = Source.WIKIMEDIA_COMMONS.name
    override val displayName: String = Source.WIKIMEDIA_COMMONS.displayName
    override val requiresApiKey: Boolean = false

    private val audioExts = setOf("ogg", "oga", "mp3", "flac", "wav", "m4a", "opus", "aac")
    private val videoExts = setOf("ogv", "webm", "mp4")

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val offset = (page - 1) * pageSize
        val response = try {
            api.search(query = query, limit = pageSize, offset = offset)
        } catch (e: Exception) {
            return emptyList()
        }
        return response.query?.search.orEmpty()
            .filter { doc ->
                val ext = doc.title?.substringAfterLast('.', "")?.lowercase()
                ext in audioExts || ext in videoExts
            }
            .mapNotNull { doc ->
                val title = doc.title ?: return@mapNotNull null
                val ext = title.substringAfterLast('.', "").lowercase()
                val isVideo = ext in videoExts
                val cleanTitle = title.substringAfter("File:").substringBeforeLast('.')
                SearchResult(
                    id = "$id:${title.hashCode()}",
                    sourceId = title,  // 含 "File:" 前缀
                    source = Source.WIKIMEDIA_COMMONS,
                    title = cleanTitle,
                    author = null,
                    thumbnailUrl = null, // 搜索 API 不给缩略图，resolve 时一起拿
                    durationSeconds = null,
                    bitrate = null,
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                    streamUrl = null,
                    downloadUrl = null,
                    originalUrl = "https://commons.wikimedia.org/wiki/$title",
                    publishedAt = null,
                    playable = true,
                    downloadable = true
                )
            }
    }

    private val urlCache = LinkedHashMap<String, PlayableUrl>(32, 0.75f, true)
    private val cacheLock = Mutex()

    override suspend fun resolveStreamUrl(sourceId: String): PlayableUrl? {
        cacheLock.withLock { urlCache[sourceId]?.let { return it } }

        val info = try {
            api.fileInfo(titles = sourceId)
        } catch (e: Exception) {
            return null
        }
        val page = info.query?.pages?.values?.firstOrNull() ?: return null
        val ii = page.imageinfo?.firstOrNull() ?: return null
        val url = ii.url ?: return null
        val mime = ii.mime ?: ""
        val ext = sourceId.substringAfterLast('.', "").lowercase()

        val result = PlayableUrl(
            streamUrl = url,
            downloadUrl = url,
            fileExtension = ext,
            durationSeconds = ii.duration?.toLong(),
            bitrate = null,
            fileSizeBytes = ii.size,
            mime = mime
        )
        cacheLock.withLock {
            if (urlCache.size > 64) urlCache.remove(urlCache.keys.first())
            urlCache[sourceId] = result
        }
        return result
    }
}
