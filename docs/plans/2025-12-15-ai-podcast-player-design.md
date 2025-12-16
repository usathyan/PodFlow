# AI Podcast Player - Design Document

**Date**: 2025-12-15
**Status**: Approved
**Base**: PixelPlayer (MIT License)

---

## Executive Summary

Build a focused, AI-powered podcast player by forking PixelPlayer and adding podcast capabilities. This approach prioritizes simplicity, modern architecture, and AI-first design over feature completeness.

---

## Decision: PixelPlayer vs PodFlow (AntennaPod)

### Comparison Table

| Factor | PodFlow (AntennaPod) | PixelPlayer + Podcast |
|--------|---------------------|----------------------|
| **Codebase Size** | ~200k+ lines | ~30k lines |
| **Language** | Java + Kotlin (mixed) | 100% Kotlin |
| **UI Framework** | XML + Compose (mixed) | 100% Compose |
| **Design System** | Material 2 + 3 (mixed) | 100% Material 3 |
| **Architecture** | MVP-ish + MVVM (mixed) | Clean MVVM |
| **Dependency Injection** | Manual/Service Locator | Hilt |
| **Async** | RxJava + Coroutines (mixed) | 100% Coroutines |
| **License** | GPL v3 (restrictive) | MIT (permissive) |
| **Technical Debt** | 10+ years accumulated | Minimal |
| **Podcast Features** | Complete | Need to add |
| **Crossfade** | Implemented | Need to port |
| **Volume Normalization** | Has it | Need to add |
| **AI Integration** | None | Gemini ready |

### Rationale

**Why PixelPlayer wins:**

1. **Alignment with Goals**: User wants simple, focused, AI-first. PixelPlayer is a clean canvas without bloat.

2. **Adding vs Fixing**: Adding podcast features to clean code (~2-3 weeks) is faster than refactoring 10 years of legacy (~months of ongoing work).

3. **Knowledge Transfer**: We learned crossfade, volume normalization, and podcast patterns from PodFlow. We port the *knowledge*, not the *code*.

4. **License Freedom**: MIT allows commercial use, proprietary modifications. GPL v3 requires open-sourcing all derivatives.

5. **Architectural Consistency**: 100% Kotlin + Compose + Hilt + Coroutines means one way to do things, better tooling, easier maintenance.

6. **Scope Control**: Start minimal, add only what's needed. PodFlow would require removing/hiding unwanted features.

---

## Product Vision

### Core Philosophy
> "One tap to start your perfectly curated podcast session"

### Target User
Podcast listeners who want effortless, intelligent playback without fiddling with settings. The app thinks ahead so you don't have to.

### Key Differentiators
1. **AI-Curated Playlists**: OpenRouter AI arranges episodes based on mood, time available, listening history
2. **Seamless Audio**: True crossfade, volume normalization - no jarring transitions
3. **Session-Based**: "Commute", "Work", "Evening Wind-down" - named listening contexts
4. **Zero-Config Start**: Import OPML or search, one tap to play

---

## Feature Specification

### Phase 1: Foundation (MVP)

#### 1.1 Subscription Management
- OPML import/export
- Podcast search via iTunes API / PodcastIndex
- Subscribe/unsubscribe
- Feed refresh (manual + background)
- Default to latest episodes per subscription

#### 1.2 Episode Management
- Episode list per subscription
- Stream or download
- Download queue with priority
- Storage management (auto-delete old)
- Playback position tracking per episode

#### 1.3 Playback Engine
- Media3/ExoPlayer foundation (from PixelPlayer)
- Background playback with notification
- Lock screen controls
- Streaming + local file playback

#### 1.4 Basic Home Screen
- Subscribed podcasts grid/list
- "Play All Latest" one-tap button
- Currently playing mini-player
- Quick access to downloads

### Phase 2: Audio Polish

#### 2.1 True Crossfade
- Dual ExoPlayer architecture (port from PodFlow)
- Configurable duration (default 5s)
- Triggers on: skip button, last 30s of episode, episode end
- Smooth volume curves (not linear)

#### 2.2 Volume Normalization
- ReplayGain scanning for downloads
- Real-time loudness adjustment for streams
- Target loudness: -16 LUFS (podcast standard)
- Per-episode gain stored in database

