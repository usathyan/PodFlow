package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Minimal waveform visualizer with blurred album art background.
 *
 * Features:
 * - Simple oscilloscope line
 * - Semi-transparent overlay on blurred artwork
 * - Clean, understated aesthetic
 */
@Composable
fun WaveformVisualizer(
    data: VisualizerData,
    albumArtUrl: String?,
    modifier: Modifier = Modifier
) {
    val waveformColor = Color.White
    val overlayColor = Color.Black.copy(alpha = 0.7f)

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        // Blurred album art background
        if (albumArtUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumArtUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
            )
        }

        // Dark overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = overlayColor)
        }

        // Waveform
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val waveformHeight = height * 0.4f

            if (data.waveformData.isNotEmpty()) {
                val path = Path()

                data.waveformData.forEachIndexed { index, value ->
                    val x = (index.toFloat() / data.waveformData.size) * width
                    val y = centerY + (value - 0.5f) * waveformHeight * 2

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            waveformColor.copy(alpha = 0.3f),
                            waveformColor,
                            waveformColor.copy(alpha = 0.3f)
                        )
                    ),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
