package de.danoeh.antennapod.ui.visualizer

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

/**
 * Bridge class to set up Compose visualizer content from Java code.
 */
object VisualizerBridge {

    /**
     * Set up the ComposeView with visualizer content.
     *
     * @param composeView The ComposeView to set up
     * @param viewModel The VisualizerViewModel
     * @param albumArtUrlProvider Lambda to get the current album art URL
     */
    @JvmStatic
    fun setupVisualizerView(
        composeView: ComposeView,
        viewModel: VisualizerViewModel,
        albumArtUrlProvider: () -> String?
    ) {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoverVisualizerContent(
                    viewModel = viewModel,
                    albumArtUrl = albumArtUrlProvider()
                )
            }
        }
    }
}
