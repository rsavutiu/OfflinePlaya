package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AppButton
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.SyncStatusLine
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Home page: title bar with a Settings affordance, centered actions to pick a
 * music folder and (once anything is scanned) open the library, and a live
 * scan status caption. Pure presentation — every action is a callback.
 */
@Composable
fun HomePage(
    status: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanning = status is SyncStatus.Scanning
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            AppTopBar(
                title = "OfflinePlaya",
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Header section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .padding(14.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "OfflinePlaya",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sync Status pill
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = when (status) {
                            SyncStatus.Idle -> MaterialTheme.colorScheme.surfaceVariant
                            is SyncStatus.Scanning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            is SyncStatus.Completed -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            is SyncStatus.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when (status) {
                                            SyncStatus.Idle -> MaterialTheme.colorScheme.outline
                                            is SyncStatus.Scanning -> MaterialTheme.colorScheme.primary
                                            is SyncStatus.Completed -> MaterialTheme.colorScheme.secondary
                                            is SyncStatus.Failed -> MaterialTheme.colorScheme.error
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SyncStatusLine(status = status)
                        }
                    }
                }

                // Action cards section
                if (trackCount > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Open library hero card
                        ElevatedCard(
                            onClick = onOpenLibrary,
                            enabled = !scanning,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "My Music Library",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (scanning) "Scanning..." else "$trackCount tracks available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Secondary row cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Playlists card
                            ElevatedCard(
                                onClick = onOpenPlaylists,
                                enabled = !scanning,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Playlists",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Collections",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Import Folder card
                            ElevatedCard(
                                onClick = onPickFolder,
                                enabled = !scanning,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Add Folders",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Import tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty library hero card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Start Your Music Journey",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select a folder containing audio files on your device to build your offline library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            androidx.compose.material3.Button(
                                onClick = onPickFolder,
                                enabled = !scanning,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (scanning) "Scanning..." else "Pick music folder",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomePageIdlePreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Idle,
            trackCount = 0,
            onPickFolder = {}, onOpenLibrary = {}, onOpenPlaylists = {},
            onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageScanningPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Scanning("content://tree/root"),
            trackCount = 0,
            onPickFolder = {}, onOpenLibrary = {}, onOpenPlaylists = {},
            onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 87, tracksFailed = 0),
            ),
            trackCount = 87,
            onPickFolder = {}, onOpenLibrary = {}, onOpenPlaylists = {},
            onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageFailedPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Failed("permission denied"),
            trackCount = 142,
            onPickFolder = {}, onOpenLibrary = {}, onOpenPlaylists = {},
            onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedDarkPreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 85, tracksFailed = 2),
            ),
            trackCount = 85,
            onPickFolder = {}, onOpenLibrary = {}, onOpenPlaylists = {},
            onOpenSearch = {}, onOpenSettings = {},
        )
    }
}
