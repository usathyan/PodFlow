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
    STUDIO,      // VU meters + spectrum bars (pro audio style)
    LIQUID       // Organic flowing waves with morphing blob
}

/**
 * UI state for the visualizer
 */
data class VisualizerUiState(
    val isVisible: Boolean = false,
    val style: VisualizerStyle = VisualizerStyle.STUDIO,
    val data: VisualizerData = VisualizerData(),
    val hasPermission: Boolean = false,
    val isCapturing: Boolean = false
)
