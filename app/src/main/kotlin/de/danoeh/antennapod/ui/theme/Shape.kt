package de.danoeh.antennapod.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * PodFlow Shape System
 *
 * Consistent corner radius system for a cohesive, modern look.
 * Using generous roundness for a friendly, approachable feel.
 */

val PodFlowShapes = Shapes(
    // Extra small - for chips, small badges
    extraSmall = RoundedCornerShape(4.dp),

    // Small - for buttons, text fields
    small = RoundedCornerShape(8.dp),

    // Medium - for cards, dialogs
    medium = RoundedCornerShape(12.dp),

    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(16.dp),

    // Extra large - for full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)

// Common shape values used across the app
object PodFlowCornerRadius {
    val None = 0.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Full = 100.dp  // For circular elements like FABs
}

// Podcast tile specific shapes
val PodcastTileShape = RoundedCornerShape(12.dp)
val PlayButtonShape = RoundedCornerShape(100) // Circular
val MiniPlayerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val SearchBarShape = RoundedCornerShape(28.dp)
val CategoryChipShape = RoundedCornerShape(16.dp)
