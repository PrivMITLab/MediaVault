package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object VaultCipher {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "MediaVaultMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val BUFFER_SIZE = 64 * 1024

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    suspend fun encryptFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists()) return@withContext false
        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            FileOutputStream(destination).use { fos ->
                // Write 12-byte IV at beginning of file
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(source).use { fis ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            cos.write(buffer, 0, read)
                        }
                        cos.flush()
                    }
                }
            }
            true
        } catch (_: Exception) {
            if (destination.exists()) destination.delete()
            false
        }
    }

    suspend fun decryptFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        if (!source.exists()) return@withContext false
        try {
            val key = getOrCreateKey()
            FileInputStream(source).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val ivRead = fis.read(iv)
                if (ivRead != GCM_IV_LENGTH) return@withContext false

                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(destination).use { fos ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                        fos.flush()
                    }
                }
            }
            true
        } catch (_: Exception) {
            if (destination.exists()) destination.delete()
            false
        }
    }
}
