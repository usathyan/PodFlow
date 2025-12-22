package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Composable content for the visualizer embedded in CoverFragment's ComposeView.
 * Uses the existing VisualizerContainer with swipe gestures and style indicator.
 */
@Composable
fun CoverVisualizerContent(
    viewModel: VisualizerViewModel,
    albumArtUrl: String?,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    VisualizerContainer(
        data = uiState.data,
        currentStyle = uiState.style,
        albumArtUrl = albumArtUrl,
        onStyleChange = { newStyle -> viewModel.setStyle(newStyle) },
        modifier = modifier.fillMaxSize()
    )
}
