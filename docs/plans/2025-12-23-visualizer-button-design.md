# Visualizer Toggle Button Design

> **Status:** Implemented

## Summary

Move the visualizer trigger from long-press on album cover to a dedicated toolbar button for better discoverability and cleaner UX.

## Requirements

- Add visualizer toggle button to toolbar (first position, left of star)
- Use filled/outline icon to indicate state
- Remove long-press gesture from album cover
- Preserve single-tap play/pause on album cover

## Visual Layout

```
┌─────────────────────────────────────────┐
│  ←   [📊] [⭐] [🌙] [📤] [⋮]           │  <- toolbar
│                                         │
│         ┌─────────────┐                 │
│         │             │                 │
│         │  Album Art  │                 │  <- tap = play/pause only
│         │             │                 │
│         └─────────────┘                 │
│                                         │
│        "Podcast Title"                  │
│        "Episode Title"                  │
└─────────────────────────────────────────┘

📊 = visualizer icon (outline when off, filled when on)
```

## Implementation

### Files to Modify

1. **`app/src/main/res/menu/mediaplayer.xml`**
   - Add visualizer toggle menu item at first position
   - Two items: `show_visualizer_item` and `hide_visualizer_item`

2. **`app/src/main/java/.../AudioPlayerFragment.java`**
   - Handle visualizer menu item clicks
   - Update icon visibility based on visualizer state
   - Communicate with CoverFragment to toggle visualizer

3. **`app/src/main/java/.../CoverFragment.java`**
   - Remove long-press listeners from album cover and visualizer view
   - Expose `toggleVisualizer()` as public method
   - Add callback/event to notify parent of state changes

4. **New drawables**
   - `ic_visualizer` - outline waveform icon (visualizer off)
   - `ic_visualizer_off` - filled/active icon (visualizer on)

### Behavior Flow

1. User taps visualizer button in toolbar
2. AudioPlayerFragment receives click
3. AudioPlayerFragment calls CoverFragment.toggleVisualizer()
4. CoverFragment checks RECORD_AUDIO permission (requests if needed)
5. CoverFragment shows/hides visualizer
6. CoverFragment notifies parent of state change
7. AudioPlayerFragment updates toolbar icon

### Icon States

| State | Icon | Menu Item Visible |
|-------|------|-------------------|
| Visualizer OFF | `ic_visualizer` (outline) | `show_visualizer_item` |
| Visualizer ON | `ic_visualizer_off` (filled) | `hide_visualizer_item` |
