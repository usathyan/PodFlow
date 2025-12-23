# Cover Flow Home Screen Design

> **Status:** Future Enhancement (not scheduled)

## Summary

Replace the current horizontal podcast carousel with a classic iPod-style Cover Flow interface featuring 3D perspective, reflections, and momentum-based scrolling.

## Requirements

- **Location:** Replace home screen carousel
- **Interaction:** Tap to immediately play latest episode
- **Style:** Classic 3D perspective with reflections, using app's dark theme
- **Navigation:** Swipe with physics-based momentum
- **Labels:** Podcast title appears below Cover Flow area

## Visual Design

### Core Layout

```
┌─────────────────────────────────┐
│  (status bar)                   │
│                                 │
│      ┌───┐   ┌─────┐   ┌───┐   │
│    ┌─┤   │   │     │   │   ├─┐ │
│    │ │ ◢ │   │  ●  │   │ ◣ │ │ │
│    └─┤   │   │     │   │   ├─┘ │
│      └───┘   └─────┘   └───┘   │
│        ╲       │         ╱     │
│         ╲______│________╱      │  <- reflections
│                                 │
│        "The Daily"              │  <- podcast title
│                                 │
│  (bottom nav bar)               │
└─────────────────────────────────┘
```

- Center cover is largest and flat (facing viewer)
- Side covers tilt away in 3D perspective
- Reflections fade downward on dark surface
- Podcast title centered below covers

### 3D Perspective & Reflections

**3D Transforms:**
- Center cover: Full size (~200dp), no rotation, slight elevation shadow
- Adjacent covers: ~70% scale, rotated 45-55° on Y-axis (tilting away)
- Distant covers: Progressively smaller, more tilted, stacked closer
- Z-depth: Center cover appears closest, others recede into distance

**Reflections:**
- Mirror image below each cover, flipped vertically
- Gradient fade from ~40% opacity at top to 0% at bottom
- Reflection height: ~30-40% of cover height
- Uses app's dark theme background

**Spacing:**
- Covers overlap slightly as they recede (creating depth)
- Gap between center and adjacent covers is larger for emphasis

**Animation:**
- Smooth spring physics for settling into position
- Covers scale and rotate fluidly as they move center/off-center
- Momentum-based flicking with gradual deceleration

## Technical Considerations

- Implement using Jetpack Compose with `graphicsLayer` for 3D transforms
- Use `Modifier.pointerInput` with velocity tracking for momentum
- Consider `LazyRow` with custom layout or fully custom `Layout` composable
- Reflections can be drawn using `drawWithContent` and scale/alpha modifiers

## References

- Original iPod Cover Flow (2007)
- iOS Music app Cover Flow mode
