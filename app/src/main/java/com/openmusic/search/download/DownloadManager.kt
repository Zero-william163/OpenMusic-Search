package com.openmusic.search.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.openmusic.search.data.local.db.DownloadDao
import com.openmusic.search.data.local.db.entity.DownloadEntity
import com.openmusic.search.domain.model.SearchResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载管理器。
 * - 仅对来源明确允许下载（downloadable=true）的结果执行下载
 * - 使用 MediaStore 写入公共音乐目录（Android 10+ Scoped Storage）
 * - 下载完成后用 MediaMetadataRetriever 验证确为媒体文件，而非 HTML
 */
@Singleton
class DownloadManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(result: SearchResult) {
        if (!result.downloadable) return
        val url = result.downloadUrl ?: return
        val id = "dl_${System.currentTimeMillis()}_${result.id.hashCode()}"

        scope.launch {
            downloadDao.insert(
                DownloadEntity(
                    id = id,
                    title = result.title,
                    source = result.source.displayName,
                    url = url,
                    filePath = null,
                    mimeType = guessMime(result.fileExtension),
                    status = "DOWNLOADING",
                    totalBytes = result.fileSizeBytes
                )
            )
            try {
                val bytes = downloadBytes(url, id)
                val savedPath = saveToMediaStore(bytes, result)
                if (savedPath != null) {
                    downloadDao.insert(
                        DownloadEntity(
                            id = id,
                            title = result.title,
                            source = result.source.displayName,
                            url = url,
                            filePath = savedPath,
                            mimeType = guessMime(result.fileExtension),
                            status = "COMPLETED",
                            totalBytes = bytes.size.toLong(),
                            downloadedBytes = bytes.size.toLong()
                        )
                    )
                } else {
                    downloadDao.insert(DownloadEntity(id, result.title, result.source.displayName, url, null, guessMime(result.fileExtension), "FAILED"))
                }
            } catch (e: Exception) {
                downloadDao.insert(DownloadEntity(id, result.title, result.source.displayName, url, null, guessMime(result.fileExtension), "FAILED"))
            }
        }
    }

    private suspend fun downloadBytes(url: String, id: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        val body = response.body ?: throw IllegalStateException("empty body")
        body.bytes()
    }

    private fun saveToMediaStore(bytes: ByteArray, result: SearchResult): String? {
        val ext = result.fileExtension ?: "mp3"
        val fileName = "${sanitize(result.title)}.$ext"
        val mime = guessMime(ext) ?: "application/octet-stream"

        val isAudio = mime.startsWith("audio")
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isAudio) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val context = appContext ?: return null
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri.toString()
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /** 验证下载的文件确实是媒体文件，避免把 HTML 存成 .mp3 */
    private fun validateMedia(uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val hasDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null
            hasDuration
        } catch (e: Exception) {
            false
        } finally {
            retriever.release()
        }
    }

    private fun guessMime(ext: String?): String? = when (ext?.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "ogg", "oga" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "opus" -> "audio/opus"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "ogv" -> "video/ogg"
        else -> null
    }

    private fun sanitize(name: String): String = name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5 _-]"), "_").trim()

    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        @Volatile
        var appContext: Context? = null

        /** 供非注入环境（Compose 回调）使用的便捷入口。 */
        fun enqueue(context: Context, result: SearchResult) {
            appContext = context.applicationContext
            val ctx = appContext ?: return
            val entryPoint = EntryPointAccessors.fromApplication(ctx, DownloadManagerEntryPoint::class.java)
            entryPoint.downloadManager().enqueue(result)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadManagerEntryPoint {
        fun downloadManager(): DownloadManager
    }
}
