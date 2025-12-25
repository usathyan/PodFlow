# PodFlow iOS & Cross-Platform Strategy

> **Status:** Planning
> **Priority:** Best long-term architecture
> **Goal:** Run PodFlow on both Android and iOS/iPadOS

## Current State Analysis

### Codebase Composition
| Language | Files | Notes |
|----------|-------|-------|
| Java | 598 | AntennaPod base (inherited) |
| Kotlin | 27 | PodFlow additions (visualizer, etc.) |
| XML | ~200+ | Android layouts, resources |

### PodFlow-Specific Features (Priority to Port)
1. **Audio Visualizer** - Winamp-style spectrum with flowing waves
   - `VisualizerManager.kt` - Audio data capture
   - `StudioVisualizer.kt`, `LiquidVisualizer.kt` - Render styles
   - `VisualizerViewModel.kt` - State management
   - Uses Android `Visualizer` API (platform-specific)

2. **UI Enhancements**
   - Toolbar visualizer toggle button
   - Dark theme optimizations
   - Cover art display improvements

3. **Planned Features**
   - Cover Flow carousel (iPod-style 3D)
   - AI playlist curation (future)

### AntennaPod Base Features (Also Port)
- Podcast subscription & RSS parsing
- Episode download management
- Audio playback with streaming
- Background playback service
- Sleep timer
- Playback speed control
- Chapter support
- Queue management
- OPML import/export

---

## Architecture Options

### Option A: Kotlin Multiplatform (KMP)
**Approach:** Share business logic, platform-specific UIs

```
┌─────────────────────────────────────────────────────────┐
│                    Shared (KMP)                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │ Data Models │ │ Repositories│ │ Business Logic  │   │
│  │ (Podcast,   │ │ (Feed, DB,  │ │ (Playback state,│   │
│  │  Episode)   │ │  Network)   │ │  Queue logic)   │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
         │                                    │
         ▼                                    ▼
┌─────────────────────┐          ┌─────────────────────┐
│   Android App       │          │     iOS App         │
│ ┌─────────────────┐ │          │ ┌─────────────────┐ │
│ │ Jetpack Compose │ │          │ │    SwiftUI      │ │
│ │ ExoPlayer       │ │          │ │    AVFoundation │ │
│ │ Android Viz API │ │          │ │    Accelerate   │ │
│ └─────────────────┘ │          │ └─────────────────┘ │
└─────────────────────┘          └─────────────────────┘
```

| Pros | Cons |
|------|------|
| Keep existing Kotlin code | iOS UI must be written in Swift/SwiftUI |
| Gradual migration possible | Learning curve for iOS-specific APIs |
| Native performance | Two UI codebases to maintain |
| Mature ecosystem (JetBrains) | Visualizer needs platform-specific impl |

**Code Reuse:** ~50-60% (business logic only)

---

### Option B: Flutter (Full Rewrite)
**Approach:** Single codebase for everything

```
┌─────────────────────────────────────────────────────────┐
│                   Flutter (Dart)                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │ Data Models │ │ Repositories│ │ Business Logic  │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │  UI Widgets │ │ State Mgmt  │ │ Platform Plugins│   │
│  │  (Shared)   │ │ (Riverpod)  │ │ (audio, viz)    │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
         │                                    │
         ▼                                    ▼
┌─────────────────────┐          ┌─────────────────────┐
│   Android Runtime   │          │     iOS Runtime     │
│   (Native plugins)  │          │   (Native plugins)  │
└─────────────────────┘          └─────────────────────┘
```

| Pros | Cons |
|------|------|
| Single codebase (~95% shared) | Complete rewrite required |
| Hot reload for fast dev | Dart learning curve |
| Great UI toolkit | Some native plugins needed |
| Strong audio plugin ecosystem | Larger app size |

**Code Reuse:** ~90-95%

**Relevant Flutter Packages:**
- `just_audio` - Cross-platform audio playback
- `audio_service` - Background playback
- `flutter_audio_query` - Audio visualization data
- `webfeed` - RSS/Atom parsing

---

### Option C: Native iOS (Separate Codebase)
**Approach:** Dedicated Swift/SwiftUI iOS app

```
┌─────────────────────┐          ┌─────────────────────┐
│   Android (Kotlin)  │          │   iOS (Swift)       │
│ ┌─────────────────┐ │          │ ┌─────────────────┐ │
│ │ Current PodFlow │ │          │ │ New PodFlow iOS │ │
│ │ (Keep as-is)    │ │          │ │ (SwiftUI)       │ │
│ └─────────────────┘ │          │ └─────────────────┘ │
└─────────────────────┘          └─────────────────────┘
```

