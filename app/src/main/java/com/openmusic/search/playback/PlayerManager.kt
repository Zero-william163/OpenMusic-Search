package com.openmusic.search.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.openmusic.search.domain.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放器管理器。持有播放列表与播放状态，供 UI 层观察。
 * 与 PlayerService 共享同一个 ExoPlayer 单例。
 */
@Singleton
class PlayerManager @Inject constructor(
    private val player: ExoPlayer
) {

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

    val currentItem: SearchResult?
        get() = _queue.value.getOrNull(_currentIndex.value)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex
                _currentIndex.value = idx
            }
        })
    }

    fun playQueue(items: List<SearchResult>, startIndex: Int = 0) {
        _queue.value = items
        val mediaItems = items.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() { player.seekToNextMediaItem() }
    fun previous() { player.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { player.seekTo(positionMs) }

    fun setPlaybackSpeed(speed: Float) { player.setPlaybackSpeed(speed) }

    fun setRepeatMode(mode: Int) { player.repeatMode = mode }
    fun setShuffle(enabled: Boolean) { player.shuffleModeEnabled = enabled }

    fun release() { player.release() }

    /** 进度轮询由 UI 层调用，更新 currentPosition/duration。 */
    fun tick() {
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.coerceAtLeast(0L)
    }

    private fun SearchResult.toMediaItem(): MediaItem {
        val uri = streamUrl ?: downloadUrl ?: ""
        return MediaItem.Builder()
            .setUri(uri)
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