#### 2.3 Gapless Playback
- Pre-buffer next episode
- Zero gap between episodes when crossfade disabled

### Phase 3: AI Intelligence

#### 3.1 OpenRouter Integration
- API client for OpenRouter.ai
- Model selection (Claude, GPT-4, Llama, etc.)
- Prompt templates for playlist curation
- Conversation context for refinement

#### 3.2 Smart Playlists
- Named sessions: "Daily Commute", "Work Focus", "Evening Relaxed"
- Time-aware: fits episodes to available duration
- Mood-aware: AI considers episode topics, your history
- Learning: improves recommendations over time

#### 3.3 AI Features
- "Surprise me" - AI picks based on mood prompt
- Episode summaries (AI-generated)
- "Skip to interesting parts" - AI chapter markers
- Cross-podcast topic threading

### Phase 4: Polish & Delight

#### 4.1 Widgets
- Home screen playback widget (Glance)
- "Start Commute" quick action widget

#### 4.2 Personalization
- Listening statistics
- Playback speed per podcast (remembered)
- Skip intro/outro (learned per podcast)

#### 4.3 Social (Optional)
- Share playlist sessions
- Import friend's subscriptions

---

## Technical Architecture

### Stack
```
Language:       Kotlin 2.0+
UI:             Jetpack Compose + Material 3
Architecture:   MVVM + Clean Architecture
DI:             Hilt
Async:          Kotlin Coroutines + Flow
Database:       Room
Networking:     Retrofit + OkHttp
Media:          Media3 (ExoPlayer)
Images:         Coil
AI:             OpenRouter API (Retrofit)
```

### Module Structure
```
app/
├── data/
│   ├── local/          # Room DB, DAOs, entities
│   ├── remote/         # Retrofit services (RSS, iTunes, OpenRouter)
│   └── repository/     # Repository implementations
├── di/                 # Hilt modules
├── domain/
│   ├── model/          # Domain models (Podcast, Episode, Playlist)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic
├── playback/
│   ├── service/        # MediaService, crossfade logic
│   ├── normalization/  # Volume normalization
│   └── queue/          # Playlist/queue management
├── ai/
│   ├── client/         # OpenRouter API client
│   ├── prompts/        # Prompt templates
│   └── curator/        # AI playlist curation logic
├── presentation/
│   ├── home/           # Home screen
│   ├── subscriptions/  # Podcast management
│   ├── player/         # Now playing screen
│   ├── playlists/      # Session/playlist management
│   └── settings/       # App settings
└── ui/
    ├── theme/          # Material 3 theme
    ├── components/     # Reusable composables
    └── widget/         # Glance widgets
```

### Data Models

```kotlin
// Core entities
data class Podcast(
    val id: Long,
    val feedUrl: String,
    val title: String,
    val author: String,
    val imageUrl: String?,
    val description: String,
    val lastUpdated: Instant,
    val subscribed: Boolean
)

data class Episode(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val audioUrl: String,
    val duration: Duration,
    val publishDate: Instant,
    val description: String,
    val imageUrl: String?,
    val playbackPosition: Duration,
    val completed: Boolean,
    val downloaded: Boolean,
    val localPath: String?,
    val gainDb: Float?  // Volume normalization
)

data class ListeningSession(
    val id: Long,
    val name: String,  // "Daily Commute", "Work", etc.
    val targetDuration: Duration?,
    val episodeIds: List<Long>,
    val aiGenerated: Boolean,
    val createdAt: Instant
)
```

---

## Implementation Plan

### Week 1: Foundation
- [ ] Fork PixelPlayer, rename to "PodAI" (or chosen name)
- [ ] Set up podcast data models (Room entities)
- [ ] Implement RSS feed parser
- [ ] Implement OPML import/export
- [ ] Basic subscription management UI

### Week 2: Playback & Downloads
- [ ] Adapt Media3 service for podcast streaming
- [ ] Episode download manager
- [ ] Playback position persistence
- [ ] Basic home screen with subscriptions
- [ ] "Play Latest" functionality

