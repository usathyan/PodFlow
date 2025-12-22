# Audio Visualizer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a flip-to-reveal audio visualizer on the NowPlayingScreen with Winamp-style spectrum bars as the primary style.

**Architecture:** Use Android's Visualizer API to capture FFT and waveform data from ExoPlayer's audio session. Data flows through a manager class to StateFlow, consumed by Compose Canvas for rendering. FlipCard composable provides the tap-to-flip interaction.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, Android Visualizer API, StateFlow, Compose Animation

---

## Task 1: Add RECORD_AUDIO Permission

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:14` (after VIBRATE permission)

**Step 1: Add the permission**

In `AndroidManifest.xml`, add after line 14:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

**Step 2: Verify the change**

Run: `grep -n "RECORD_AUDIO" app/src/main/AndroidManifest.xml`
Expected: Line showing the permission

**Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(visualizer): add RECORD_AUDIO permission for audio visualizer"
```

---

## Task 2: Create VisualizerState Data Classes

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerState.kt`

**Step 1: Create the data classes**

```kotlin
package de.danoeh.antennapod.ui.visualizer

/**
 * Represents a single frame of visualizer data
 */
data class VisualizerData(
    val fftData: FloatArray = FloatArray(64) { 0f },
    val waveformData: FloatArray = FloatArray(128) { 0f },
    val peakLevels: FloatArray = FloatArray(64) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerData
        return fftData.contentEquals(other.fftData) &&
                waveformData.contentEquals(other.waveformData) &&
                peakLevels.contentEquals(other.peakLevels)
    }

    override fun hashCode(): Int {
        var result = fftData.contentHashCode()
        result = 31 * result + waveformData.contentHashCode()
        result = 31 * result + peakLevels.contentHashCode()
        return result
    }
}

/**
 * Available visualizer styles
 */
enum class VisualizerStyle {
    WINAMP,      // Classic spectrum bars + oscilloscope
    CIRCULAR,    // Radial frequency display
    WAVEFORM     // Minimal line waveform
}

/**
 * UI state for the visualizer
 */
data class VisualizerUiState(
    val isVisible: Boolean = false,
    val style: VisualizerStyle = VisualizerStyle.WINAMP,
    val data: VisualizerData = VisualizerData(),
    val hasPermission: Boolean = false,
    val isCapturing: Boolean = false
)
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL or no errors related to VisualizerState

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerState.kt
git commit -m "feat(visualizer): add VisualizerState data classes"
```

---

## Task 3: Create VisualizerManager

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerManager.kt`

**Step 1: Create the manager class**

