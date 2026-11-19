package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BatteryHistoryEntity
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.DiscoveredBluetoothDevice
import com.example.ui.components.AnimatedBatteryRing
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose

@Composable
fun DashboardScreen(
    isHardwareSupported: Boolean,
    isBluetoothEnabled: Boolean,
    hasBluetoothPermissions: Boolean,
    isScanning: Boolean,
    scanStatusMessage: String?,
    connectedEarbud: ConnectedEarbudState?,
    bondedDevices: List<DiscoveredBluetoothDevice>,
    discoveredDevices: List<DiscoveredBluetoothDevice>,
    batteryHistory: List<BatteryHistoryEntity>,
    maskMac: Boolean,
    lowBatteryThreshold: Int,
    formatMac: (String) -> String,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onPairDevice: (String) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectCurrent: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onPreviewPopup: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                // Brand Hero Banner Card
                LuxuryGlassCard(cornerRadius = 28.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_earbuds_hero),
                            contentDescription = stringResource(R.string.brand_full_title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xD907090E))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "HARDIK BUDS PRO ULTRA",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    !isHardwareSupported -> stringResource(R.string.bt_status_unavailable)
                                    !isBluetoothEnabled -> stringResource(R.string.bt_status_disabled)
                                    connectedEarbud != null -> "Connected to ${connectedEarbud.deviceName} (${connectedEarbud.brand.displayName})"
                                    else -> "Ready • Real Bluetooth & BLE Audio Companion"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD5DDEA)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    onHaptic(false)
                                    onOpenAiAssistant()
                                },
                                shape = CircleShape,
                                color = Color(0xBB0D121D),
                                border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("open_ai_assistant_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Open Gemini AI Studio",
                                        tint = tokens.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Gemini AI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    onHaptic(false)
                                    onPreviewPopup()
                                },
                                shape = CircleShape,
                                color = Color(0x990D121D),
                                border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.55f)),
                                modifier = Modifier.testTag("preview_popup_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "Preview connection popup",
                                        tint = tokens.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Popup UI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Permission & Radio Quick Actions
                    if (!hasBluetoothPermissions) {
                        Surface(
                            color = StatusAmber.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Grant Nearby Devices / Bluetooth permissions to detect and scan real earbuds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onHaptic(true)
                                        onRequestPermissions()
                                    },
                                    modifier = Modifier.testTag("grant_bt_permissions_btn")
                                ) {
                                    Text("Grant")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onHaptic(false)
                                if (isScanning) onStopScan() else onStartScan()
                            },
                            enabled = isHardwareSupported && isBluetoothEnabled && hasBluetoothPermissions,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_toggle_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.bt_scan_stop))
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.bt_scan_start))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onHaptic(false)
                                onOpenBluetoothSettings()
                            },
                            modifier = Modifier.testTag("open_bt_settings_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("BT Settings")
                        }

                        IconButton(
                            onClick = {
                                onHaptic(false)
                                onRefreshBluetooth()
                            },
                            modifier = Modifier.testTag("refresh_bt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Bluetooth state"
                            )
                        }
                    }

                    if (!scanStatusMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = scanStatusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accent
                        )
                    }
                }
            }
        }

        // Connected Earbuds & Smart Battery Dashboard
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when {
                                    !isBluetoothEnabled -> Icons.Rounded.BluetoothDisabled
                                    connectedEarbud != null -> Icons.Rounded.BluetoothConnected
                                    else -> Icons.Rounded.Bluetooth
                                },
                                contentDescription = null,
                                tint = if (connectedEarbud != null) StatusEmerald else tokens.accent
                            )
                            Column {
                                Text(
                                    text = connectedEarbud?.deviceName ?: "Smart Battery Dashboard",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = if (connectedEarbud != null) {
                                        "${connectedEarbud.brand.displayName} • ${formatMac(connectedEarbud.address)}"
                                    } else {
                                        "No active Bluetooth earbuds connected"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (connectedEarbud != null) {
                            OutlinedButton(
                                onClick = {
                                    onHaptic(true)
                                    onDisconnectCurrent()
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Disconnect")
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Low Battery Warning Banner
                    val combined = connectedEarbud?.combinedBatteryPercent
                    if (combined != null && combined <= lowBatteryThreshold) {
                        Surface(
                            color = StatusRose.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StatusRose.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.BatteryAlert, contentDescription = null, tint = StatusRose)
                                Text(
                                    text = "Low Battery Warning: Earbuds at $combined% (below $lowBatteryThreshold%).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusRose
                                )
                            }
                        }
                    }

                    // Four Battery Rings: Left, Right, Case, Combined
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnimatedBatteryRing(
                            label = stringResource(R.string.battery_left),
                            percent = connectedEarbud?.leftBatteryPercent,
                            size = 72.dp
                        )
                        AnimatedBatteryRing(
                            label = stringResource(R.string.battery_right),
                            percent = connectedEarbud?.rightBatteryPercent,
                            size = 72.dp
                        )
                        AnimatedBatteryRing(
                            label = stringResource(R.string.battery_case),
                            percent = connectedEarbud?.caseBatteryPercent,
                            size = 72.dp
                        )
                        AnimatedBatteryRing(
                            label = "Headset",
                            percent = connectedEarbud?.combinedBatteryPercent,
                            size = 72.dp
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (connectedEarbud != null) {
                                    "Telemetry Source: ${connectedEarbud.batterySourceNote}"
                                } else {
                                    "Connect Bluetooth earbuds to inspect real HFP / BLE GATT (0x180F) battery telemetry. Unavailable sensors always show N/A."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (connectedEarbud != null) {
                                val profiles = buildList {
                                    if (connectedEarbud.a2dpConnected) add("A2DP Audio")
                                    if (connectedEarbud.hfpConnected) add("HFP Call")
                                    if (connectedEarbud.gattConnected) add("BLE GATT")
                                }.ifEmpty { listOf("Link Active") }
                                Text(
                                    text = "Active Profiles: ${profiles.joinToString(" • ")} | Charging: ${
                                        when (connectedEarbud.isCharging) {
                                            true -> "Charging"
                                            false -> "Discharging"
                                            null -> "Unavailable"
                                        }
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.accent
                                )
                            }
                        }
                    }

                    if (batteryHistory.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Battery Telemetry History (${batteryHistory.size} samples)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        BatteryHistoryChart(
                            samples = batteryHistory.take(20).reversed(),
                            accentColor = tokens.accent,
                            modifier = Modifier.fillMaxWidth().height(92.dp)
                        )
                    }
                }
            }
        }

        // Previously Paired Devices
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Text(
                    text = "${stringResource(R.string.bt_paired_devices)} (${bondedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (bondedDevices.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    LuxuryGlassCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (!hasBluetoothPermissions) {
                                    "Bluetooth permission needed to list paired devices."
                                } else if (!isBluetoothEnabled) {
                                    "Turn on Bluetooth to view previously paired earbuds."
                                } else {
                                    "No paired Bluetooth devices found. Pair your earbuds via Scan Nearby or System Settings."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(bondedDevices, key = { "bonded_${it.address}" }) { device ->
                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    BluetoothDeviceRowCard(
                        device = device,
                        displayAddress = formatMac(device.address),
                        actionLabel = if (device.isConnected) "Connected" else "Reconnect",
                        isActionPrimary = !device.isConnected,
                        onAction = {
                            onHaptic(true)
                            if (!device.isConnected) onConnectDevice(device.address)
                        }
                    )
                }
            }
        }

        // Discovered Nearby Devices
        if (discoveredDevices.isNotEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    Text(
                        text = "${stringResource(R.string.bt_discovered_devices)} (${discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            items(discoveredDevices, key = { "disc_${it.address}" }) { device ->
                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    BluetoothDeviceRowCard(
                        device = device,
                        displayAddress = formatMac(device.address),
                        actionLabel = if (device.isBonded) "Connect" else "Pair",
                        isActionPrimary = true,
                        onAction = {
                            onHaptic(true)
                            if (device.isBonded) onConnectDevice(device.address) else onPairDevice(device.address)
                        }
                    )
                }
            }
        }

        // Recent Battery Log rows
        if (batteryHistory.isNotEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    LuxuryGlassCard {
                        Text("Recent Battery Events", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        batteryHistory.take(5).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.deviceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.combinedPercent ?: "--"}% • ${
                                        DateUtils.getRelativeTimeSpanString(
                                            item.timestamp,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS
                                        )
                                    }",
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
}

@Composable
private fun BluetoothDeviceRowCard(
    device: DiscoveredBluetoothDevice,
    displayAddress: String,
    actionLabel: String,
    isActionPrimary: Boolean,
    onAction: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    LuxuryGlassCard(cornerRadius = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (device.isConnected) StatusEmerald.copy(alpha = 0.16f) else tokens.accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (device.isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = if (device.isConnected) StatusEmerald else tokens.accent
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(device.brand.displayName)
                            append(" • ")
                            append(displayAddress)
                            device.rssiDbm?.let { append(" • $it dBm") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (isActionPrimary) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(actionLabel)
                }
            } else {
                TextButton(onClick = onAction) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(actionLabel, color = StatusEmerald)
                }
            }
        }
    }
}

@Composable
private fun BatteryHistoryChart(
    samples: List<BatteryHistoryEntity>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val valid = samples.mapNotNull { it.combinedPercent }
        if (valid.size < 2) {
            drawLine(
                color = accentColor.copy(alpha = 0.35f),
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }
        val stepX = size.width / (valid.size - 1).coerceAtLeast(1)
        val path = Path()
        valid.forEachIndexed { idx, pct ->
            val x = idx * stepX
            val y = size.height - ((pct.coerceIn(0, 100) / 100f) * (size.height - 12.dp.toPx()) + 6.dp.toPx())
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = accentColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
        }
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
