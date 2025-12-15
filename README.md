# PodFlow - Modern Podcast Player

> Your podcasts, flowing seamlessly

PodFlow is a fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod) redesigned for a **radio-like listening experience**. Instead of managing individual episodes, PodFlow focuses on automatic playback with minimal interaction - just pick a podcast and listen.

## Latest Updates (v1.0.0)

**Radio Mode Enhancements:**
- ✅ **Radio Mode enabled by default** - Seamless listening experience out of the box
- ✅ **Fixed inbox count** - Now shows total NEW episodes, not just podcast count
- ✅ **Smart episode downloads** - Only downloads latest episode per podcast
- ✅ **Seamless podcast transitions** - Auto-advance to next podcast with optional audio blending
- ✅ **Audio crossfade/blend** - Configure fade time (0s, 30s, 1m, 5m, 10m) for smooth transitions between podcasts
- ✅ **Skip behavior fixed** - Skip marks episode as listened, advances to next podcast (no delete)
- ✅ **Smart deletion** - Episodes only deleted when next episode of same podcast arrives
- ✅ **HTTP 416 handling** - Download system properly handles already-downloaded files

---

## How PodFlow Differs from AntennaPod

| Feature | AntennaPod | PodFlow |
|---------|------------|---------|
| **Home Screen** | Episode-centric feed | Tiled podcast grid with one-tap play |
| **Auto-Download** | Downloads all new episodes | Downloads only the latest episode per podcast |
| **Radio Mode** | Not available | Auto-deletes played episodes, auto-advances to next |
| **Volume Normalization** | Manual boost only | Real-time audio normalization across podcasts |
| **Episode Management** | Manual queue management | Automatic - episodes disappear after playing |
| **Philosophy** | Full podcast management | Hands-off radio experience |

### Key PodFlow Features

- **Radio Mode** (Enabled by Default) - Episodes auto-delete after playback and volume is normalized across all podcasts for seamless listening. Configure audio blend/crossfade when transitioning between podcasts.
- **Audio Blending/Crossfade** - When advancing to the next podcast, optionally fade out the current episode and fade in the next over your choice of duration:
  - **No Blend** - Instant transition
  - **30 seconds** - Quick fade
  - **1 minute** - Standard crossfade
  - **5 minutes** - Extended blend
  - **10 minutes** - Slow, smooth transition
- **Smart Latest-Only Downloads** - Only downloads the most recent episode from each podcast. If a podcast drops multiple episodes on the same day, all are downloaded. Never re-downloads already-downloaded files.
- **Tiled Home Screen** - Visual grid of your podcasts. Tap to play instantly. Configurable 2/3/Auto columns with list view option.
- **Real-Time Volume Normalization** - Uses Android's DynamicsProcessing API to compress and limit audio, ensuring consistent volume across different podcasts.
- **Smart Inbox** - Shows total count of all new episodes across all podcasts. Swipe to mark as listened and auto-advance to next podcast.

### App Navigation

| Screen | What it shows |
|--------|---------------|
| **Home** | Grid of podcast tiles with play buttons. Tap to play the latest downloaded episode. |
| **Queue** | Episodes queued for sequential playback. Drag to reorder. |
| **Inbox** | New episodes (one per podcast). Swipe to dismiss or add to queue. |
| **Downloads** | All downloaded episodes for offline listening. |

### Inherited from AntennaPod

PodFlow inherits all the great features from AntennaPod:
- Variable playback speed (0.5x - 3x) with per-podcast settings
- Silence trimming (Smart Speed)
- Sleep timer with shake-to-reset and fade-out
- Chapter support and transcript viewing
- OPML import/export
- Streaming and offline playback
- Chromecast support

---

## Complete Changelog from AntennaPod

This section documents all modifications made to the AntennaPod codebase to create PodFlow.

### Core Features Added

#### 1. **Radio Mode** (`772aad2bd`, `2f7efcab6`, `88fb8b93e`, `79f567a9d`, `68dafad7d`)
- **New playback mode** that auto-advances to the next podcast when an episode finishes
- **Auto-deletion**: Episodes are automatically deleted after playback (smart deletion - only when next episode arrives)
- **Volume normalization**: Real-time audio processing using Android's `LoudnessEnhancer` and `DynamicsProcessing` APIs
- **Audio crossfade/blend**: Configurable fade times (0s, 30s, 1m, 5m, 10m) for smooth transitions between podcasts
- **Skip behavior**: Skip button marks episode as listened and advances to next podcast (no deletion)
- **Enabled by default**: Radio Mode is the default experience for new installs

**Files Modified:**
- `playback/service/src/main/java/de/danoeh/antennapod/playback/service/PlaybackService.java`
  - Added Radio Mode playback logic at lines 1105-1298
  - Implemented `getNextInQueue()` override for Radio Mode
  - Added blend/crossfade support on episode completion
  - Volume normalization integration
