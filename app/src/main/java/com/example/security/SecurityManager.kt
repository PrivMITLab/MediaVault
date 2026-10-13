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

    // Biometrics
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
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
        title: String = "MediaVault Unlock",
        subtitle: String = "Verify identity to access private vault",
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
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric authentication failed")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
