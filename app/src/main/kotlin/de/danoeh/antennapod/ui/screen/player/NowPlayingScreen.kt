package de.danoeh.antennapod.ui.screen.player

import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.danoeh.antennapod.ui.components.FlipCard
import de.danoeh.antennapod.ui.theme.PodFlowPurple
import de.danoeh.antennapod.ui.visualizer.VisualizerContainer
import de.danoeh.antennapod.ui.visualizer.VisualizerViewModel

/**
 * Now Playing Screen - Full screen audio player
 * Beautiful, modern design with large artwork and intuitive controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSkipBack: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSkipPreviousPodcast: () -> Unit = {},
    onSkipNextPodcast: () -> Unit = {},
    onSeek: (Long) -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Queue */ }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            // Episode info
            EpisodeInfoSection(
                title = playbackState.title,
                podcastName = playbackState.podcastName
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress slider
            ProgressSection(
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                onSeek = { position ->
                    viewModel.seekTo(position.toLong())
                    onSeek(position.toLong())
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls
            PlaybackControlsSection(
                isPlaying = playbackState.isPlaying,
                onPlayPause = {
                    viewModel.togglePlayPause()
                    onPlayPause()
                },
                onSkipBack = {
                    viewModel.skipBackward()
                    onSkipBack()
                },
                onSkipForward = {
                    viewModel.skipForward()
                    onSkipForward()
                },
                onSkipPreviousPodcast = onSkipPreviousPodcast,
                onSkipNextPodcast = onSkipNextPodcast
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Additional controls
            AdditionalControlsSection(
                playbackSpeed = playbackState.playbackSpeed,
                sleepTimerActive = playbackState.sleepTimerActive,
                onSpeedClick = onSpeedClick,
                onSleepTimerClick = onSleepTimerClick
            )
        }
    }
}

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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeInfoSection(
    title: String,
    podcastName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title.ifEmpty { "No episode playing" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = podcastName.ifEmpty { "Select a podcast" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressSection(
    currentPosition: Long,
    duration: Long,
    onSeek: (Float) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (isUserDragging > 0f) sliderPosition else progress,
            onValueChange = { value ->
                sliderPosition = value
                isUserDragging = 1f
            },
            onValueChangeFinished = {
                onSeek(sliderPosition * duration)
                isUserDragging = 0f
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackControlsSection(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipPreviousPodcast: () -> Unit,
    onSkipNextPodcast: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip to previous podcast button
        ControlButton(
            onClick = onSkipPreviousPodcast,
            icon = { modifier ->
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous podcast",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Skip backward button
        ControlButton(
            onClick = onSkipBack,
            icon = { modifier ->
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "Skip back 10 seconds",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Play/Pause button
        PlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPause
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Skip forward button
        ControlButton(
            onClick = onSkipForward,
            icon = { modifier ->
                Icon(
                    Icons.Default.Forward30,
                    contentDescription = "Skip forward 30 seconds",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Skip to next podcast button
        ControlButton(
            onClick = onSkipNextPodcast,
            icon = { modifier ->
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next podcast",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            size = 40.dp
        )
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "control_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon(Modifier.size(size * 0.7f))
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "play_scale"
    )

    Surface(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun AdditionalControlsSection(
    playbackSpeed: Float,
    sleepTimerActive: Boolean,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed control
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSpeedClick() },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = "Playback speed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sleep timer
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSleepTimerClick() },
            color = if (sleepTimerActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Bedtime,
                    contentDescription = "Sleep timer",
                    modifier = Modifier.size(20.dp),
                    tint = if (sleepTimerActive)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sleepTimerActive) "On" else "Sleep",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sleepTimerActive)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Format milliseconds to mm:ss or hh:mm:ss
 */
private fun formatTime(millis: Long): String {
    if (millis < 0) return "0:00"

    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
