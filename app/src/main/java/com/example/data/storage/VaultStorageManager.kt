package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class VaultStorageManager(private val context: Context) {

    val vaultDir: File by lazy {
        File(context.filesDir, "vault").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
            // Ensure .nomedia file exists to hide from gallery and system scanners
            val noMediaFile = File(dir, ".nomedia")
            if (!noMediaFile.exists()) {
                runCatching { noMediaFile.createNewFile() }
            }
        }
    }

    init {
        // Guarantee vault initialization on creation
        vaultDir
    }

    suspend fun importFile(
        uri: Uri,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): ImportedFileResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var originalName = "media_${System.currentTimeMillis()}"
        var reportedSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    cursor.getString(nameIndex)?.let { originalName = it }
                }
                if (sizeIndex != -1) {
                    reportedSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFilename(originalName)
        val mediaType = determineMediaType(mimeType, originalName)

        // Sanitize filename and create unique storage destination
        val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val uniquePrefix = UUID.randomUUID().toString().take(8)
        val destinationFile = File(vaultDir, "${uniquePrefix}_$sanitizedName")

        var bytesCopied = 0L
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                val buffer = ByteArray(128 * 1024) // 128KB buffer for smooth multi-GB streaming
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    onProgress?.invoke(bytesCopied, reportedSize.coerceAtLeast(bytesCopied))
                }
                outputStream.flush()
            }
        } ?: throw IllegalStateException("Failed to open input stream for $uri")

        // Attempt to remove original file from public storage / media store
        var deletedOriginal = false
        try {
            deletedOriginal = DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (_: Exception) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                deletedOriginal = rows > 0
            } catch (_: Exception) {
                // If permission was read-only, keep copied file safely in vault
            }
        }

        ImportedFileResult(
            file = destinationFile,
            originalName = originalName,
            mimeType = mimeType,
            mediaType = mediaType,
            sizeBytes = destinationFile.length(),
            deletedOriginal = deletedOriginal
        )
    }

    suspend fun exportFile(sourceFile: File, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext false
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    val buffer = ByteArray(128 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    suspend fun clearVault(): Unit = withContext(Dispatchers.IO) {
        vaultDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                file.delete()
            }
        }
    }

    fun getVaultSizeBytes(): Long {
        var total = 0L
        vaultDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name != ".nomedia") {
                total += file.length()
            }
        }
        return total
    }

    companion object {
        fun determineMediaType(mimeType: String, fileName: String): MediaType {
            val lowerMime = mimeType.lowercase()
            val lowerName = fileName.lowercase()
            return when {
                lowerMime.startsWith("audio/") ||
                        lowerName.endsWith(".mp3") || lowerName.endsWith(".aac") ||
                        lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") ||
                        lowerName.endsWith(".flac") || lowerName.endsWith(".m4a") -> MediaType.AUDIO

                lowerMime.startsWith("video/") ||
                        lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                        lowerName.endsWith(".avi") || lowerName.endsWith(".mov") ||
                        lowerName.endsWith(".webm") -> MediaType.VIDEO

                else -> MediaType.PHOTO
            }
        }

        fun getMimeTypeFromFilename(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "mp3" -> "audio/mpeg"
                "aac" -> "audio/aac"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                "m4a" -> "audio/mp4"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "avi" -> "video/x-msvideo"
                "mov" -> "video/quicktime"
                "webm" -> "video/webm"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "*/*"
            }
        }
    }
}

data class ImportedFileResult(
    val file: File,
    val originalName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val deletedOriginal: Boolean
)
