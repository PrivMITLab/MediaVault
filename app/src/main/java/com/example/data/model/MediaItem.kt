package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val playCount: Int = 0
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: fileName
}
