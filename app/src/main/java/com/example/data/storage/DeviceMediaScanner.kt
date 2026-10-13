package com.example.data.storage

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

data class DeviceAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val uri: Uri,
    val fileName: String
)

object DeviceMediaScanner {

    suspend fun queryDeviceAudio(context: Context, searchQuery: String = ""): List<DeviceAudioTrack> =
        withContext(Dispatchers.IO) {
            val audioList = mutableListOf<DeviceAudioTrack>()
            val contentResolver = context.contentResolver
            val collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DISPLAY_NAME
            )

            val selection = if (searchQuery.isNotBlank()) {
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
            } else null

            val selectionArgs = if (searchQuery.isNotBlank()) {
                val wild = "%$searchQuery%"
                arrayOf(wild, wild, wild)
            } else null

            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            try {
                contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Track"
                        val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Device Audio"
                        val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Music"
                        val duration = cursor.getLong(durCol)
                        val size = cursor.getLong(sizeCol)
                        val fileName = if (nameCol != -1) cursor.getString(nameCol) ?: "$title.mp3" else "$title.mp3"
                        val contentUri = ContentUris.withAppendedId(collectionUri, id)

                        audioList.add(
                            DeviceAudioTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                sizeBytes = size,
                                uri = contentUri,
                                fileName = fileName
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DeviceMediaScanner", "Error querying device audio: ${e.message}")
            }

            audioList
        }

    fun findUnderlyingMediaStoreUri(
        context: Context,
        originalName: String,
        sizeBytes: Long,
        mediaType: MediaType
    ): Uri? {
        val contentResolver = context.contentResolver
        val collectionUri = when (mediaType) {
            MediaType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = if (sizeBytes > 0) {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
            } else {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            }
            val selectionArgs = if (sizeBytes > 0) {
                arrayOf(originalName, sizeBytes.toString())
            } else {
                arrayOf(originalName)
            }

            contentResolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(collectionUri, id)
                }
            }
        } catch (_: Exception) {}

        // Fallback: match by display name only
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            contentResolver.query(
                collectionUri,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(originalName),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(collectionUri, id)
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Generates a 5-second clean 44.1kHz WAV melody tone directly in vaultDir.
     * Perfect for instant testing on emulators and devices with no audio tracks.
     */
    fun generateDemoAudioFile(vaultDir: File, title: String): File {
        val uniquePrefix = UUID.randomUUID().toString().take(8)
        val file = File(vaultDir, "${uniquePrefix}_${title.replace(" ", "_")}.wav")
        val sampleRate = 44100
        val durationSeconds = 5
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples * 2 // 16-bit mono
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            // RIFF chunk
            fos.write("RIFF".toByteArray())
            fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalSize).array())
            fos.write("WAVE".toByteArray())

            // fmt chunk
            fos.write("fmt ".toByteArray())
            fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
            fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // PCM
            fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // 1 channel (mono)
            fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate * 2).array())
            fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array())
            fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array())

            // data chunk
            fos.write("data".toByteArray())
            fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize).array())

            // Harmonic chime chords: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz), A5 (880Hz)
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 880.00)
            val samplesPerNote = numSamples / notes.size
            val buffer = ByteArray(2)

            for (i in 0 until numSamples) {
                val noteIdx = (i / samplesPerNote).coerceIn(0, notes.lastIndex)
                val freq = notes[noteIdx]
                val t = i % samplesPerNote
                val decay = Math.exp(-3.0 * t / samplesPerNote)
                val wave = Math.sin(2.0 * Math.PI * freq * i / sampleRate) * decay
                val sampleVal = (wave * 20000.0).toInt().coerceIn(-32768, 32767).toShort()
                buffer[0] = (sampleVal.toInt() and 0xFF).toByte()
                buffer[1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
                fos.write(buffer)
            }
            fos.flush()
        }
        return file
    }
}
