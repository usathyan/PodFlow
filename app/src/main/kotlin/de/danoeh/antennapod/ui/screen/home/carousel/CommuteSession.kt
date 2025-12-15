package de.danoeh.antennapod.ui.screen.home.carousel

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a commute listening session for a single day.
 * Tracks which podcasts have been completed and current playback position.
 */
data class CommuteSession(
    val date: String,                         // Date string in yyyy-MM-dd format
    val podcastOrder: List<Long>,             // Feed IDs in carousel order
    val completedPodcasts: Set<Long>,         // Feed IDs that finished
    val currentPodcastId: Long?,              // Currently playing feed ID
    val currentEpisodeId: Long?,              // Currently playing episode ID
    val currentPositionMs: Long               // Playback position in milliseconds
) {
    companion object {
        private const val PREFS_NAME = "commute_session"
        private const val KEY_DATE = "session_date"
        private const val KEY_PODCAST_ORDER = "podcast_order"
        private const val KEY_COMPLETED = "completed_podcasts"
        private const val KEY_CURRENT_PODCAST = "current_podcast_id"
        private const val KEY_CURRENT_EPISODE = "current_episode_id"
        private const val KEY_POSITION = "current_position_ms"

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        private fun getTodayString(): String = dateFormat.format(Date())

        /**
         * Load session from SharedPreferences.
         * Returns null if no session exists or if it's from a different day.
         */
        fun load(context: Context): CommuteSession? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val dateStr = prefs.getString(KEY_DATE, null) ?: return null

            // Check if session is from today
            if (dateStr != getTodayString()) {
                return null
            }

            val podcastOrderStr = prefs.getString(KEY_PODCAST_ORDER, "") ?: ""
            val podcastOrder = if (podcastOrderStr.isBlank()) {
                emptyList()
            } else {
                podcastOrderStr.split(",").mapNotNull { it.toLongOrNull() }
            }

            val completedStr = prefs.getString(KEY_COMPLETED, "") ?: ""
            val completed = if (completedStr.isBlank()) {
                emptySet()
            } else {
                completedStr.split(",").mapNotNull { it.toLongOrNull() }.toSet()
            }

            val currentPodcastId = prefs.getLong(KEY_CURRENT_PODCAST, -1L).takeIf { it != -1L }
            val currentEpisodeId = prefs.getLong(KEY_CURRENT_EPISODE, -1L).takeIf { it != -1L }
            val positionMs = prefs.getLong(KEY_POSITION, 0L)

            return CommuteSession(
                date = dateStr,
                podcastOrder = podcastOrder,
                completedPodcasts = completed,
                currentPodcastId = currentPodcastId,
                currentEpisodeId = currentEpisodeId,
                currentPositionMs = positionMs
            )
        }

        /**
         * Create a new session for today with the given podcast order.
         */
        fun createNew(podcastOrder: List<Long>): CommuteSession {
            return CommuteSession(
                date = getTodayString(),
                podcastOrder = podcastOrder,
                completedPodcasts = emptySet(),
                currentPodcastId = null,
                currentEpisodeId = null,
                currentPositionMs = 0L
            )
        }

        /**
         * Clear the stored session.
         */
        fun clear(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                clear()
            }
        }
    }

    /**
     * Save session to SharedPreferences.
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_DATE, date)
            putString(KEY_PODCAST_ORDER, podcastOrder.joinToString(","))
            putString(KEY_COMPLETED, completedPodcasts.joinToString(","))
            putLong(KEY_CURRENT_PODCAST, currentPodcastId ?: -1L)
            putLong(KEY_CURRENT_EPISODE, currentEpisodeId ?: -1L)
            putLong(KEY_POSITION, currentPositionMs)
        }
    }

    /**
     * Mark a podcast as completed.
     */
    fun markCompleted(feedId: Long): CommuteSession {
        return copy(completedPodcasts = completedPodcasts + feedId)
    }

    /**
     * Update current playback position.
     */
    fun updatePlayback(feedId: Long, episodeId: Long, positionMs: Long): CommuteSession {
        return copy(
            currentPodcastId = feedId,
            currentEpisodeId = episodeId,
            currentPositionMs = positionMs
        )
    }

    /**
     * Check if a podcast is completed.
     */
    fun isCompleted(feedId: Long): Boolean = feedId in completedPodcasts

    /**
     * Check if all podcasts are completed.
     */
    fun isAllCompleted(): Boolean = completedPodcasts.containsAll(podcastOrder.toSet())

    /**
     * Get the next unplayed podcast in sequence, starting from the given index.
     * Returns null if all podcasts are completed.
     */
    fun getNextUnplayedIndex(startIndex: Int): Int? {
        if (podcastOrder.isEmpty()) return null

        var index = startIndex
        var checked = 0

        while (checked < podcastOrder.size) {
            val feedId = podcastOrder[index]
            if (feedId !in completedPodcasts) {
                return index
            }
            index = (index + 1) % podcastOrder.size
            checked++
        }

        return null // All completed
    }

    /**
     * Get index of a podcast by feed ID.
     */
    fun getIndexForFeed(feedId: Long): Int = podcastOrder.indexOf(feedId)
}
