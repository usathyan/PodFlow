package de.danoeh.antennapod.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings Screen - App configuration and preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var wifiOnlyDownloads by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Playback section
            item {
                SettingsSectionHeader(title = "Playback")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.PlayCircleOutline,
                    title = "Playback Settings",
                    subtitle = "Speed, skip intervals, continuous playback",
                    onClick = onPlaybackClick
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Default Playback Speed",
                    subtitle = "1.0x",
                    onClick = {}
                )
            }

            // Downloads section
            item {
                SettingsSectionHeader(title = "Downloads")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Download,
                    title = "Download Settings",
                    subtitle = "Auto-download, episode limit",
                    onClick = onDownloadsClick
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Wifi,
                    title = "Download over WiFi only",
                    subtitle = "Save mobile data",
                    checked = wifiOnlyDownloads,
                    onCheckedChange = { wifiOnlyDownloads = it }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Download Location",
                    subtitle = "Internal storage",
                    onClick = {}
                )
            }

            // Storage section
            item {
                SettingsSectionHeader(title = "Storage")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Storage,
                    title = "Storage Usage",
                    subtitle = "Manage downloaded episodes",
                    onClick = onStorageClick
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Auto-cleanup",
                    subtitle = "Delete played episodes after 24 hours",
                    onClick = {}
                )
            }

            // Sync section
            item {
                SettingsSectionHeader(title = "Sync & Backup")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.CloudSync,
                    title = "Synchronization",
                    subtitle = "gpodder.net sync",
                    onClick = onSyncClick
                )
            }

            // Appearance section
            item {
                SettingsSectionHeader(title = "Appearance")
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = "System default",
                    onClick = onAppearanceClick
                )
            }

            // Notifications section
            item {
                SettingsSectionHeader(title = "Notifications")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Settings",
                    subtitle = "New episode alerts",
                    onClick = onNotificationsClick
                )
            }

            // About section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "About PodFlow",
                    subtitle = "Version 1.0.0",
                    onClick = onAboutClick
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.Code,
                    title = "Open Source Licenses",
                    subtitle = "Built on AntennaPod (GPL v3)",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Help & Feedback",
                    subtitle = "Get support or report issues",
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Outlined.BugReport,
                    title = "Report a Bug",
                    subtitle = "Help us improve",
                    onClick = {}
                )
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
