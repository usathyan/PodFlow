package de.danoeh.antennapod.ui.screen.home.carousel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.model.feed.FeedItemFilter
import de.danoeh.antennapod.model.feed.SortOrder
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter
import de.danoeh.antennapod.storage.database.DBReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class representing a podcast in the carousel
 */
data class CarouselPodcastData(
    val feed: Feed,
    val latestDownloadedEpisode: FeedItem?,
    val playbackState: PodcastPlaybackState,
    val progressPercent: Float = 0f   // 0-100 for in-progress episodes
)

/**
 * Playback state for a podcast in the carousel
 */
enum class PodcastPlaybackState {
    UNPLAYED,       // Full color, play icon
    IN_PROGRESS,    // Full color, progress ring
    COMPLETED       // Grayed out, checkmark
}

/**
 * UI state for the Carousel Home screen
 */
sealed class CarouselHomeUiState {
    object Loading : CarouselHomeUiState()
    data class Success(
        val podcasts: List<CarouselPodcastData>,
        val currentIndex: Int,
        val allCompleted: Boolean
    ) : CarouselHomeUiState()
    data class Error(val message: String) : CarouselHomeUiState()
    object Empty : CarouselHomeUiState()
}

/**
 * ViewModel for the Carousel Home screen.
 * Manages podcast list, session tracking, and playback state.
 */
class CarouselHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CarouselHomeUiState>(CarouselHomeUiState.Loading)
    val uiState: StateFlow<CarouselHomeUiState> = _uiState.asStateFlow()

    private var session: CommuteSession? = null
    private var appContext: Context? = null

    /**
     * Initialize the view model with application context.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPodcasts(context)
    }

    /**
     * Load podcasts and session state.
     */
    fun loadPodcasts(context: Context) {
        viewModelScope.launch {
            _uiState.value = CarouselHomeUiState.Loading
            try {
                val podcasts = withContext(Dispatchers.IO) {
                    loadPodcastsFromDb()
                }

                if (podcasts.isEmpty()) {
                    _uiState.value = CarouselHomeUiState.Empty
                    return@launch
                }

                // Load or create session
                val existingSession = withContext(Dispatchers.IO) {
                    CommuteSession.load(context)
                }

                val podcastFeedIds = podcasts.map { it.feed.id }

                session = if (existingSession != null && existingSession.podcastOrder == podcastFeedIds) {
                    // Existing session for today with same podcasts
                    existingSession
                } else {
                    // New day or podcast list changed - create new session
                    CommuteSession.createNew(podcastFeedIds).also {
                        it.save(context)
                    }
                }

                updateUiState(podcasts)

            } catch (e: Exception) {
                _uiState.value = CarouselHomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadPodcastsFromDb(): List<CarouselPodcastData> {
        val feeds = DBReader.getFeedList()
            .filter { it.state == Feed.STATE_SUBSCRIBED }

        return feeds.mapNotNull { feed ->
            // Get downloaded, unplayed episodes for this feed
            val filter = FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.UNPLAYED)
            val episodes = DBReader.getFeedItemList(feed, filter, SortOrder.DATE_NEW_OLD, 0, Int.MAX_VALUE)

            val latestEpisode = episodes.firstOrNull()

            // Only include podcasts with downloaded episodes
            if (latestEpisode != null) {
                CarouselPodcastData(
                    feed = feed,
                    latestDownloadedEpisode = latestEpisode,
                    playbackState = PodcastPlaybackState.UNPLAYED
                )
            } else {
                null
            }
        }.sortedByDescending { it.latestDownloadedEpisode?.pubDate }
    }

    private fun updateUiState(basePodcasts: List<CarouselPodcastData>) {
        val currentSession = session ?: return

        val podcasts = basePodcasts.map { podcast ->
            val feedId = podcast.feed.id
            val state = when {
                currentSession.isCompleted(feedId) -> PodcastPlaybackState.COMPLETED
                currentSession.currentPodcastId == feedId -> PodcastPlaybackState.IN_PROGRESS
                else -> PodcastPlaybackState.UNPLAYED
            }

            // Calculate progress for in-progress podcast
            val progress = if (state == PodcastPlaybackState.IN_PROGRESS) {
                val duration = podcast.latestDownloadedEpisode?.media?.getDuration() ?: 1
                if (duration > 0) {
                    (currentSession.currentPositionMs.toFloat() / duration * 100).coerceIn(0f, 100f)
                } else 0f
            } else 0f

            podcast.copy(playbackState = state, progressPercent = progress)
        }

        // Find current index (in-progress or first unplayed)
        val currentIndex = podcasts.indexOfFirst {
            it.playbackState == PodcastPlaybackState.IN_PROGRESS
        }.takeIf { it >= 0 }
            ?: podcasts.indexOfFirst { it.playbackState == PodcastPlaybackState.UNPLAYED }
                .takeIf { it >= 0 }
            ?: 0

        _uiState.value = CarouselHomeUiState.Success(
            podcasts = podcasts,
            currentIndex = currentIndex,
            allCompleted = currentSession.isAllCompleted()
        )
    }

    /**
     * Play a podcast from the carousel.
     * Returns true if playback was started.
     */
    fun playPodcast(context: Context, podcast: CarouselPodcastData): Boolean {
        val episode = podcast.latestDownloadedEpisode ?: return false
        val media = episode.media ?: return false

        // Update session
        session = session?.updatePlayback(
            feedId = podcast.feed.id,
            episodeId = episode.id,
            positionMs = 0L
        )
        session?.save(context)

        // Start playback
        PlaybackServiceStarter(context, media)
            .callEvenIfRunning(true)
            .start()

        // Refresh UI
        viewModelScope.launch {
            val basePodcasts = withContext(Dispatchers.IO) { loadPodcastsFromDb() }
            updateUiState(basePodcasts)
        }

        return true
    }

    /**
     * Mark a podcast as completed and advance to next.
     * Called when an episode finishes.
     */
    fun markPodcastCompleted(feedId: Long) {
        val context = appContext ?: return

        session = session?.markCompleted(feedId)
        session?.save(context)

        viewModelScope.launch {
            val basePodcasts = withContext(Dispatchers.IO) { loadPodcastsFromDb() }
            updateUiState(basePodcasts)
        }
    }

    /**
     * Update playback position for current podcast.
     */
    fun updatePlaybackPosition(feedId: Long, episodeId: Long, positionMs: Long) {
        val context = appContext ?: return

        session = session?.updatePlayback(feedId, episodeId, positionMs)
        session?.save(context)

        // Update UI with new progress
        val currentState = _uiState.value
        if (currentState is CarouselHomeUiState.Success) {
            val updatedPodcasts = currentState.podcasts.map { podcast ->
                if (podcast.feed.id == feedId) {
                    val duration = podcast.latestDownloadedEpisode?.media?.getDuration() ?: 1
                    val progress = if (duration > 0) {
                        (positionMs.toFloat() / duration * 100).coerceIn(0f, 100f)
                    } else 0f
                    podcast.copy(
                        playbackState = PodcastPlaybackState.IN_PROGRESS,
                        progressPercent = progress
                    )
                } else {
                    podcast
                }
            }
            _uiState.value = currentState.copy(podcasts = updatedPodcasts)
        }
    }

    /**
     * Get the next podcast in sequence after the given feed ID.
     * Wraps around to beginning if needed.
     */
    fun getNextPodcast(currentFeedId: Long): CarouselPodcastData? {
        val currentState = _uiState.value
        if (currentState !is CarouselHomeUiState.Success) return null

        val currentIndex = currentState.podcasts.indexOfFirst { it.feed.id == currentFeedId }
        if (currentIndex < 0) return null

        val nextIndex = session?.getNextUnplayedIndex((currentIndex + 1) % currentState.podcasts.size)
            ?: return null

        return currentState.podcasts.getOrNull(nextIndex)
    }

    /**
     * Get the previous podcast in sequence before the given feed ID.
     */
    fun getPreviousPodcast(currentFeedId: Long): CarouselPodcastData? {
        val currentState = _uiState.value
        if (currentState !is CarouselHomeUiState.Success) return null

        val podcasts = currentState.podcasts
        val currentIndex = podcasts.indexOfFirst { it.feed.id == currentFeedId }
        if (currentIndex < 0) return null

        // Find previous unplayed podcast
        var prevIndex = if (currentIndex == 0) podcasts.size - 1 else currentIndex - 1
        var checked = 0

        while (checked < podcasts.size) {
            val podcast = podcasts[prevIndex]
            if (podcast.playbackState != PodcastPlaybackState.COMPLETED) {
                return podcast
            }
            prevIndex = if (prevIndex == 0) podcasts.size - 1 else prevIndex - 1
            checked++
        }

        return null
    }

    /**
     * Reset session for a new day.
     */
    fun resetSession(context: Context) {
        CommuteSession.clear(context)
        session = null
        loadPodcasts(context)
    }
}
