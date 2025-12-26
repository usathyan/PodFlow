# PodFlow Development Plans

> Last updated: December 25, 2025

## Completed

| Plan | Description | Date |
|------|-------------|------|
| [Tiled Home Screen](2025-12-11-tiled-home-screen-design.md) | Grid-based home screen with subscribed podcasts, play overlays, and auto-play | Dec 11 |
| [Audio Visualizer Design](2025-12-22-audio-visualizer-design.md) | Flip-card visualizer on NowPlayingScreen with Winamp-style spectrum | Dec 22 |
| [Audio Visualizer Implementation](2025-12-22-audio-visualizer-implementation.md) | 13-task implementation plan for visualizer feature | Dec 22 |
| [Visualizer Button](2025-12-23-visualizer-button-design.md) | Toolbar button to toggle visualizer (replaced long-press) | Dec 23 |

## Approved / In Progress

| Plan | Description | Status |
|------|-------------|--------|
| [iOS Cross-Platform Strategy](2025-12-25-ios-cross-platform-strategy.md) | Flutter rewrite for Android + iOS | **In Progress** |
| [PodFlow Production App](2025-12-11-podflow-production-app-design.md) | Complete tech stack, Material 3 UI, multi-phase roadmap | In Progress |
| [Radio Mode Crash Fix](2025-12-15-radio-mode-crash-fix-and-always-fresh-priority-design.md) | Fix ClassCastException, implement always-fresh priority | Approved |
| [Commute Radio Carousel](2025-12-15-commute-radio-carousel-design.md) | Horizontal carousel for daily commute use case | Approved |

## Future / Not Scheduled

| Plan | Description | Complexity |
|------|-------------|------------|
| [Cover Flow Home Screen](2025-12-23-cover-flow-design.md) | iPod-style 3D Cover Flow with reflections and momentum scrolling | High |
| [AI Podcast Player](2025-12-15-ai-podcast-player-design.md) | OpenRouter AI integration for smart playlist curation | High |

---

## Active: Flutter Cross-Platform Rewrite

**Decision:** Flutter (December 25, 2025)
**Branch:** `feature/flutter-rewrite`
**Project:** `/podflow_flutter/`

### Why Flutter?
- ~95% code sharing between Android and iOS
- Single codebase, single language (Dart)
- Strong audio ecosystem (just_audio, audio_service)
- Hot reload for fast development

### Project Structure (Created)
```
podflow_flutter/
├── lib/
│   ├── core/
│   │   ├── models/       # Podcast, Episode
│   │   ├── services/     # Audio, Network
│   │   └── repositories/ # Data access
│   ├── features/
│   │   ├── player/
│   │   ├── subscriptions/
│   │   ├── downloads/
│   │   └── visualizer/
│   └── shared/widgets/
└── test/
```

### Key Dependencies
- `just_audio` - Cross-platform audio playback
- `audio_service` - Background playback
- `flutter_riverpod` - State management
- `webfeed_plus` - RSS parsing
- `sqflite` - Local database

### PodFlow Features to Port (Priority)
1. Audio Visualizer (custom FFT implementation)
2. Cover Flow carousel
3. All playback features
4. Download management

See full details: [iOS Cross-Platform Strategy](2025-12-25-ios-cross-platform-strategy.md)
