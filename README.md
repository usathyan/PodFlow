# PodFlow

PodFlow is a fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod) modified for continuous playback with automatic episode management.

<table>
  <tr>
    <td><img src="docs/images/Screenshot_20251217-122515.png" width="160" /></td>
    <td><img src="docs/images/Screenshot_20251217-122538.png" width="160" /></td>
    <td><img src="docs/images/Screenshot_20251217-122600.png" width="160" /></td>
    <td><img src="docs/images/Screenshot_20251217-122615.png" width="160" /></td>
    <td><img src="docs/images/Screenshot_20251217-122635.png" width="160" /></td>
  </tr>
</table>

## What's Different from AntennaPod

### Core Modifications

**Radio Mode**
- Automatic playback: Episodes advance to the next podcast when finished
- Automatic deletion: Played episodes are removed when the next episode from that podcast is downloaded
- Volume normalization: Audio levels are equalized across different podcasts using Android's DynamicsProcessing API
- Gapless playback: Seamless transitions between podcasts using Media3's built-in gapless support
- Skip podcast controls: Dedicated buttons in player to jump to previous/next podcast
- Wrap-around playback: Plays through all podcasts in sequence, loops back to start
- Default behavior: Enabled on fresh installs

**Home Screen**
- Horizontal carousel of podcast covers for quick visual selection
- Direct playback: Tap any podcast cover to start playing
- Visual states: Unplayed (full color), In-progress (progress ring), Completed (grayed out)
- Session tracking: Completed podcasts gray out, progress persists across app restarts
- Daily reset: Fresh session each day with all podcasts unplayed
- Sorting: Podcasts ordered by latest episode date
- Implemented using Jetpack Compose

**Download Behavior**
- Latest episode only: Auto-download retrieves only the most recent unplayed episode per podcast
- Multi-episode support: Downloads all episodes if multiple are published on the same day
- HTTP 416 handling: Prevents re-downloading already downloaded files

**Inbox Display**
- Episode count: Shows total new episodes across all podcasts (not podcast count)

**Queue (Playlist Management)**
- The queue is your playlist: Episodes play in queue order from top to bottom
- Drag to reorder: Long-press and drag episodes to change playback sequence
- Auto-advances: When an episode finishes, the next queued episode plays automatically
- Add episodes: Swipe or tap "Add to Queue" on any episode to include it
- Remove episodes: Swipe to remove from queue without deleting the download
- Lock queue: Prevent accidental reordering with the lock toggle
- Smart enqueue: New downloads automatically added to queue (configurable position: front, back, or after current)

### Comparison Table

| Feature | AntennaPod | PodFlow |
|---------|------------|---------|
| Home screen | Episode list | Horizontal podcast carousel |
| Episode selection | Manual | Automatic (latest) |
| Playback flow | Manual queue management | Automatic advancement with wrap-around |
| Audio transitions | None | Gapless playback (Media3) |
| Session tracking | None | Daily session with visual progress |
| Download strategy | All new episodes | Latest episode per podcast |
| Episode lifecycle | Manual deletion | Auto-delete after playback |
| Volume handling | Per-episode manual boost | Automatic normalization |
| Audio visualizer | None | Studio/Liquid wave styles |
| Default mode | Standard playback | Radio Mode |

### Feature Details

**Radio Mode Playback Logic**
- Checks for additional same-day episodes from current podcast first
- Advances to next podcast with downloaded episodes if none found
- Respects skip intro/outro settings per podcast
- Gapless transition using Media3's built-in playlist support

**Download Algorithm**
- Queries for latest episode per subscribed podcast
- Downloads if episode is unplayed and not already downloaded
- Handles same-day multi-episode scenarios
- Prevents duplicate downloads via HTTP range requests

**Volume Normalization**
- LoudnessEnhancer: Adjusts perceived loudness to target level
- DynamicsProcessing: Compresses dynamic range and limits peaks
- Auto-enabled: Activates when Radio Mode is enabled
- Per-podcast override: Can be disabled in podcast settings

**Audio Visualizer**
- Tap album art to reveal visualizer: Tap the album artwork on the player screen to flip and show the visualizer
- Two visualization styles:
  - **Studio**: Smooth flowing waves with pulsating colors that respond to audio frequencies
  - **Liquid**: Organic morphing blob with color-shifting gradients