- `playback/service/src/main/java/de/danoeh/antennapod/playback/service/internal/ExoPlayerWrapper.java`
  - Integrated `LoudnessEnhancer` and `DynamicsProcessing` for volume normalization
  - Auto-enables when Radio Mode is active
- `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBReader.java`
  - Added `getNextForRadioMode()` method (lines 499-522)
  - Added `getNextSameDayEpisode()` helper (lines 528-557)
  - Added `getNextPodcastEpisode()` helper (lines 564-602)
- `storage/preferences/src/main/java/de/danoeh/antennapod/storage/preferences/UserPreferences.java`
  - Added Radio Mode preference flags (lines 71-74)
  - Added blend time preference management (lines 975-985)
  - Added volume normalization preferences (lines 958-997)

#### 2. **Tiled Home Screen** (`77104c903`, `280874a38`)
- **Jetpack Compose UI**: Modern, grid-based podcast view
- **One-tap play**: Each podcast tile has a play button to instantly start the latest episode
- **Configurable layout**: 2/3/Auto columns, list/grid view toggle
- **Date sorting**: Podcasts sorted by latest episode date (newest first)
- **Download badges**: Visual indicators showing downloaded episode count
- **Set as default home page**: Replaces traditional episode-centric feed

**Files Created:**
- `app/src/main/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeScreen.kt` (808 lines)
  - Jetpack Compose UI implementation
  - Grid/List view rendering
  - Play button tap handling
- `app/src/main/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeViewModel.kt` (100 lines)
  - ViewModel for managing podcast list state
  - Sorting by latest episode date
- `app/src/main/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeFragment.kt` (45 lines)
  - Fragment wrapper for Compose UI
- `app/src/test/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeViewModelTest.kt` (134 lines)
  - Unit tests for ViewModel

**Files Modified:**
- `app/src/main/java/de/danoeh/antennapod/activity/MainActivity.java`
  - Changed default home fragment to `TiledHomeFragment` (line 410-412)
  - Added navigation support for tiled home

#### 3. **Smart Latest-Only Downloads** (`772aad2bd`, `1fce9c9bf`)
- **One episode per podcast**: Auto-download only downloads the latest unplayed episode
- **Same-day multi-episode support**: If podcast publishes multiple episodes on same day, all are downloaded
- **HTTP 416 handling**: Properly handles already-downloaded files without re-downloading
- **Auto-download on subscription**: New subscriptions automatically download latest episode

**Files Modified:**
- `net/download/service/src/main/java/de/danoeh/antennapod/net/download/service/episode/autodownload/AutomaticDownloadAlgorithm.java`
  - Modified logic to download only latest episode per podcast (85+ line changes)
  - Added same-day multi-episode detection
- `net/download/service/src/main/java/de/danoeh/antennapod/net/download/service/feed/remote/HttpDownloader.java`
  - Added HTTP 416 (Range Not Satisfiable) handling for resume support

#### 4. **Smart Inbox** (`772aad2bd`)
- **Accurate episode count**: Shows total NEW episodes across all podcasts (not just podcast count)
- **Swipe actions**: Swipe to mark as listened and auto-advance to next

**Files Modified:**
- `app/src/main/java/de/danoeh/antennapod/ui/screen/home/sections/InboxSection.java`
  - Fixed inbox count calculation to show episode count
- `app/src/main/java/de/danoeh/antennapod/ui/screen/InboxFragment.java`
  - Updated swipe behavior for Radio Mode

### Bug Fixes

#### Radio Mode Crash Fixes
- **Lambda variable scope fix** (`68dafad7d`): Fixed `ClassCastException` where `nextItem` variable wasn't final in lambda expressions
- **Audio threading crash fix** (`79f567a9d`): Fixed thread safety issues in blend transitions
- **Blend time preference fix** (pending): Fixed `ClassCastException` when reading blend time preference (String vs Integer type mismatch)

#### Other Fixes
- **OPML import auto-download** (`1fce9c9bf`): Fixed auto-download triggering on OPML import
- **HTTP 416 handling** (`1fce9c9bf`): Fixed download resume for already-downloaded files

### UI/UX Changes

#### Branding
- **Rebranded from AntennaPod to PodFlow** (`111bf068b`, `def690fe1`)
- **New app icon** (`1fce9c9bf`)
- **Updated screenshots** (`def690fe1`, `a7208f5fa`, `3c935d6f5`)
- **Material 3 theme**: Custom color scheme and typography

**Files Created:**
- `app/src/main/kotlin/de/danoeh/antennapod/ui/theme/Color.kt`
- `app/src/main/kotlin/de/danoeh/antennapod/ui/theme/Type.kt`
- `app/src/main/kotlin/de/danoeh/antennapod/ui/theme/Shape.kt`
- `app/src/main/kotlin/de/danoeh/antennapod/ui/theme/Theme.kt`

#### Settings UI
- Added Radio Mode settings category in Playback preferences
- Added blend/crossfade time picker
- Added volume normalization toggle

