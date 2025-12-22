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
