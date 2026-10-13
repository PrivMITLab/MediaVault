package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityManager
import com.example.ui.components.formatBytes
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    securityManager: SecurityManager,
    onLockNow: () -> Unit
) {
    val totalSize by viewModel.totalVaultSize.collectAsState()
    val audioItems by viewModel.rawAudio.collectAsState(emptyList())
    val videoItems by viewModel.rawVideos.collectAsState(emptyList())
    val photoItems by viewModel.rawPhotos.collectAsState(emptyList())

    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var disguiseCalculatorEnabled by remember { mutableStateOf(securityManager.isDisguiseCalculatorEnabled()) }
    var aesEncryptionEnabled by remember { mutableStateOf(securityManager.isAesEncryptionEnabled()) }
    var screenProtectionEnabled by remember { mutableStateOf(securityManager.isScreenProtectionEnabled()) }
    var autoLockSeconds by remember { mutableIntStateOf(securityManager.getAutoLockTimeoutSeconds()) }
    var autoLockMenuExpanded by remember { mutableStateOf(false) }

    var isDecoySet by remember { mutableStateOf(securityManager.isDecoyPinSet()) }
    var isPanicSet by remember { mutableStateOf(securityManager.isPanicPinSet()) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showPanicPinDialog by remember { mutableStateOf(false) }
    var showClearVaultDialog by remember { mutableStateOf(false) }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Master PIN", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Current PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("current_pin_input")
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("New PIN (4-6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_input")
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = RoseError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!securityManager.verifyPin(oldPin)) {
                            errorMsg = "Current PIN is incorrect"
                        } else if (newPin.length < 4 || newPin.length > 6) {
                            errorMsg = "PIN must be 4 to 6 digits"
                        } else if (newPin != confirmPin) {
                            errorMsg = "New PINs do not match"
                        } else {
                            securityManager.setPin(newPin)
                            showChangePinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Decoy PIN Dialog
    if (showDecoyPinDialog) {
        var decoyPinInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDecoyPinDialog = false },
            title = { Text("Configure Decoy PIN", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Entering this decoy PIN opens a fake empty vault with 0 files, keeping your real vault completely secret under duress.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = decoyPinInput,
                        onValueChange = { decoyPinInput = it },
                        label = { Text("Decoy PIN (4-6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = RoseError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDecoySet) {
                        TextButton(
                            onClick = {
                                securityManager.setDecoyPin(null)
                                isDecoySet = false
                                showDecoyPinDialog = false
                            }
                        ) {
                            Text("Remove", color = RoseError)
                        }
                    }
                    Button(
                        onClick = {
                            if (decoyPinInput.length < 4 || decoyPinInput.length > 6) {
                                errorMsg = "Decoy PIN must be 4 to 6 digits"
                            } else {
                                securityManager.setDecoyPin(decoyPinInput)
                                isDecoySet = true
                                showDecoyPinDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Save Decoy PIN")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Panic PIN Dialog
    if (showPanicPinDialog) {
        var panicPinInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPanicPinDialog = false },
            title = { Text("Configure Panic PIN (Emergency Wipe)", color = RoseError, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Entering this emergency PIN will SILENTLY WIPE all private vault files and database records immediately. Use this to sanitize the vault in an emergency.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = panicPinInput,
                        onValueChange = { panicPinInput = it },
                        label = { Text("Panic PIN (4-6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = RoseError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPanicSet) {
                        TextButton(
                            onClick = {
                                securityManager.setPanicPin(null)
                                isPanicSet = false
                                showPanicPinDialog = false
                            }
                        ) {
                            Text("Remove", color = RoseError)
                        }
                    }
                    Button(
                        onClick = {
                            if (panicPinInput.length < 4 || panicPinInput.length > 6) {
                                errorMsg = "Panic PIN must be 4 to 6 digits"
                            } else {
                                securityManager.setPanicPin(panicPinInput)
                                isPanicSet = true
                                showPanicPinDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("Save Panic PIN")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear Vault Confirmation Dialog
    if (showClearVaultDialog) {
        AlertDialog(
            onDismissRequest = { showClearVaultDialog = false },
            title = { Text("Clear Entire Vault?", color = RoseError, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently delete all stored songs, videos, and photos from private vault storage. This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearVault()
                        showClearVaultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Permanently Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearVaultDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Storage Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(IndigoPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Vault Storage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                    Text(
                        text = formatBytes(totalSize ?: 0L),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = IndigoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StorageStatPill("Audio", "${audioItems.size}", IndigoPrimary)
                    StorageStatPill("Video", "${videoItems.size}", EmeraldAccent)
                    StorageStatPill("Photos", "${photoItems.size}", Color(0xFF38BDF8))
                }
            }
        }

        // Stealth & Disguise Section
        Text(
            text = "STEALTH & DISGUISE",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Calculator Disguise Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = IndigoPrimary)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Calculator Disguise", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Opens as a real calculator. Unlock with PIN + '='", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = disguiseCalculatorEnabled,
                        onCheckedChange = {
                            disguiseCalculatorEnabled = it
                            securityManager.setDisguiseCalculatorEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary
                        )
                    )
                }

                // Decoy PIN Setup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDecoyPinDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, tint = EmeraldAccent)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Decoy PIN (Duress Mode)", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(if (isDecoySet) "Active (Opens empty safe vault)" else "Not set", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }

                // Panic PIN Setup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPanicPinDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, tint = RoseError)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Panic PIN (Silent Wipe)", color = RoseError, fontWeight = FontWeight.Medium)
                            Text(if (isPanicSet) "Active (Triggers instant vault wipe)" else "Not set", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Encryption & Cryptography Section
        Text(
            text = "HARDWARE CRYPTOGRAPHY",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.EnhancedEncryption, contentDescription = null, tint = IndigoPrimary)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("AES-256 GCM KeyStore", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Hardware-backed master key encryption", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = aesEncryptionEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleAesEncryption(enabled) {
                                aesEncryptionEnabled = enabled
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary
                        )
                    )
                }
            }
        }

        // Security Settings Section
        Text(
            text = "SECURITY & ACCESS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Change PIN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChangePinDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Pin, contentDescription = null, tint = IndigoPrimary)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Change Master PIN", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Update 4-6 digit security PIN", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }

                // Biometrics Toggle
                if (securityManager.canAuthenticateWithBiometrics()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = IndigoPrimary)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Biometric Unlock", color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("Fingerprint & Face Recognition", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                securityManager.setBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoPrimary
                            )
                        )
                    }
                }

                // Screen Protection (FLAG_SECURE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = IndigoPrimary)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Screen Protection (FLAG_SECURE)", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Block screenshots & task previews on phone", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = screenProtectionEnabled,
                        onCheckedChange = {
                            screenProtectionEnabled = it
                            securityManager.setScreenProtectionEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary
                        )
                    )
                }

                // Auto-lock Timeout
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoLockMenuExpanded = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = IndigoPrimary)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Auto-Lock Timeout", color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(
                                    when (autoLockSeconds) {
                                        0 -> "Immediate on exit"
                                        30 -> "After 30 seconds"
                                        60 -> "After 1 minute"
                                        else -> "After 5 minutes"
                                    },
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = autoLockMenuExpanded,
                        onDismissRequest = { autoLockMenuExpanded = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        listOf(
                            Pair("Immediate on exit", 0),
                            Pair("30 Seconds", 30),
                            Pair("1 Minute", 60),
                            Pair("5 Minutes", 300)
                        ).forEach { (label, sec) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (autoLockSeconds == sec) IndigoPrimary else TextPrimary) },
                                onClick = {
                                    autoLockSeconds = sec
                                    securityManager.setAutoLockTimeoutSeconds(sec)
                                    autoLockMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Vault Controls Section
        Text(
            text = "VAULT ACTIONS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Lock Vault Now
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLockNow() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = EmeraldAccent)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Lock Vault Now", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Instantly seal vault and return to lock screen", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }

                // Clear Vault
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearVaultDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = RoseError)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Clear Entire Vault", color = RoseError, fontWeight = FontWeight.Medium)
                            Text("Erase all private media and databases", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Offline Integrity Note
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF10141E),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "100% Offline & Isolated",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "FLAG_SECURE active. No internet permission requested. All files reside strictly in internal sandbox storage with .nomedia flags.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageStatPill(label: String, count: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}
