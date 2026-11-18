package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ConnectionEventEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityLogEntity
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.SecurityState
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose

@Composable
fun SecurityScreen(
    securityState: SecurityState,
    connectedEarbud: ConnectedEarbudState?,
    registeredDevices: List<RegisteredDeviceEntity>,
    connectionEvents: List<ConnectionEventEntity>,
    securityLogs: List<SecurityLogEntity>,
    formatMac: (String) -> String,
    onConfigurePin: (String, String?) -> Unit,
    onDisablePin: () -> Unit,
    onLockAppNow: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAlertUnauthorized: (Boolean) -> Unit,
    onRegisterOwnerDevice: (String) -> Unit,
    onUnregisterOwnerDevice: (String) -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Honest Security Scope Banner
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StatusAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = StatusAmber)
                        Column {
                            Text("Hardware Pairing Scope", style = MaterialTheme.typography.titleMedium, color = StatusAmber)
                            Text(
                                text = stringResource(R.string.security_pairing_disclaimer),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Keystore PIN & Biometric Lock Controls
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = tokens.accent)
                            Column {
                                Text("Keystore Companion Vault", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (securityState.isPinConfigured) {
                                        "${securityState.pinLength}-Digit AES-GCM Salted PIN Active"
                                    } else {
                                        "No Companion PIN configured"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (securityState.isPinConfigured) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onHaptic(false)
                                showPinSetupDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("setup_pin_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (securityState.isPinConfigured) "Update PIN" else "Set 4/6-Digit PIN")
                        }
                        if (securityState.isPinConfigured) {
                            OutlinedButton(
                                onClick = {
                                    onHaptic(true)
                                    onLockAppNow()
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Lock Now")
                            }
                            OutlinedButton(
                                onClick = {
                                    onHaptic(true)
                                    onDisablePin()
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Disable")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = tokens.accent)
                            Column {
                                Text("Biometric Authentication", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (securityState.isBiometricHardwareAvailable) {
                                        "Fingerprint / Face unlock via AndroidX BiometricPrompt"
                                    } else {
                                        "No biometric hardware enrolled on this device"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = securityState.isBiometricEnabled,
                            enabled = securityState.isPinConfigured && securityState.isBiometricHardwareAvailable,
                            onCheckedChange = {
                                onHaptic(false)
                                onSetBiometricEnabled(it)
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unauthorized App-Access Alerts", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Notify and log after failed PIN attempts (5-attempt rate limit)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = securityState.alertOnUnauthorizedAccess,
                            onCheckedChange = {
                                onHaptic(false)
                                onSetAlertUnauthorized(it)
                            }
                        )
                    }
                }
            }
        }

        // Owner Device Registration
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = tokens.accent)
                            Text("Owner Device Registration", style = MaterialTheme.typography.titleLarge)
                        }
                        if (connectedEarbud != null) {
                            Button(
                                onClick = {
                                    onHaptic(false)
                                    showRegisterDialog = true
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Register")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    if (registeredDevices.isEmpty()) {
                        Text(
                            text = if (connectedEarbud != null) {
                                "Tap Register to bind ${connectedEarbud.deviceName} to your local owner registry."
                            } else {
                                "Connect your earbuds first to register them as an owner-verified device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        registeredDevices.forEach { dev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dev.customAlias, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${dev.brandName} • ${formatMac(dev.address)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.accent
                                    )
                                }
                                IconButton(onClick = { onUnregisterOwnerDevice(dev.address) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove owner device", tint = StatusRose)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security Audit & Connection History
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = tokens.accent)
                        Text("Security & Connection History", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(10.dp))

                    if (securityLogs.isEmpty() && connectionEvents.isEmpty()) {
                        Text(
                            text = "No security alerts or connection events logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        securityLogs.take(6).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (log.isAlert) Icons.Rounded.Warning else Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (log.isAlert) StatusRose else StatusEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.description, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = DateUtils.getRelativeTimeSpanString(log.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        connectionEvents.take(6).forEach { evt ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${evt.deviceName} • ${evt.eventType}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DateUtils.getRelativeTimeSpanString(evt.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinSetupDialog) {
        var pin by remember { mutableStateOf("") }
        var recovery by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Configure Keystore PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a 4-digit or 6-digit PIN plus a recovery phrase for emergency reset.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("4 or 6-Digit PIN") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = recovery,
                        onValueChange = { recovery = it },
                        label = { Text("Recovery Phrase (Recommended)") },
                        singleLine = true
                    )
                    err?.let { Text(it, color = StatusRose, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pin.length in listOf(4, 6)) {
                        onConfigurePin(pin, recovery.ifBlank { null })
                        showPinSetupDialog = false
                    } else {
                        err = "PIN must be exactly 4 or 6 digits."
                    }
                }) { Text("Save PIN") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRegisterDialog && connectedEarbud != null) {
        var alias by remember { mutableStateOf(connectedEarbud.deviceName) }
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Register Owner Device") },
            text = {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Device Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRegisterOwnerDevice(alias)
                    showRegisterDialog = false
                }) { Text("Register") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRegisterDialog = false }) { Text("Cancel") }
            }
        )
    }
}
