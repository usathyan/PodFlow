# Radio Mode Crash Fix and Always-Fresh Priority Design

**Date:** 2025-12-15
**Status:** Approved
**Author:** Claude Sonnet 4.5

## Overview

This design addresses the Radio Mode auto-transition crash and implements always-fresh playback priority to ensure PodFlow continuously plays the latest available content.

## Problem Statement

### Critical Issues
1. **App crashes during auto-transition** when an episode finishes naturally and tries to advance to the next podcast
2. **Alphabetical podcast ordering** doesn't prioritize fresh content - newer episodes get ignored while cycling through older podcasts

### User Scenarios

**Scenario 1: Fresh Install with OPML Import**
- User installs PodFlow for the first time
- Default settings should be: Radio Mode ON, Auto-download ON, Crossfading ON (30s), Volume normalization ON
- User imports subscriptions via OPML
- Going to home page, clicking play on any episode should create a seamless radio experience
- Episodes auto-advance to the next latest podcast with no crashes

**Scenario 2: Daily Commute Resume**
- User starts PodFlow and clicks resume to continue from previous session
- App resumes from where they left off
- When current episode finishes, Radio Mode should jump to the latest available episode (prioritizing fresh content published since last session)
- No crashes during transitions

## Root Cause Analysis

### Crash: ClassCastException

**Error:**
```
FATAL EXCEPTION: main
java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer
at android.app.SharedPreferencesImpl.getInt(SharedPreferencesImpl.java:329)
at de.danoeh.antennapod.storage.preferences.UserPreferences.getRadioModeBlendTimeMs(UserPreferences.java:979)
at de.danoeh.antennapod.playback.service.PlaybackService.onPostPlayback(PlaybackService.java:1257)
```

**Location:** `UserPreferences.java:979`

**Problem:**
```java
public static int getRadioModeBlendTimeMs() {
    int blendTimeMs = prefs.getInt(PREF_RADIO_MODE_BLEND_TIME, 0);  // ❌ WRONG
    return Math.max(0, blendTimeMs);
}
```

The Radio Mode blend time preference is defined as a `MaterialListPreference` in XML:
```xml
<MaterialListPreference
    android:key="prefRadioModeBlendTime"
    android:defaultValue="0"
    android:entries="@array/radio_mode_blend_options"
    android:entryValues="@array/radio_mode_blend_values"/>
```

Android's `ListPreference` stores values as **strings**, not integers. The code attempts to read it as an integer, causing a `ClassCastException`.

**Why it works on manual skip but not auto-transition:**
- Manual skip path may not have triggered the blend logic in all code paths
- Auto-transition always calls `onPostPlayback()` with `ended=true`, which triggers the blend check at line 1257

### Current Radio Mode Logic Issues

**Alphabetical Sorting (Current):**
```java
// DBReader.java:564-602
private static FeedItem getNextPodcastEpisode(long currentFeedId) {
    List<Feed> feeds = getFeedList();

    // Sorts alphabetically by title
    Collections.sort(feeds, (a, b) -> {
        String titleA = a.getTitle() != null ? a.getTitle() : "";
        String titleB = b.getTitle() != null ? b.getTitle() : "";
        return titleA.compareToIgnoreCase(titleB);
    });

    // Finds current feed position, cycles to next in alphabetical order
    // Wraps around when reaching the end
}
```

**Problems:**
- Doesn't prioritize fresh content
- If new episodes arrive while playing, they're ignored until the alphabetical cycle reaches them
- User expectation: always play the latest available content (radio-like experience)

## Design Solution

### 1. Fix Blend Time Preference Reading

**File:** `storage/preferences/src/main/java/de/danoeh/antennapod/storage/preferences/UserPreferences.java`

**Change getter to read string and parse:**
```java
public static int getRadioModeBlendTimeMs() {
    String blendTimeStr = prefs.getString(PREF_RADIO_MODE_BLEND_TIME, "30000");  // Default: 30s
    try {
        return Math.max(0, Integer.parseInt(blendTimeStr));
    } catch (NumberFormatException e) {
        Log.e(TAG, "Invalid blend time value: " + blendTimeStr, e);
        return 30000;  // Fallback: 30s
    }
}
```

