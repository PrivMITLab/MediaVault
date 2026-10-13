package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaItemId"],
    indices = [Index("mediaItemId")]
)
data class PlaylistItem(
    val playlistId: Long,
    val mediaItemId: Long,
    val orderIndex: Int = 0
)