### Week 3: Audio Polish
- [ ] Port dual ExoPlayer crossfade from PodFlow
- [ ] Implement volume normalization (ReplayGain)
- [ ] Gapless playback setup
- [ ] Crossfade settings UI

### Week 4: AI Integration
- [ ] OpenRouter API client
- [ ] Prompt engineering for playlist curation
- [ ] Smart playlist generation
- [ ] Named sessions ("Commute", "Work", etc.)
- [ ] AI-powered home screen suggestions

### Week 5: Polish
- [ ] Widgets (Glance)
- [ ] Settings screens
- [ ] Error handling & edge cases
- [ ] Performance optimization
- [ ] Beta testing

---

## Success Metrics

1. **Time to First Play**: < 60 seconds from install to listening
2. **Session Start**: One tap from home screen
3. **Audio Quality**: No volume jumps, smooth transitions
4. **AI Relevance**: >80% of AI suggestions accepted

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| RSS parsing edge cases | Use battle-tested library (Rome, kotlin-rss) |
| OpenRouter API costs | Cache responses, batch requests, fallback to local logic |
| ExoPlayer complexity | Start with PixelPlayer's working implementation |
| Scope creep | Strict MVP, defer nice-to-haves to Phase 4 |

---

## Open Questions

1. **App Name**: PodAI? FlowCast? CommuteCast?
2. **Monetization**: Free with AI limits? Premium subscription?
3. **Sync**: Local-only MVP, or cloud sync from start?

---

## Appendix A: Learnings from PodFlow

Key implementations to port (concepts, not code):

1. **Dual ExoPlayer Crossfade**
   - Create secondary player 30s before episode end
   - Simultaneous playback with inverse volume curves
   - Clean handoff and resource release

2. **Volume Normalization**
   - Scan downloaded episodes for loudness
   - Store gain adjustment per episode
   - Apply via ExoPlayer's audio processor

3. **Download Management**
   - Priority queue with pause/resume
   - Partial download recovery
   - Storage quota management

---

## Appendix B: MVP Implementation Checklist

### Prerequisites
- [ ] Clone PixelPlayer repository
- [ ] Verify build succeeds locally
- [ ] Create new repository (e.g., `PodAI`)
- [ ] Update package name (`com.podai.app` or similar)
- [ ] Update app name and icons
- [ ] Commit clean fork as baseline

---

### Phase 1: Data Layer

#### 1.1 Database Schema
- [ ] Create `PodcastEntity` Room entity
  ```kotlin
  @Entity(tableName = "podcasts")
  data class PodcastEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val feedUrl: String,
      val title: String,
      val author: String,
      val imageUrl: String?,
      val description: String,
      val lastFetched: Long,
      val subscribed: Boolean
  )
  ```
- [ ] Create `EpisodeEntity` Room entity
  ```kotlin
  @Entity(tableName = "episodes")
  data class EpisodeEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val podcastId: Long,
      val guid: String,  // Unique per feed
      val title: String,
      val audioUrl: String,
      val durationMs: Long,
      val publishedAt: Long,
      val description: String?,
      val imageUrl: String?,
      val playbackPositionMs: Long = 0,
      val completed: Boolean = false,
      val downloadStatus: Int = 0,  // 0=none, 1=queued, 2=downloading, 3=complete
      val localPath: String?,
      val fileSizeBytes: Long?,
      val gainDb: Float?
  )
  ```
- [ ] Create `ListeningSessionEntity` Room entity
- [ ] Create `PodcastDao` with CRUD operations
- [ ] Create `EpisodeDao` with queries (latest per podcast, downloaded, etc.)
- [ ] Create `SessionDao` for playlists
- [ ] Add database migrations strategy
- [ ] Write unit tests for DAOs

#### 1.2 RSS Parser
- [ ] Add RSS parsing dependency (e.g., `com.rometools:rome:2.1.0` or `kotlin-rss`)
- [ ] Create `RssParser` class
  ```kotlin
  interface RssParser {
      suspend fun parseFeed(url: String): Result<ParsedFeed>
  }
  ```
- [ ] Handle common RSS formats (RSS 2.0, Atom)
- [ ] Extract: title, author, image, description, episodes
- [ ] Handle enclosures (audio URLs)
- [ ] Handle iTunes namespace extensions
- [ ] Error handling for malformed feeds
- [ ] Write unit tests with sample feeds

