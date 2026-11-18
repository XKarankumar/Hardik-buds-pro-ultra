package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.AppPreferencesState
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserProfile
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusRose

@Composable
fun SettingsScreen(
    prefs: AppPreferencesState,
    hasBluetoothPermissions: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasMicPermission: Boolean,
    onSetThemeStyle: (AppThemeStyle) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentPalette: (AccentPalette) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetConnectionPopup: (Boolean) -> Unit,
    onSetDynamicIsland: (Boolean) -> Unit,
    onSetMaskMac: (Boolean) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onUpdateNotifications: (NotificationPreferences) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenCompatibilityMatrix: () -> Unit,
    onClearLogs: () -> Unit,
    onResetSettings: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) } ?: "Silent"
            onUpdateNotifications(
                prefs.notifications.copy(
                    customSoundUri = uri?.toString(),
                    customSoundName = title
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Profile & Language
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = tokens.accent)
                            Column {
                                Text(prefs.userProfile.displayName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Preferred Brand: ${prefs.userProfile.preferredBrandLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = { showProfileDialog = true }) {
                            Text("Edit")
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = tokens.accent)
                        Text("App Language", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = {
                                    onHaptic(false)
                                    onSetLanguage(lang)
                                },
                                label = { Text(lang.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // Theme, Light/Dark & Accent Colors
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = tokens.accent)
                        Text("Theme & Luxury Aesthetics", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    AppThemeStyle.entries.forEach { style ->
                        val selected = prefs.themeStyle == style
                        Surface(
                            onClick = {
                                onHaptic(false)
                                onSetThemeStyle(style)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) tokens.accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (selected) tokens.accent else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("theme_${style.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(style.label, style = MaterialTheme.typography.titleMedium)
                                    Text(style.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = tokens.accent)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Brightness Mode", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = {
                                    onHaptic(false)
                                    onSetThemeMode(mode)
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Accent Colour", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AccentPalette.entries.forEach { accent ->
                            val selected = prefs.accentPalette == accent
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.colorHex))
                                    .clickable {
                                        onHaptic(false)
                                        onSetAccentPalette(accent)
                                    }
                                    .testTag("accent_${accent.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = accent.label,
                                        tint = Color(0xFF07090D)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SettingSwitchRow("Haptic Feedback", "Tactile response on controls & PIN pad", prefs.hapticsEnabled, onSetHaptics)
                    SettingSwitchRow("iOS-Style Connection Popup", "Show animated popup on real connection", prefs.connectionPopupEnabled, onSetConnectionPopup)
                    SettingSwitchRow("Dynamic Island Status Pill", "Floating top status pill on BT events", prefs.dynamicIslandBannerEnabled, onSetDynamicIsland)
                }
            }
        }

        // Smart Notifications & Custom Sound
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = tokens.accent)
                        Text("Smart Notifications", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    val n = prefs.notifications
                    SettingSwitchRow("Enable Smart Notifications", "Master Android NotificationChannel toggle", n.masterEnabled) {
                        onUpdateNotifications(n.copy(masterEnabled = it))
                    }
                    SettingSwitchRow("Earbuds Connected", "Alert when audio profile connects", n.onConnected) {
                        onUpdateNotifications(n.copy(onConnected = it))
                    }
                    SettingSwitchRow("Earbuds Disconnected / Lost", "Alert on disconnection or dropped link", n.onConnectionLost) {
                        onUpdateNotifications(n.copy(onConnectionLost = it, onDisconnected = it))
                    }
                    SettingSwitchRow("Low Battery (${n.lowBatteryThreshold}%)", "Warn when reported battery drops low", n.onLowBattery) {
                        onUpdateNotifications(n.copy(onLowBattery = it))
                    }
                    SettingSwitchRow("Bluetooth Disabled", "Notify when phone Bluetooth turns off", n.onBluetoothDisabled) {
                        onUpdateNotifications(n.copy(onBluetoothDisabled = it))
                    }
                    SettingSwitchRow("Unauthorized App Access", "Alert on failed companion PIN entries", n.onUnauthorizedAccess) {
                        onUpdateNotifications(n.copy(onUnauthorizedAccess = it))
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Companion Notification Sound")
                                n.customSoundUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                }
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Notification Sound: ${n.customSoundName}")
                    }
                }
            }
        }

        // Permissions, Privacy, Compatibility, Reset & About
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = tokens.accent)
                        Text("Permissions & Privacy Dashboard", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("• Bluetooth / Nearby Devices: ${if (hasBluetoothPermissions) "Granted" else "Not Granted"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Location (Last-Known Phone Loc): ${if (hasLocationPermission) "Granted" else "Not Granted"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Notifications (Android 13+): ${if (hasNotificationPermission) "Granted" else "Not Granted"}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Microphone (Audio Test): ${if (hasMicPermission) "Granted" else "Not Granted"}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onHaptic(false)
                            onRequestPermissions()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Review & Grant Runtime Permissions")
                    }

                    Spacer(Modifier.height(10.dp))
                    SettingSwitchRow(
                        "Mask Bluetooth MAC Addresses",
                        "Hide full hardware MAC addresses on dashboard",
                        prefs.maskMacAddressesInUi,
                        onSetMaskMac
                    )

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onHaptic(false)
                            onOpenCompatibilityMatrix()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Devices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Device Compatibility Matrix (11 Brands)")
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onHaptic(true)
                                onClearLogs()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Clear Logs")
                        }
                        Button(
                            onClick = {
                                onHaptic(true)
                                showResetDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRose),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset All")
                        }
                    }
                }
            }
        }

        // About HARDIK BUDS PRO ULTRA
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = tokens.accent)
                        Text("About HARDIK BUDS PRO", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Edition: HARDIK BUDS PRO ULTRA", style = MaterialTheme.typography.titleMedium)
                    Text("Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium)
                    Text("Architecture: Clean Architecture + MVVM • Jetpack Compose + Room + Keystore", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.copyright_notice), style = MaterialTheme.typography.labelSmall, color = tokens.accent)
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings & Data?") },
            text = { Text("This clears preferences, Room history logs, custom EQ presets, owner registrations, and companion PIN.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRose),
                    onClick = {
                        onResetSettings()
                        showResetDialog = false
                    }
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showProfileDialog) {
        var name by remember { mutableStateOf(prefs.userProfile.displayName) }
        var brand by remember { mutableStateOf(prefs.userProfile.preferredBrandLabel) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit User Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Preferred Earbuds Brand") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(UserProfile(name.ifBlank { "Hardik Listener" }, brand.ifBlank { "Universal" }))
                    showProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProfileDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
