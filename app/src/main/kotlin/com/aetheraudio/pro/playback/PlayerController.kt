package com.aetheraudio.pro.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aetheraudio.pro.data.model.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, REPEAT_ALL, REPEAT_SINGLE }
enum class ShuffleMode { OFF, RANDOM }

data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF
)

/**
 * App-wide singleton that owns the MediaController connection to [PlaybackService] and exposes
 * a simple StateFlow the whole Compose UI (mini player, Now Playing, queue sheet) observes.
 */
object PlayerController {

    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state

    private var originalQueue: List<Song> = emptyList()

    fun connect(context: Context) {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            val song = _state.value.queue.find { it.mediaStoreId == id }
            _state.value = _state.value.copy(
                currentSong = song,
                queueIndex = _state.value.queue.indexOf(song),
                durationMs = song?.durationMs ?: 0L
            )
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let {
                _state.value = _state.value.copy(durationMs = it.duration.coerceAtLeast(0))
            }
        }
    }

    /** Call periodically (e.g. every 500ms from a UI-scoped coroutine) to update the seek position. */
    fun pollPosition() {
        controller?.let { _state.value = _state.value.copy(positionMs = it.currentPosition.coerceAtLeast(0)) }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        originalQueue = songs
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex, 0L)
        _state.value = _state.value.copy(queue = songs, queueIndex = startIndex, currentSong = songs.getOrNull(startIndex))
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrevious() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_SINGLE
            RepeatMode.REPEAT_SINGLE -> RepeatMode.OFF
        }
        controller?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.REPEAT_SINGLE -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = next)
    }

    fun toggleShuffle() {
        val next = if (_state.value.shuffleMode == ShuffleMode.OFF) ShuffleMode.RANDOM else ShuffleMode.OFF
        controller?.shuffleModeEnabled = next == ShuffleMode.RANDOM
        _state.value = _state.value.copy(shuffleMode = next)
    }

    fun addToQueue(song: Song) {
        controller?.addMediaItem(song.toMediaItem())
        _state.value = _state.value.copy(queue = _state.value.queue + song)
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        val insertAt = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertAt, song.toMediaItem())
        val q = _state.value.queue.toMutableList().apply { add(insertAt, song) }
        _state.value = _state.value.copy(queue = q)
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
        val q = _state.value.queue.toMutableList().apply { if (index in indices) removeAt(index) }
        _state.value = _state.value.copy(queue = q)
    }

    fun moveInQueue(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
        val q = _state.value.queue.toMutableList().apply { add(to, removeAt(from)) }
        _state.value = _state.value.copy(queue = q)
    }

    fun clearQueueKeepCurrent() {
        val c = controller ?: return
        val current = c.currentMediaItemIndex
        for (i in c.mediaItemCount - 1 downTo 0) if (i != current) c.removeMediaItem(i)
        val keep = _state.value.currentSong
        _state.value = _state.value.copy(queue = listOfNotNull(keep), queueIndex = 0)
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(mediaStoreId.toString())
        .setUri(android.content.ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        ).build()
}