#### 1.3 OPML Import/Export
- [ ] Create `OpmlParser` class
- [ ] Parse OPML file to list of feed URLs
- [ ] Generate OPML from subscriptions
- [ ] File picker integration (SAF)
- [ ] Write unit tests

#### 1.4 Podcast Search API
- [ ] Create `iTunesSearchApi` Retrofit interface
  ```kotlin
  interface ITunesSearchApi {
      @GET("search")
      suspend fun search(
          @Query("term") term: String,
          @Query("media") media: String = "podcast",
          @Query("limit") limit: Int = 25
      ): ITunesSearchResponse
  }
  ```
- [ ] Create search response models
- [ ] Add Hilt module for API
- [ ] Optional: Add PodcastIndex API as secondary source
- [ ] Write integration tests

#### 1.5 Repositories
- [ ] Create `PodcastRepository` interface
- [ ] Implement `PodcastRepositoryImpl`
  - Subscribe/unsubscribe
  - Refresh feed
  - Get all subscribed
- [ ] Create `EpisodeRepository` interface
- [ ] Implement `EpisodeRepositoryImpl`
  - Get episodes for podcast
  - Get latest unplayed
  - Mark played/unplayed
  - Update playback position
- [ ] Create `DownloadRepository` for download state
- [ ] Add Hilt bindings

---

### Phase 2: Playback Engine

#### 2.1 Adapt Existing MediaService
- [ ] Review PixelPlayer's `MediaService` implementation
- [ ] Add support for streaming URLs (not just local files)
- [ ] Add episode metadata to MediaSession
- [ ] Update notification with podcast artwork
- [ ] Save playback position on pause/stop
- [ ] Handle audio focus properly

#### 2.2 Queue Management
- [ ] Create `PlaybackQueue` class
  ```kotlin
  class PlaybackQueue {
      val currentEpisode: StateFlow<Episode?>
      val queue: StateFlow<List<Episode>>
      fun playEpisode(episode: Episode)
      fun addToQueue(episode: Episode)
      fun removeFromQueue(episodeId: Long)
      fun skipToNext()
      fun skipToPrevious()
      fun clearQueue()
  }
  ```
- [ ] Persist queue to database
- [ ] Restore queue on app restart
- [ ] "Play All Latest" builds queue from subscriptions

#### 2.3 Download Manager
- [ ] Create `DownloadManager` class using WorkManager
- [ ] Download queue with priorities
- [ ] Progress tracking via Flow
- [ ] Pause/resume/cancel downloads
- [ ] Retry failed downloads
- [ ] Storage location management
- [ ] Auto-delete old episodes (configurable)
- [ ] Download over WiFi only option

---

### Phase 3: UI Screens

#### 3.1 Home Screen
- [ ] Design home screen layout (Figma or sketch)
- [ ] Create `HomeScreen` composable
- [ ] Subscribed podcasts grid (3 columns)
- [ ] "Play All Latest" FAB or prominent button
- [ ] Mini-player at bottom (current episode)
- [ ] Pull-to-refresh for feed updates
- [ ] Empty state for no subscriptions
- [ ] Create `HomeViewModel`

#### 3.2 Podcast Detail Screen
- [ ] Create `PodcastDetailScreen` composable
- [ ] Podcast header (image, title, author, description)
- [ ] Episode list (newest first)
- [ ] Episode item: title, date, duration, download/stream buttons
- [ ] Unsubscribe button
- [ ] Create `PodcastDetailViewModel`

#### 3.3 Discover/Search Screen
- [ ] Create `DiscoverScreen` composable
- [ ] Search bar with debounce
- [ ] Search results list
- [ ] Subscribe button per result
- [ ] OPML import button
- [ ] Create `DiscoverViewModel`

#### 3.4 Now Playing Screen
- [ ] Adapt PixelPlayer's player screen for podcasts
- [ ] Large artwork
- [ ] Episode title and podcast name
- [ ] Seek bar with time labels
- [ ] Play/pause, skip 15s back/forward, next/previous
- [ ] Playback speed selector (0.5x - 3x)
- [ ] Queue view (swipe up or button)
- [ ] Create `NowPlayingViewModel`

