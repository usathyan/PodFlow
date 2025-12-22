package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular visualizer with radial frequency bars.
 *
 * Features:
 * - Bars arranged in a circle radiating outward
 * - Purple/teal color gradient
 * - Subtle pulse effect with overall audio energy
 */
@Composable
fun CircularVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF6750A4) // Purple
    val secondaryColor = Color(0xFF006B5B) // Teal
    val backgroundColor = Color(0xFF0A0A0A)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val minDimension = minOf(size.width, size.height)
        val innerRadius = minDimension * 0.2f
        val maxBarLength = minDimension * 0.3f

        val barCount = data.fftData.size
        val angleStep = (2 * Math.PI / barCount).toFloat()

        // Calculate overall energy for pulse effect
        val energy = data.fftData.average().toFloat()
        val pulseRadius = innerRadius + (energy * innerRadius * 0.1f)

        // Draw center circle with pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.3f + energy * 0.4f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = pulseRadius
            ),
            radius = pulseRadius,
            center = Offset(centerX, centerY)
        )

        // Draw inner circle outline
        drawCircle(
            color = primaryColor.copy(alpha = 0.5f),
            radius = innerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Draw radial bars
        for (i in 0 until barCount) {
            val angle = i * angleStep - Math.PI.toFloat() / 2 // Start from top
            val barLength = data.fftData[i] * maxBarLength

            val startX = centerX + cos(angle) * innerRadius
            val startY = centerY + sin(angle) * innerRadius
            val endX = centerX + cos(angle) * (innerRadius + barLength)
            val endY = centerY + sin(angle) * (innerRadius + barLength)

            // Gradient from purple to teal based on position
            val progress = i.toFloat() / barCount
            val barColor = lerp(primaryColor, secondaryColor, progress)

            drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
