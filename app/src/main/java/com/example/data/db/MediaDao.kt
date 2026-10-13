package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateAdded DESC")
    fun getItemsByType(type: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 AND mediaType = :type ORDER BY dateAdded DESC")
    fun getFavoriteItemsByType(type: MediaType): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 10): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE fileName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    fun searchItems(query: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE filePath = :path LIMIT 1")
    suspend fun getItemByPath(path: String): MediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MediaItem>): List<Long>

    @Update
    suspend fun updateItem(item: MediaItem)

    @Delete
    suspend fun deleteItem(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>)

    @Query("DELETE FROM media_items")
    suspend fun deleteAllItems()

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun updateFavorites(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE media_items SET lastPlayedTimestamp = :timestamp, playCount = playCount + 1 WHERE id = :id")
    suspend fun updatePlayStats(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT SUM(sizeBytes) FROM media_items")
    fun getTotalSizeBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM media_items")
    fun getItemCount(): Flow<Int>

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaItemId = :mediaItemId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaItemId: Long)

    @Query("SELECT m.* FROM media_items m INNER JOIN playlist_items p ON m.id = p.mediaItemId WHERE p.playlistId = :playlistId ORDER BY p.orderIndex ASC")
    fun getPlaylistTracks(playlistId: Long): Flow<List<MediaItem>>
}