- Swipe to change styles: Swipe left/right on visualizer to switch between styles
- Style indicator dots: Visual indicator shows current style
- Long-press to play/pause: While visualizer is showing, long-press to control playback
- Speech-optimized: FFT processing tuned for podcast vocals with noise floor filtering
- Requires RECORD_AUDIO permission: Needed to capture audio data for visualization

**Android Auto**
- Full Android Auto integration for hands-free listening
- Radio Mode quick-start: One tap to start playing your queue
- Browse queue, downloads, episodes, and subscriptions from car display
- Voice commands: "Hey Google, play my podcasts"
- Playback controls: play/pause, skip, next/previous

### Inherited Features

All standard AntennaPod features remain available:
- Variable playback speed (0.5x-3x)
- Silence trimming
- Sleep timer with shake-to-reset
- Chapter support
- OPML import/export
- Streaming and offline playback
- Chromecast support
- Per-podcast skip intro/outro settings

---

## Technology Stack

PodFlow builds on AntennaPod's mature codebase while adding modern Android components for new features.

| Component | AntennaPod (Inherited) | PodFlow (Added) |
|-----------|------------------------|-----------------|
| Language | Java | + **Kotlin** |
| UI Framework | XML Views + Fragments | + **Jetpack Compose** (carousel home) |
| Design System | Material 2 | + **Material 3** (new screens) |
| Architecture | MVP-style | + **MVVM** (Compose screens) |
| Async | RxJava3 | + **Kotlin Coroutines** |
| DI | Manual/Service Locator | Hilt (planned) |
| Database | Room | Room |
| Networking | OkHttp | OkHttp |
| Image Loading | Glide | + **Coil** (Compose screens) |
| Media | ExoPlayer (single) | ExoPlayer (gapless playback) |
| Audio Visualization | None | **Android Visualizer API** (FFT + Waveform) |

**Note**: PodFlow is a hybrid codebase. New features (carousel home) use modern Kotlin/Compose, while inherited screens (subscriptions, queue, settings) retain the original Java/XML implementation. This allows rapid feature development while maintaining stability of the battle-tested AntennaPod core.

## Installation

### Download from Releases (Recommended)

Download the latest signed APK from [GitHub Releases](https://github.com/usathyan/PodFlow/releases):

1. Go to the [Releases page](https://github.com/usathyan/PodFlow/releases)
2. Download `PodFlow-vX.X.X.apk`
3. Enable "Install from unknown sources" on your Android device
4. Install the APK

### Google Play

Coming soon!

---

## Building from Source

### Prerequisites
- JDK 17
- Android SDK with API 35
- (Optional) Android Studio Hedgehog or newer

### Local Development Build

```bash
# Clone the repository
git clone https://github.com/usathyan/PodFlow.git
cd PodFlow

# Build debug APK
./gradlew assemblePlayDebug

# Install on connected device
./gradlew installPlayDebug
```

### Release Build (via GitHub Actions)

Release builds are automated via GitHub Actions:

1. **Manual build**: Go to [Actions](https://github.com/usathyan/PodFlow/actions) → "Build Release" → "Run workflow"
2. **Tagged release**: Push a version tag to create a GitHub Release with signed APK/AAB

```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```

This automatically:
- Builds signed AAB (for Google Play)
- Builds signed APK (for direct install)
- Creates a GitHub Release with both files attached
- Generates release notes from commits

## Project Structure

```
app/src/main/kotlin/de/danoeh/antennapod/
├── ui/
│   ├── theme/              # Material 3 theme (Color, Type, Shape, Theme)
│   ├── visualizer/         # Audio visualizer components
│   │   ├── VisualizerManager.kt      # Audio capture using Android Visualizer API
│   │   ├── VisualizerViewModel.kt    # State management for visualizer
│   │   ├── StudioVisualizer.kt       # Flowing waves with pulsating colors
│   │   └── LiquidVisualizer.kt       # Organic morphing blob style
│   └── screen/
│       ├── home/
│       │   ├── carousel/   # Carousel home screen (CommuteSession, ViewModel, UI)
│       │   └── tiled/      # Fragment wrapper
│       ├── player/         # Now Playing screen (skip podcast buttons)
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