**Change setter to write string:**
```java
public static void setRadioModeBlendTimeMs(int blendTimeMs) {
    prefs.edit().putString(PREF_RADIO_MODE_BLEND_TIME, String.valueOf(blendTimeMs)).apply();
}
```

**Rationale:**
- Matches how Android `ListPreference` stores values
- Includes error handling for robustness
- Uses sensible fallback (30s) if parsing fails

### 2. Implement Always-Fresh Playback Priority

**File:** `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBReader.java`

**Replace alphabetical sorting with date-based sorting:**
```java
private static FeedItem getNextPodcastEpisode(long currentFeedId) {
    List<Feed> feeds = getFeedList();

    // Sort by LATEST episode date (newest first)
    Collections.sort(feeds, (a, b) -> {
        Date dateA = getLatestEpisodeDate(a);
        Date dateB = getLatestEpisodeDate(b);
        if (dateA == null && dateB == null) return 0;
        if (dateA == null) return 1;  // No date = push to end
        if (dateB == null) return -1;
        return dateB.compareTo(dateA);  // Newest first (descending)
    });

    // Always start from top (newest) and find first with downloaded episode
    for (Feed feed : feeds) {
        if (feed.getId() == currentFeedId) continue;  // Skip current podcast

        List<FeedItem> episodes = getFeedItemList(feed,
            new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.UNPLAYED),
            SortOrder.DATE_NEW_OLD, 0, 1);

        if (!episodes.isEmpty()) {
            FeedItem episode = episodes.get(0);
            loadAdditionalFeedItemListData(episodes);
            return episode;
        }
    }

    return null;  // No more episodes available
}

// Helper method to get latest episode date from a feed
private static Date getLatestEpisodeDate(Feed feed) {
    List<FeedItem> items = getFeedItemList(feed,
        new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.UNPLAYED),
        SortOrder.DATE_NEW_OLD, 0, 1);

    if (!items.isEmpty() && items.get(0).getPubDate() != null) {
        return items.get(0).getPubDate();
    }
    return null;
}
```

**Key Changes:**
- Re-evaluates podcast order on **every transition** based on current state
- Always jumps to the **freshest available content**
- No fixed cycling order - dynamically adapts as new episodes arrive

**Trade-offs:**
- ✅ Ensures users always hear the latest content (true radio experience)
- ✅ Handles new episodes arriving during playback session
- ⚠️ Older unplayed episodes may never be played if fresh content keeps arriving
- ⚠️ Users who want sequential playback should manually use the Queue feature instead

### 3. Update Default Settings

**Files:**
- `storage/preferences/src/main/java/de/danoeh/antennapod/storage/preferences/UserPreferences.java`
- `ui/preferences/src/main/res/xml/preferences_playback.xml`

**Change defaults to Radio Mode-first experience:**

**Java defaults:**
```java
public static boolean isRadioMode() {
    return prefs.getBoolean(PREF_RADIO_MODE, true);  // Changed: true
}

public static int getRadioModeBlendTimeMs() {
    String blendTimeStr = prefs.getString(PREF_RADIO_MODE_BLEND_TIME, "30000");  // Changed: 30s
    // ... parsing logic
}

public static boolean isAutoNormalizeVolume() {
    return prefs.getBoolean(PREF_AUTO_NORMALIZE_VOLUME, true);  // Changed: true
}
```

**XML defaults:**
```xml
<SwitchPreferenceCompat
    android:key="prefRadioMode"
    android:defaultValue="true"  <!-- Changed from false -->
    android:title="@string/pref_radio_mode_title"/>

<MaterialListPreference
    android:key="prefRadioModeBlendTime"
    android:defaultValue="30000"  <!-- Changed from "0" -->
    android:entries="@array/radio_mode_blend_options"
    android:entryValues="@array/radio_mode_blend_values"/>

<SwitchPreferenceCompat
    android:key="prefAutoNormalizeVolume"
    android:defaultValue="true"  <!-- Changed from false -->
    android:title="@string/pref_auto_normalize_volume_title"/>
```

