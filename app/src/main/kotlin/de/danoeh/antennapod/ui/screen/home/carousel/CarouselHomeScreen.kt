package de.danoeh.antennapod.ui.screen.home.carousel

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.danoeh.antennapod.ui.theme.PodcastTileShape

/**
 * Carousel Home Screen - Horizontal scrollable podcast covers.
 * Tap to play, visual states for unplayed/in-progress/completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselHomeScreen(
    viewModel: CarouselHomeViewModel = viewModel(),
    onPodcastClick: (CarouselPodcastData) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Initialize view model
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PodFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadPodcasts(context) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when (val state = uiState) {
                is CarouselHomeUiState.Loading -> LoadingState()
                is CarouselHomeUiState.Empty -> EmptyState()
                is CarouselHomeUiState.Error -> ErrorState(state.message)
                is CarouselHomeUiState.Success -> {
                    CarouselContent(
                        podcasts = state.podcasts,
                        currentIndex = state.currentIndex,
                        allCompleted = state.allCompleted,
                        onPlayClick = { podcast ->
                            if (podcast.playbackState != PodcastPlaybackState.COMPLETED) {
                                val started = viewModel.playPodcast(context, podcast)
                                if (!started) {
                                    Toast.makeText(context, "No episode available", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Already played today", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselContent(
    podcasts: List<CarouselPodcastData>,
    currentIndex: Int,
    allCompleted: Boolean,
    onPlayClick: (CarouselPodcastData) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to current index on first composition
    LaunchedEffect(currentIndex) {
        if (podcasts.isNotEmpty() && currentIndex >= 0) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -100 // Center the item a bit
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status text
        if (allCompleted) {
            Text(
                text = "All caught up for today!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "Tap to play",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Horizontal carousel - large items
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(podcasts) { index, podcast ->
                val isFocused = index == currentIndex
                PodcastCarouselItem(
                    podcast = podcast,
                    isFocused = isFocused,
                    onPlayClick = { onPlayClick(podcast) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress indicator dots
        CarouselDots(
            total = podcasts.size,
            current = currentIndex,
            completedSet = podcasts.mapIndexedNotNull { index, p ->
                if (p.playbackState == PodcastPlaybackState.COMPLETED) index else null
            }.toSet()
        )
    }
}

@Composable
private fun PodcastCarouselItem(
    podcast: CarouselPodcastData,
    isFocused: Boolean,
    onPlayClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale animation
    val baseScale = if (isFocused) 1.1f else 1f
    val pressScale = if (isPressed) 0.95f else 1f
    val scale by animateFloatAsState(
        targetValue = baseScale * pressScale,
        animationSpec = tween(200),
        label = "carousel_scale"
    )

    // Alpha for completed state
    val alpha = if (podcast.playbackState == PodcastPlaybackState.COMPLETED) 0.4f else 1f

    Box(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(1f)
            .scale(scale)
            .alpha(alpha)
            .shadow(
                elevation = if (isFocused) 16.dp else 8.dp,
                shape = PodcastTileShape
            )
            .clip(PodcastTileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onPlayClick() }
    ) {
        // Cover image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(podcast.feed.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = podcast.feed.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay based on state
        when (podcast.playbackState) {
            PodcastPlaybackState.UNPLAYED -> {
                // Play icon overlay
                PlayOverlay()
            }
            PodcastPlaybackState.IN_PROGRESS -> {
                // Progress ring overlay
                ProgressOverlay(progress = podcast.progressPercent)
            }
            PodcastPlaybackState.COMPLETED -> {
                // Checkmark overlay
                CompletedOverlay()
            }
        }
    }
}

@Composable
private fun PlayOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(80.dp)
                .shadow(12.dp, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(96.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            strokeWidth = 6.dp
        )

        // Progress circle
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.size(96.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )

        // Play icon in center
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun CarouselDots(
    total: Int,
    current: Int,
    completedSet: Set<Int>
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        repeat(total) { index ->
            val isCompleted = index in completedSet
            val isCurrent = index == current

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Podcasts,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No podcasts ready",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Subscribe to podcasts and download episodes to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
