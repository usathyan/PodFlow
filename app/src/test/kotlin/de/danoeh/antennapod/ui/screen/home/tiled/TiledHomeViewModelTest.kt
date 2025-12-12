package de.danoeh.antennapod.ui.screen.home.tiled

import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.model.feed.FeedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests for TiledHomeViewModel and related data classes.
 *
 * Note: Full ViewModel tests require Robolectric for Android context.
 * These tests focus on the data model logic.
 */
class TiledHomeViewModelTest {

    @Test
    fun `PodcastTileData with no episodes has null latestDownloadedEpisode`() {
        val feed = createTestFeed(1, "Test Podcast")
        val tileData = PodcastTileData(
            feed = feed,
            downloadedEpisodeCount = 0,
            latestDownloadedEpisode = null
        )

        assertEquals(0, tileData.downloadedEpisodeCount)
        assertNull(tileData.latestDownloadedEpisode)
    }

    @Test
    fun `PodcastTileData with episodes has correct count and latest episode`() {
        val feed = createTestFeed(1, "Test Podcast")
        val episode = createTestEpisode(1, feed, Date())

        val tileData = PodcastTileData(
            feed = feed,
            downloadedEpisodeCount = 5,
            latestDownloadedEpisode = episode
        )

        assertEquals(5, tileData.downloadedEpisodeCount)
        assertNotNull(tileData.latestDownloadedEpisode)
        assertEquals(episode.id, tileData.latestDownloadedEpisode?.id)
    }

    @Test
    fun `TiledHomeUiState Loading state is correct type`() {
        val state: TiledHomeUiState = TiledHomeUiState.Loading
        assertTrue(state is TiledHomeUiState.Loading)
    }

    @Test
    fun `TiledHomeUiState Success state contains podcasts`() {
        val feed = createTestFeed(1, "Test Podcast")
        val podcasts = listOf(
            PodcastTileData(feed, 3, null)
        )

        val state = TiledHomeUiState.Success(podcasts)

        assertTrue(state is TiledHomeUiState.Success)
        assertEquals(1, state.podcasts.size)
        assertEquals("Test Podcast", state.podcasts[0].feed.title)
    }

    @Test
    fun `TiledHomeUiState Error state contains message`() {
        val errorMessage = "Network error"
        val state = TiledHomeUiState.Error(errorMessage)

        assertTrue(state is TiledHomeUiState.Error)
        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `TiledHomeUiState Empty state is correct type`() {
        val state: TiledHomeUiState = TiledHomeUiState.Empty
        assertTrue(state is TiledHomeUiState.Empty)
    }

    @Test
    fun `PodcastTileData equality works correctly`() {
        val feed = createTestFeed(1, "Test Podcast")
        val episode = createTestEpisode(1, feed, Date())

        val tileData1 = PodcastTileData(feed, 5, episode)
        val tileData2 = PodcastTileData(feed, 5, episode)

        assertEquals(tileData1, tileData2)
    }

    @Test
    fun `PodcastTileData sorting by download count works`() {
        val feed1 = createTestFeed(1, "Podcast A")
        val feed2 = createTestFeed(2, "Podcast B")
        val feed3 = createTestFeed(3, "Podcast C")

        val podcasts = listOf(
            PodcastTileData(feed1, 2, null),
            PodcastTileData(feed2, 10, null),
            PodcastTileData(feed3, 5, null)
        )

        val sorted = podcasts.sortedByDescending { it.downloadedEpisodeCount }

        assertEquals("Podcast B", sorted[0].feed.title)
        assertEquals("Podcast C", sorted[1].feed.title)
        assertEquals("Podcast A", sorted[2].feed.title)
    }

    // Helper methods to create test objects

    private fun createTestFeed(id: Long, title: String): Feed {
        // Use the proper constructor: Feed(url, lastModified, title)
        val feed = Feed("https://example.com/feed/$id", null, title)
        feed.id = id
        feed.imageUrl = "https://example.com/image.jpg"
        feed.state = Feed.STATE_SUBSCRIBED
        return feed
    }

    private fun createTestEpisode(id: Long, feed: Feed, pubDate: Date): FeedItem {
        // FeedItem(id, title, link, itemIdentifier, pubDate, state, feed)
        val item = FeedItem(id, "Episode $id", "https://example.com/episode/$id", "item-$id", pubDate, FeedItem.UNPLAYED, feed)
        val media = FeedMedia(item, "https://example.com/episode.mp3", 1000, "audio/mpeg")
        item.setMedia(media)
        return item
    }
}