| Pros | Cons |
|------|------|
| Best native experience | Two codebases to maintain |
| No migration needed for Android | Features may drift apart |
| Full platform capabilities | Double the development effort |
| Familiar Swift ecosystem | No code sharing |

**Code Reuse:** 0% (design/UX can be shared)

---

## Recommendation

### For Best Long-Term Architecture: **Kotlin Multiplatform (KMP)**

**Rationale:**
1. **Preserves Investment** - Existing Kotlin code (visualizer) can be kept
2. **Gradual Migration** - Don't need full rewrite, can migrate incrementally
3. **Native UIs** - SwiftUI for iOS gives best Apple experience
4. **Future-Proof** - KMP is JetBrains' strategic direction, growing ecosystem
5. **Compose Multiplatform** - Can eventually share UI too (maturing rapidly)

### Migration Path

```
Phase 1: Extract Shared Module (2-3 weeks)
├── Create :shared KMP module
├── Move data models (Podcast, Episode, Feed)
├── Move repository interfaces
└── Keep Android app working throughout

Phase 2: iOS Foundation (3-4 weeks)
├── Set up iOS project with SwiftUI
├── Integrate shared KMP module
├── Implement basic podcast browsing
└── Audio playback with AVFoundation

Phase 3: Feature Parity - Core (4-6 weeks)
├── Episode downloads
├── Background playback
├── Queue management
├── Sleep timer

Phase 4: PodFlow Features (3-4 weeks)
├── Audio visualizer (iOS: Accelerate framework)
├── Cover Flow carousel
├── UI polish and animations

Phase 5: Polish & Release (2-3 weeks)
├── Testing on devices
├── App Store submission
├── Beta testing
```

**Total Estimated Timeline:** 14-20 weeks for iOS feature parity

---

## Platform-Specific Considerations

### Audio Visualizer on iOS

Android uses `Visualizer` API attached to audio session. iOS equivalent:

```swift
// iOS approach using AVAudioEngine + Accelerate
let audioEngine = AVAudioEngine()
let playerNode = AVAudioPlayerNode()

// Tap the audio for FFT data
playerNode.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, time in
    // Use Accelerate framework for FFT
    var realParts = [Float](repeating: 0, count: bufferSize/2)
    var imagParts = [Float](repeating: 0, count: bufferSize/2)
    // vDSP_fft_zrip for FFT computation
    // Send to visualizer UI
}
```

### Background Playback

| Android | iOS |
|---------|-----|
| Foreground Service | Audio Background Mode |
| MediaBrowserService | MPNowPlayingInfoCenter |
| ExoPlayer | AVPlayer / AVAudioEngine |

### Data Persistence

**Shared (KMP):**
- SQLDelight for database (generates Kotlin & Swift)
- Ktor for networking
- Kotlinx.serialization for JSON

---

## Alternative: Start Fresh with Flutter

If willing to do a full rewrite, Flutter offers:
- Faster time to both platforms (~12-16 weeks)
- Single codebase maintenance
- Existing podcast app templates/examples

**Recommended Flutter architecture:**
```
lib/
├── core/
│   ├── models/
│   ├── services/
│   └── repositories/
├── features/
│   ├── player/
│   ├── subscriptions/
│   ├── downloads/
│   └── visualizer/
├── shared/
│   └── widgets/
└── main.dart
```

---

## Decision Matrix

| Criteria | KMP | Flutter | Native iOS |
|----------|-----|---------|------------|
| Code Reuse | 60% | 95% | 0% |
| Time to iOS | 14-20 wks | 12-16 wks | 16-24 wks |
| Native Feel | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Maintenance | Medium | Low | High |
| Learning Curve | Medium | Medium | Low (if know Swift) |
| Long-term | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Keeps Android Code | Yes | No | Yes |

---

## Next Steps

1. **Decision:** Choose KMP vs Flutter vs Native
2. **Branch:** Create `feature/ios-app` or `feature/flutter-rewrite`
3. **Prototype:** Build minimal audio player on chosen platform
4. **Iterate:** Add features incrementally

---

## Questions to Resolve

1. Do you have iOS development experience (Swift/Xcode)?
2. Is maintaining two UI codebases acceptable (KMP) or prefer single codebase (Flutter)?
3. Timeline expectations - when do you want iOS version?
4. Will you publish to App Store yourself or need help with that process?
