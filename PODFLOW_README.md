# PodFlow - Modern Podcast Player

> Your podcasts, flowing seamlessly

PodFlow is a modern, beautifully designed podcast player for Android built with Jetpack Compose and Material 3. It's built on the foundation of AntennaPod, one of the most trusted open-source podcast apps.

## Features

### Core Features
- **Beautiful Tiled Home Screen** - Grid view of your subscribed podcasts with instant play
- **Smart Playback** - Variable speed, silence trimming, volume boost
- **Discovery** - Find new podcasts with search and category browsing
- **Offline Listening** - Download episodes for offline playback
- **Queue Management** - Smart queue with drag-and-drop reordering

### Playback Features
- Variable playback speed (0.5x - 3x)
- Silence trimming (Smart Speed)
- Volume boost/normalization
- Per-podcast speed settings
- Sleep timer with fade-out
- Chapter support

### Design
- Material 3 with Dynamic Color support
- Light, Dark, and AMOLED themes
- Beautiful animations and transitions
- Thumb-friendly navigation

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

[Screenshots to be added]

## Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 35

### Build Steps

```bash
# Clone the repository
git clone https://github.com/[your-repo]/podflow.git
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

- Issues: [GitHub Issues](https://github.com/[your-repo]/podflow/issues)

---

Made with love for podcast lovers everywhere.