#### 3.5 Downloads Screen
- [ ] Create `DownloadsScreen` composable
- [ ] Downloaded episodes list
- [ ] Download queue with progress
- [ ] Delete downloaded episode
- [ ] Storage usage indicator
- [ ] Create `DownloadsViewModel`

#### 3.6 Settings Screen
- [ ] Create `SettingsScreen` composable
- [ ] Playback settings (default speed, skip intervals)
- [ ] Download settings (WiFi only, auto-delete)
- [ ] Storage management
- [ ] OPML export
- [ ] About section
- [ ] Create `SettingsViewModel`

#### 3.7 Navigation
- [ ] Set up Navigation Compose
- [ ] Bottom navigation: Home, Discover, Downloads, Settings
- [ ] Deep links for episodes

---

### Phase 4: Audio Polish

#### 4.1 Crossfade Implementation
- [ ] Create `CrossfadeController` class
- [ ] Initialize secondary ExoPlayer on demand
- [ ] Trigger crossfade at configurable seconds before end (default: 30s)
- [ ] Implement volume ducking curves
  ```kotlin
  // Linear or ease-in-out curve over duration
  fun calculateVolume(progress: Float, fadeOut: Boolean): Float
  ```
- [ ] Handle skip button triggering immediate crossfade
- [ ] Clean up secondary player after transition
- [ ] Handle edge cases (very short episodes, no next episode)
- [ ] Add crossfade toggle in settings
- [ ] Add crossfade duration setting (5s, 10s, 15s, 30s)

#### 4.2 Volume Normalization
- [ ] Research ReplayGain vs EBU R128 for podcasts
- [ ] Create `LoudnessScanner` using FFmpeg or native audio analysis
- [ ] Scan downloaded episodes on completion
- [ ] Store gainDb per episode
- [ ] Apply gain via ExoPlayer `AudioProcessor`
- [ ] Option: Real-time normalization for streaming (harder)
- [ ] Add toggle in settings

#### 4.3 Gapless Playback
- [ ] Configure ExoPlayer for gapless
- [ ] Pre-buffer next episode when current is near end
- [ ] Test with various audio formats

---

### Phase 5: AI Integration

#### 5.1 OpenRouter Client
- [ ] Create `OpenRouterApi` Retrofit interface
  ```kotlin
  interface OpenRouterApi {
      @POST("api/v1/chat/completions")
      suspend fun chat(
          @Header("Authorization") apiKey: String,
          @Body request: ChatRequest
      ): ChatResponse
  }
  ```
- [ ] Create request/response models
- [ ] Add API key storage (encrypted SharedPreferences)
- [ ] Add Hilt module

#### 5.2 Playlist Curator
- [ ] Create `AiPlaylistCurator` class
- [ ] Design prompt template for curation
  ```
  You are a podcast playlist curator. Given:
  - Available episodes: [list with titles, durations, topics]
  - Target duration: X minutes
  - User preference: [commute/work/relaxed]
  - Listening history: [recently played]

  Return a JSON array of episode IDs in recommended order.
  ```
- [ ] Parse AI response to episode list
- [ ] Handle AI errors gracefully (fallback to chronological)
- [ ] Cache AI responses to reduce API calls

#### 5.3 Smart Sessions
- [ ] Create `SessionManager` class
- [ ] Named sessions: "Commute", "Work", "Evening"
- [ ] Session creation UI
- [ ] "Generate Playlist" button triggers AI curation
- [ ] Save/load sessions
- [ ] Quick-start session from home screen

---

### Phase 6: Polish

#### 6.1 Widgets
- [ ] Create Glance-based home screen widget
- [ ] Show current episode with play/pause
- [ ] "Start Commute" quick action
- [ ] Widget configuration

#### 6.2 Error Handling
- [ ] Network error states (offline mode)
- [ ] Feed parsing error handling
- [ ] Playback error recovery
- [ ] Download failure handling
- [ ] User-friendly error messages

#### 6.3 Performance
- [ ] Profile app for memory leaks
- [ ] Optimize image loading (Coil caching)
- [ ] Lazy loading for long lists
- [ ] Background work optimization

