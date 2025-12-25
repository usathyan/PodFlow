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
| [Cover Flow Home Screen](2025-12-23-cover-flow-design.md) | iPod-style 3D Cover Flow with reflections and momentum scrolling | High |
| [AI Podcast Player](2025-12-15-ai-podcast-player-design.md) | OpenRouter AI integration for smart playlist curation | High |

---

## Next Up: Cover Flow Feature

**Target:** Replace home screen carousel with classic iPod-style Cover Flow

### Readiness Assessment

**Prerequisites (Complete):**
- [x] Basic app structure and navigation
- [x] Home screen with podcast carousel exists
- [x] Jetpack Compose UI foundation
- [x] Podcast data models and subscriptions working

**Technical Requirements:**
- [ ] Implement 3D transforms with `graphicsLayer`
- [ ] Create reflection effect with gradient fade
- [ ] Add momentum-based scrolling with velocity tracking
- [ ] Handle cover scaling/rotation animations
- [ ] Integrate tap-to-play functionality

**Estimated Scope:**
- New Composable: `CoverFlowCarousel.kt`
- Modify: Home screen to use new carousel
- Assets: None (uses existing podcast artwork)

### Recommended Approach
1. Create feature branch: `feature/cover-flow`
2. Prototype basic 3D transforms
3. Add reflection rendering
4. Implement gesture/momentum handling
5. Polish animations and transitions
6. Integration testing
