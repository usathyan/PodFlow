package de.danoeh.antennapod.ui.visualizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flowing Wave Visualizer - Smooth, gentle waves that pulse with music.
 *
 * Features:
 * - Few smooth flowing wave ribbons (3-4 layers)
 * - Bezier curves for gentle, organic motion
 * - Blue → Cyan → Magenta pulsing colors
 * - Smooth, progressive amplitude changes
 * - Water reflection at bottom
 */
@Composable
fun StudioVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    // Deep blue/black background
    val backgroundColor = Color(0xFF000820)

    // Smoothed energy for gentle pulsing (not raw FFT)
    var smoothedEnergy by remember { mutableFloatStateOf(0f) }
    var smoothedBass by remember { mutableFloatStateOf(0f) }
    var smoothedMid by remember { mutableFloatStateOf(0f) }
    var smoothedHigh by remember { mutableFloatStateOf(0f) }

    // Phase for smooth wave animation
    var wavePhase by remember { mutableFloatStateOf(0f) }

    // Color pulse phase
    var colorPulse by remember { mutableFloatStateOf(0f) }

    // Process audio - SMOOTH and GENTLE
    remember(data.fftData) {
        if (data.fftData.size >= 16) {
            // Extract frequency bands
            val bass = data.fftData.take(4).average().toFloat()
            val mid = data.fftData.slice(4..10).average().toFloat()
            val high = data.fftData.slice(11..15).average().toFloat()
            val energy = (bass * 0.5f + mid * 0.3f + high * 0.2f)

            // SMOOTH transitions - gentle attack AND decay
            val smoothFactor = 0.15f  // Lower = smoother
            smoothedBass = smoothedBass * (1 - smoothFactor) + bass * smoothFactor
            smoothedMid = smoothedMid * (1 - smoothFactor) + mid * smoothFactor
            smoothedHigh = smoothedHigh * (1 - smoothFactor) + high * smoothFactor
            smoothedEnergy = smoothedEnergy * (1 - smoothFactor) + energy * smoothFactor

            // Advance wave phase - speed varies with energy
            wavePhase += 0.03f + smoothedEnergy * 0.05f
            if (wavePhase > 2 * PI.toFloat()) wavePhase -= 2 * PI.toFloat()

            // Color pulse advances with bass
            colorPulse += 0.5f + smoothedBass * 2f
            if (colorPulse > 360f) colorPulse -= 360f
        } else {
            // Gentle decay
            smoothedEnergy *= 0.95f
            smoothedBass *= 0.95f
            smoothedMid *= 0.95f
            smoothedHigh *= 0.95f
            wavePhase += 0.02f
            colorPulse += 0.3f
        }
    }

    // Animated values for ultra-smooth motion
    val animatedEnergy by animateFloatAsState(
        targetValue = smoothedEnergy,
        animationSpec = tween(durationMillis = 150),
        label = "energy"
    )

    val animatedBass by animateFloatAsState(
        targetValue = smoothedBass,
        animationSpec = tween(durationMillis = 120),
        label = "bass"
    )

    val animatedMid by animateFloatAsState(
        targetValue = smoothedMid,
        animationSpec = tween(durationMillis = 140),
        label = "mid"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Smooth waves section (top 45%)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(bottom = 8.dp)
            ) {
                drawSmoothWaves(
                    phase = wavePhase,
                    energy = animatedEnergy,
                    bass = animatedBass,
                    mid = animatedMid,
                    colorPulse = colorPulse
                )
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
}

/**
 * Draw smooth, gentle flowing waves that pulse with the music.
 * Only 3 wave layers with bezier curves for organic motion.
 */
