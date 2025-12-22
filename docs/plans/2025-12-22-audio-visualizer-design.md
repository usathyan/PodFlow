# Audio Visualizer Feature Design

## Overview

Add an audio visualizer to PodFlow's NowPlayingScreen that users can reveal by tapping the album artwork. The visualizer enhances the listening experience by making the app feel more alive and engaging during playback.

## Goals

- Enhance the listening experience with reactive visual feedback
- Provide a nostalgic Winamp-inspired aesthetic as the primary style
- Keep the feature unobtrusive (album artwork remains the default view)
- Offer 2-3 visualizer style options for variety

## Interaction Model

### The Flip Card Concept

The album artwork on NowPlayingScreen becomes a flippable card with two sides:

- **Front (default)**: Album artwork, as it exists today
- **Back**: Audio visualizer

### User Interactions

| Action | Result |
|--------|--------|
| Tap album artwork | 3D flip animation reveals visualizer |
| Tap visualizer | Flip back to album artwork |
| Swipe left/right on visualizer | Cycle through visualizer styles |

### Visual Feedback

- Flip animation: ~300ms horizontal 3D rotation
- Small visual hint on artwork to indicate it's tappable
- Dot indicators below visualizer showing current style position

### State Persistence

- Visualizer state persists during the session
- Resets to album artwork on app restart

## Visualizer Styles

### Style 1: Classic Winamp (Default)

The iconic combination visualizer:

- **Spectrum Analyzer Bars**: 32-64 vertical bars representing frequency bands (bass on left, treble on right)
- **Color Gradient**: Green at bottom, yellow in middle, red at peaks
- **Peak Indicators**: Small lines above each bar that rise with peaks and fall slowly
- **Oscilloscope Overlay**: Centered horizontal waveform line showing the actual audio signal
- **Background**: Dark/black to make colors pop

### Style 2: Circular Pulse

Modern alternative with radial design:

- **Circular Bars**: Frequency bars arranged in a circle, radiating outward from center
- **Color**: PodFlow purple/teal gradient or extracted from album artwork
- **Center**: Small circular area
- **Pulse Effect**: Circle subtly breathes/pulses with overall audio energy

### Style 3: Minimal Waveform

Clean, understated option:

- **Single Line**: Smooth oscilloscope waveform across the center
- **Color**: White or single accent color
- **Background**: Blurred/darkened album artwork
- **Feel**: Less distracting, more ambient

## Technical Architecture

### Audio Data Flow

```
ExoPlayer → Android Visualizer API → VisualizerManager → StateFlow → Compose UI
```

### Key Components

| Component | Responsibility |
|-----------|----------------|
| `VisualizerManager` | Attaches to audio session, captures FFT & waveform data |
| `VisualizerState` | Data class holding frequency bars, waveform points, current style |
| `VisualizerViewModel` | Processes raw audio data into display-ready values |
| `FlipCard` composable | Handles flip animation between artwork and visualizer |
| `VisualizerCanvas` composable | Renders the actual visualizer using Compose Canvas |

### Performance Considerations

- Visualizer updates at ~30fps (balance between smoothness and battery)
- Only active when visualizer is visible (pauses when flipped to artwork)
- Uses Android's native `Visualizer` class (requires `RECORD_AUDIO` permission)
- Smoothing applied to prevent jarring jumps between frames

### Permission Handling

- `RECORD_AUDIO` permission required for Visualizer API
- Request on first tap of album artwork with explanation
- Graceful fallback: if denied, show simpler animation based on playback position

## File Structure

### New Files

```
app/src/main/kotlin/de/danoeh/antennapod/
├── ui/
│   ├── components/
│   │   ├── FlipCard.kt              # Reusable flip animation composable
│   │   └── visualizer/
│   │       ├── VisualizerCanvas.kt  # Main visualizer rendering
│   │       ├── WinampVisualizer.kt  # Style 1: Bars + oscilloscope
│   │       ├── CircularVisualizer.kt # Style 2: Radial design
│   │       └── WaveformVisualizer.kt # Style 3: Minimal waveform
│   └── screen/player/
│       └── VisualizerViewModel.kt   # Visualizer state management
│
├── audio/
│   └── VisualizerManager.kt         # Audio data capture from ExoPlayer
│
└── model/
    └── VisualizerState.kt           # Data classes for visualizer state
```

### Files to Modify

| File | Change |
|------|--------|
| `NowPlayingScreen.kt` | Replace artwork Image with FlipCard composable |
| `NowPlayingViewModel.kt` | Add visualizer visibility state |
| `AndroidManifest.xml` | Add RECORD_AUDIO permission |
| `ExoPlayerWrapper.java` | Expose audio session ID for Visualizer API |

## Implementation Phases

### Phase 1: Foundation
- Create `VisualizerManager` to capture audio data via Android Visualizer API
- Expose audio session ID from `ExoPlayerWrapper`
- Add `RECORD_AUDIO` permission with rationale dialog
- Create `VisualizerState` data classes

### Phase 2: Core UI Components
- Build `FlipCard` composable with 3D flip animation
- Integrate into `NowPlayingScreen` replacing static artwork
- Add flip state to `NowPlayingViewModel`
- Implement tap-to-flip interaction

### Phase 3: Winamp Visualizer (Style 1)
- Create `WinampVisualizer` composable with Canvas
- Implement spectrum bars with gradient coloring
- Add peak indicators with slow fall-off
- Overlay oscilloscope waveform
- Connect to live audio data via StateFlow

### Phase 4: Additional Styles
- Implement `CircularVisualizer` (Style 2)
- Implement `WaveformVisualizer` (Style 3)
- Add swipe gesture to cycle styles
- Add dot indicators for current style

### Phase 5: Polish
- Performance optimization
- Smooth transitions between styles
- Handle edge cases (no audio, permission denied, background playback)
- Testing across device sizes
