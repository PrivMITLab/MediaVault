package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaType
import com.example.data.storage.VaultStorageManager
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches MediaVault`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MediaVault", appName)
    }

    @Test
    fun `vault storage creates hidden nomedia file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageManager = VaultStorageManager(context)
        val vaultDir = storageManager.vaultDir

        assertTrue(vaultDir.exists())
        assertTrue(vaultDir.isDirectory)

        val noMedia = File(vaultDir, ".nomedia")
        assertTrue(noMedia.exists())
    }

    @Test
    fun `security manager hashes and verifies pin properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        securityManager.setPin("1234")
        assertTrue(securityManager.isPinSet())
        assertTrue(securityManager.verifyPin("1234"))
        assertFalse(securityManager.verifyPin("9999"))
        assertFalse(securityManager.verifyPin("123"))
    }

    @Test
    fun `media type detection works correctly for extensions`() {
        assertEquals(MediaType.AUDIO, VaultStorageManager.determineMediaType("audio/mpeg", "song.mp3"))
        assertEquals(MediaType.AUDIO, VaultStorageManager.determineMediaType("audio/flac", "track.flac"))
        assertEquals(MediaType.VIDEO, VaultStorageManager.determineMediaType("video/mp4", "movie.mp4"))
        assertEquals(MediaType.VIDEO, VaultStorageManager.determineMediaType("video/x-matroska", "clip.mkv"))
        assertEquals(MediaType.PHOTO, VaultStorageManager.determineMediaType("image/jpeg", "secret.jpg"))
        assertEquals(MediaType.PHOTO, VaultStorageManager.determineMediaType("image/png", "photo.png"))
    }
}
