package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit,
    onRestoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) IndigoPrimary.copy(alpha = 0.18f) else SurfaceCard,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = IndigoPrimary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Thumbnail or Media Type Emblem
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E2330), Color(0xFF2B3347))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.mediaType == MediaType.PHOTO || item.mediaType == MediaType.VIDEO) {
                    AsyncImage(
                        model = File(item.filePath),
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (item.mediaType == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = if (isPlaying) EmeraldAccent else IndigoPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp)
            ) {
                Text(
                    text = item.displayTitle,
                    color = if (isPlaying) EmeraldAccent else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subText = buildString {
                    if (!item.artist.isNullOrBlank()) {
                        append(item.artist)
                        append(" • ")
                    }
                    if (item.durationMs > 0) {
                        append(formatDuration(item.durationMs))
                        append(" • ")
                    }
                    append(formatBytes(item.sizeBytes))
                }
                Text(
                    text = subText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) RoseError else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(SurfaceCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Details & Info", color = TextPrimary) },
                        onClick = {
                            menuExpanded = false
                            onInfoClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Unhide to Gallery", color = EmeraldAccent) },
                        onClick = {
                            menuExpanded = false
                            onRestoreClick?.invoke()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Storage", color = TextPrimary) },
                        onClick = {
                            menuExpanded = false
                            onExportClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete from Vault", color = RoseError) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.mediaType == MediaType.PHOTO || item.mediaType == MediaType.VIDEO) {
                AsyncImage(
                    model = File(item.filePath),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E2330), Color(0xFF11141C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Dark bottom gradient for legible label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.displayTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.durationMs > 0) {
                        Text(
                            text = formatDuration(item.durationMs),
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Top action overlays
            if (isSelectionMode) {
                Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart)) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = IndigoPrimary,
                            checkmarkColor = Color.White
                        )
                    )
                }
            } else if (item.isFavorite) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
