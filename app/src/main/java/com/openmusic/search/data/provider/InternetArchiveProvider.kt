package com.openmusic.search.data.provider

import com.openmusic.search.data.remote.api.InternetArchiveApi
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
 * Internet Archive 搜索源。
 * - 无需 API Key
 * - 提供可直接播放的音频直链（MP3/OGG/FLAC 等）
 * - 提供可合法下载的直链（CC / 公有领域内容）
 *
 * 直链格式：https://archive.org/download/{identifier}/{filename}
 */
@Singleton
class InternetArchiveProvider @Inject constructor(
    private val api: InternetArchiveApi
) : SearchProvider {

    override val id: String = Source.INTERNET_ARCHIVE.name
    override val displayName: String = Source.INTERNET_ARCHIVE.displayName
    override val requiresApiKey: Boolean = false

    private val playableFormats = setOf("MP3", "OGG", "FLAC", "M4A", "AAC", "WAV", "OPUS", "VBR MP3", "128Kbps MP3", "64Kbps MP3")
    private val videoFormats = setOf("MP4", "WEBM", "OGV", "MPEG4")

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val q = "($query) AND mediatype:(audio OR movies)"
        val response = try {
            api.search(query = q, rows = pageSize, page = page)
        } catch (e: Exception) {
            return emptyList()
        }
        val docs = response.response?.docs.orEmpty()

        return coroutineScope {
            docs.map { doc ->
                async { docToResult(doc) }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun docToResult(doc: com.openmusic.search.data.remote.dto.IaSearchDoc): SearchResult? {
        val identifier = doc.identifier ?: return null
        val metadata = try {
            api.metadata(identifier)
        } catch (e: Exception) {
            null
        }

        val files = metadata?.files.orEmpty()
        val audioFile = files.firstOrNull { it.format in playableFormats }
        val videoFile = files.firstOrNull { it.format in videoFormats }
        val chosen = audioFile ?: videoFile
        val isVideo = chosen == videoFile && audioFile == null

        val fileName = chosen?.name
        val baseUrl = "https://archive.org/download/$identifier"
        val streamUrl = if (fileName != null) "$baseUrl/$fileName" else null
        val downloadUrl = streamUrl

        val title = doc.title ?: metadata?.metadata?.title ?: identifier
        val author = doc.creator?.firstOrNull()
            ?: (metadata?.metadata?.creator as? String)
            ?: (metadata?.metadata?.creator as? List<*>)?.firstOrNull()?.toString()

        val duration = chosen?.length?.toLongOrNull()
        val bitrate = chosen?.bitrate?.toIntOrNull()
        val size = chosen?.size?.toLongOrNull()
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()

        val thumbnail = "https://archive.org/services/img/$identifier"

        return SearchResult(
            id = "$id:$identifier",
            sourceId = identifier,
            source = Source.INTERNET_ARCHIVE,
            title = title,
            author = author,
            thumbnailUrl = thumbnail,
            durationSeconds = duration,
            bitrate = bitrate,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
            streamUrl = streamUrl,
            downloadUrl = downloadUrl,
            originalUrl = "https://archive.org/details/$identifier",
            publishedAt = doc.date,
            playable = streamUrl != null,
            downloadable = downloadUrl != null,
            fileExtension = ext,
            fileSizeBytes = size
        )
    }
}
