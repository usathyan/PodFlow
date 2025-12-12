# PodFlow - Modern Podcast Player
## Production App Design Document

### App Identity

**Name:** PodFlow
**Tagline:** "Your podcasts, flowing seamlessly"
**Package Name:** `com.podflow.app`
**License:** GPL v3 (derived from AntennaPod)
**Attribution:** "Built on the foundation of AntennaPod open-source project"

---

## Feature Requirements (Based on User Research)

### Must-Have Features (MVP)

Based on research from [Transistor](https://transistor.fm/global-stats/), [9to5Google](https://9to5google.com/2024/04/02/favorite-android-apps-replace-google-podcasts/), and [MakeUseOf](https://www.makeuseof.com/tag/4-app-showdown-which-is-the-best-android-podcast-app/):

#### 1. Beautiful Tiled Home Screen
- Grid view of subscribed podcasts with artwork
- Play button overlay for instant playback
- Downloaded episode count badges
- Pull-to-refresh

#### 2. Smart Playback Features
- Variable speed (0.5x - 3x)
- Silence trimming (Smart Speed)
- Volume boost/normalization
- Per-podcast speed settings
- Auto-skip intros/outros
- Sleep timer with fade-out

#### 3. Discovery & Search
- Podcast Index integration
- iTunes/Apple Podcasts search
- Trending/Popular podcasts
- Category browsing
- OPML import/export

#### 4. Queue & Downloads
- Smart queue management
- Auto-download new episodes
- Download over WiFi only option
- Storage management
- Episode filters (played/unplayed/downloaded)

#### 5. Sync & Backup
- Cloud sync (optional)
- Local backup/restore
- OPML export
- gpodder.net sync

### Nice-to-Have Features (Post-MVP)

#### 6. Advanced Features
- Chapter support with thumbnails
- Transcript view (when available)
- Bookmarks/clips
- Episode notes
- Share clips to social

#### 7. Customization
- Multiple themes (Light/Dark/AMOLED)
- Accent color picker
- Custom notification controls
- Widget variations
- Tablet layout optimization

---

## Technology Stack

### Core Architecture
```
Language:        Kotlin 2.0+ (100%)
UI Framework:    Jetpack Compose + Material 3
Architecture:    MVVM + Clean Architecture
DI:              Hilt
Async:           Kotlin Coroutines + Flow
Database:        Room
Networking:      Retrofit + OkHttp
Image Loading:   Coil
Media:           Media3 (ExoPlayer)
Navigation:      Compose Navigation
```

### Why This Stack:
1. **Kotlin 2.0** - Best performance, modern features, null safety
2. **Compose** - Declarative UI, 60fps rendering, less code
3. **Material 3** - Modern design, dynamic colors, Pixel-optimized
4. **Media3** - Google's latest media library, superior playback
5. **Hilt** - Standard DI, testable, Google-recommended
6. **Room** - Type-safe database, coroutines support

---

## UI/UX Design System

### Design Principles
1. **Thumb-friendly** - Important actions within thumb reach
2. **Glanceable** - Key info visible at a glance
3. **Fast** - < 100ms response to all taps
4. **Beautiful** - Podcast artwork as the star
5. **Accessible** - WCAG 2.1 AA compliant

### Color System (Material 3 Dynamic)
```kotlin
// Primary: Deep Purple (brand color)
val PodFlowPurple = Color(0xFF6750A4)

// Supports dynamic color on Android 12+
// Falls back to brand purple on older devices
```

### Navigation Structure
```
Bottom Navigation:
├── Home (Tiled podcast grid)
├── Discover (Search & browse)
├── Queue (Up next)
├── Downloads (Offline episodes)
└── Settings (Preferences)
```

### Key Screens

#### 1. Home Screen (Tiled Grid)
- 2-3 column adaptive grid
- Podcast artwork tiles (square, rounded corners)
- Centered play button overlay
- Download count badge (top-right)
- Podcast title (bottom overlay)
- Pull-to-refresh
- Floating mini-player at bottom

#### 2. Now Playing Screen
- Large artwork (60% of screen)
- Playback controls (play/pause, skip, rewind)
- Speed control (tap to cycle)
- Sleep timer button
- Progress slider with chapter markers
- Episode title and podcast name
- Queue button

#### 3. Podcast Detail Screen
- Hero artwork with gradient overlay
- Subscribe/Unsubscribe button
- Episode list (newest first)
- Filter tabs (All/Unplayed/Downloaded)
- Per-podcast settings

#### 4. Discover Screen
- Search bar at top
- Trending podcasts carousel
- Categories grid
- Recent searches

---

## Project Structure

```
com.podflow.app/
├── di/                     # Hilt modules
├── data/
│   ├── local/             # Room database
│   ├── remote/            # API services
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Domain models
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Use cases
├── ui/
│   ├── theme/             # Material 3 theme
│   ├── components/        # Reusable composables
│   ├── home/              # Home screen
│   ├── discover/          # Discovery screen
│   ├── player/            # Now playing
│   ├── podcast/           # Podcast detail
│   ├── queue/             # Queue management
│   ├── downloads/         # Downloads screen
│   └── settings/          # Settings
├── service/
│   └── playback/          # Media3 playback service
└── util/                  # Utilities
```

---

## Google Play Requirements

### Store Listing
- **Category:** Music & Audio > Podcasts
- **Content Rating:** Everyone
- **Target SDK:** 35 (Android 15)
- **Min SDK:** 24 (Android 7.0)

### Required Assets
- [ ] App Icon (512x512 PNG)
- [ ] Feature Graphic (1024x500)
- [ ] Screenshots (phone + tablet)
- [ ] Short description (80 chars)
- [ ] Full description (4000 chars)
- [ ] Privacy Policy URL
- [ ] Source Code URL (GPL requirement)

### Privacy & Data Safety
- No ads, no tracking
- Optional cloud sync (user-controlled)
- Local storage only by default
- GDPR compliant

---

## Implementation Phases

### Phase 1: Core App - COMPLETED
- [x] Material 3 theme and design system (Color, Type, Shape, Theme)
- [x] Home screen (tiled grid with animations)
- [x] Basic playback integration
- [x] Podcast subscription display
- [x] MiniPlayer component

### Phase 2: Enhanced Features - COMPLETED
- [x] Now Playing screen (full Compose UI)
- [x] Queue management screen
- [x] Discovery/Search screen
- [x] Settings screen
- [x] All unit tests passing

### Phase 3: Polish & Production - IN PROGRESS
- [ ] OPML import/export UI
- [ ] Notifications enhancement
- [ ] Widgets
- [ ] Performance optimization
- [ ] Accessibility audit
- [ ] Store listing assets
- [ ] Google Play deployment

---

## License Compliance

This app is a derivative work of AntennaPod and is licensed under GPL v3.

### Required:
1. Include GPL v3 license text
2. Credit AntennaPod as the source
3. Make source code publicly available
4. Include link to source repository
5. Document all modifications

### Attribution Text:
```
PodFlow is built on the foundation of AntennaPod,
an open-source podcast manager for Android.
https://github.com/AntennaPod/AntennaPod

This app is free software licensed under GPL v3.
Source code: https://github.com/[your-repo]/podflow
```
