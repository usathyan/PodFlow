package de.danoeh.antennapod.ui.screen.home.tiled

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import de.danoeh.antennapod.storage.preferences.UserPreferences
import de.danoeh.antennapod.ui.theme.PodcastTileShape

/**
 * Main Tiled Home Screen composable.
 * Displays podcasts in a grid with play buttons to start the latest downloaded episode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiledHomeScreen(
    viewModel: TiledHomeViewModel = viewModel(),
    onPodcastClick: (PodcastTileData) -> Unit = {},
    onAddPodcastClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Layout preferences state
    var gridColumns by remember { mutableStateOf(UserPreferences.getHomeGridColumns()) }
    var viewMode by remember { mutableStateOf(UserPreferences.getHomeViewMode()) }
    var showMenu by remember { mutableStateOf(false) }
    var radioMode by remember { mutableStateOf(UserPreferences.isRadioMode()) }

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
                    // View mode toggle button
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == UserPreferences.HomeViewMode.GRID) {
                                UserPreferences.HomeViewMode.LIST
                            } else {
                                UserPreferences.HomeViewMode.GRID
                            }
                            UserPreferences.setHomeViewMode(viewMode)
                        }
                    ) {
                        Icon(
                            if (viewMode == UserPreferences.HomeViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (viewMode == UserPreferences.HomeViewMode.GRID) "Switch to List" else "Switch to Grid",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.loadPodcasts() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Text(
                                "Grid Columns",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            DropdownMenuItem(
                                text = { Text("2 Columns") },
                                onClick = {
                                    gridColumns = UserPreferences.HomeGridColumns.TWO
                                    UserPreferences.setHomeGridColumns(gridColumns)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (gridColumns == UserPreferences.HomeGridColumns.TWO) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("3 Columns") },
                                onClick = {
                                    gridColumns = UserPreferences.HomeGridColumns.THREE
                                    UserPreferences.setHomeGridColumns(gridColumns)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (gridColumns == UserPreferences.HomeGridColumns.THREE) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Auto (Adaptive)") },
                                onClick = {
                                    gridColumns = UserPreferences.HomeGridColumns.AUTO
                                    UserPreferences.setHomeGridColumns(gridColumns)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    if (gridColumns == UserPreferences.HomeGridColumns.AUTO) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                "Radio Mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Radio Mode")
                                        Text(
                                            "Auto-delete after play, normalize volume",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    radioMode = !radioMode
                                    UserPreferences.setRadioMode(radioMode)
                                },
                                leadingIcon = {
                                    if (radioMode) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPodcastClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Podcast")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is TiledHomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is TiledHomeUiState.Empty -> {
                    EmptyState()
                }
                is TiledHomeUiState.Error -> {
                    ErrorState(message = state.message) {
                        viewModel.loadPodcasts()
                    }
                }
                is TiledHomeUiState.Success -> {
                    if (viewMode == UserPreferences.HomeViewMode.LIST) {
                        PodcastList(
                            podcasts = state.podcasts,
                            onPlayClick = { podcast ->
                                val started = viewModel.playLatestEpisode(context, podcast)
                                if (!started) {
                                    Toast.makeText(
                                        context,
                                        "No downloaded episodes available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onPodcastClick = onPodcastClick
                        )
                    } else {
                        PodcastGrid(
                            podcasts = state.podcasts,
                            gridColumns = gridColumns,
                            onPlayClick = { podcast ->
                                val started = viewModel.playLatestEpisode(context, podcast)
                                if (!started) {
                                    Toast.makeText(
                                        context,
                                        "No downloaded episodes available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onPodcastClick = onPodcastClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastGrid(
    podcasts: List<PodcastTileData>,
    gridColumns: UserPreferences.HomeGridColumns = UserPreferences.HomeGridColumns.AUTO,
    onPlayClick: (PodcastTileData) -> Unit,
    onPodcastClick: (PodcastTileData) -> Unit
) {
    val columns = when (gridColumns) {
        UserPreferences.HomeGridColumns.TWO -> GridCells.Fixed(2)
        UserPreferences.HomeGridColumns.THREE -> GridCells.Fixed(3)
        UserPreferences.HomeGridColumns.AUTO -> GridCells.Adaptive(minSize = 150.dp)
    }

    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(podcasts, key = { it.feed.id }) { podcast ->
            PodcastTile(
                podcast = podcast,
                onPlayClick = { onPlayClick(podcast) },
                onClick = { onPodcastClick(podcast) }
            )
        }
    }
}

@Composable
private fun PodcastList(
    podcasts: List<PodcastTileData>,
    onPlayClick: (PodcastTileData) -> Unit,
    onPodcastClick: (PodcastTileData) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(podcasts, key = { it.feed.id }) { podcast ->
            PodcastListItem(
                podcast = podcast,
                onPlayClick = { onPlayClick(podcast) },
                onClick = { onPodcastClick(podcast) }
            )
        }
    }
}

@Composable
private fun PodcastListItem(
    podcast: PodcastTileData,
    onPlayClick: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "list_item_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Podcast cover image
            Card(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(podcast.feed.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = podcast.feed.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and episode count
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = podcast.feed.title ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (podcast.downloadedEpisodeCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${podcast.downloadedEpisodeCount} downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Play button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onPlayClick() },
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play latest episode",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastTile(
    podcast: PodcastTileData,
    onPlayClick: () -> Unit,
    onClick: () -> Unit
) {
    // Interactive press state for tile
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "tile_scale"
    )

    // Interactive press state for play button
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.9f else 1f,
        label = "play_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = PodcastTileShape,
                clip = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = PodcastTileShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Podcast cover image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(podcast.feed.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = podcast.feed.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for better text/button visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Play button in center with animation
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .scale(playScale)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null
                    ) { onPlayClick() },
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play latest episode",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Episode count badge (if there are downloaded episodes)
            if (podcast.downloadedEpisodeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ) {
                        Text(
                            text = podcast.downloadedEpisodeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Podcast title at bottom with gradient background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = podcast.feed.title ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
        // Large podcast icon
        Icon(
            imageVector = Icons.Outlined.Podcasts,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Text(
            text = "No Podcasts Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Tap the + button to discover and subscribe to your favorite podcasts",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading podcasts",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
        IconButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry")
        }
    }
}

/**
 * Preview-only composable showing the tiled grid with sample data.
 * This demonstrates the UI without requiring actual database data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiledHomeScreenPreview(
    samplePodcasts: List<PreviewPodcastData>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podcasts") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(samplePodcasts) { podcast ->
                PreviewPodcastTile(podcast = podcast)
            }
        }
    }
}

/**
 * Simple data class for preview podcasts (doesn't depend on database models)
 */
data class PreviewPodcastData(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val downloadedEpisodeCount: Int
)

@Composable
private fun PreviewPodcastTile(podcast: PreviewPodcastData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(6.dp, PodcastTileShape),
        shape = PodcastTileShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder background color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when ((podcast.id % 4).toInt()) {
                            0 -> Color(0xFF6750A4)
                            1 -> Color(0xFF7D5260)
                            2 -> Color(0xFF625B71)
                            else -> Color(0xFF4A4458)
                        }
                    )
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Play button in center
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play latest episode",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Episode count badge
            if (podcast.downloadedEpisodeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ) {
                        Text(
                            text = podcast.downloadedEpisodeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Podcast title at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