**Files Modified:**
- `ui/preferences/src/main/res/xml/preferences_playback.xml`
  - Added Radio Mode preference switches
  - Added blend time list preference

### Configuration Changes

#### Default Settings (for Fresh Installs)
- Radio Mode: **ON** (was OFF)
- Audio crossfade: **30 seconds** (was 0/disabled)
- Volume normalization: **ON** (was OFF)
- Auto-download: **ON** (unchanged)

#### Build Configuration
- Package name: `app.podflow.player` (was `de.danoeh.antennapod`)
- App name: PodFlow (was AntennaPod)
- Updated icons and branding assets

**Files Modified:**
- `app/build.gradle`: Updated applicationId
- `build.gradle`: Updated dependencies
- `common.gradle`: Build configuration tweaks

### Documentation

#### New Documentation
- `docs/plans/2025-12-11-podflow-production-app-design.md`: Overall app design
- `docs/plans/2025-12-11-tiled-home-screen-design.md`: Tiled home screen design
- `docs/plans/2025-12-15-radio-mode-crash-fix-and-always-fresh-priority-design.md`: Radio Mode fixes and improvements

#### Updated Documentation
- `README.md`: Rebranded, updated features, added changelog
- `CONTRIBUTING.md`: Updated for PodFlow
- `CONTRIBUTORS.md`: PodFlow contributors

**Files Removed:**
- Cleaned up AntennaPod-specific documentation
- Removed outdated READMEs from submodules

### Testing

#### New Tests
- `app/src/test/kotlin/de/danoeh/antennapod/ui/screen/home/tiled/TiledHomeViewModelTest.kt`
  - Unit tests for TiledHomeViewModel

#### Modified Tests
- `app/src/androidTest/java/de/test/antennapod/ui/MainActivityTest.java`
  - Updated for TiledHomeFragment as default

### File Structure Summary

**Total Files Changed:** 40+ files modified/created

**Major Components:**
- **Playback Service** (Radio Mode logic): 5 files
- **UI Layer** (Tiled Home, Theme): 12 files
- **Database/Storage** (Radio Mode queries): 3 files
- **Download System** (Smart downloads): 2 files
- **Preferences** (Settings): 3 files
- **Documentation**: 5 files
- **Build Config**: 3 files

### Key Technical Decisions

1. **Why Jetpack Compose for Tiled Home?**
   - Modern, declarative UI
   - Better performance for grid layouts
   - Easier state management

2. **Why Always-Fresh Playback Priority?**
   - True "radio" experience prioritizes latest content
   - Users who want sequential playback can use Queue
   - Dynamic re-evaluation on every transition

3. **Why 30s Default Crossfade?**
   - Smooth transitions without being too long
   - Balances seamlessness with user control
   - Users can disable or adjust

4. **Why Latest-Only Downloads?**
   - Reduces storage usage
   - Focuses on fresh content (radio philosophy)
   - Same-day multi-episode support handles edge cases

### Future Roadmap (Not Yet Implemented)

- Smart algorithm to mix fresh + older unplayed content
- User preference for "always fresh" vs "sequential" Radio Mode
- Visual indicator showing "up next in Radio Mode"
- Statistics/history of auto-played episodes
- Hilt dependency injection migration

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0+ |
| UI Framework | Jetpack Compose |
| Design System | Material 3 |
| Architecture | MVVM + Clean Architecture |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt (planned) |
| Database | Room |
| Networking | Retrofit + OkHttp |
| Image Loading | Coil |
| Media | Media3 (ExoPlayer) |

## Screenshots

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01_home.png" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02_queue.png" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03_subscriptions.png" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04_player.png" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05_downloads.png" width="180" />
</p>

## Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 35

### Build Steps

```bash
# Clone the repository
git clone https://github.com/usathyan/PodFlow.git
cd podflow

# Build debug APK
./gradlew :app:assembleFreeDebug

# Install on connected device
./gradlew :app:installFreeDebug
```

## Project Structure

```
app/src/main/kotlin/de/danoeh/antennapod/
├── ui/
│   ├── theme/              # Material 3 theme (Color, Type, Shape, Theme)
│   └── screen/
│       ├── home/tiled/     # Tiled home screen
│       ├── player/         # Now Playing screen
│       └── discover/       # Discovery/Search screen
```

## License

PodFlow is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

This is a derivative work of [AntennaPod](https://github.com/AntennaPod/AntennaPod), an open-source podcast manager for Android.

### Attribution

```
PodFlow is built on the foundation of AntennaPod,
an open-source podcast manager for Android.
https://github.com/AntennaPod/AntennaPod

This app is free software licensed under GPL v3.
```

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## Acknowledgments

- [AntennaPod](https://github.com/AntennaPod/AntennaPod) - The foundation this app is built upon
- [Material Design 3](https://m3.material.io/) - Design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit

## Contact

- Issues: [GitHub Issues](https://github.com/usathyan/PodFlow/issues)

---

Made with love for podcast lovers everywhere.