```kotlin
package de.danoeh.antennapod.ui.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages audio visualization data capture from ExoPlayer's audio session.
 * Uses Android's Visualizer API to get FFT and waveform data.
 */
class VisualizerManager {

    companion object {
        private const val TAG = "VisualizerManager"
        private const val CAPTURE_SIZE = 256
        private const val BAR_COUNT = 64
        private const val PEAK_FALL_RATE = 0.08f
    }

    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0

    private val _visualizerData = MutableStateFlow(VisualizerData())
    val visualizerData: StateFlow<VisualizerData> = _visualizerData.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val currentPeaks = FloatArray(BAR_COUNT) { 0f }

    /**
     * Initialize the visualizer with an audio session ID.
     * Must be called after playback starts.
     */
    fun initialize(sessionId: Int): Boolean {
        if (sessionId == 0) {
            Log.w(TAG, "Invalid audio session ID: 0")
            return false
        }

        if (audioSessionId == sessionId && visualizer != null) {
            Log.d(TAG, "Already initialized with session $sessionId")
            return true
        }

        release()
        audioSessionId = sessionId

        return try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = CAPTURE_SIZE
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { processWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { processFft(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
            }
            Log.d(TAG, "Visualizer initialized for session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize visualizer: ${e.message}")
            false
        }
    }

    /**
     * Start capturing audio data.
     */
    fun startCapture() {
        visualizer?.let {
            try {
                it.enabled = true
                _isCapturing.value = true
                Log.d(TAG, "Visualizer capture started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start capture: ${e.message}")
            }
        }
    }

    /**
     * Stop capturing audio data.
     */
    fun stopCapture() {
        visualizer?.let {
            try {
                it.enabled = false
                _isCapturing.value = false
                Log.d(TAG, "Visualizer capture stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop capture: ${e.message}")
            }
        }
    }

    /**
     * Release the visualizer resources.
     */
    fun release() {
        stopCapture()
        visualizer?.release()
        visualizer = null
        audioSessionId = 0
        Log.d(TAG, "Visualizer released")
    }

    private fun processWaveform(waveform: ByteArray) {
        val normalized = FloatArray(waveform.size.coerceAtMost(128)) { i ->
            (waveform[i].toInt() and 0xFF) / 255f
        }

        _visualizerData.value = _visualizerData.value.copy(
            waveformData = normalized
        )
    }

    private fun processFft(fft: ByteArray) {
        val magnitudes = FloatArray(BAR_COUNT)

        // Convert FFT data to magnitudes (skip DC component at index 0)
        for (i in 0 until BAR_COUNT) {
            val realIndex = (i + 1) * 2
            val imagIndex = realIndex + 1

            if (realIndex < fft.size && imagIndex < fft.size) {
                val real = fft[realIndex].toFloat()
                val imag = fft[imagIndex].toFloat()
                val magnitude = kotlin.math.sqrt(real * real + imag * imag)

                // Normalize and apply logarithmic scaling for better visual
                magnitudes[i] = (kotlin.math.log10(1 + magnitude) / 2f).coerceIn(0f, 1f)
            }
        }

        // Update peak levels with fall-off
        for (i in 0 until BAR_COUNT) {
            if (magnitudes[i] > currentPeaks[i]) {
                currentPeaks[i] = magnitudes[i]
            } else {
                currentPeaks[i] = (currentPeaks[i] - PEAK_FALL_RATE).coerceAtLeast(magnitudes[i])
            }
        }

        _visualizerData.value = _visualizerData.value.copy(
            fftData = magnitudes,
            peakLevels = currentPeaks.copyOf()
        )
    }
}
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL or no errors related to VisualizerManager

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerManager.kt
git commit -m "feat(visualizer): add VisualizerManager for audio data capture"
```

---

## Task 4: Create FlipCard Composable

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/components/FlipCard.kt`

**Step 1: Create the FlipCard composable**

```kotlin
package de.danoeh.antennapod.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A card that flips between front and back content with a 3D rotation animation.
 *
 * @param isFlipped Whether to show the back content
 * @param onFlip Called when the card is tapped
 * @param modifier Modifier for the card
 * @param front Content to show on the front (default)
 * @param back Content to show on the back (when flipped)
 */
@Composable
fun FlipCard(
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "flip_rotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onFlip() }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        if (rotation <= 90f) {
            front()
        } else {
            Box(
                modifier = Modifier.graphicsLayer {
                    rotationY = 180f
                }
            ) {
                back()
            }
        }
    }
}
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/components/FlipCard.kt
git commit -m "feat(visualizer): add FlipCard composable with 3D flip animation"
```

---

## Task 5: Create WinampVisualizer Composable

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/WinampVisualizer.kt`

**Step 1: Create the Winamp-style visualizer**

```kotlin
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
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/WinampVisualizer.kt
git commit -m "feat(visualizer): add WinampVisualizer with spectrum bars and oscilloscope"
```

---

## Task 6: Create CircularVisualizer Composable

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/CircularVisualizer.kt`

**Step 1: Create the circular visualizer**

```kotlin
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
import de.danoeh.antennapod.ui.theme.PodFlowPurple
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular visualizer with radial frequency bars.
 *
 * Features:
 * - Bars arranged in a circle radiating outward
 * - PodFlow purple/teal color gradient
 * - Subtle pulse effect with overall audio energy
 */
