package de.danoeh.antennapod.ui.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * Flowing liquid wave visualizer with multiple colored layers.
 *
 * Features:
 * - Multiple layered flowing waves
 * - Each wave has distinct colors
 * - Waves react to different frequency ranges
 * - Smooth organic movement
 */
@Composable
fun LiquidVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF050510)

    // Wave color schemes - each layer has its own gradient
    val wave1Colors = listOf(Color(0xFF6366F1), Color(0xFF818CF8))  // Indigo
    val wave2Colors = listOf(Color(0xFF06B6D4), Color(0xFF22D3EE))  // Cyan
    val wave3Colors = listOf(Color(0xFFEC4899), Color(0xFFF472B6))  // Pink
    val wave4Colors = listOf(Color(0xFF10B981), Color(0xFF34D399))  // Emerald
    val wave5Colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))  // Amber

    // Infinite animation for wave movement
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Calculate energy from different frequency bands
    val bassEnergy = remember(data.fftData) {
        if (data.fftData.size >= 8) data.fftData.take(8).average().toFloat()
        else if (data.fftData.isNotEmpty()) data.fftData.average().toFloat()
        else 0f
    }

    val lowMidEnergy = remember(data.fftData) {
        if (data.fftData.size >= 16) data.fftData.drop(8).take(8).average().toFloat()
        else 0f
    }

    val midEnergy = remember(data.fftData) {
        if (data.fftData.size >= 24) data.fftData.drop(16).take(8).average().toFloat()
        else 0f
    }

    val highMidEnergy = remember(data.fftData) {
        if (data.fftData.size >= 32) data.fftData.drop(24).take(8).average().toFloat()
        else 0f
    }

    val highEnergy = remember(data.fftData) {
        if (data.fftData.size >= 40) data.fftData.drop(32).average().toFloat()
        else 0f
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val width = size.width
        val height = size.height

        // Draw subtle ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    wave1Colors[0].copy(alpha = 0.08f + bassEnergy * 0.1f),
                    Color.Transparent
                ),
                center = Offset(width / 2, height / 2),
                radius = width * 0.7f
            ),
            radius = width * 0.7f,
            center = Offset(width / 2, height / 2)
        )

        // Wave 5 (back) - Amber - reacts to highs
        drawFlowingWave(
            waveData = data.fftData,
            phase = wavePhase + PI.toFloat() * 1.6f,
            baseY = height * 0.75f,
            amplitude = height * 0.08f,
            energy = highEnergy,
            colors = wave5Colors,
            alpha = 0.35f,
            frequency = 2.5f,
            fillToBottom = true
        )

        // Wave 4 (back-mid) - Emerald - reacts to high-mids
        drawFlowingWave(
            waveData = data.fftData,
            phase = wavePhase + PI.toFloat() * 1.2f,
            baseY = height * 0.62f,
            amplitude = height * 0.1f,
            energy = highMidEnergy,
            colors = wave4Colors,
            alpha = 0.4f,
            frequency = 2f,
            fillToBottom = true
        )

        // Wave 3 (middle) - Pink - reacts to mids
        drawFlowingWave(
            waveData = data.fftData,
            phase = wavePhase + PI.toFloat() * 0.8f,
            baseY = height * 0.5f,
            amplitude = height * 0.12f,
            energy = midEnergy,
            colors = wave3Colors,
            alpha = 0.5f,
            frequency = 1.5f,
            fillToBottom = true
        )

        // Wave 2 (front-mid) - Cyan - reacts to low-mids
        drawFlowingWave(
            waveData = data.fftData,
            phase = wavePhase + PI.toFloat() * 0.4f,
            baseY = height * 0.4f,
            amplitude = height * 0.14f,
            energy = lowMidEnergy,
            colors = wave2Colors,
            alpha = 0.6f,
            frequency = 1.2f,
            fillToBottom = true
        )

        // Wave 1 (front) - Indigo - reacts to bass
        drawFlowingWave(
            waveData = data.fftData,
            phase = wavePhase,
            baseY = height * 0.3f,
            amplitude = height * 0.15f,
            energy = bassEnergy,
            colors = wave1Colors,
            alpha = 0.7f,
            frequency = 1f,
            fillToBottom = true
        )
    }
}

