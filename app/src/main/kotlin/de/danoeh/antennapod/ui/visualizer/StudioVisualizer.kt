package de.danoeh.antennapod.ui.visualizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Studio-style visualizer combining VU meters with modern spectrum analyzer.
 *
 * Features:
 * - Dual VU meters (L/R) at the top
 * - Modern spectrum bars below
 * - Professional audio workstation aesthetic
 */
@Composable
fun StudioVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF0A0A0F)

    // Calculate RMS levels for VU meters
    val leftLevel = remember(data.waveformData) {
        if (data.waveformData.isEmpty()) 0f
        else {
            val leftSamples = data.waveformData.filterIndexed { i, _ -> i % 2 == 0 }
            val rms = kotlin.math.sqrt(leftSamples.map { (it - 0.5f) * (it - 0.5f) }.average().toFloat())
            (rms * 4f).coerceIn(0f, 1f)
        }
    }

    val rightLevel = remember(data.waveformData) {
        if (data.waveformData.isEmpty()) 0f
        else {
            val rightSamples = data.waveformData.filterIndexed { i, _ -> i % 2 == 1 }
            val rms = kotlin.math.sqrt(rightSamples.map { (it - 0.5f) * (it - 0.5f) }.average().toFloat())
            (rms * 4f).coerceIn(0f, 1f)
        }
    }

    // Smooth needle animation
    val animatedLeftLevel by animateFloatAsState(
        targetValue = leftLevel,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "leftNeedle"
    )

    val animatedRightLevel by animateFloatAsState(
        targetValue = rightLevel,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rightNeedle"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        // VU Meters section (top 45%)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .padding(bottom = 8.dp)
        ) {
            // Left VU Meter
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(end = 6.dp)
            ) {
                drawCompactVUMeter(
                    level = animatedLeftLevel,
                    label = "L"
                )
            }

            // Right VU Meter
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(start = 6.dp)
            ) {
                drawCompactVUMeter(
                    level = animatedRightLevel,
                    label = "R"
                )
            }
        }

        // Spectrum section (bottom 55%)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            drawModernSpectrum(data.fftData)
        }
    }
}

