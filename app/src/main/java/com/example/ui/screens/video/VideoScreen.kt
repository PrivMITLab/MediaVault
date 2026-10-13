package com.example.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.FileInfoDialog
import com.example.ui.components.MediaGridItem
import com.example.ui.components.MediaListItem
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.ViewMode

@Composable
fun VideoScreen(
    viewModel: VaultViewModel,
    onExportItem: (MediaItem) -> Unit,
    onImportClick: () -> Unit
) {
    val videos by viewModel.filteredVideos.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    var activePlayingVideo by remember { mutableStateOf<MediaItem?>(null) }
    var infoItem by remember { mutableStateOf<MediaItem?>(null) }

    if (infoItem != null) {
        FileInfoDialog(
            item = infoItem!!,
            onDismiss = { infoItem = null },
            onExport = {
                onExportItem(infoItem!!)
                infoItem = null
            }
        )
    }

    if (activePlayingVideo != null) {
        VideoPlayerScreen(
            videoItem = activePlayingVideo!!,
            onDismiss = { activePlayingVideo = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Videos in Vault",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import MP4, MKV, AVI, MOV videos to securely hide them from all system galleries.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    androidx.compose.material3.Button(
                        onClick = onImportClick,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_import_videos_btn")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Videos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            if (viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        MediaGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    activePlayingVideo = item
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.startSelectionWith(item.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        MediaListItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    activePlayingVideo = item
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.startSelectionWith(item.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onInfoClick = { infoItem = item },
                            onDeleteClick = { viewModel.deleteItem(item) },
                            onExportClick = { onExportItem(item) }
                        )
                    }
                }
            }
        }
    }
}