private fun DrawScope.drawFlowingWave(
    waveData: FloatArray,
    phase: Float,
    baseY: Float,
    amplitude: Float,
    energy: Float,
    colors: List<Color>,
    alpha: Float,
    frequency: Float,
    fillToBottom: Boolean
) {
    val path = Path()
    val width = size.width
    val height = size.height
    val points = 120

    // Dynamic amplitude based on energy
    val dynamicAmplitude = amplitude * (0.3f + energy * 1.5f)

    // Build the wave path
    for (i in 0..points) {
        val x = (i.toFloat() / points) * width
        val normalizedX = i.toFloat() / points

        // Primary sine wave
        val primaryWave = sin(normalizedX * 2 * PI.toFloat() * frequency + phase) * dynamicAmplitude * 0.6f

        // Secondary wave for organic feel
        val secondaryWave = sin(normalizedX * 4 * PI.toFloat() * frequency + phase * 1.3f) * dynamicAmplitude * 0.25f

        // Tertiary subtle wave
        val tertiaryWave = sin(normalizedX * 6 * PI.toFloat() * frequency - phase * 0.7f) * dynamicAmplitude * 0.1f

        // FFT reactivity
        val fftIndex = (normalizedX * waveData.size).toInt().coerceIn(0, waveData.size - 1)
        val fftComponent = if (waveData.isNotEmpty()) {
            waveData[fftIndex] * dynamicAmplitude * 0.5f * sin(phase + normalizedX * PI.toFloat() * 2)
        } else {
            0f
        }

        val y = baseY + primaryWave + secondaryWave + tertiaryWave + fftComponent

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    if (fillToBottom) {
        // Close the path to bottom for filled wave
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        // Draw filled wave with gradient
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors[0].copy(alpha = alpha),
                    colors[1].copy(alpha = alpha * 0.6f),
                    colors[1].copy(alpha = alpha * 0.2f),
                    Color.Transparent
                ),
                startY = baseY - dynamicAmplitude,
                endY = height
            )
        )
    }

    // Draw wave outline for definition
    val outlinePath = Path()
    for (i in 0..points) {
        val x = (i.toFloat() / points) * width
        val normalizedX = i.toFloat() / points

        val primaryWave = sin(normalizedX * 2 * PI.toFloat() * frequency + phase) * dynamicAmplitude * 0.6f
        val secondaryWave = sin(normalizedX * 4 * PI.toFloat() * frequency + phase * 1.3f) * dynamicAmplitude * 0.25f
        val tertiaryWave = sin(normalizedX * 6 * PI.toFloat() * frequency - phase * 0.7f) * dynamicAmplitude * 0.1f

        val fftIndex = (normalizedX * waveData.size).toInt().coerceIn(0, waveData.size - 1)
        val fftComponent = if (waveData.isNotEmpty()) {
            waveData[fftIndex] * dynamicAmplitude * 0.5f * sin(phase + normalizedX * PI.toFloat() * 2)
        } else {
            0f
        }

        val y = baseY + primaryWave + secondaryWave + tertiaryWave + fftComponent

        if (i == 0) {
            outlinePath.moveTo(x, y)
        } else {
            outlinePath.lineTo(x, y)
        }
    }

    // Draw glowing outline
    drawPath(
        path = outlinePath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                colors[0].copy(alpha = 0.3f),
                colors[0].copy(alpha = alpha * 0.9f),
                colors[1].copy(alpha = alpha * 0.9f),
                colors[1].copy(alpha = 0.3f)
            )
        ),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Draw bright highlight line on top
    drawPath(
        path = outlinePath,
        color = Color.White.copy(alpha = alpha * 0.3f * (0.5f + energy)),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round)
    )
}
