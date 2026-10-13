package com.example.ui.screens.lock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.SecurityManager
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    securityManager: SecurityManager,
    onUnlocked: (isDecoy: Boolean) -> Unit,
    onPanicTriggered: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPinAlreadySet = remember { securityManager.isPinSet() }

    var isSetupMode by remember { mutableStateOf(!isPinAlreadySet) }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter new PIN, 2: Confirm new PIN
    var initialEnteredPin by remember { mutableStateOf("") }

    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shakeOffset = remember { Animatable(0f) }

    var showBiometricSetupDialog by remember { mutableStateOf(false) }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.2f, stiffness = 800f)
            )
        }
    }

    fun triggerBiometricAuth() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            errorMessage = "Unable to start biometric authentication"
            return
        }
        val status = securityManager.getBiometricStatus()
        when (status) {
            com.example.security.SecurityManager.BiometricStatus.READY -> {
                securityManager.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { onUnlocked(false) },
                    onError = { errorMessage = it }
                )
            }
            com.example.security.SecurityManager.BiometricStatus.NOT_ENROLLED -> {
                errorMessage = "No fingerprint or face enrolled in device settings"
            }
            com.example.security.SecurityManager.BiometricStatus.NO_HARDWARE -> {
                errorMessage = "Biometrics not supported on this device"
            }
            com.example.security.SecurityManager.BiometricStatus.HARDWARE_UNAVAILABLE -> {
                errorMessage = "Biometric sensor is currently unavailable"
            }
            else -> {
                errorMessage = "Biometrics unavailable"
            }
        }
    }

    // Auto trigger biometric if enabled
    LaunchedEffect(Unit) {
        if (!isSetupMode && securityManager.isBiometricEnabled() && securityManager.canAuthenticateWithBiometrics()) {
            triggerBiometricAuth()
        }
    }

    fun handleDigit(digit: String) {
        if (currentInput.length >= 6) return
        val updated = currentInput + digit
        currentInput = updated
        errorMessage = null

        if (!isSetupMode) {
            // Check immediately on 4, 5, or 6 digits
            if (updated.length >= 4) {
                when (securityManager.verifyInput(updated)) {
                    com.example.security.UnlockResult.SUCCESS_MASTER -> onUnlocked(false)
                    com.example.security.UnlockResult.SUCCESS_DECOY -> onUnlocked(true)
                    com.example.security.UnlockResult.TRIGGER_PANIC -> onPanicTriggered()
                    com.example.security.UnlockResult.FAILED -> {
                        if (updated.length == 6) {
                            errorMessage = "Incorrect PIN"
                            currentInput = ""
                            triggerShake()
                        }
                    }
                }
            }
        }
    }

    fun handleConfirmSetup() {
        if (setupStep == 1) {
            if (currentInput.length < 4) {
                errorMessage = "PIN must be at least 4 digits"
                return
            }
            initialEnteredPin = currentInput
            currentInput = ""
            setupStep = 2
        } else {
            if (currentInput == initialEnteredPin) {
                securityManager.setPin(currentInput)
                if (securityManager.isBiometricHardwareAvailable()) {
                    showBiometricSetupDialog = true
                } else {
                    isSetupMode = false
                    securityManager.unlock(false)
                    onUnlocked(false)
                }
            } else {
                errorMessage = "PINs do not match. Try again."
                currentInput = ""
                setupStep = 1
                triggerShake()
            }
        }
    }

    fun handleDelete() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianBlack, Color(0xFF0F131D), Color(0xFF151A26))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            // Shield Emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(IndigoPrimary.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, IndigoPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when {
                    isSetupMode && setupStep == 1 -> "Create Vault PIN"
                    isSetupMode && setupStep == 2 -> "Confirm Your PIN"
                    else -> "MediaVault"
                },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when {
                    isSetupMode && setupStep == 1 -> "Enter 4 to 6 digits to secure your files"
                    isSetupMode && setupStep == 2 -> "Re-enter the same PIN to verify"
                    else -> "Offline & Invisible Media Vault"
                },
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                val maxDots = if (currentInput.length > 4) currentInput.length else 4
                (0 until maxDots).forEach { index ->
                    val isFilled = index < currentInput.length
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            errorMessage != null -> RoseError
                            isFilled -> IndigoPrimary
                            else -> Color(0xFF2B3347)
                        },
                        animationSpec = tween(150),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(dotColor, CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (isFilled) IndigoPrimary else Color(0xFF3B455D),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = RoseError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Biometric Quick Unlock affordance
            if (!isSetupMode && securityManager.isBiometricHardwareAvailable()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { triggerBiometricAuth() }
                        .background(IndigoPrimary.copy(alpha = 0.15f))
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("biometric_quick_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Touch to Unlock with Biometrics",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (errorMessage == null) {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Biometric Setup Dialog when first creating PIN
            if (showBiometricSetupDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showBiometricSetupDialog = false
                        isSetupMode = false
                        securityManager.unlock(false)
                        onUnlocked(false)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Enable Biometric Unlock?",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Use fingerprint or face recognition for instant, secure access to your media vault alongside your PIN.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                securityManager.setBiometricEnabled(true)
                                showBiometricSetupDialog = false
                                isSetupMode = false
                                securityManager.unlock(false)
                                onUnlocked(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("enable_biometric_confirm_button")
                        ) {
                            Text("Enable Biometrics")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                securityManager.setBiometricEnabled(false)
                                showBiometricSetupDialog = false
                                isSetupMode = false
                                securityManager.unlock(false)
                                onUnlocked(false)
                            }
                        ) {
                            Text("PIN Only", color = TextMuted)
                        }
                    },
                    containerColor = SurfaceCard,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            if (isSetupMode && currentInput.length >= 4) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { handleConfirmSetup() }
                        .testTag("confirm_pin_setup_button"),
                    color = IndigoPrimary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (setupStep == 1) "Next →" else "Save & Unlock",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "del")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypad.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "bio" -> {
                                    if (!isSetupMode && securityManager.isBiometricHardwareAvailable()) {
                                        KeypadButton(
                                            onClick = { triggerBiometricAuth() },
                                            content = {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometric Unlock",
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("keypad_biometric_button")
                                                )
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }

                                "del" -> {
                                    KeypadButton(
                                        onClick = { handleDelete() },
                                        content = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Delete",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    )
                                }

                                else -> {
                                    KeypadButton(
                                        onClick = { handleDigit(key) },
                                        content = {
                                            Text(
                                                text = key,
                                                color = TextPrimary,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = Color(0xFF161B26),
        shape = CircleShape,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