#### 6.4 Testing
- [ ] Unit tests for repositories
- [ ] Unit tests for ViewModels
- [ ] Integration tests for RSS parsing
- [ ] UI tests for critical flows
- [ ] Manual testing on multiple devices

---

### Launch Checklist
- [ ] App icon finalized
- [ ] Play Store listing prepared
- [ ] Screenshots captured
- [ ] Privacy policy written
- [ ] Version 1.0.0 tagged
- [ ] Signed release APK built
- [ ] Beta testing complete
- [ ] Crash reporting integrated (Firebase Crashlytics)
- [ ] Analytics integrated (optional)

---

## Appendix C: Key Libraries

| Purpose | Library | Version |
|---------|---------|---------|
| RSS Parsing | `com.rometools:rome` | 2.1.0 |
| HTTP | `com.squareup.retrofit2:retrofit` | 2.9.0 |
| JSON | `com.squareup.moshi:moshi-kotlin` | 1.15.0 |
| DI | `com.google.dagger:hilt-android` | 2.50 |
| Database | `androidx.room:room-runtime` | 2.6.1 |
| Media | `androidx.media3:media3-exoplayer` | 1.2.1 |
| Images | `io.coil-kt:coil-compose` | 2.5.0 |
| Background | `androidx.work:work-runtime-ktx` | 2.9.0 |
| Audio Analysis | `com.github.nicehash:ffmpeg-android` | (for loudness) |

---

## Appendix D: File Structure After MVP

```
app/src/main/java/com/podai/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── PodcastDao.kt
│   │   ├── EpisodeDao.kt
│   │   ├── SessionDao.kt
│   │   └── entity/
│   │       ├── PodcastEntity.kt
│   │       ├── EpisodeEntity.kt
│   │       └── SessionEntity.kt
│   ├── remote/
│   │   ├── ITunesSearchApi.kt
│   │   ├── OpenRouterApi.kt
│   │   └── dto/
│   │       ├── ITunesSearchResponse.kt
│   │       └── ChatModels.kt
│   ├── repository/
│   │   ├── PodcastRepositoryImpl.kt
│   │   ├── EpisodeRepositoryImpl.kt
│   │   └── DownloadRepositoryImpl.kt
│   └── parser/
│       ├── RssParser.kt
│       └── OpmlParser.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/
│   │   ├── Podcast.kt
│   │   ├── Episode.kt
│   │   └── ListeningSession.kt
│   ├── repository/
│   │   ├── PodcastRepository.kt
│   │   ├── EpisodeRepository.kt
│   │   └── DownloadRepository.kt
│   └── usecase/
│       ├── SubscribeToPodcastUseCase.kt
│       ├── RefreshFeedsUseCase.kt
│       ├── GetLatestEpisodesUseCase.kt
│       └── GeneratePlaylistUseCase.kt
├── playback/
│   ├── MediaService.kt
│   ├── PlaybackQueue.kt
│   ├── CrossfadeController.kt
│   ├── LoudnessScanner.kt
│   └── DownloadManager.kt
├── ai/
│   ├── OpenRouterClient.kt
│   ├── PlaylistCurator.kt
│   └── PromptTemplates.kt
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── discover/
│   │   ├── DiscoverScreen.kt
│   │   └── DiscoverViewModel.kt
│   ├── podcast/
│   │   ├── PodcastDetailScreen.kt
│   │   └── PodcastDetailViewModel.kt
│   ├── player/
│   │   ├── NowPlayingScreen.kt
│   │   └── NowPlayingViewModel.kt
│   ├── downloads/
│   │   ├── DownloadsScreen.kt
│   │   └── DownloadsViewModel.kt
│   ├── sessions/
│   │   ├── SessionsScreen.kt
│   │   └── SessionsViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   └── components/
│       ├── PodcastCard.kt
│       ├── EpisodeItem.kt
│       ├── MiniPlayer.kt
│       └── SearchBar.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── widget/
│       └── PlayerWidget.kt
└── util/
    ├── Extensions.kt
    ├── DateFormatter.kt
    └── NetworkMonitor.kt
```
