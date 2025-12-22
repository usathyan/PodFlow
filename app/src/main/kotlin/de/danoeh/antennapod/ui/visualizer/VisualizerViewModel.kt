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

    /**
     * Release visualizer resources.
     * Call this in Fragment's onStop() to prevent resource leaks.
     */
    fun releaseVisualizer() {
        visualizerManager.release()
    }

    override fun onCleared() {
        super.onCleared()
        visualizerManager.release()
    }
}
