package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Container for visualizers with swipe-to-switch functionality.
 *
 * @param data Current visualizer data
 * @param currentStyle Currently selected style
 * @param albumArtUrl Album art URL for WaveformVisualizer
 * @param onStyleChange Called when user swipes to change style
 */
@Composable
fun VisualizerContainer(
    data: VisualizerData,
    currentStyle: VisualizerStyle,
    albumArtUrl: String?,
    onStyleChange: (VisualizerStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(currentStyle) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffset) > swipeThreshold) {
                                val styles = VisualizerStyle.entries
                                val currentIndex = styles.indexOf(currentStyle)
                                val newIndex = if (dragOffset > 0) {
                                    (currentIndex - 1 + styles.size) % styles.size
                                } else {
                                    (currentIndex + 1) % styles.size
                                }
                                onStyleChange(styles[newIndex])
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
        ) {
            when (currentStyle) {
                VisualizerStyle.WINAMP -> WinampVisualizer(data = data)
                VisualizerStyle.CIRCULAR -> CircularVisualizer(data = data)
                VisualizerStyle.WAVEFORM -> WaveformVisualizer(
                    data = data,
                    albumArtUrl = albumArtUrl
                )
            }
        }

        // Style indicator dots
        Spacer(modifier = Modifier.height(8.dp))
        StyleIndicator(
            currentStyle = currentStyle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun StyleIndicator(
    currentStyle: VisualizerStyle,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        VisualizerStyle.entries.forEach { style ->
            val isSelected = style == currentStyle
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.5f)
                    )
            )
        }
    }
}
