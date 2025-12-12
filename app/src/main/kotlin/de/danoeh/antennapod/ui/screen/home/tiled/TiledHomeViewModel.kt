package de.danoeh.antennapod.ui.screen.home.tiled

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
 * Data class representing a podcast with its latest downloaded episode info
 */
data class PodcastTileData(
    val feed: Feed,
    val downloadedEpisodeCount: Int,
    val latestDownloadedEpisode: FeedItem?
)

/**
 * UI state for the Tiled Home screen
 */
sealed class TiledHomeUiState {
    object Loading : TiledHomeUiState()
    data class Success(val podcasts: List<PodcastTileData>) : TiledHomeUiState()
    data class Error(val message: String) : TiledHomeUiState()
    object Empty : TiledHomeUiState()
}

/**
 * ViewModel for the Tiled Home screen.
 * Loads subscribed podcasts and finds their latest downloaded episodes.
 */
class TiledHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TiledHomeUiState>(TiledHomeUiState.Loading)
    val uiState: StateFlow<TiledHomeUiState> = _uiState.asStateFlow()

    init {
        loadPodcasts()
    }

    fun loadPodcasts() {
        viewModelScope.launch {
            _uiState.value = TiledHomeUiState.Loading
            try {
                val podcasts = withContext(Dispatchers.IO) {
                    loadPodcastsFromDb()
                }
                _uiState.value = if (podcasts.isEmpty()) {
                    TiledHomeUiState.Empty
                } else {
                    TiledHomeUiState.Success(podcasts)
                }
            } catch (e: Exception) {
                _uiState.value = TiledHomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadPodcastsFromDb(): List<PodcastTileData> {
        val feeds = DBReader.getFeedList()
            .filter { it.state == Feed.STATE_SUBSCRIBED }

        return feeds.map { feed ->
            // Get downloaded, unplayed episodes for this feed, sorted by date (newest first)
            val filter = FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.UNPLAYED)
            val episodes = DBReader.getFeedItemList(feed, filter, SortOrder.DATE_NEW_OLD, 0, Int.MAX_VALUE)

            PodcastTileData(
                feed = feed,
                downloadedEpisodeCount = episodes.size,
                latestDownloadedEpisode = episodes.firstOrNull()
            )
        }.sortedByDescending { it.downloadedEpisodeCount }
    }

    /**
     * Plays the latest downloaded episode for the given podcast.
     * Returns true if playback was started, false if no episode available.
     */
    fun playLatestEpisode(context: Context, podcast: PodcastTileData): Boolean {
        val episode = podcast.latestDownloadedEpisode ?: return false
        val media = episode.media ?: return false

        PlaybackServiceStarter(context, media)
            .callEvenIfRunning(true)
            .start()

        return true
    }
}