private fun DrawScope.drawCompactVUMeter(
    level: Float,
    label: String
) {
    val meterFaceColor = Color(0xFF1A1A1A)
    val meterFrameColor = Color(0xFF2D2D2D)
    val scaleColor = Color(0xFFD4C4A8)
    val needleColor = Color(0xFFE8D5B7)
    val dangerZoneColor = Color(0xFFCC3333)
    val glowColor = Color(0xFFFFE4B5).copy(alpha = 0.12f)

    val width = size.width
    val height = size.height

    // Draw frame
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3D3D3D), meterFrameColor, Color(0xFF1D1D1D))
        ),
        topLeft = Offset.Zero,
        size = Size(width, height),
        cornerRadius = CornerRadius(10f)
    )

    // Draw inner face
    val padding = 6f
    drawRoundRect(
        color = meterFaceColor,
        topLeft = Offset(padding, padding),
        size = Size(width - padding * 2, height - padding * 2),
        cornerRadius = CornerRadius(6f)
    )

    // Draw glow
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(width / 2, height * 0.4f),
            radius = width * 0.7f
        ),
        topLeft = Offset(padding, padding),
        size = Size(width - padding * 2, height - padding * 2),
        cornerRadius = CornerRadius(6f)
    )

    // VU meter arc
    val arcCenterX = width / 2
    val arcCenterY = height * 0.75f
    val arcRadius = width * 0.4f
    val startAngle = -150f
    val sweepAngle = 120f

    // Scale arc
    drawArc(
        color = scaleColor.copy(alpha = 0.25f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(arcCenterX - arcRadius, arcCenterY - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = 2.5f)
    )

    // Danger zone
    val dangerStart = startAngle + sweepAngle * 0.75f
    drawArc(
        color = dangerZoneColor,
        startAngle = dangerStart,
        sweepAngle = sweepAngle * 0.25f,
        useCenter = false,
        topLeft = Offset(arcCenterX - arcRadius, arcCenterY - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = 5f)
    )

    // Scale markings
    val markings = listOf(-20f, -10f, -5f, 0f, 3f)
    markings.forEach { db ->
        val normalizedPos = (db + 20f) / 23f
        val angle = startAngle + normalizedPos * sweepAngle
        val angleRad = Math.toRadians(angle.toDouble())
        val innerR = arcRadius - 12f
        val outerR = arcRadius - 4f

        val markColor = if (db >= 0) dangerZoneColor else scaleColor
        drawLine(
            color = markColor,
            start = Offset(
                arcCenterX + cos(angleRad).toFloat() * innerR,
                arcCenterY + sin(angleRad).toFloat() * innerR
            ),
            end = Offset(
                arcCenterX + cos(angleRad).toFloat() * outerR,
                arcCenterY + sin(angleRad).toFloat() * outerR
            ),
            strokeWidth = 2f
        )
    }

    // Pivot point
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF4A4A4A), Color(0xFF2A2A2A)),
            center = Offset(arcCenterX, arcCenterY)
        ),
        radius = 8f,
        center = Offset(arcCenterX, arcCenterY)
    )

    // Needle
    val needleAngle = startAngle + level * sweepAngle
    rotate(needleAngle + 90f, pivot = Offset(arcCenterX, arcCenterY)) {
        // Needle glow
        drawLine(
            color = needleColor.copy(alpha = 0.3f),
            start = Offset(arcCenterX, arcCenterY),
            end = Offset(arcCenterX, arcCenterY - arcRadius + 4f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        // Main needle
        drawLine(
            color = needleColor,
            start = Offset(arcCenterX, arcCenterY),
            end = Offset(arcCenterX, arcCenterY - arcRadius + 4f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        // Needle tip
        drawCircle(
            color = dangerZoneColor,
            radius = 3f,
            center = Offset(arcCenterX, arcCenterY - arcRadius + 4f)
        )
    }

    // LED bar at bottom
    val ledY = height - 16f
    val ledCount = 8
    val ledWidth = (width - padding * 4) / ledCount
    val ledStartX = padding * 2

    for (i in 0 until ledCount) {
        val threshold = i.toFloat() / ledCount
        val isLit = level > threshold
        val isRed = i >= 6

        val ledColor = when {
            isRed && isLit -> Color(0xFFFF3333)
            isRed -> Color(0xFF331111)
            isLit -> Color(0xFF33FF33)
            else -> Color(0xFF113311)
        }

        drawRoundRect(
            color = ledColor,
            topLeft = Offset(ledStartX + i * ledWidth + 2f, ledY),
            size = Size(ledWidth - 4f, 8f),
            cornerRadius = CornerRadius(2f)
        )

        if (isLit) {
            drawRoundRect(
                color = ledColor.copy(alpha = 0.4f),
                topLeft = Offset(ledStartX + i * ledWidth, ledY - 2f),
                size = Size(ledWidth, 12f),
                cornerRadius = CornerRadius(3f)
            )
        }
    }
}

private fun DrawScope.drawModernSpectrum(fftData: FloatArray) {
    // Colors based on amplitude (height)
    val colorLow = Color(0xFF00D4FF)     // Cyan - quiet
    val colorMid = Color(0xFFFF00FF)     // Magenta - medium
    val colorHigh = Color(0xFFFF3333)    // Red - loud
    val glowAlpha = 0.4f

    val width = size.width
    val height = size.height
    val barCount = minOf(fftData.size, 32)
    val barWidth = (width / barCount) * 0.7f
    val barSpacing = (width / barCount) * 0.3f
    val maxBarHeight = height * 0.9f

    // Smooth FFT data
    val smoothedData = if (fftData.size >= barCount) {
        FloatArray(barCount) { i ->
            val startIdx = (i * fftData.size / barCount)
            val endIdx = ((i + 1) * fftData.size / barCount).coerceAtMost(fftData.size)
            fftData.slice(startIdx until endIdx).average().toFloat()
        }
    } else {
        fftData
    }

    for (i in 0 until barCount) {
        if (i >= smoothedData.size) break

        val amplitude = smoothedData[i]
        val barHeight = amplitude * maxBarHeight
        val x = i * (barWidth + barSpacing) + barSpacing / 2 + (width - barCount * (barWidth + barSpacing)) / 2
        val y = height - barHeight

        // Color based on amplitude (height) - cyan → magenta → red
        val barColor = when {
            amplitude < 0.4f -> lerpColorStudio(colorLow, colorMid, amplitude / 0.4f)
            else -> lerpColorStudio(colorMid, colorHigh, (amplitude - 0.4f) / 0.6f)
        }

        // Glow - more intense for louder bars
        for (g in 2 downTo 1) {
            val expand = g * 0.25f
            drawRoundRect(
                color = barColor.copy(alpha = glowAlpha * amplitude / (g + 1)),
                topLeft = Offset(x - barWidth * expand / 2, y - barHeight * expand / 4),
                size = Size(barWidth * (1 + expand), barHeight * (1 + expand / 2)),
                cornerRadius = CornerRadius(barWidth * (1 + expand))
            )
        }

        // Main bar with vertical gradient (brighter at top)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    barColor,
                    lerpColorStudio(barColor, colorLow, 0.5f).copy(alpha = 0.8f)
                ),
                startY = y,
                endY = y + barHeight
            ),
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2)
        )

        // Top highlight - brighter on loud bars
        if (barHeight > 4f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.4f * amplitude + 0.1f),
                topLeft = Offset(x + barWidth * 0.2f, y),
                size = Size(barWidth * 0.3f, minOf(barHeight * 0.12f, 5f)),
                cornerRadius = CornerRadius(2f)
            )
        }
    }
}

private fun lerpColorStudio(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
