package com.openmusic.search.data.provider

import com.openmusic.search.data.remote.api.InternetArchiveApi
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
 * Internet Archive 搜索源。
 * - search() 秒返回基础数据（不查 metadata，不阻塞）
 * - resolveStreamUrl() 异步解析真实直链（metadata API 较慢但只在点击时调用）
 * - metadata 结果带缓存，避免重复查同一个 identifier
 */
@Singleton
class InternetArchiveProvider @Inject constructor(
    private val api: InternetArchiveApi
) : SearchProvider {

    override val id: String = Source.INTERNET_ARCHIVE.name
    override val displayName: String = Source.INTERNET_ARCHIVE.displayName
    override val requiresApiKey: Boolean = false

    private val playableFormats = setOf("MP3", "OGG", "FLAC", "M4A", "AAC", "WAV", "OPUS", "VBR MP3", "MP3 Audio", "64Kbps MP3", "128Kbps MP3", "256Kbps MP3", "512Kbps MP3", "2kbit MP3", "48bit MP3", "h.264", "Matroska")
    private val videoFormats = setOf("MP4", "WEBM", "OGV", "MPEG4", "Matroska")
    private val audioMimes = setOf("mp3", "ogg", "oga", "flac", "wav", "m4a", "opus", "aac")
    private val videoMimes = setOf("mp4", "webm", "ogv", "mkv", "m4v")

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val q = "($query) AND mediatype:(audio OR movies)"
        val response = try {
            api.search(query = q, rows = pageSize, page = page)
        } catch (e: Exception) {
            return emptyList()
        }
        val docs = response.response?.docs.orEmpty()
        return docs.mapNotNull { doc ->
            val identifier = doc.identifier ?: return@mapNotNull null
            val mediatype = doc.mediatype
            val title = doc.title ?: identifier
            val author = doc.creator?.firstOrNull()
            val isVideo = mediatype == "movies" || mediatype == "video"
            SearchResult(
                id = "$id:$identifier",
                sourceId = identifier,
                source = Source.INTERNET_ARCHIVE,
                title = title,
                author = author,
                thumbnailUrl = "https://archive.org/services/img/$identifier",
                durationSeconds = null,
                bitrate = null,
                mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                streamUrl = null,     // 搜索阶段不查直链
                downloadUrl = null,
                originalUrl = "https://archive.org/details/$identifier",
                publishedAt = doc.date,
                playable = true,      // IA 条目几乎总有可下载文件
                downloadable = true
            )
        }
    }

    /** LRU 缓存 metadata 结果，避免重复查同一个 identifier */
    private val urlCache = LinkedHashMap<String, PlayableUrl>(32, 0.75f, true)
    private val cacheLock = Mutex()

    override suspend fun resolveStreamUrl(sourceId: String): PlayableUrl? {
        // 查缓存
        cacheLock.withLock { urlCache[sourceId]?.let { return it } }

        val meta = try {
            api.metadata(sourceId)
        } catch (e: Exception) {
            return null
        }
        val files = meta.files.orEmpty()
        // 优先音频，其次视频
        val audioFile = files.firstOrNull { isPlayableAudio(it.format ?: "", it.name ?: "") }
        val videoFile = files.firstOrNull { isPlayableVideo(it.format ?: "", it.name ?: "") }
        val chosen = audioFile ?: videoFile ?: return null

        val fileName = chosen.name ?: return null
        val baseUrl = "https://archive.org/download/$sourceId"
        val url = "$baseUrl/$fileName"
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val isAudio = audioFile != null

        val result = PlayableUrl(
            streamUrl = url,
            downloadUrl = url,
            fileExtension = ext,
            durationSeconds = chosen.length?.toLongOrNull(),
            bitrate = chosen.bitrate?.toIntOrNull(),
            fileSizeBytes = chosen.size?.toLongOrNull(),
            mime = if (isAudio && ext in audioMimes) "audio/$ext" else
                   if (!isAudio && ext in videoMimes) "video/$ext" else null
        )
        cacheLock.withLock {
            if (urlCache.size > 64) urlCache.remove(urlCache.keys.first())
            urlCache[sourceId] = result
        }
        return result
    }

    private fun isPlayableAudio(format: String, name: String): Boolean {
        if (format in playableFormats) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in audioMimes
    }

    private fun isPlayableVideo(format: String, name: String): Boolean {
        if (format in videoFormats) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in videoMimes
    }
}
