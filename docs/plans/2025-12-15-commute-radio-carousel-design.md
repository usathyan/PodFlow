# Commute Radio Carousel Design

**Date:** 2025-12-15
**Status:** Approved
**Author:** Claude (via brainstorming session)

## Overview

Redesign PodFlow's home screen as a horizontal carousel optimized for the daily commute use case: quick visual selection, seamless playback through all subscribed podcasts, and automatic session management.

## User Requirements

1. **9-12 subscriptions** refreshed and downloaded daily
2. **Home page as podcast list** (carousel of covers) for commute
3. **Resume previous session** if incomplete, then reset to fresh content
4. **Seamless radio experience** — same volume, crossfade between podcasts
5. **Visual progress** — podcasts gray out as they complete
6. **Start from any podcast** — loop through sequence from that point
7. **Stop when done** — silence when all podcasts played

## Design Decisions

| Question | Decision |
|----------|----------|
| After episode finishes, reset to latest? | **Option C:** Finish all downloaded episodes first, reset next day |
| Podcast completion visual? | **Option C:** Gray out completed, keep visible for progress tracking |
| When all complete? | **Option A:** Stop playback, wait for tomorrow |
| Home layout? | **Option B:** Horizontal carousel of covers |
| Play action? | **Option B:** Tap cover to play, no separate button |
| Resume behavior? | **Option B:** Show progress ring, wait for tap |

## Home Screen Design

### Layout
- Full-width horizontal carousel (`LazyRow` in Compose)
- Cover art ~120dp square, 16dp spacing
- Center item scaled 1.1x to indicate focus
- Edge items fade slightly to indicate scrollability
- Background: subtle blur of center podcast artwork (optional)

### Visual States

```
┌─────────────────────────────────────────────────────────────┐
│                    CAROUSEL HOME SCREEN                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌───────┐   ┌───────┐   ┌─────────┐   ┌───────┐   ┌───────┐│
│   │ cover │   │ cover │   │  cover  │   │ cover │   │ cover ││
│   │  ▶️   │   │  ▶️   │   │   ▶️    │   │  ✓    │   │  ✓    ││
│   │ fade  │   │       │   │ FOCUS   │   │ gray  │   │ fade  ││
│   └───────┘   └───────┘   └─────────┘   └───────┘   └───────┘│
│    unplayed    unplayed    unplayed     completed   completed │
│                            (1.1x scale)  (dimmed)   (dimmed)  │
│                                                              │
│              ←  swipe horizontally to browse  →              │
└─────────────────────────────────────────────────────────────┘
```

**State Definitions:**
- **Unplayed:** Full color cover, play icon (▶️) overlay centered
- **In-progress:** Full color, circular progress ring around cover, play/resume icon
- **Completed:** Desaturated (40% opacity), checkmark (✓) overlay

### Interaction
- Horizontal swipe to browse
- Tap cover → start/resume playback
- Long-press → show podcast info (optional, future)

## Session & Playback Logic

### Session Concept
- Session = one day's listening activity
- Tracks: podcasts played, current position, completion state
- Resets at midnight (or configurable, e.g., 4 AM)

### Playback Flow
```
1. User taps podcast #5
2. Plays podcast #5's latest downloaded episode
3. Episode finishes → crossfade → auto-advance to #6
4. Continues: #6 → #7 → #8 → #9 → wraps to #1 → #2 → #3 → #4
5. All podcasts grayed → playback stops
```

### Wrap-Around Logic
```kotlin
fun getNextPodcastIndex(currentIndex: Int, totalPodcasts: Int, completedSet: Set<Int>): Int? {
    var next = (currentIndex + 1) % totalPodcasts
    var checked = 0
    while (checked < totalPodcasts) {
        if (next !in completedSet) return next
        next = (next + 1) % totalPodcasts
        checked++
    }
    return null // All completed
}
```

### Resume Behavior
- App persists: current podcast index, playback position, completion set
- On app open: carousel scrolls to in-progress podcast, shows progress ring
- Tap to resume from exact position

### Session Reset (Daily)
- Trigger: midnight or configurable time
- Actions:
  1. Mark partially-played episodes as "played"
  2. Clear completion set
  3. Rebuild carousel with fresh downloaded content
  4. All covers return to "unplayed" state

## Audio Experience

### Crossfade (existing, no changes)
- Fade-out current episode over configured duration (default 30s)
- Fade-in next episode simultaneously
- Respects per-podcast skip outro/intro settings

### Volume Normalization (existing, no changes)
- LoudnessEnhancer equalizes perceived loudness
- DynamicsProcessing compresses dynamic range

### Skip Podcast Controls (new)
Add to NowPlayingScreen.kt:
- **Skip Previous Podcast:** Jump to previous podcast in sequence
- **Skip Next Podcast:** Jump to next podcast in sequence

```
┌─────────────────────────────────────────┐
│           NOW PLAYING CONTROLS          │
├─────────────────────────────────────────┤
│                                         │
│  ⏮️    ⏪10s    ▶️/⏸️    30s⏩    ⏭️   │
│  prev   rewind  play    forward  next   │
│  pod                              pod   │
│                                         │
└─────────────────────────────────────────┘
```

## Data Model

### Session State (new)
```kotlin
data class CommuteSession(
    val date: LocalDate,
    val podcastOrder: List<Long>,        // Feed IDs in carousel order
    val completedPodcasts: Set<Long>,    // Feed IDs that finished
    val currentPodcastId: Long?,         // Currently playing feed ID
    val currentEpisodeId: Long?,         // Currently playing episode ID
    val currentPositionMs: Long          // Playback position
)
```

### Persistence
- Store in SharedPreferences or Room database
- Key: `commute_session`
- Clear/reset on new day

## Implementation Plan

### Files to Create
1. `CarouselHomeScreen.kt` — New Compose UI (~300 lines)
2. `CarouselHomeViewModel.kt` — Session logic (~150 lines)
3. `CommuteSession.kt` — Data model (~30 lines)

### Files to Modify
1. `TiledHomeFragment.kt` — Switch to CarouselHomeScreen
2. `PlaybackService.java` — Add skipToNextPodcast/skipToPreviousPodcast
3. `NowPlayingScreen.kt` — Add skip podcast buttons
4. `DBReader.java` — Add session-aware queries
5. `UserPreferences.java` — Add session reset time preference

### Files Unchanged
- `AutomaticDownloadAlgorithm.java` (download logic)
- `ExoPlayerWrapper.java` (volume normalization)
- Crossfade logic in PlaybackService
- All other AntennaPod core

### Estimated Scope
- ~500-700 lines new code
- ~100-150 lines modifications
- 3 new files, 5 modified files

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| No downloads ready | Show "No episodes available" message |
| Network unavailable | Play downloaded content, show offline indicator |
| Single podcast subscription | Plays that one, grays out, stops |
| App killed mid-playback | Resume from persisted position on next open |
| Midnight crosses during playback | Continue current session until app reopens |

## Future Enhancements (Not in Scope)

- Configurable session reset time
- Shuffle mode for carousel order
- Statistics: episodes played per day
- Widget showing next podcast

## Success Criteria

1. Open app → see horizontal carousel of podcast covers
2. Tap any cover → playback starts
3. Episode ends → crossfades to next podcast
4. Completed podcasts visually grayed
5. All done → playback stops
6. Next day → fresh content, all covers unplayed
7. Skip buttons work in player