**Rationale:**
- Aligns with PodFlow's "radio-first" philosophy
- Users get optimal experience out of the box
- Settings are adjustable - not forced
- 30s crossfade provides smooth transitions without being too long

### 4. Home Screen Podcast Ordering

**File:** `app/src/main/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeViewModel.kt`

**Ensure Home screen sorts by latest episode date:**
```kotlin
// Already implemented correctly in TiledHomeViewModel
// Podcasts are sorted by latest episode date (newest first)
// This matches the Radio Mode playback order
```

**Verify consistency:** Home screen visual order = Radio Mode playback order

## Flow Diagrams

### Scenario 1: Fresh Install Flow
```
User installs PodFlow
  ↓
App starts with defaults:
  - Radio Mode: ON
  - Crossfade: 30s
  - Volume normalization: ON
  - Auto-download: ON
  ↓
User imports OPML subscriptions
  ↓
Latest episodes auto-download
  ↓
User goes to Home screen (shows podcasts sorted by latest episode date)
  ↓
User clicks Play on any podcast
  ↓
Episode plays with volume normalization
  ↓
Episode finishes → 30s crossfade → Jump to latest available episode
  ↓
Radio Mode continues seamlessly (always-fresh priority)
```

### Scenario 2: Resume Playback Flow
```
User opens PodFlow next day
  ↓
New episodes arrived overnight (auto-downloaded)
  ↓
User clicks Resume
  ↓
App resumes from previous position (e.g., Friday's episode)
  ↓
Episode finishes playing
  ↓
getNextForRadioMode() called:
  1. Check for same-day episode from same podcast → None
  2. getNextPodcastEpisode() re-sorts feeds by latest date
  3. Finds freshest available episode (e.g., Monday's new episode)
  ↓
Plays latest episode (skips older unplayed content)
  ↓
Radio Mode continues with always-fresh priority
```

### Auto-Transition Logic Flow
```
Episode finishes playing
  ↓
onPostPlayback() called with ended=true
  ↓
Radio Mode blend check:
  - Read blend time: getRadioModeBlendTimeMs()
  - Parse string preference (FIX: was reading as int)
  - Apply 30s crossfade
  ↓
getNextForRadioMode() called:
  ↓
Step 1: Check for same-day episode from current podcast
  - Query: same feed ID, same calendar day, downloaded, unplayed
  - If found → return it (prioritize completing daily releases)
  ↓
Step 2: Get next podcast with always-fresh priority
  - getFeedList() → all subscribed podcasts
  - Sort by latest episode date (NEW: was alphabetical)
  - Iterate from newest to oldest
  - Skip current podcast
  - Find first podcast with downloaded, unplayed episode
  - Return that episode
  ↓
If episode found:
  - loadAdditionalFeedItemListData() (load feed info, tags)
  - Start playback with 30s fade-in
  ↓
If no episode found:
  - Return null → playback stops
```

## Implementation Checklist

### Phase 1: Crash Fix (Critical)
- [ ] Fix `getRadioModeBlendTimeMs()` to read string preference
- [ ] Fix `setRadioModeBlendTimeMs()` to write string preference
- [ ] Add error handling and fallback
- [ ] Test: auto-transition no longer crashes

### Phase 2: Always-Fresh Priority
- [ ] Implement `getLatestEpisodeDate()` helper method
- [ ] Replace alphabetical sort with date-based sort in `getNextPodcastEpisode()`
- [ ] Update logic to always start from freshest podcast
- [ ] Test: new episodes arriving during playback are prioritized

### Phase 3: Default Settings
- [ ] Change `isRadioMode()` default to `true`
- [ ] Change `getRadioModeBlendTimeMs()` default to `30000`
- [ ] Change `isAutoNormalizeVolume()` default to `true`
- [ ] Update XML preference defaults to match
- [ ] Test: fresh install has Radio Mode enabled with 30s crossfade

### Phase 4: Integration Testing
- [ ] Test Scenario 1: Fresh install + OPML import + play
- [ ] Test Scenario 2: Resume from previous session + auto-advance
- [ ] Test: Home screen order matches Radio Mode playback order
- [ ] Test: Crossfade works correctly (30s default)
- [ ] Test: Volume normalization is active

