package com.openmusic.search.data.provider

import android.util.Log
import com.openmusic.search.domain.model.MediaType
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.PlayableUrl
import com.openmusic.search.domain.provider.SearchProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * SoundHelix 保底音乐源。
 *
 * SoundHelix 提供 16 首可直接流式播放 / 下载的免费 MP3，无需 API，
 * 固定 CDN URL。不依赖任何网络请求，**永远有结果**。
 *
 * 作用：当 Jamendo / Internet Archive / Wikimedia 全部超时 / 网络不通时，
 *      保证用户搜任何东西都能看到至少一些可以播放的内容。
 *
 * URL 格式：https://www.soundhelix.com/examples/mp3/SoundHelix-Song-{1..16}.mp3
 */
@Singleton
class SoundHelixProvider @Inject constructor() : SearchProvider {

    companion object {
        private const val TAG = "SoundHelix"
        private const val TOTAL_SONGS = 16
        private const val BASE = "https://www.soundhelix.com/examples/mp3"

        // SoundHelix 示例歌曲的已知信息
        private val SONG_META = mapOf(
            1 to  "Ascent",
            2 to  "Descent",
            3 to  "Flight",
            4 to  "Journey",
            5 to  "Motive",
            6 to  "Reverie",
            7 to  "Tranquility",
            8 to  "Wanderlust",
            9 to  "Dreamscape",
            10 to "Horizon",
            11 to "Illusion",
            12 to "Mirage",
            13 to "Echoes",
            14 to "Prism",
            15 to "Reflections",
            16 to "Universe"
        )
    }

    override val id: String = Source.SOUNDHELIX.name
    override val displayName: String = "SoundHelix (免费示例)"
    override val requiresApiKey: Boolean = false

    // 不参与"空结果过滤" —— 其他源都挂了才用它
    // 通过 enabled = false 让它默认不启动，由 Repository 特殊处理？
    // 不，让它默认启用但 Repository 只在其他 provider 都返回空时才 fallback
    // 更简单：让它直接返回少量混合结果，作为保底
    override val enabled: Boolean = true

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        Log.d(TAG, "提供 ${pageSize} 首保底音乐")
        val start = ((page - 1) * pageSize) % TOTAL_SONGS
        return (0 until pageSize).map { i ->
            val num = ((start + i) % TOTAL_SONGS) + 1
            val title = SONG_META[num] ?: "SoundHelix Song $num"
            val url = "$BASE/SoundHelix-Song-$num.mp3"
            SearchResult(
                id = "$id:$num",
                sourceId = num.toString(),
                source = Source.SOUNDHELIX,
                title = "$title（示例）",
                author = "SoundHelix",
                thumbnailUrl = null,
                durationSeconds = null,
                bitrate = 128,
                mediaType = MediaType.AUDIO,
                streamUrl = url,        // 直接可播放
                downloadUrl = url,      // 直接可下载
                originalUrl = "https://www.soundhelix.com/",
                publishedAt = null,
                playable = true,
                downloadable = true
            )
        }
    }

    override suspend fun resolveStreamUrl(sourceId: String): PlayableUrl? {
        val num = sourceId.toIntOrNull() ?: return null
        val url = "$BASE/SoundHelix-Song-$num.mp3"
        return PlayableUrl(
            streamUrl = url,
            downloadUrl = url,
            fileExtension = "mp3",
            durationSeconds = null,
            bitrate = 128,
            fileSizeBytes = null,
            mime = "audio/mpeg"
        )
    }
}
