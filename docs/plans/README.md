# PodFlow Development Plans

> Last updated: December 24, 2025

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
| [PodFlow Production App](2025-12-11-podflow-production-app-design.md) | Complete tech stack, Material 3 UI, multi-phase roadmap | In Progress |
| [Radio Mode Crash Fix](2025-12-15-radio-mode-crash-fix-and-always-fresh-priority-design.md) | Fix ClassCastException, implement always-fresh priority | Approved |
| [Commute Radio Carousel](2025-12-15-commute-radio-carousel-design.md) | Horizontal carousel for daily commute use case | Approved |

## Future / Not Scheduled

| Plan | Description | Complexity |
|------|-------------|------------|
| [iOS Cross-Platform Strategy](2025-12-25-ios-cross-platform-strategy.md) | Run PodFlow on Android AND iOS/iPadOS | **Major** |
| [Cover Flow Home Screen](2025-12-23-cover-flow-design.md) | iPod-style 3D Cover Flow with reflections and momentum scrolling | High |
| [AI Podcast Player](2025-12-15-ai-podcast-player-design.md) | OpenRouter AI integration for smart playlist curation | High |

---

## Next Up: iOS Cross-Platform

**Target:** Run PodFlow on both Android AND iOS/iPadOS

### Readiness Assessment

**Prerequisites (Complete):**
- [x] Android app stable and published to Play Store
- [x] PodFlow-specific features implemented (visualizer)
- [x] Modular codebase structure
- [x] Kotlin code for new features

**Architecture Decision Required:**

| Option | Code Reuse | Timeline | Recommendation |
|--------|------------|----------|----------------|
| **Kotlin Multiplatform** | ~60% | 14-20 weeks | ⭐ Best long-term |
| Flutter | ~95% | 12-16 weeks | Full rewrite |
| Native iOS | 0% | 16-24 weeks | Most effort |

### PodFlow Features to Port (Priority)
1. Audio Visualizer (platform-specific APIs)
2. Cover Flow carousel
3. All playback features
4. Download management

### Recommended Approach (KMP)
1. Create feature branch: `feature/ios-kmp`
2. Extract shared module with data models
3. Set up iOS project with SwiftUI
4. Implement audio playback (AVFoundation)
5. Port visualizer (Accelerate framework)
6. Add remaining features incrementally

See full details: [iOS Cross-Platform Strategy](2025-12-25-ios-cross-platform-strategy.md)
