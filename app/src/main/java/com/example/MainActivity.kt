package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MediaItem
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.ImportProgressDialog
import com.example.ui.components.VaultBottomNavBar
import com.example.ui.components.VaultTopBar
import com.example.ui.screens.audio.AudioScreen
import com.example.ui.screens.lock.LockScreen
import com.example.ui.screens.photos.PhotoScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.video.VideoScreen
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MediaVaultTheme
import com.example.ui.viewmodel.VaultTab
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.VaultViewModelFactory

class MainActivity : FragmentActivity() {

    private var pendingShareUris: List<Uri> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Screen protection (FLAG_SECURE) is configurable so AI Studio emulator preview can stream video
        val app = application as MediaVaultApplication
        if (app.securityManager.isScreenProtectionEnabled()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        handleShareIntent(intent)

        setContent {
            MediaVaultTheme {
                val app = application as MediaVaultApplication
                val securityManager = app.securityManager
                val isUnlocked by securityManager.isUnlocked.collectAsState()

                val viewModel: VaultViewModel = viewModel(
                    factory = VaultViewModelFactory(app.repository, securityManager)
                )

                var showDirectPinScreen by remember { mutableStateOf(false) }
                val isDisguiseCalculator = securityManager.isDisguiseCalculatorEnabled()

                LaunchedEffect(pendingShareUris) {
                    if (pendingShareUris.isNotEmpty()) {
                        viewModel.importUris(pendingShareUris)
                        pendingShareUris = emptyList()
                    }
                }

                if (!isUnlocked) {
                    if (isDisguiseCalculator && !showDirectPinScreen) {
                        com.example.ui.screens.disguise.CalculatorDisguiseScreen(
                            securityManager = securityManager,
                            onUnlocked = { isDecoy -> securityManager.unlock(isDecoy) },
                            onPanicTriggered = {
                                viewModel.triggerPanicWipe {
                                    Toast.makeText(this@MainActivity, "Vault reset complete", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenPinScreenDirectly = { showDirectPinScreen = true }
                        )
                    } else {
                        LockScreen(
                            securityManager = securityManager,
                            onUnlocked = { isDecoy -> securityManager.unlock(isDecoy) },
                            onPanicTriggered = {
                                viewModel.triggerPanicWipe {
                                    Toast.makeText(this@MainActivity, "Vault reset complete", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                } else {
                    MainVaultScaffold(
                        viewModel = viewModel,
                        app = app,
                        onLockNow = {
                            showDirectPinScreen = false
                            securityManager.lock()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type ?: return

        if (Intent.ACTION_SEND == action) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                pendingShareUris = listOf(uri)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                pendingShareUris = uris
            }
        }
    }
}

@Composable
fun MainVaultScaffold(
    viewModel: VaultViewModel,
    app: MediaVaultApplication,
    onLockNow: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedItemIds.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var pendingExportItem by remember { mutableStateOf<MediaItem?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // SAF File Import Launcher (Multiple documents: audio, video, photo)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // SAF File Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri: Uri? ->
        if (destUri != null && pendingExportItem != null) {
            val item = pendingExportItem!!
            viewModel.exportMedia(item, destUri) { success ->
                val msg = if (success) "Exported ${item.fileName} successfully" else "Export failed"
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
            }
            pendingExportItem = null
        }
    }

    // Import Progress Modal
    if (importProgress.isImporting) {
        ImportProgressDialog(
            currentIndex = importProgress.currentFileIndex,
            totalFiles = importProgress.totalFiles,
            bytesCopied = importProgress.bytesCopied,
            totalBytes = importProgress.totalBytes
        )
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onSelectPlaylist = { plId ->
                viewModel.addSelectedToPlaylist(plId)
                showAddToPlaylistDialog = false
            },
            onCreateNew = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedTab != VaultTab.SETTINGS) {
                VaultTopBar(
                    title = when (selectedTab) {
                        VaultTab.AUDIO -> "Audio Vault"
                        VaultTab.VIDEO -> "Video Vault"
                        VaultTab.PHOTOS -> "Photo Vault"
                        else -> "MediaVault"
                    },
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedIds.size,
                    isSearching = isSearching,
                    searchQuery = searchQuery,
                    viewMode = viewMode,
                    sortOrder = sortOrder,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleSearch = { viewModel.toggleSearching(it) },
                    onToggleViewMode = { viewModel.toggleViewMode() },
                    onSelectSortOrder = { viewModel.setSortOrder(it) },
                    onSelectAll = {
                        val currentList = when (selectedTab) {
                            VaultTab.AUDIO -> viewModel.filteredAudio.value
                            VaultTab.VIDEO -> viewModel.filteredVideos.value
                            VaultTab.PHOTOS -> viewModel.filteredPhotos.value
                            else -> emptyList()
                        }
                        viewModel.selectAll(currentList)
                    },
                    onClearSelection = { viewModel.clearSelection() },
                    onBatchFavorite = {
                        val currentList = when (selectedTab) {
                            VaultTab.AUDIO -> viewModel.filteredAudio.value
                            VaultTab.VIDEO -> viewModel.filteredVideos.value
                            VaultTab.PHOTOS -> viewModel.filteredPhotos.value
                            else -> emptyList()
                        }
                        viewModel.batchFavoriteSelected(currentList, true)
                    },
                    onBatchDelete = {
                        val currentList = when (selectedTab) {
                            VaultTab.AUDIO -> viewModel.filteredAudio.value
                            VaultTab.VIDEO -> viewModel.filteredVideos.value
                            VaultTab.PHOTOS -> viewModel.filteredPhotos.value
                            else -> emptyList()
                        }
                        viewModel.batchDeleteSelected(currentList)
                    },
                    onBatchAddToPlaylist = { showAddToPlaylistDialog = true }
                )
            }
        },
        bottomBar = {
            VaultBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (isSelectionMode) viewModel.clearSelection()
                    if (isSearching) viewModel.toggleSearching(false)
                    viewModel.selectTab(tab)
                }
            )
        },
        floatingActionButton = {
            if (selectedTab != VaultTab.SETTINGS && !isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        val mimeTypes = when (selectedTab) {
                            VaultTab.AUDIO -> arrayOf("audio/*")
                            VaultTab.VIDEO -> arrayOf("video/*")
                            VaultTab.PHOTOS -> arrayOf("image/*")
                            else -> arrayOf("*/*")
                        }
                        importLauncher.launch(mimeTypes)
                    },
                    containerColor = IndigoPrimary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.testTag("import_media_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Import Files")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                VaultTab.AUDIO -> AudioScreen(
                    viewModel = viewModel,
                    playerManager = app.playerManager,
                    onExportItem = { item ->
                        pendingExportItem = item
                        exportLauncher.launch(item.fileName)
                    }
                )

                VaultTab.VIDEO -> VideoScreen(
                    viewModel = viewModel,
                    onExportItem = { item ->
                        pendingExportItem = item
                        exportLauncher.launch(item.fileName)
                    }
                )

                VaultTab.PHOTOS -> PhotoScreen(
                    viewModel = viewModel,
                    onExportItem = { item ->
                        pendingExportItem = item
                        exportLauncher.launch(item.fileName)
                    }
                )

                VaultTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    securityManager = app.securityManager,
                    onLockNow = onLockNow
                )
            }
        }
    }
}
