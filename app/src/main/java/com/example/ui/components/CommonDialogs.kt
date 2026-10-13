package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.Playlist
import com.example.playback.PlayerManager
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileInfoDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val dateStr = remember(item.dateAdded) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(item.dateAdded))
    }

    val sizeFormatted = remember(item.sizeBytes) {
        formatBytes(item.sizeBytes)
    }

    val durationFormatted = remember(item.durationMs) {
        if (item.durationMs > 0) formatDuration(item.durationMs) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Media Details", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("File Name", item.fileName)
                if (item.title != null && item.title != item.fileName) {
                    InfoRow("Title", item.title)
                }
                if (!item.artist.isNullOrBlank()) {
                    InfoRow("Artist", item.artist)
                }
                if (!item.album.isNullOrBlank()) {
                    InfoRow("Album", item.album)
                }
                InfoRow("Type", item.mediaType.name)
                InfoRow("MIME Type", item.mimeType)
                InfoRow("File Size", sizeFormatted)
                if (durationFormatted != null) {
                    InfoRow("Duration", durationFormatted)
                }
                if (item.width > 0 && item.height > 0) {
                    InfoRow("Resolution", "${item.width} x ${item.height}")
                }
                InfoRow("Imported On", dateStr)
                InfoRow("Protection", "Private Vault (.nomedia)")
            }
        },
        confirmButton = {
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("export_file_button")
            ) {
                Text("Export File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(
            value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun ImportProgressDialog(
    currentIndex: Int,
    totalFiles: Int,
    bytesCopied: Long,
    totalBytes: Long
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Moving into Vault...", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Importing file $currentIndex of $totalFiles securely to private storage.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                val progress = if (totalBytes > 0) {
                    (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldAccent,
                    trackColor = Color(0xFF232A38)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatBytes(bytesCopied), color = TextMuted, fontSize = 12.sp)
                    if (totalBytes > 0) {
                        Text(formatBytes(totalBytes), color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SleepTimerDialog(
    playerManager: PlayerManager,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair("15 Minutes", 15),
        Pair("30 Minutes", 30),
        Pair("45 Minutes", 45),
        Pair("60 Minutes", 60),
        Pair("90 Minutes", 90)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Sleep Timer", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stop audio playback automatically after:", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { (label, minutes) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerManager.startSleepTimer(minutes)
                                onDismiss()
                            },
                        color = Color(0xFF161A23),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("+${minutes}m", color = IndigoPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    playerManager.cancelSleepTimer()
                    onDismiss()
                }
            ) {
                Text("Turn Off Timer", color = Color(0xFFF43F5E))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EqualizerDialog(
    playerManager: PlayerManager,
    onDismiss: () -> Unit
) {
    val presets = remember { playerManager.getEqualizerPresets() }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    val numBands = remember { playerManager.getEqualizerNumberOfBands().toInt() }
    val levelRange: Pair<Short, Short> = remember { playerManager.getBandLevelRange() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Audio Equalizer", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (presets.isNotEmpty()) {
                    Text("Presets", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyColumn(
                        modifier = Modifier.height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets.indices.toList()) { index ->
                            val isSelected = selectedPresetIndex == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPresetIndex = index
                                        playerManager.setEqualizerPreset(index.toShort())
                                    },
                                color = if (isSelected) IndigoPrimary.copy(alpha = 0.2f) else Color(0xFF161A23),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        presets[index],
                                        color = if (isSelected) IndigoPrimary else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Text("Active", color = IndigoPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (numBands > 0) {
                    Text("Frequency Bands", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    (0 until numBands.coerceAtMost(5)).forEach { bandIndex ->
                        val freqHz = playerManager.getBandCenterFreq(bandIndex.toShort()) / 1000
                        var currentLevel by remember {
                            mutableIntStateOf(playerManager.getBandLevel(bandIndex.toShort()).toInt())
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${freqHz} Hz", color = TextSecondary, fontSize = 12.sp)
                                Text("${currentLevel / 100} dB", color = TextMuted, fontSize = 12.sp)
                            }
                            Slider(
                                value = currentLevel.toFloat(),
                                onValueChange = { newVal ->
                                    val intVal = newVal.toInt()
                                    currentLevel = intVal
                                    playerManager.setBandLevel(bandIndex.toShort(), intVal.toShort())
                                },
                                valueRange = levelRange.first.toFloat()..levelRange.second.toFloat()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Done")
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("New Playlist", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create one below!", color = TextSecondary, fontSize = 14.sp)
                } else {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(playlists) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectPlaylist(playlist.id) },
                                color = Color(0xFF161A23),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    playlist.name,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(14.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create New Playlist")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ImportSourceDialog(
    tab: com.example.ui.viewmodel.VaultTab,
    onDismiss: () -> Unit,
    onSelectGallery: () -> Unit,
    onSelectFiles: () -> Unit,
    onCreateDemoAudio: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = when (tab) {
                        com.example.ui.viewmodel.VaultTab.AUDIO -> "Import Audio into Vault"
                        com.example.ui.viewmodel.VaultTab.VIDEO -> "Import Video into Vault"
                        com.example.ui.viewmodel.VaultTab.PHOTOS -> "Import Photos into Vault"
                        else -> "Import Media into Vault"
                    },
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Choose how you want to add items to your vault:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                // Option 1: Gallery / Device Media Scanner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectGallery() }
                        .testTag("import_source_primary"),
                    color = Color(0xFF161A23),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (tab == com.example.ui.viewmodel.VaultTab.AUDIO) Icons.Default.GraphicEq else Icons.Default.Info,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (tab == com.example.ui.viewmodel.VaultTab.AUDIO) "Device Audio & Music Scanner" else "Device Gallery (Photos & Videos)",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (tab == com.example.ui.viewmodel.VaultTab.AUDIO) "Scan and select songs & voice notes on device" else "Select pictures or videos directly from camera roll",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Option 2: Files / Documents
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectFiles() }
                        .testTag("import_source_documents"),
                    color = Color(0xFF161A23),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Device Storage & Documents (Files)",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Browse downloads, file manager, SD card",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Option 3: Demo / Test Audio (for emulator or instant test)
                if (tab == com.example.ui.viewmodel.VaultTab.AUDIO && onCreateDemoAudio != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCreateDemoAudio() }
                            .testTag("import_source_demo_audio"),
                        color = Color(0xFF161A23),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Generate Demo Voice Memo",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Instant test recording (ideal for emulators)",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Privacy guarantee note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🔒 Safe Vault Security: Like Google Photos Locked Folder, imported files are kept in an isolated encrypted vault with .nomedia protection.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}


fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
