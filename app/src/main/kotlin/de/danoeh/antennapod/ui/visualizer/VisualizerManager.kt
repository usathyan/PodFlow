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

        // Noise reduction settings - tuned for speech responsiveness
        private const val NOISE_FLOOR = 8f         // Minimum magnitude threshold (lowered for speech)
        private const val MIN_OUTPUT = 0.03f       // Minimum output threshold after scaling
        private const val SMOOTHING = 0.2f         // Smoothing factor (reduced for faster response)
    }

    private val lock = Any()
    @Volatile
    private var visualizer: Visualizer? = null
    @Volatile
    private var audioSessionId: Int = 0

    private val _visualizerData = MutableStateFlow(VisualizerData())
    val visualizerData: StateFlow<VisualizerData> = _visualizerData.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val currentPeaks = FloatArray(BAR_COUNT) { 0f }
    private val smoothedMagnitudes = FloatArray(BAR_COUNT) { 0f }

    /**
     * Initialize the visualizer with an audio session ID.
     * Must be called after playback starts.
     * Thread-safe: synchronized on internal lock.
     */
    fun initialize(sessionId: Int): Boolean {
        Log.d(TAG, "initialize() called with sessionId=$sessionId")
        if (sessionId == 0) {
            Log.w(TAG, "Invalid audio session ID: 0")
            return false
        }

        synchronized(lock) {
            if (audioSessionId == sessionId && visualizer != null) {
                Log.d(TAG, "Already initialized with session $sessionId")
                return true
            }

            releaseInternal()
            audioSessionId = sessionId

            return try {
                Log.d(TAG, "Creating Visualizer for session $sessionId")
                visualizer = Visualizer(sessionId).apply {
                    captureSize = CAPTURE_SIZE
                    Log.d(TAG, "Setting data capture listener, maxCaptureRate=${Visualizer.getMaxCaptureRate()}")
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
                Log.d(TAG, "Visualizer initialized successfully for session $sessionId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize visualizer: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Start capturing audio data.
     * Thread-safe: synchronized on internal lock.
     */
    fun startCapture() {
        Log.d(TAG, "startCapture() called, visualizer=${visualizer != null}")
        synchronized(lock) {
            visualizer?.let {
                try {
                    it.enabled = true
                    _isCapturing.value = true
                    Log.d(TAG, "Visualizer capture started successfully, enabled=${it.enabled}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start capture: ${e.message}", e)
                }
            } ?: Log.w(TAG, "startCapture() called but visualizer is null!")
        }
    }

    /**
     * Stop capturing audio data.
     * Thread-safe: synchronized on internal lock.
     */
    fun stopCapture() {
        synchronized(lock) {
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
    }

    /**
     * Release the visualizer resources.
     * Thread-safe: synchronized on internal lock.
     */
    fun release() {
        synchronized(lock) {
            stopCaptureInternal()
            visualizer?.release()
            visualizer = null
            audioSessionId = 0
            Log.d(TAG, "Visualizer released")
        }
    }

    /** Internal stop without acquiring lock (called from release which already holds lock) */
    private fun stopCaptureInternal() {
        visualizer?.let {
            try {
                it.enabled = false
                _isCapturing.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop capture: ${e.message}")
            }
        }
    }

    /** Internal release without acquiring lock (called from initialize which already holds lock) */
    private fun releaseInternal() {
        stopCaptureInternal()
        visualizer?.release()
        visualizer = null
        audioSessionId = 0
        Log.d(TAG, "Visualizer released (internal)")
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

        // Skip first bins (very low frequencies / DC-adjacent / rumble) - start at bin 5
        val startBin = 5

        // Convert FFT data to magnitudes
        for (i in 0 until BAR_COUNT) {
            val binIndex = i + startBin
            val realIndex = binIndex * 2
            val imagIndex = realIndex + 1

            if (realIndex < fft.size && imagIndex < fft.size) {
                val real = fft[realIndex].toFloat()
                val imag = fft[imagIndex].toFloat()
                val magnitude = kotlin.math.sqrt(real * real + imag * imag)

                // Frequency-dependent scaling - balance bass reduction with speech preservation
                // Bars 0-3: very low bass (reduce heavily)
                // Bars 4-24: speech range ~200Hz-2kHz (preserve well)
                // Bars 25+: higher frequencies (full weight)
                val freqWeight = when {
                    i < 4 -> 0.2f + (i.toFloat() / 4f) * 0.3f    // 0.2 to 0.5 for lowest bass
                    i < 24 -> 0.5f + ((i - 4).toFloat() / 20f) * 0.4f  // 0.5 to 0.9 for speech range
                    else -> 0.9f + ((i - 24).toFloat() / (BAR_COUNT - 24)) * 0.1f  // 0.9 to 1.0 for highs
                }

                // Apply noise floor - ignore values below threshold
                val filteredMagnitude = if (magnitude < NOISE_FLOOR) 0f else (magnitude - NOISE_FLOOR) * freqWeight

                // Normalize and apply logarithmic scaling for better visual
                val scaled = (kotlin.math.log10(1 + filteredMagnitude) / 2.5f).coerceIn(0f, 1f)

                // Apply minimum output threshold
                magnitudes[i] = if (scaled < MIN_OUTPUT) 0f else scaled
            }
        }

        // Apply smoothing between frames to reduce flickering
        for (i in 0 until BAR_COUNT) {
            smoothedMagnitudes[i] = smoothedMagnitudes[i] * SMOOTHING + magnitudes[i] * (1 - SMOOTHING)
        }

        // Update peak levels with fall-off
        for (i in 0 until BAR_COUNT) {
            if (smoothedMagnitudes[i] > currentPeaks[i]) {
                currentPeaks[i] = smoothedMagnitudes[i]
            } else {
                currentPeaks[i] = (currentPeaks[i] - PEAK_FALL_RATE).coerceAtLeast(smoothedMagnitudes[i])
            }
        }

        _visualizerData.value = _visualizerData.value.copy(
            fftData = smoothedMagnitudes.copyOf(),
            peakLevels = currentPeaks.copyOf()
        )
    }
}