private fun DrawScope.drawSmoothWaves(
    phase: Float,
    energy: Float,
    bass: Float,
    mid: Float,
    colorPulse: Float
) {
    val width = size.width
    val height = size.height
    val centerY = height * 0.42f
    val reflectionY = height * 0.72f

    // Pulsing colors based on audio
    val hue1 = (colorPulse % 360f)
    val hue2 = ((colorPulse + 60f) % 360f)
    val hue3 = ((colorPulse + 180f) % 360f)

    // Colors pulse and shift with the music
    val color1 = Color.hsv(hue1, 0.9f, 1f)  // Primary - shifts with bass
    val color2 = Color.hsv(hue2, 0.85f, 1f) // Secondary
    val color3 = Color.hsv(hue3, 0.8f, 0.95f) // Accent

    // Base amplitude - grows with energy
    val baseAmplitude = height * 0.15f * (1f + energy * 2f)

    // Background glow that pulses
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                color1.copy(alpha = 0.12f + bass * 0.15f),
                color2.copy(alpha = 0.06f + energy * 0.08f),
                Color.Transparent
            ),
            center = Offset(width / 2, centerY),
            radius = width * 0.8f
        ),
        size = size
    )

    // Only 3 smooth wave layers
    data class SmoothWave(
        val frequency: Float,      // Wave frequency
        val phaseOffset: Float,    // Phase offset
        val amplitudeScale: Float, // Amplitude multiplier
        val energySource: Float,   // Which energy drives this wave
        val color: Color,
        val strokeWidth: Float,
        val glowWidth: Float,
        val alpha: Float
    )

    val waves = listOf(
        // Main wave - driven by bass
        SmoothWave(1.5f, 0f, 1.0f, bass, color1, 4f, 28f, 0.85f),
        // Secondary wave - driven by mid, slightly offset
        SmoothWave(2f, PI.toFloat() * 0.4f, 0.8f, mid, color2, 3f, 22f, 0.7f),
        // Accent wave - driven by overall energy
        SmoothWave(1.2f, PI.toFloat() * 0.8f, 0.6f, energy, color3, 2.5f, 18f, 0.55f)
    )

    // Draw each wave using smooth quadratic bezier curves
    for (wave in waves) {
        val path = Path()
        val segments = 8  // Few segments = smoother curve

        // Calculate control points for smooth bezier
        val points = mutableListOf<Offset>()
        for (i in 0..segments) {
            val x = (i.toFloat() / segments) * width
            val normalizedX = i.toFloat() / segments

            // Smooth sine wave modulated by energy
            val waveValue = sin(normalizedX * PI.toFloat() * wave.frequency * 2 + phase + wave.phaseOffset)

            // Amplitude varies with the energy source
            val amplitude = baseAmplitude * wave.amplitudeScale * (0.5f + wave.energySource * 1.5f)

            // Smooth envelope - tapers at edges
            val envelope = sin(normalizedX * PI.toFloat()).coerceIn(0f, 1f)

            val y = centerY + waveValue * amplitude * envelope
            points.add(Offset(x, y))
        }

        // Build smooth path using quadratic bezier curves
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]

                // Control point is midway between points
                val controlX = (prev.x + curr.x) / 2
                val controlY = (prev.y + curr.y) / 2

                path.quadraticBezierTo(prev.x, prev.y, controlX, controlY)
            }

            // Connect to last point
            val last = points.last()
            path.lineTo(last.x, last.y)
        }

        // Stroke width pulses with energy
        val pulseScale = 1f + wave.energySource * 0.4f

        // Draw outer glow (wide, soft)
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    wave.color.copy(alpha = wave.alpha * 0.15f),
                    wave.color.copy(alpha = wave.alpha * 0.3f),
                    wave.color.copy(alpha = wave.alpha * 0.3f),
                    wave.color.copy(alpha = wave.alpha * 0.15f)
                )
            ),
            style = Stroke(
                width = wave.glowWidth * pulseScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw main wave line
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    wave.color.copy(alpha = wave.alpha * 0.5f),
                    wave.color.copy(alpha = wave.alpha),
                    wave.color.copy(alpha = wave.alpha),
                    wave.color.copy(alpha = wave.alpha * 0.5f)
                )
            ),
            style = Stroke(
                width = wave.strokeWidth * pulseScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Draw soft reflection
    for (wave in waves.take(2)) {
        val path = Path()
        val segments = 6

        val points = mutableListOf<Offset>()
        for (i in 0..segments) {
            val x = (i.toFloat() / segments) * width
            val normalizedX = i.toFloat() / segments

            val waveValue = sin(normalizedX * PI.toFloat() * wave.frequency * 2 + phase + wave.phaseOffset)
            val amplitude = baseAmplitude * wave.amplitudeScale * 0.25f * (0.5f + wave.energySource)
            val envelope = sin(normalizedX * PI.toFloat()).coerceIn(0f, 1f)

            // Reflection is inverted
            val y = reflectionY - waveValue * amplitude * envelope
            points.add(Offset(x, y))
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val controlX = (prev.x + curr.x) / 2
                val controlY = (prev.y + curr.y) / 2
                path.quadraticBezierTo(prev.x, prev.y, controlX, controlY)
            }
            path.lineTo(points.last().x, points.last().y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    wave.color.copy(alpha = wave.alpha * 0.08f),
                    wave.color.copy(alpha = wave.alpha * 0.15f),
                    wave.color.copy(alpha = wave.alpha * 0.15f),
                    wave.color.copy(alpha = wave.alpha * 0.08f)
                )
            ),
            style = Stroke(
                width = wave.glowWidth * 0.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Water surface gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color1.copy(alpha = 0.08f + bass * 0.06f),
                color2.copy(alpha = 0.04f)
            ),
            startY = reflectionY - height * 0.08f,
            endY = height
        ),
        topLeft = Offset(0f, reflectionY - height * 0.08f),
        size = Size(width, height - reflectionY + height * 0.08f)
    )

    // Central glow that pulses with bass
    val glowRadius = width * 0.4f * (0.7f + bass * 0.5f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color1.copy(alpha = 0.2f + bass * 0.2f),
                color2.copy(alpha = 0.1f + energy * 0.1f),
                Color.Transparent
            ),
            center = Offset(width / 2, centerY),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(width / 2, centerY)
    )
}

