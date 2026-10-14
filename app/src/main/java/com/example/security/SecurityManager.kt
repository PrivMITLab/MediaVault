package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

enum class UnlockResult {
    SUCCESS_MASTER,
    SUCCESS_DECOY,
    TRIGGER_PANIC,
    FAILED
}

class SecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("media_vault_sec_prefs", Context.MODE_PRIVATE)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_hash"
        private const val KEY_DECOY_PIN_SALT = "decoy_pin_salt"
        private const val KEY_PANIC_PIN_HASH = "panic_pin_hash"
        private const val KEY_PANIC_PIN_SALT = "panic_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_sec"
        private const val KEY_DISGUISE_CALCULATOR = "disguise_calculator"
        private const val KEY_AES_ENCRYPTION = "aes_encryption"
        private const val DEFAULT_AUTO_LOCK_TIMEOUT = 0 // Immediate
    }

    fun isPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(pin: String) {
        val salt = System.currentTimeMillis().toString()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    fun isDecoyPinSet(): Boolean {
        return prefs.getString(KEY_DECOY_PIN_HASH, null) != null
    }

    fun setDecoyPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove(KEY_DECOY_PIN_HASH).remove(KEY_DECOY_PIN_SALT).apply()
        } else {
            val salt = System.currentTimeMillis().toString()
            val hash = hashPin(pin, salt)
            prefs.edit()
                .putString(KEY_DECOY_PIN_HASH, hash)
                .putString(KEY_DECOY_PIN_SALT, salt)
                .apply()
        }
    }

    fun isPanicPinSet(): Boolean {
        return prefs.getString(KEY_PANIC_PIN_HASH, null) != null
    }

    fun setPanicPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove(KEY_PANIC_PIN_HASH).remove(KEY_PANIC_PIN_SALT).apply()
        } else {
            val salt = System.currentTimeMillis().toString()
            val hash = hashPin(pin, salt)
            prefs.edit()
                .putString(KEY_PANIC_PIN_HASH, hash)
                .putString(KEY_PANIC_PIN_SALT, salt)
                .apply()
        }
    }

    fun verifyInput(input: String): UnlockResult {
        // 1. Check Panic PIN (Highest priority)
        val panicHash = prefs.getString(KEY_PANIC_PIN_HASH, null)
        val panicSalt = prefs.getString(KEY_PANIC_PIN_SALT, "") ?: ""
        if (panicHash != null && panicHash == hashPin(input, panicSalt)) {
            return UnlockResult.TRIGGER_PANIC
        }

        // 2. Check Decoy PIN
        val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null)
        val decoySalt = prefs.getString(KEY_DECOY_PIN_SALT, "") ?: ""
        if (decoyHash != null && decoyHash == hashPin(input, decoySalt)) {
            _isDecoyMode.value = true
            _isUnlocked.value = true
            return UnlockResult.SUCCESS_DECOY
        }

        // 3. Check Master PIN
        val masterHash = prefs.getString(KEY_PIN_HASH, null) ?: return UnlockResult.FAILED
        val masterSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        if (masterHash == hashPin(input, masterSalt)) {
            _isDecoyMode.value = false
            _isUnlocked.value = true
            return UnlockResult.SUCCESS_MASTER
        }

        return UnlockResult.FAILED
    }

    fun verifyPin(pin: String): Boolean {
        return verifyInput(pin) == UnlockResult.SUCCESS_MASTER
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$pin:$salt".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Calculator Disguise
    fun isDisguiseCalculatorEnabled(): Boolean {
        return prefs.getBoolean(KEY_DISGUISE_CALCULATOR, false)
    }

    fun setDisguiseCalculatorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISGUISE_CALCULATOR, enabled).apply()
    }

    // AES-256 Encryption
    fun isAesEncryptionEnabled(): Boolean {
        return prefs.getBoolean(KEY_AES_ENCRYPTION, false)
    }

    fun setAesEncryptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AES_ENCRYPTION, enabled).apply()
    }

    // Screen Protection (FLAG_SECURE)
    fun isScreenProtectionEnabled(): Boolean {
        return prefs.getBoolean("screen_protection_enabled", false)
    }

    fun setScreenProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("screen_protection_enabled", enabled).apply()
    }

    // Auto-Delete Originals from Phone Storage (Google Photos / Google Files Safe Folder style)
    fun isDeleteOriginalsEnabled(): Boolean {
        return prefs.getBoolean("delete_originals_after_import", true)
    }

    fun setDeleteOriginalsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("delete_originals_after_import", enabled).apply()
    }

    // Biometrics
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    enum class BiometricStatus {
        READY,
        NOT_ENROLLED,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED
    }

    fun getBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        return getBiometricStatus() == BiometricStatus.READY
    }

    fun canAuthenticateWithDeviceCredential(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val keyguardManager = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            keyguardManager?.isDeviceSecure == true
        }
    }

    fun canAuthenticateSystemLock(): Boolean {
        return canAuthenticateWithBiometrics() || canAuthenticateWithDeviceCredential()
    }

    fun isBiometricHardwareAvailable(): Boolean {
        val status = getBiometricStatus()
        return status == BiometricStatus.READY || status == BiometricStatus.NOT_ENROLLED
    }

    fun openBiometricEnrollment(activity: FragmentActivity) {
        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            try {
                activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
            } catch (_: Exception) {}
        }
    }

    // Auto-Lock
    fun getAutoLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT)
    }

    fun setAutoLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, seconds).apply()
    }

    fun unlock(isDecoy: Boolean = false) {
        _isDecoyMode.value = isDecoy
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
        _isDecoyMode.value = false
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
        val timeoutSec = getAutoLockTimeoutSeconds()
        if (timeoutSec == 0) {
            lock()
        }
    }

    fun onAppForegrounded() {
        val timeoutSec = getAutoLockTimeoutSeconds()
        if (timeoutSec > 0 && backgroundTimestamp > 0) {
            val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            if (elapsedSec >= timeoutSec) {
                lock()
            }
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "MediaVault Authentication",
        subtitle: String = "Verify fingerprint or screen lock to open vault",
        allowDeviceCredential: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isDecoyMode.value = false
                _isUnlocked.value = true
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Please try again.")
            }
        })

        val canUseDeviceCredential = allowDeviceCredential && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
        try {
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (canUseDeviceCredential) {
                // Google Photos Locked Folder style: allow both Biometric and Device Screen Lock PIN/Pattern
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                promptInfoBuilder.setNegativeButtonText("Use Vault PIN")
            }

            prompt.authenticate(promptInfoBuilder.build())
        } catch (_: Exception) {
            // Fallback to strong/weak with negative button
            try {
                val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Use Vault PIN")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    .build()
                prompt.authenticate(fallbackPromptInfo)
            } catch (fallbackEx: Exception) {
                onError(fallbackEx.localizedMessage ?: "Biometric prompt unavailable")
            }
        }
    }
}