@Composable
fun CircularVisualizer(
    data: VisualizerData,
    modifier: Modifier = Modifier
) {
    val primaryColor = PodFlowPurple
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
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/CircularVisualizer.kt
git commit -m "feat(visualizer): add CircularVisualizer with radial bars"
```

---

## Task 7: Create WaveformVisualizer Composable

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/WaveformVisualizer.kt`

**Step 1: Create the minimal waveform visualizer**

```kotlin
package de.danoeh.antennapod.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                modifier = Modifier.fillMaxSize()
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
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/WaveformVisualizer.kt
git commit -m "feat(visualizer): add WaveformVisualizer with minimal line style"
```

---

## Task 8: Create VisualizerContainer Composable

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerContainer.kt`

**Step 1: Create the container with swipe gestures**

```kotlin
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
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerContainer.kt
git commit -m "feat(visualizer): add VisualizerContainer with swipe gestures"
```

---

## Task 9: Create VisualizerViewModel

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerViewModel.kt`

**Step 1: Create the ViewModel**

```kotlin
package de.danoeh.antennapod.ui.visualizer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for managing visualizer state and audio data capture.
 */
class VisualizerViewModel : ViewModel() {

    private val visualizerManager = VisualizerManager()

    private val _uiState = MutableStateFlow(VisualizerUiState())
    val uiState: StateFlow<VisualizerUiState> = _uiState.asStateFlow()

    init {
        // Collect visualizer data updates
        viewModelScope.launch {
            visualizerManager.visualizerData.collectLatest { data ->
                _uiState.value = _uiState.value.copy(data = data)
            }
        }

        viewModelScope.launch {
            visualizerManager.isCapturing.collectLatest { capturing ->
                _uiState.value = _uiState.value.copy(isCapturing = capturing)
            }
        }
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    fun checkPermission(context: Context): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
        return hasPermission
    }

    /**
     * Update permission status after user grants/denies.
     */
    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    /**
     * Toggle visualizer visibility.
     */
    fun toggleVisibility() {
        val newVisibility = !_uiState.value.isVisible
        _uiState.value = _uiState.value.copy(isVisible = newVisibility)

        if (newVisibility && _uiState.value.hasPermission) {
            visualizerManager.startCapture()
        } else {
            visualizerManager.stopCapture()
        }
    }

    /**
     * Set visualizer visibility explicitly.
     */
    fun setVisibility(visible: Boolean) {
        if (_uiState.value.isVisible != visible) {
            _uiState.value = _uiState.value.copy(isVisible = visible)

            if (visible && _uiState.value.hasPermission) {
                visualizerManager.startCapture()
            } else {
                visualizerManager.stopCapture()
            }
        }
    }

    /**
     * Change the visualizer style.
     */
    fun setStyle(style: VisualizerStyle) {
        _uiState.value = _uiState.value.copy(style = style)
    }

    /**
     * Initialize visualizer with audio session ID.
     * Call this when playback starts or audio session changes.
     */
    fun initializeVisualizer(audioSessionId: Int) {
        if (visualizerManager.initialize(audioSessionId)) {
            if (_uiState.value.isVisible && _uiState.value.hasPermission) {
                visualizerManager.startCapture()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        visualizerManager.release()
    }
}
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerViewModel.kt
git commit -m "feat(visualizer): add VisualizerViewModel for state management"
```

---

## Task 10: Expose Audio Session ID from ExoPlayerWrapper

**Files:**
- Modify: `playback/service/src/main/java/de/danoeh/antennapod/playback/service/internal/ExoPlayerWrapper.java`

**Step 1: Add getter method**

Add after line 201 (after `isPlaying()` method):

```java
public int getAudioSessionId() {
    return exoPlayer != null ? exoPlayer.getAudioSessionId() : 0;
}
```

**Step 2: Verify the file compiles**

Run: `./gradlew :playback:service:compileDebugJava 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add playback/service/src/main/java/de/danoeh/antennapod/playback/service/internal/ExoPlayerWrapper.java
git commit -m "feat(visualizer): expose audio session ID from ExoPlayerWrapper"
```

---

## Task 11: Integrate FlipCard into NowPlayingScreen

**Files:**
- Modify: `app/src/main/kotlin/de/danoeh/antennapod/ui/screen/player/NowPlayingScreen.kt`

**Step 1: Add imports at the top of the file (after line 68)**

```kotlin
import de.danoeh.antennapod.ui.components.FlipCard
import de.danoeh.antennapod.ui.visualizer.VisualizerContainer
import de.danoeh.antennapod.ui.visualizer.VisualizerViewModel
import de.danoeh.antennapod.ui.visualizer.VisualizerStyle
import androidx.compose.runtime.mutableStateOf
```

**Step 2: Add visualizer state to NowPlayingScreen**

Replace the `ArtworkSection` call (lines 143-148) with:

```kotlin
            // Flippable artwork/visualizer
            val visualizerViewModel: VisualizerViewModel = viewModel()
            val visualizerState by visualizerViewModel.uiState.collectAsState()

            FlipCard(
                isFlipped = visualizerState.isVisible,
                onFlip = { visualizerViewModel.toggleVisibility() },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                front = {
                    ArtworkSection(
                        imageUrl = playbackState.imageUrl,
                        title = playbackState.title
                    )
                },
                back = {
                    VisualizerContainer(
                        data = visualizerState.data,
                        currentStyle = visualizerState.style,
                        albumArtUrl = playbackState.imageUrl,
                        onStyleChange = { visualizerViewModel.setStyle(it) }
                    )
                }
            )
```

**Step 3: Update ArtworkSection to not include its own Surface wrapper**

Replace the `ArtworkSection` composable (lines 203-243) with:

```kotlin
@Composable
private fun ArtworkSection(
    imageUrl: String?,
    title: String
) {
    if (imageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
```

**Step 4: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -30`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/screen/player/NowPlayingScreen.kt
git commit -m "feat(visualizer): integrate FlipCard into NowPlayingScreen"
```

---

## Task 12: Add Permission Request Dialog

**Files:**
- Create: `app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerPermissionDialog.kt`

**Step 1: Create the permission dialog**

```kotlin
package de.danoeh.antennapod.ui.visualizer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Dialog explaining why RECORD_AUDIO permission is needed for the visualizer.
 */
@Composable
fun VisualizerPermissionDialog(
    onPermissionResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Visualizer") },
        text = {
            Text(
                "To show the audio visualizer, PodFlow needs permission to access " +
                "audio data. This is only used to create visual effects that sync " +
                "with your podcast audio."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
```

**Step 2: Verify the file compiles**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | head -20`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/kotlin/de/danoeh/antennapod/ui/visualizer/VisualizerPermissionDialog.kt
git commit -m "feat(visualizer): add permission request dialog"
```

---

## Task 13: Build and Test

**Step 1: Build the debug APK**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 2: Verify no new lint errors**

Run: `./gradlew :app:lintDebug 2>&1 | tail -20`
Expected: No new errors (warnings OK)

**Step 3: Commit all changes**

```bash
git add -A
git commit -m "feat(visualizer): complete audio visualizer implementation

- Add flip-to-reveal visualizer on NowPlayingScreen
- Implement Winamp-style spectrum analyzer (default)
- Add circular and waveform alternative styles
- Support swipe gesture to switch styles
- Handle RECORD_AUDIO permission with rationale dialog"
```

---

## Summary

This implementation adds:

1. **RECORD_AUDIO permission** for Android Visualizer API access
2. **VisualizerManager** - Captures FFT and waveform data from audio session
3. **FlipCard** - 3D flip animation composable
4. **WinampVisualizer** - Classic spectrum bars + oscilloscope
5. **CircularVisualizer** - Radial frequency display
6. **WaveformVisualizer** - Minimal line with blurred artwork
7. **VisualizerContainer** - Swipe gesture handling and style switching
8. **VisualizerViewModel** - State management and permission handling
9. **Integration** - FlipCard wrapping artwork in NowPlayingScreen

The user taps album artwork to flip and reveal the visualizer. Swipe left/right to switch between styles.
