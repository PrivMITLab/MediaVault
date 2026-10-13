package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.Playlist
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultTab {
    AUDIO,
    VIDEO,
    PHOTOS,
    SETTINGS
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    DURATION_DESC,
    SIZE_DESC
}

enum class ViewMode {
    GRID,
    LIST
}

data class ImportProgressState(
    val isImporting: Boolean = false,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val bytesCopied: Long = 0L,
    val totalBytes: Long = 0L
)

class VaultViewModel(
    private val repository: VaultRepository,
    val securityManager: com.example.security.SecurityManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(VaultTab.AUDIO)
    val selectedTab: StateFlow<VaultTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _importProgress = MutableStateFlow(ImportProgressState())
    val importProgress: StateFlow<ImportProgressState> = _importProgress.asStateFlow()

    private val _inspectingItem = MutableStateFlow<MediaItem?>(null)
    val inspectingItem: StateFlow<MediaItem?> = _inspectingItem.asStateFlow()

    val isDecoyMode = securityManager.isDecoyMode

    // Raw flows from repository
    val rawAudio = repository.allAudio
    val rawVideos = repository.allVideos
    val rawPhotos = repository.allPhotos
    val rawFavorites = repository.allFavorites
    val recentlyPlayed = repository.recentlyPlayed.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val totalVaultSize = repository.totalSizeBytes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )
    val totalItemCount = repository.totalItemCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val playlists = repository.allPlaylists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Filtered & Sorted Audio (empty in Decoy mode)
    val filteredAudio: StateFlow<List<MediaItem>> = combine(
        rawAudio, _searchQuery, _sortOrder, isDecoyMode
    ) { list, query, sort, decoy ->
        if (decoy) emptyList() else filterAndSort(list, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Video (empty in Decoy mode)
    val filteredVideos: StateFlow<List<MediaItem>> = combine(
        rawVideos, _searchQuery, _sortOrder, isDecoyMode
    ) { list, query, sort, decoy ->
        if (decoy) emptyList() else filterAndSort(list, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Photos (empty in Decoy mode)
    val filteredPhotos: StateFlow<List<MediaItem>> = combine(
        rawPhotos, _searchQuery, _sortOrder, isDecoyMode
    ) { list, query, sort, decoy ->
        if (decoy) emptyList() else filterAndSort(list, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filterAndSort(
        items: List<MediaItem>,
        query: String,
        sort: SortOrder
    ): List<MediaItem> {
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter {
                it.fileName.contains(query, ignoreCase = true) ||
                (it.title?.contains(query, ignoreCase = true) == true) ||
                (it.artist?.contains(query, ignoreCase = true) == true) ||
                (it.album?.contains(query, ignoreCase = true) == true)
            }
        }

        return when (sort) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.dateAdded }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.displayTitle.lowercase() }
            SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }

    fun selectTab(tab: VaultTab) {
        _selectedTab.value = tab
        // Set appropriate default view mode
        _viewMode.value = if (tab == VaultTab.PHOTOS || tab == VaultTab.VIDEO) ViewMode.GRID else ViewMode.LIST
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(enable: Boolean) {
        _isSearching.value = enable
        if (!enable) _searchQuery.value = ""
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun toggleSelection(id: Long) {
        val current = _selectedItemIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedItemIds.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun startSelectionWith(id: Long) {
        _selectedItemIds.value = setOf(id)
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedItemIds.value = items.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun inspectItem(item: MediaItem?) {
        _inspectingItem.value = item
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun batchDeleteSelected(allItems: List<MediaItem>) {
        val selectedIds = _selectedItemIds.value
        val itemsToDelete = allItems.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            repository.batchDelete(itemsToDelete)
            clearSelection()
        }
    }

    fun batchFavoriteSelected(allItems: List<MediaItem>, isFav: Boolean) {
        val selectedIds = _selectedItemIds.value.toList()
        viewModelScope.launch {
            repository.batchFavorite(selectedIds, isFav)
            clearSelection()
        }
    }

    fun importUris(
        uris: List<Uri>,
        targetType: MediaType? = null,
        onComplete: ((success: Int, failed: Int, deleteUris: List<Uri>) -> Unit)? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importProgress.value = ImportProgressState(
                isImporting = true,
                currentFileIndex = 0,
                totalFiles = uris.size,
                bytesCopied = 0L,
                totalBytes = 0L
            )

            var successCount = 0
            var failedCount = 0
            val deleteUris = mutableListOf<Uri>()

            uris.forEachIndexed { index, uri ->
                _importProgress.value = _importProgress.value.copy(currentFileIndex = index + 1)
                try {
                    val imported = repository.importFile(uri, targetType) { copied, total ->
                        _importProgress.value = _importProgress.value.copy(
                            bytesCopied = copied,
                            totalBytes = total
                        )
                    }
                    successCount++
                    if (!imported.result.deletedOriginal) {
                        val toDelete = imported.result.mediaStoreUri ?: imported.result.sourceUri
                        if (toDelete != null) {
                            deleteUris.add(toDelete)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VaultViewModel", "Failed to import uri: $uri", e)
                    failedCount++
                }
            }

            _importProgress.value = ImportProgressState(isImporting = false)
            onComplete?.invoke(successCount, failedCount, deleteUris)
        }
    }

    fun createDemoAudio(title: String = "Secret Voice Note", onComplete: ((MediaItem) -> Unit)? = null) {
        viewModelScope.launch {
            val item = repository.createDemoAudio(title)
            onComplete?.invoke(item)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        val selected = _selectedItemIds.value
        viewModelScope.launch {
            selected.forEach { itemId ->
                repository.addTrackToPlaylist(playlistId, itemId)
            }
            clearSelection()
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearVault()
        }
    }

    fun exportMedia(item: MediaItem, destinationUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.exportItem(item, destinationUri)
            onResult(success)
        }
    }

    fun triggerPanicWipe(onWiped: () -> Unit) {
        viewModelScope.launch {
            repository.clearVault()
            securityManager.lock()
            onWiped()
        }
    }

    fun toggleAesEncryption(enabled: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            securityManager.setAesEncryptionEnabled(enabled)
            onComplete()
        }
    }
}
