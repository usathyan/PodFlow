package de.danoeh.antennapod.ui.visualizer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