private fun DrawScope.drawModernSpectrum(fftData: FloatArray) {
    // Colors based on amplitude (volume) - cyan for quiet, magenta for medium, red for loud
    val colorLow = Color(0xFF00D4FF)     // Cyan - quiet
    val colorMid = Color(0xFFFF00FF)     // Magenta - medium
    val colorHigh = Color(0xFFFF3333)    // Red - loud
    val glowAlpha = 0.4f

    val width = size.width
    val height = size.height
    val labelHeight = 24f  // Space for frequency labels
    val barCount = minOf(fftData.size, 32)
    val barWidth = (width / barCount) * 0.7f
    val barSpacing = (width / barCount) * 0.3f
    val maxBarHeight = (height - labelHeight) * 0.9f

    // Map FFT bins to bars - each bar represents a frequency range
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
        val y = height - labelHeight - barHeight

        // Color based on volume (amplitude) - cyan → magenta → red
        val barColor = when {
            amplitude < 0.4f -> lerpColorStudio(colorLow, colorMid, amplitude / 0.4f)
            else -> lerpColorStudio(colorMid, colorHigh, (amplitude - 0.4f) / 0.6f)
        }

        // Glow effect - intensity based on volume
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

        // Top highlight
        if (barHeight > 4f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.4f * amplitude + 0.1f),
                topLeft = Offset(x + barWidth * 0.2f, y),
                size = Size(barWidth * 0.3f, minOf(barHeight * 0.12f, 5f)),
                cornerRadius = CornerRadius(2f)
            )
        }
    }

    // Draw frequency labels at bottom
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        textSize = 22f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    // Frequency labels: map bar positions to approximate frequencies
    // Assuming 22kHz sample rate, FFT gives us 0-11kHz range across bars
    val freqLabels = listOf(
        0 to "Bass",
        barCount / 4 to "Low",
        barCount / 2 to "Mid",
        (barCount * 3) / 4 to "High",
        barCount - 1 to "Air"
    )

    drawContext.canvas.nativeCanvas.apply {
        freqLabels.forEach { (barIndex, label) ->
            val x = barIndex * (barWidth + barSpacing) + barSpacing / 2 +
                    (width - barCount * (barWidth + barSpacing)) / 2 + barWidth / 2
            drawText(label, x, height - 4f, labelPaint)
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