### Phase 5: Documentation
- [ ] Update README with new default settings
- [ ] Document always-fresh playback behavior
- [ ] Update changelog with crash fix and new behavior
- [ ] Document complete list of PodFlow changes from AntennaPod fork

## Testing Strategy

### Unit Tests
- `UserPreferences.getRadioModeBlendTimeMs()` with various string values
- `DBReader.getLatestEpisodeDate()` with different feed states
- `DBReader.getNextPodcastEpisode()` sorting logic

### Integration Tests
- Auto-transition from episode end to next episode
- Crossfade audio effect application
- Resume playback + auto-advance flow

### Manual Testing
1. **Fresh Install:**
   - Install app, verify defaults (Radio Mode ON, 30s crossfade)
   - Import OPML
   - Play episode, let it finish naturally
   - Verify: no crash, smooth transition to next episode

2. **Resume Playback:**
   - Play episode partway
   - Close app, wait
   - Reopen, click Resume
   - Let episode finish
   - Verify: advances to latest available episode

3. **Always-Fresh Priority:**
   - Start playing oldest podcast
   - While playing, trigger download of new episode (simulate via manual download)
   - Let current episode finish
   - Verify: jumps to newest episode, not next in alphabetical order

## Risks & Mitigations

### Risk: Performance Impact
**Concern:** Sorting feeds by date on every transition could be slow with many subscriptions

**Mitigation:**
- Sorting is in-memory on already-loaded feed list (fast)
- Only retrieves latest episode date per feed (1 DB query per feed)
- Can optimize later with caching if needed

### Risk: User Confusion
**Concern:** Users might not understand why older episodes are being skipped

**Mitigation:**
- This is the intended "radio" behavior - always fresh content
- Users who want sequential playback can use the Queue feature
- Document behavior clearly in app and README

### Risk: Default Settings Change
**Concern:** Existing users might be surprised by new defaults on app update

**Mitigation:**
- Defaults only apply to fresh installs
- Existing users' preferences are preserved
- Mention in changelog/release notes

## Success Criteria

1. ✅ No crashes during auto-transition from episode to episode
2. ✅ Fresh install has Radio Mode enabled with 30s crossfade by default
3. ✅ Auto-advance always prioritizes latest available content
4. ✅ Resume playback works seamlessly and transitions to fresh content
5. ✅ Home screen order matches Radio Mode playback order
6. ✅ Both test scenarios pass without crashes

## Future Enhancements (Out of Scope)

- Smart algorithm to mix fresh + older unplayed content
- User preference for "always fresh" vs "sequential" Radio Mode
- Visual indicator on Home screen showing "up next in Radio Mode"
- Statistics/history of what was auto-played in Radio Mode

## Files to Modify

### Critical (Crash Fix)
1. `storage/preferences/src/main/java/de/danoeh/antennapod/storage/preferences/UserPreferences.java`
   - Fix `getRadioModeBlendTimeMs()` and `setRadioModeBlendTimeMs()`

### Core Logic (Always-Fresh)
2. `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBReader.java`
   - Add `getLatestEpisodeDate()`
   - Rewrite `getNextPodcastEpisode()` with date-based sorting

### Configuration (Defaults)
3. `ui/preferences/src/main/res/xml/preferences_playback.xml`
   - Update default values for preferences

### Documentation
4. `README.md`
   - Update with new behavior and complete changelog
5. `docs/plans/` (this file)
   - Design documentation

## Estimated Complexity

- **Crash Fix:** Low (1-2 hours)
- **Always-Fresh Logic:** Medium (3-4 hours)
- **Default Settings:** Low (1 hour)
- **Testing:** Medium (2-3 hours)
- **Documentation:** Low (1-2 hours)

**Total:** ~8-12 hours

## Approved By

User confirmed all design sections:
- Section 1: Crash fix approach ✓
- Section 2: Always-fresh priority logic ✓
- Section 3: Default settings configuration ✓
- Section 4: Resume playback integration ✓
