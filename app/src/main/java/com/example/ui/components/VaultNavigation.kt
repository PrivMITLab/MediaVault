package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SortOrder
import com.example.ui.viewmodel.VaultTab
import com.example.ui.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    title: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    isSearching: Boolean,
    searchQuery: String,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onToggleViewMode: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBatchFavorite: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchAddToPlaylist: () -> Unit,
    onBatchRestore: (() -> Unit)? = null
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    when {
        isSelectionMode -> {
            TopAppBar(
                title = {
                    Text(
                        "$selectedCount Selected",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Selection", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll) {
                        Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select All", tint = TextPrimary)
                    }
                    if (onBatchRestore != null) {
                        IconButton(onClick = onBatchRestore) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Unhide to Gallery", tint = EmeraldAccent)
                        }
                    }
                    IconButton(onClick = onBatchFavorite) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite Selected", tint = RoseError)
                    }
                    IconButton(onClick = onBatchAddToPlaylist) {
                        Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = IndigoPrimary)
                    }
                    IconButton(onClick = onBatchDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = RoseError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceElevated)
            )
        }

        isSearching -> {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search vault...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_search_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onToggleSearch(false) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close Search", tint = TextPrimary)
                    }
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceElevated)
            )
        }

        else -> {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(IndigoPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleSearch(true) }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    IconButton(onClick = onToggleViewMode) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = TextPrimary
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort Options", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(SurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)", color = if (sortOrder == SortOrder.DATE_DESC) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    onSelectSortOrder(SortOrder.DATE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest First)", color = if (sortOrder == SortOrder.DATE_ASC) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    onSelectSortOrder(SortOrder.DATE_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)", color = if (sortOrder == SortOrder.NAME_ASC) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    onSelectSortOrder(SortOrder.NAME_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duration (Longest)", color = if (sortOrder == SortOrder.DURATION_DESC) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    onSelectSortOrder(SortOrder.DURATION_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Size (Largest)", color = if (sortOrder == SortOrder.SIZE_DESC) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    onSelectSortOrder(SortOrder.SIZE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        }
    }
}

@Composable
fun VaultBottomNavBar(
    selectedTab: VaultTab,
    onTabSelected: (VaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = SurfaceElevated,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == VaultTab.AUDIO,
            onClick = { onTabSelected(VaultTab.AUDIO) },
            icon = { Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Audio") },
            label = { Text("Audio", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IndigoPrimary,
                selectedTextColor = IndigoPrimary,
                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = selectedTab == VaultTab.VIDEO,
            onClick = { onTabSelected(VaultTab.VIDEO) },
            icon = { Icon(imageVector = Icons.Default.Movie, contentDescription = "Video") },
            label = { Text("Video", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IndigoPrimary,
                selectedTextColor = IndigoPrimary,
                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = selectedTab == VaultTab.PHOTOS,
            onClick = { onTabSelected(VaultTab.PHOTOS) },
            icon = { Icon(imageVector = Icons.Default.Photo, contentDescription = "Photos") },
            label = { Text("Photos", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IndigoPrimary,
                selectedTextColor = IndigoPrimary,
                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = selectedTab == VaultTab.SETTINGS,
            onClick = { onTabSelected(VaultTab.SETTINGS) },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IndigoPrimary,
                selectedTextColor = IndigoPrimary,
                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}
