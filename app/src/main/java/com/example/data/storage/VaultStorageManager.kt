package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
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
        targetMediaTypeHint: MediaType? = null,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): ImportedFileResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var originalName = ""
        var reportedSize = 0L

        // Safely extract filename and size from content provider
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let { originalName = it }
                    }
                    if (sizeIndex != -1) {
                        reportedSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("VaultStorageManager", "Could not query URI metadata for $uri", e)
        }

        // If filename couldn't be resolved, try URI path segment or fallback timestamp
        if (originalName.isBlank()) {
            val lastSegment = uri.lastPathSegment?.substringAfterLast('/')
            originalName = if (!lastSegment.isNullOrBlank() && lastSegment.contains('.')) {
                lastSegment
            } else {
                "media_${System.currentTimeMillis()}"
            }
        }

        val detectedMime = runCatching { contentResolver.getType(uri) }.getOrNull()
            ?: getMimeTypeFromFilename(originalName)

        val mediaType = determineMediaType(detectedMime, originalName, targetMediaTypeHint)

        // Ensure file destination has a proper extension so ExoPlayer and Coil can decode it
        val sanitizedBase = originalName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val existingExt = if (originalName.contains('.')) {
            originalName.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }
        } else null

        val finalExt = existingExt ?: getDefaultExtensionForType(mediaType, detectedMime)
        val finalFileName = "$sanitizedBase.$finalExt"

        val uniquePrefix = UUID.randomUUID().toString().take(8)
        val destinationFile = File(vaultDir, "${uniquePrefix}_$finalFileName")

        var bytesCopied = 0L
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                val buffer = ByteArray(64 * 1024) // 64KB buffer
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    onProgress?.invoke(bytesCopied, reportedSize.coerceAtLeast(bytesCopied))
                }
                outputStream.flush()
            }
        } ?: throw IllegalStateException("Failed to open input stream for $uri")

        // Accurately verify actual media container and streams from written bytes
        var verifiedMediaType = mediaType
        var verifiedMime = detectedMime
        var verifiedExt = finalExt

        val retriever = android.media.MediaMetadataRetriever()
        var hasVideoTrack = false
        var hasAudioTrack = false
        try {
            retriever.setDataSource(destinationFile.absolutePath)
            hasVideoTrack = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
            hasAudioTrack = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
            val extractedMime = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!extractedMime.isNullOrBlank()) {
                verifiedMime = extractedMime
            }
        } catch (_: Exception) {
            // Non-media or plain image files
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (hasVideoTrack) {
            verifiedMediaType = MediaType.VIDEO
            if (!verifiedMime.startsWith("video/")) verifiedMime = "video/mp4"
            verifiedExt = "mp4"
        } else if (hasAudioTrack || targetMediaTypeHint == MediaType.AUDIO) {
            verifiedMediaType = MediaType.AUDIO
            if (!verifiedMime.startsWith("audio/")) verifiedMime = "audio/mpeg"
            if (verifiedExt == "jpg" || verifiedExt == "jpeg" || verifiedExt == "png" || verifiedExt == "bin") {
                verifiedExt = "mp3"
            }
        } else if (targetMediaTypeHint == MediaType.VIDEO) {
            verifiedMediaType = MediaType.VIDEO
            if (!verifiedMime.startsWith("video/")) verifiedMime = "video/mp4"
            if (verifiedExt == "jpg" || verifiedExt == "jpeg" || verifiedExt == "png") {
                verifiedExt = "mp4"
            }
        }

        // Ensure extension on disk aligns with verified media type so ExoPlayer and Coil decode properly
        var finalDestinationFile = destinationFile
        var resolvedFileName = finalFileName
        if (!finalDestinationFile.name.endsWith(".$verifiedExt", ignoreCase = true)) {
            val baseWithoutExt = finalDestinationFile.name.substringBeforeLast('.')
            val newFile = File(vaultDir, "$baseWithoutExt.$verifiedExt")
            if (destinationFile.renameTo(newFile)) {
                finalDestinationFile = newFile
                resolvedFileName = "${sanitizedBase}.$verifiedExt"
            }
        }

        // Check if URI is already a MediaStore URI or query underlying MediaStore item
        val isDirectMediaStoreUri = uri.toString().startsWith("content://media/external/")
        val mediaStoreUri = if (isDirectMediaStoreUri) {
            uri
        } else {
            DeviceMediaScanner.findUnderlyingMediaStoreUri(
                context = context,
                originalName = originalName,
                sizeBytes = reportedSize.coerceAtLeast(bytesCopied),
                mediaType = verifiedMediaType
            )
        }

        // Attempt to remove original file from public storage / media store (direct delete attempt)
        var deletedOriginal = false
        try {
            deletedOriginal = DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (_: Exception) {}

        if (!deletedOriginal) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                deletedOriginal = rows > 0
            } catch (_: Exception) {}
        }

        if (!deletedOriginal && mediaStoreUri != null && mediaStoreUri != uri) {
            try {
                val rows = contentResolver.delete(mediaStoreUri, null, null)
                deletedOriginal = rows > 0
            } catch (_: Exception) {}
        }

        ImportedFileResult(
            file = finalDestinationFile,
            originalName = resolvedFileName,
            mimeType = verifiedMime,
            mediaType = verifiedMediaType,
            sizeBytes = finalDestinationFile.length(),
            deletedOriginal = deletedOriginal,
            sourceUri = uri,
            mediaStoreUri = mediaStoreUri
        )
    }

    suspend fun createDemoAudio(title: String = "Secret Voice Note"): ImportedFileResult = withContext(Dispatchers.IO) {
        val file = DeviceMediaScanner.generateDemoAudioFile(vaultDir, title)
        ImportedFileResult(
            file = file,
            originalName = file.name,
            mimeType = "audio/wav",
            mediaType = MediaType.AUDIO,
            sizeBytes = file.length(),
            deletedOriginal = true,
            sourceUri = Uri.fromFile(file),
            mediaStoreUri = null
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
        fun determineMediaType(mimeType: String, fileName: String, hint: MediaType? = null): MediaType {
            val lowerMime = mimeType.lowercase()
            val lowerName = fileName.lowercase()

            val isExplicitAudio = lowerMime.startsWith("audio/") ||
                    lowerName.endsWith(".mp3") || lowerName.endsWith(".aac") ||
                    lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") ||
                    lowerName.endsWith(".flac") || lowerName.endsWith(".m4a") ||
                    lowerName.endsWith(".wma") || lowerName.endsWith(".opus") ||
                    lowerName.endsWith(".amr") || lowerName.endsWith(".mid") ||
                    lowerName.endsWith(".midi") || lowerName.endsWith(".aiff") ||
                    lowerName.endsWith(".m4b") || lowerName.endsWith(".mpga")

            val isExplicitVideo = lowerMime.startsWith("video/") ||
                    lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                    lowerName.endsWith(".avi") || lowerName.endsWith(".mov") ||
                    lowerName.endsWith(".webm") || lowerName.endsWith(".3gp") ||
                    lowerName.endsWith(".m4v") || lowerName.endsWith(".ts") ||
                    lowerName.endsWith(".flv") || lowerName.endsWith(".wmv")

            val isExplicitPhoto = lowerMime.startsWith("image/") ||
                    lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".webp") || lowerName.endsWith(".bmp") ||
                    lowerName.endsWith(".heic") || lowerName.endsWith(".heif") ||
                    lowerName.endsWith(".svg")

            // Hint from current screen tab takes precedence unless completely conflicting
            if (hint != null) {
                if (hint == MediaType.AUDIO && !isExplicitPhoto) return MediaType.AUDIO
                if (hint == MediaType.VIDEO && !isExplicitPhoto && !isExplicitAudio) return MediaType.VIDEO
                if (hint == MediaType.PHOTO && !isExplicitAudio && !isExplicitVideo) return MediaType.PHOTO
            }

            return when {
                isExplicitAudio -> MediaType.AUDIO
                isExplicitVideo -> MediaType.VIDEO
                isExplicitPhoto -> MediaType.PHOTO
                hint != null -> hint
                else -> MediaType.PHOTO
            }
        }

        fun getDefaultExtensionForType(mediaType: MediaType, mimeType: String): String {
            val fromMime = getExtensionFromMime(mimeType)
            if (fromMime.isNotBlank()) return fromMime
            return when (mediaType) {
                MediaType.AUDIO -> "mp3"
                MediaType.VIDEO -> "mp4"
                MediaType.PHOTO -> "jpg"
            }
        }

        fun getExtensionFromMime(mimeType: String): String {
            return when (mimeType.lowercase()) {
                "audio/mpeg", "audio/mp3" -> "mp3"
                "audio/aac" -> "aac"
                "audio/wav", "audio/x-wav" -> "wav"
                "audio/ogg", "application/ogg" -> "ogg"
                "audio/flac" -> "flac"
                "audio/mp4", "audio/m4a" -> "m4a"
                "audio/opus" -> "opus"
                "video/mp4" -> "mp4"
                "video/x-matroska" -> "mkv"
                "video/x-msvideo" -> "avi"
                "video/quicktime" -> "mov"
                "video/webm" -> "webm"
                "video/3gpp" -> "3gp"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "image/bmp" -> "bmp"
                "image/heic" -> "heic"
                else -> ""
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
                "opus" -> "audio/opus"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "avi" -> "video/x-msvideo"
                "mov" -> "video/quicktime"
                "webm" -> "video/webm"
                "3gp" -> "video/3gpp"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "heic" -> "image/heic"
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
    val deletedOriginal: Boolean,
    val sourceUri: Uri? = null,
    val mediaStoreUri: Uri? = null
)
