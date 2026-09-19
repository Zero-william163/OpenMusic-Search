package com.openmusic.search.playback

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.provider.SearchProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放器管理器。
 *
 * **关键设计**：search() 返回的结果 streamUrl 都是 null（秒返回）。
 * 用户点击播放时，先调用 Provider.resolveStreamUrl() 异步拿真实直链，
 * 再把可播放的条目组成队列喂给 Media3。
 *
 * 流程：点击播放 → 过滤出同 Provider 可解析的条目 → 并发解析直链 → 构建播放队列 → playQueue()
 */
@Singleton
class PlayerManager @Inject constructor(
    private val player: ExoPlayer,
    private val providers: Set<SearchProvider>
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val providerById by lazy { providers.associateBy { it.id } }

    private val _queue = MutableStateFlow<List<SearchResult>>(emptyList())
    val queue: StateFlow<List<SearchResult>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    val currentItem: SearchResult?
        get() = _queue.value.getOrNull(_currentIndex.value)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = player.currentMediaItemIndex
                _currentTitle.value = mediaItem?.mediaMetadata?.title?.toString()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _loading.value = playbackState == Player.STATE_BUFFERING
            }
        })
    }

    /**
     * 播放：解析直链 → 构建队列 → 播放。
     * 在 IO scope 异步执行，同时更新 UI 状态。
     */
    fun playItem(items: List<SearchResult>, startIndex: Int) {
        if (items.isEmpty()) return
        val startItem = items.getOrNull(startIndex) ?: return
        val provider = providerById[startItem.source.name]

        // 过滤：只保留来自同一个 Provider 的可播放条目（同 sourceId 解析）
        val sameProviderItems = items.filter { it.source.name == startItem.source.name && it.playable }
        if (sameProviderItems.isEmpty()) return

        scope.launch {
            _loading.value = true
            try {
                // 解析直链（同 Provider 才能解析）
                val resolved = coroutineScope {
                    sameProviderItems.map { item ->
                        async {
                            val url = provider?.resolveStreamUrl(item.sourceId)
                            if (url != null) item.copy(
                                streamUrl = url.streamUrl,
                                downloadUrl = url.downloadUrl,
                                fileExtension = url.fileExtension,
                                durationSeconds = url.durationSeconds ?: item.durationSeconds,
                                bitrate = url.bitrate ?: item.bitrate,
                                fileSizeBytes = url.fileSizeBytes ?: item.fileSizeBytes,
                                playable = true,
                                downloadable = true
                            ) else null
                        }
                    }.awaitAll()
                }.filterNotNull()

                if (resolved.isEmpty()) {
                    Log.w("PlayerManager", "No resolvable items")
                    return@launch
                }

                val realStartIndex = resolved.indexOfFirst { it.id == startItem.id }.coerceAtLeast(0)
                _queue.value = resolved
                playQueueInternal(resolved, realStartIndex)
            } catch (e: Exception) {
                Log.e("PlayerManager", "playItem failed", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun playQueueInternal(items: List<SearchResult>, startIndex: Int) {
        val mediaItems = items.map { it.toMediaItem() }
        scope.launch(Dispatchers.Main) {
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun playQueue(items: List<SearchResult>, startIndex: Int = 0) {
        // 这个重载用于直接调用（如解析完后）
        _queue.value = items
        playQueueInternal(items, startIndex)
    }

    fun togglePlayPause() {
        scope.launch(Dispatchers.Main) {
            if (player.isPlaying) player.pause() else player.play()
        }
    }
    fun next() { scope.launch(Dispatchers.Main) { player.seekToNextMediaItem() } }
    fun previous() { scope.launch(Dispatchers.Main) { player.seekToPreviousMediaItem() } }
    fun seekTo(positionMs: Long) { scope.launch(Dispatchers.Main) { player.seekTo(positionMs) } }
    fun setPlaybackSpeed(speed: Float) { scope.launch(Dispatchers.Main) { player.setPlaybackSpeed(speed) } }
    fun setRepeatMode(mode: Int) { scope.launch(Dispatchers.Main) { player.repeatMode = mode } }
    fun setShuffle(enabled: Boolean) { scope.launch(Dispatchers.Main) { player.shuffleModeEnabled = enabled } }

    fun tick() {
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.coerceAtLeast(0L)
    }

    fun release() { player.release() }

    private fun SearchResult.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(streamUrl ?: "")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(author ?: "")
                    .setArtworkUri(android.net.Uri.parse(thumbnailUrl))
                    .build()
            )
            .build()
    }
}
