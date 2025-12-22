package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Winamp-style visualizer with spectrum analyzer bars and oscilloscope overlay.
 *
 * Features:
 * - Vertical bars with green→yellow→red gradient based on amplitude
 * - Peak indicators that fall slowly
 * - Oscilloscope waveform overlay in the center
 */
@Composable
fun WinampVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    // Winamp classic colors
    val barColorLow = Color(0xFF00FF00)     // Green
    val barColorMid = Color(0xFFFFFF00)     // Yellow
    val barColorHigh = Color(0xFFFF0000)    // Red
    val peakColor = Color(0xFFFFFFFF)       // White
    val waveformColor = Color(0xFF00FF00).copy(alpha = 0.7f)  // Green, semi-transparent
    val backgroundColor = Color(0xFF0A0A0A) // Near black

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val width = size.width
        val height = size.height
        val barCount = data.fftData.size
        val barWidth = width / barCount * 0.8f
        val barSpacing = width / barCount * 0.2f
        val maxBarHeight = height * 0.85f

        // Draw spectrum bars
        for (i in 0 until barCount) {
            val barHeight = data.fftData[i] * maxBarHeight
            val x = i * (barWidth + barSpacing) + barSpacing / 2
            val y = height - barHeight

            // Create gradient based on bar height
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(barColorHigh, barColorMid, barColorLow),
                startY = y,
                endY = height
            )

            // Draw the bar
            drawRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Draw peak indicator
            val peakY = height - (data.peakLevels[i] * maxBarHeight)
            drawRect(
                color = peakColor,
                topLeft = Offset(x, peakY - 4f),
                size = Size(barWidth, 4f)
            )
        }

        // Draw oscilloscope waveform overlay
        if (data.waveformData.isNotEmpty()) {
            val waveformPath = Path()
            val centerY = height / 2
            val waveformHeight = height * 0.3f

            data.waveformData.forEachIndexed { index, value ->
                val x = (index.toFloat() / data.waveformData.size) * width
                val y = centerY + (value - 0.5f) * waveformHeight * 2

                if (index == 0) {
                    waveformPath.moveTo(x, y)
                } else {
                    waveformPath.lineTo(x, y)
                }
            }

            drawPath(
                path = waveformPath,
                color = waveformColor,
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
