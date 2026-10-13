package com.example.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.db.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.data.storage.ImportedFileResult
import com.example.data.storage.VaultStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VaultRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val storageManager: VaultStorageManager
) {

    val allAudio: Flow<List<MediaItem>> = mediaDao.getItemsByType(MediaType.AUDIO)
    val allVideos: Flow<List<MediaItem>> = mediaDao.getItemsByType(MediaType.VIDEO)
    val allPhotos: Flow<List<MediaItem>> = mediaDao.getItemsByType(MediaType.PHOTO)
    val allFavorites: Flow<List<MediaItem>> = mediaDao.getFavoriteItems()
    val recentlyPlayed: Flow<List<MediaItem>> = mediaDao.getRecentlyPlayed(10)
    val totalSizeBytes: Flow<Long?> = mediaDao.getTotalSizeBytes()
    val totalItemCount: Flow<Int> = mediaDao.getItemCount()
    val allPlaylists: Flow<List<Playlist>> = mediaDao.getAllPlaylists()

    fun search(query: String): Flow<List<MediaItem>> = mediaDao.searchItems(query)

    fun getFavoritesByType(type: MediaType): Flow<List<MediaItem>> =
        mediaDao.getFavoriteItemsByType(type)

    fun getPlaylistTracks(playlistId: Long): Flow<List<MediaItem>> =
        mediaDao.getPlaylistTracks(playlistId)

    suspend fun getItemById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        mediaDao.getItemById(id)
    }

    suspend fun importFile(
        uri: Uri,
        targetMediaTypeHint: MediaType? = null,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): MediaItem = withContext(Dispatchers.IO) {
        val result = storageManager.importFile(uri, targetMediaTypeHint, onProgress)
        val extractedMetadata = extractMetadata(result)
        val entity = MediaItem(
            filePath = result.file.absolutePath,
            fileName = result.originalName,
            mediaType = result.mediaType,
            mimeType = result.mimeType,
            sizeBytes = result.sizeBytes,
            durationMs = extractedMetadata.durationMs,
            title = extractedMetadata.title,
            artist = extractedMetadata.artist,
            album = extractedMetadata.album,
            width = extractedMetadata.width,
            height = extractedMetadata.height,
            dateAdded = System.currentTimeMillis()
        )
        val id = mediaDao.insertItem(entity)
        entity.copy(id = id)
    }

    private fun extractMetadata(imported: ImportedFileResult): ExtractedMetadata {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L
        var width = 0
        var height = 0

        when (imported.mediaType) {
            MediaType.AUDIO, MediaType.VIDEO -> {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(imported.file.absolutePath)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durStr?.toLongOrNull()?.let { durationMs = it }

                    if (imported.mediaType == MediaType.VIDEO) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?.let { width = it }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?.let { height = it }
                    }
                } catch (_: Exception) {
                    // Fallback to filename
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
            MediaType.PHOTO -> {
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(imported.file.absolutePath, options)
                    width = options.outWidth
                    height = options.outHeight
                } catch (_: Exception) {}
            }
        }

        return ExtractedMetadata(
            title = title ?: imported.originalName.substringBeforeLast('.'),
            artist = artist,
            album = album,
            durationMs = durationMs,
            width = width,
            height = height
        )
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    suspend fun batchFavorite(ids: List<Long>, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateFavorites(ids, isFavorite)
    }

    suspend fun deleteItem(item: MediaItem) = withContext(Dispatchers.IO) {
        storageManager.deleteFile(item.filePath)
        mediaDao.deleteItemById(item.id)
    }

    suspend fun batchDelete(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            storageManager.deleteFile(item.filePath)
        }
        mediaDao.deleteItemsByIds(items.map { it.id })
    }

    suspend fun exportItem(item: MediaItem, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        storageManager.exportFile(File(item.filePath), targetUri)
    }

    suspend fun clearVault() = withContext(Dispatchers.IO) {
        storageManager.clearVault()
        mediaDao.deleteAllItems()
    }

    suspend fun recordPlay(id: Long) = withContext(Dispatchers.IO) {
        mediaDao.updatePlayStats(id)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        mediaDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        mediaDao.clearPlaylistItems(playlistId)
        mediaDao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        mediaDao.renamePlaylist(playlistId, newName)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, mediaItemId: Long) = withContext(Dispatchers.IO) {
        mediaDao.addTrackToPlaylist(PlaylistItem(playlistId = playlistId, mediaItemId = mediaItemId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaItemId: Long) = withContext(Dispatchers.IO) {
        mediaDao.removeTrackFromPlaylist(playlistId, mediaItemId)
    }

    private data class ExtractedMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val width: Int,
        val height: Int
    )
}
