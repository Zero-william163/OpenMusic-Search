package com.openmusic.search.data.provider

import com.openmusic.search.data.remote.api.WikimediaApi
import com.openmusic.search.domain.model.MediaType
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.SearchProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wikimedia Commons 搜索源。
 * - 无需 API Key
 * - 音频（OGG/MP3/FLAC）与视频（OGV/WebM）直链可播放、可下载（CC / 公有领域）
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
        val docs = response.query?.search.orEmpty()
            .filter { doc ->
                val ext = doc.title?.substringAfterLast('.', "")?.lowercase()
                ext in audioExts || ext in videoExts
            }

        return coroutineScope {
            docs.map { doc -> async { docToResult(doc) } }.awaitAll().filterNotNull()
        }
    }

    private suspend fun docToResult(doc: com.openmusic.search.data.remote.dto.WikimediaSearchDoc): SearchResult? {
        val title = doc.title ?: return null
        val ext = title.substringAfterLast('.', "").lowercase()
        val isVideo = ext in videoExts

        val info = try {
            api.fileInfo(titles = title)
        } catch (e: Exception) {
            null
        }
        val page = info?.query?.pages?.values?.firstOrNull()
        val imageInfo = page?.imageinfo?.firstOrNull()
        val url = imageInfo?.url
        val mime = imageInfo?.mime ?: ""
        val size = imageInfo?.size
        val duration = imageInfo?.duration?.toLong()

        val fileName = title.removePrefix("File:")
        val cleanTitle = fileName.substringBeforeLast('.')

        return SearchResult(
            id = "$id:${title.hashCode()}",
            sourceId = title,
            source = Source.WIKIMEDIA_COMMONS,
            title = cleanTitle,
            author = null,
            thumbnailUrl = imageInfo?.thumburl,
            durationSeconds = duration,
            bitrate = null, // Wikimedia 不提供码率，标记为未知
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
            streamUrl = url,
            downloadUrl = url,
            originalUrl = "https://commons.wikimedia.org/wiki/$title",
            publishedAt = null,
            playable = url != null && mime.startsWith("audio") || (url != null && mime.startsWith("video")),
            downloadable = url != null,
            fileExtension = ext,
            fileSizeBytes = size
        )
    }
}
