package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.net.common.NetworkUtils;

/**
 * Implements the automatic download algorithm used by AntennaPod. This class assumes that
 * the client uses the {@link EpisodeCleanupAlgorithm}.
 */
public class AutomaticDownloadAlgorithm {
    private static final String TAG = "DownloadAlgorithm";

    /**
     * Looks for undownloaded episodes in the queue or list of new items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    public Runnable autoDownloadUndownloadedItems(final Context context) {
        return () -> {

            // true if we should auto download based on network status
            boolean networkShouldAutoDl = NetworkUtils.isAutoDownloadAllowed();

            // true if we should auto download based on power status
            boolean powerShouldAutoDl = deviceCharging(context) || UserPreferences.isEnableAutodownloadOnBattery();

            // we should only auto download if both network AND power are happy
            if (networkShouldAutoDl && powerShouldAutoDl) {

                Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                final List<FeedItem> newItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                        new FeedItemFilter(FeedItemFilter.NEW), SortOrder.DATE_NEW_OLD);
                final List<FeedItem> candidates = new ArrayList<>();
                for (FeedItem newItem : newItems) {
                    FeedPreferences feedPrefs = newItem.getFeed().getPreferences();
                    if (feedPrefs.isAutoDownload(UserPreferences.isEnableAutodownloadGlobal())
                            && !candidates.contains(newItem)
                            && feedPrefs.getFilter().shouldAutoDownload(newItem)) {
                        candidates.add(newItem);
                    }
                }

                if (UserPreferences.isEnableAutodownloadQueue()) {
                    final List<FeedItem> queue = DBReader.getQueue();
                    for (FeedItem item : queue) {
                        if (!candidates.contains(item)) {
                            candidates.add(item);
                        }
                    }
                }

                // filter items that are not auto downloadable
                Iterator<FeedItem> it = candidates.iterator();
                while (it.hasNext()) {
                    FeedItem item = it.next();
                    if (!item.isAutoDownloadEnabled()
                            || item.isDownloaded()
                            || !item.hasMedia()
                            || item.getFeed().isLocalFeed()) {
                        it.remove();
                    }
                }

                int autoDownloadableEpisodes = candidates.size();
                int downloadedEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
                downloadedEpisodes += DownloadServiceInterface.get().getNumberOfActiveDownloads(context);
                int deletedEpisodes = EpisodeCleanupAlgorithmFactory.build()
                        .makeRoomForEpisodes(context, autoDownloadableEpisodes);
                boolean cacheIsUnlimited =
                        UserPreferences.getEpisodeCacheSize() == UserPreferences.EPISODE_CACHE_SIZE_UNLIMITED;
                boolean cacheIsLatest =
                        UserPreferences.getEpisodeCacheSize() == UserPreferences.EPISODE_CACHE_SIZE_LATEST;
                int episodeCacheSize = UserPreferences.getEpisodeCacheSize();

                int episodeSpaceLeft;
                if (cacheIsUnlimited || episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
                    episodeSpaceLeft = autoDownloadableEpisodes;
                } else if (cacheIsLatest) {
                    // For "Latest" mode, find all episodes from the most recent date
                    episodeSpaceLeft = autoDownloadableEpisodes; // Will be filtered below
                } else {
                    episodeSpaceLeft = episodeCacheSize - (downloadedEpisodes - deletedEpisodes);
                }

                List<FeedItem> itemsToDownload;
                if (cacheIsLatest && !candidates.isEmpty()) {
                    // Filter to only include episodes from the latest publication date
                    itemsToDownload = filterLatestDateEpisodes(candidates);
                    Log.d(TAG, "Latest mode: filtered to " + itemsToDownload.size() + " episodes from latest date");
                } else {
                    itemsToDownload = candidates.subList(0, Math.min(episodeSpaceLeft, candidates.size()));
                }
                if (!itemsToDownload.isEmpty()) {
                    Log.d(TAG, "Enqueueing " + itemsToDownload.size() + " items for download");

                    for (FeedItem episode : itemsToDownload) {
                        DownloadServiceInterface.get().download(context, episode);
                    }
                }
            }
        };
    }

    /**
     * Filters candidates to only include episodes from the most recent publication date.
     * This handles cases where a podcast releases multiple episodes on the same day.
     *
     * @param candidates List of candidate episodes, assumed to be sorted by date (newest first)
     * @return List of episodes that share the most recent publication date
     */
    private static List<FeedItem> filterLatestDateEpisodes(List<FeedItem> candidates) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<FeedItem> latestEpisodes = new ArrayList<>();
        Date latestDate = null;

        for (FeedItem item : candidates) {
            Date pubDate = item.getPubDate();
            if (pubDate == null) {
                continue;
            }

            if (latestDate == null) {
                // First item with a date becomes our reference
                latestDate = pubDate;
                latestEpisodes.add(item);
            } else if (isSameDay(pubDate, latestDate)) {
                // Same day as latest, include it
                latestEpisodes.add(item);
            }
            // Episodes are sorted newest first, so once we find a different (older) date, we're done
        }

        return latestEpisodes;
    }

    /**
     * Checks if two dates fall on the same calendar day in the local timezone.
     */
    private static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
                && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /**
     * @return true if the device is charging
     */
    public static boolean deviceCharging(Context context) {
        // from http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);

    }
}
