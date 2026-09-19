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
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

/**
 * 播放器管理器。
 *
 * 流程：
 *  - 已有 streamUrl 的 → 直接播放，不阻塞
 *  - 没有 streamUrl 但 playable=true 的 → 异步 resolve（有超时）
 *  - 混合多个来源（Jamendo/IA/Wikimedia）也能同时播放
 */
@Singleton
class PlayerManager @Inject constructor(
    private val player: ExoPlayer,
    private val providers: Set<@JvmSuppressWildcards SearchProvider>
) {

    companion object { private const val TAG = "PlayerMgr"; private const val RESOLVE_TIMEOUT_MS = 5_000L }

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
     * 混合来源播放：已有直链的直接用，没有的异步 resolve（5s 超时）。
     */
    fun playItem(items: List<SearchResult>, startIndex: Int) {
        val playable = items.filter { it.playable }
        if (playable.isEmpty()) return
        val startItem = playable.getOrNull(startIndex) ?: return

        scope.launch {
            _loading.value = true
            try {
                val resolved = coroutineScope {
                    playable.map { item ->
                        async {
                            // 已有直链 → 直接用
                            if (!item.streamUrl.isNullOrBlank()) {
                                item
                            } else {
                                // 需要 resolve
                                val provider = providerById[item.source.name]
                                val url = provider?.let {
                                    withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                                        runCatching { it.resolveStreamUrl(item.sourceId) }
                                            .getOrNull()
                                    }
                                }
                                if (url != null) item.copy(
                                    streamUrl = url.streamUrl,
                                    downloadUrl = url.downloadUrl,
                                    fileExtension = url.fileExtension,
                                    durationSeconds = url.durationSeconds ?: item.durationSeconds,
                                    bitrate = url.bitrate ?: item.bitrate,
                                    fileSizeBytes = url.fileSizeBytes ?: item.fileSizeBytes,
                                    playable = true,
                                    downloadable = item.downloadable || !url.downloadUrl.isNullOrBlank()
                                ) else null
                            }
                        }
                    }.awaitAll()
                }.filterNotNull()

                if (resolved.isEmpty()) {
                    Log.w(TAG, "所有条目均无法播放")
                    return@launch
                }

                val realStartIndex = resolved.indexOfFirst { it.id == startItem.id }.coerceAtLeast(0)
                _queue.value = resolved
                playQueueInternal(resolved, realStartIndex)
            } catch (e: Exception) {
                Log.e(TAG, "playItem failed", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun playQueueInternal(items: List<SearchResult>, startIndex: Int) {
        val mediaItems = items.mapNotNull { it.toMediaItem() }
        if (mediaItems.isEmpty()) return
        scope.launch(Dispatchers.Main) {
            try {
                player.setMediaItems(mediaItems, startIndex.coerceAtMost(mediaItems.size - 1), 0L)
                player.prepare()
                player.play()
            } catch (e: Exception) {
                Log.e(TAG, "playQueueInternal failed", e)
            }
        }
    }

    fun playQueue(items: List<SearchResult>, startIndex: Int = 0) {
        val playable = items.filter { !it.streamUrl.isNullOrBlank() }
        if (playable.isEmpty()) return
        _queue.value = playable
        playQueueInternal(playable, startIndex)
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

    private fun SearchResult.toMediaItem(): MediaItem? {
        val url = streamUrl ?: return null
        return MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(author ?: "")
                    .setArtworkUri(thumbnailUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
}
