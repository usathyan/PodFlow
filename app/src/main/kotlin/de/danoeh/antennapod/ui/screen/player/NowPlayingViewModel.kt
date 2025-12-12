package de.danoeh.antennapod.ui.screen.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.model.playback.Playable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Data class representing the current playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val podcastName: String = "",
    val imageUrl: String? = null,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerActive: Boolean = false,
    val sleepTimerRemaining: Long = 0
)

/**
 * ViewModel for the Now Playing screen.
 * Manages playback state and controls.
 */
class NowPlayingViewModel : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentMedia: Playable? = null

    init {
        // Start position update coroutine
        viewModelScope.launch {
            while (isActive) {
                updatePlaybackPosition()
                delay(1000) // Update every second
            }
        }
    }

    /**
     * Updates playback position from the service
     */
    private fun updatePlaybackPosition() {
        // This would integrate with PlaybackService via EventBus or Flow
        // For now, keeping the state as is - will be connected later
    }

    /**
     * Load current media info
     */
    fun loadCurrentMedia(media: Playable?) {
        currentMedia = media
        media?.let {
            _playbackState.value = _playbackState.value.copy(
                title = it.episodeTitle ?: "",
                podcastName = it.feedTitle ?: "",
                imageUrl = it.imageLocation,
                duration = it.duration.toLong()
            )
        }
    }

    /**
     * Update playback state
     */
    fun updateState(isPlaying: Boolean, position: Long, duration: Long) {
        _playbackState.value = _playbackState.value.copy(
            isPlaying = isPlaying,
            currentPosition = position,
            duration = duration
        )
    }

    /**
     * Toggle play/pause
     * Note: Actual playback control is delegated to PlaybackController in the Fragment
     */
    fun togglePlayPause() {
        _playbackState.value = _playbackState.value.copy(
            isPlaying = !_playbackState.value.isPlaying
        )
    }

    /**
     * Skip forward by given milliseconds
     */
    fun skipForward(millis: Int = 30000) {
        _playbackState.value = _playbackState.value.copy(
            currentPosition = (_playbackState.value.currentPosition + millis)
                .coerceAtMost(_playbackState.value.duration)
        )
    }

    /**
     * Skip backward by given milliseconds
     */
    fun skipBackward(millis: Int = 10000) {
        _playbackState.value = _playbackState.value.copy(
            currentPosition = (_playbackState.value.currentPosition - millis)
                .coerceAtLeast(0)
        )
    }

    /**
     * Seek to position
     */
    fun seekTo(position: Long) {
        _playbackState.value = _playbackState.value.copy(
            currentPosition = position
        )
    }

    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(
            playbackSpeed = speed
        )
    }
}
