# Podcast Subscription Feature - Brainstorming Notes

> **Status:** Paused (brainstorming in progress)
> **Branch:** `feature/flutter-rewrite`
> **Last updated:** December 26, 2025

## Decisions Made

| Topic | Decision | Notes |
|-------|----------|-------|
| How users add podcasts | Search + URL | Match existing Android behavior |
| Search provider | iTunes first | Add Podcast Index, Fyyd later |
| Feed refresh | TBD | Options: manual, on-launch, background |

## Current State

### Flutter Project Created
- **Location:** `/podflow_flutter/`
- **Flutter version:** 3.38.5, Dart 3.10.4
- **Dependencies installed:** just_audio, audio_service, webfeed_plus, riverpod, sqflite, dio

### Files Created
```
lib/
├── core/
│   ├── models/
│   │   ├── podcast.dart      ✅ Created
│   │   ├── episode.dart      ✅ Created
│   │   └── models.dart       ✅ Barrel export
│   ├── services/             (empty - to be implemented)
│   └── repositories/         (empty - to be implemented)
├── features/
│   ├── player/               (empty)
│   ├── subscriptions/        (empty)
│   ├── downloads/            (empty)
│   └── visualizer/           (empty)
├── shared/widgets/           (empty)
└── main.dart                 ✅ Basic app with 4-tab navigation
```

### What Works
- App compiles and runs
- Basic Material 3 UI with light/dark theme
- 4-tab navigation: Subscriptions, Discover, Downloads, Settings
- Placeholder screens for each tab
- Tests pass

## Existing Android Implementation (Reference)

The Android app uses:
- **Search providers:** iTunes, Podcast Index, Fyyd (via `PodcastSearcherRegistry`)
- **UI flow:** OnlineSearchFragment → search → select result → OnlineFeedviewActivity → subscribe
- **RSS parsing:** Custom feed parser
- **Storage:** Room database

Key files to reference:
- `ui/discovery/src/.../OnlineSearchFragment.java` - Search UI
- `net/discovery/PodcastSearcherRegistry.java` - Search provider registry
- `net/discovery/ItunesPodcastSearcher.java` - iTunes API integration

## Next Steps (When Resuming)

### Immediate (to complete brainstorming)
1. Decide on feed refresh strategy (manual, on-launch, or background)
2. Finalize design document

### Implementation Order
1. **Podcast Service** - iTunes search API integration
2. **Feed Parser Service** - RSS fetch and parse using webfeed_plus
3. **Database Repository** - SQLite storage for subscriptions/episodes
4. **Subscriptions Provider** - Riverpod state management
5. **Discover Screen UI** - Search bar, results grid
6. **Subscriptions Screen UI** - Grid of subscribed podcasts
7. **Podcast Detail Screen** - Episode list, subscribe button

### iTunes Search API

```
GET https://itunes.apple.com/search
  ?term={query}
  &media=podcast
  &limit=25
```

Response includes: `feedUrl`, `trackName`, `artworkUrl600`, `artistName`

## Technical Notes

### Dependencies Already Configured
- `webfeed_plus: ^1.1.2` - RSS/Atom parsing
- `dio: ^5.7.0` - HTTP client
- `sqflite: ^2.4.1` - SQLite database
- `flutter_riverpod: ^2.6.1` - State management
- `cached_network_image: ^3.4.1` - Image caching

### Architecture Pattern
Using feature-first structure with Riverpod for state management.
Each feature will have: screens/, widgets/, providers/, services/.

---

## Session Summary

**Date:** December 26, 2025

**What we did:**
1. User chose Flutter for cross-platform rewrite (over KMP and native iOS)
2. Installed Flutter 3.38.5 via Homebrew
3. Created `podflow_flutter/` project with recommended architecture
4. Added core dependencies for audio, RSS, state management, database
5. Created Podcast and Episode data models
6. Built basic app shell with 4-tab navigation
7. Started brainstorming podcast subscription feature
8. Decided: iTunes search first, match existing Android behavior

**Branch state:** Clean, committed, pushed to `feature/flutter-rewrite`
