package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ConnectionEventEntity
import com.example.data.location.PhoneLocationSnapshot
import com.example.domain.model.AudioChannelTestTarget
import com.example.domain.model.ConnectedEarbudState
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

@Composable
fun FindMyScreen(
    connectedEarbud: ConnectedEarbudState?,
    lastPhoneLocation: PhoneLocationSnapshot?,
    connectionEvents: List<ConnectionEventEntity>,
    hasLocationPermission: Boolean,
    activeFinderTarget: AudioChannelTestTarget,
    isScanningBle: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRefreshLocation: () -> Unit,
    onStartFinderSound: (AudioChannelTestTarget) -> Unit,
    onStopFinderSound: () -> Unit,
    onScanRssi: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val context = LocalContext.current
    val lastEventWithCoords = connectionEvents.firstOrNull { it.latitude != null && it.longitude != null }
    val lastEvent = connectionEvents.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Honest Phone-vs-Earbuds Location Disclaimer
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
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = StatusAmber)
                        Column {
                            Text("Location Transparency Notice", style = MaterialTheme.typography.titleMedium, color = StatusAmber)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.find_my_location_disclaimer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Last Known Connection & Last Known Phone Location
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = tokens.accent)
                            Text("Last Known Connection & Location", style = MaterialTheme.typography.titleLarge)
                        }
                        if (hasLocationPermission) {
                            OutlinedButton(onClick = {
                                onHaptic(false)
                                onRefreshLocation()
                            }) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (lastEvent != null) {
                            "Last Event: ${lastEvent.deviceName} (${lastEvent.eventType}) • ${
                                DateUtils.getRelativeTimeSpanString(lastEvent.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                            }"
                        } else {
                            "No Bluetooth connection events recorded yet."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(10.dp))

                    val lat = lastEventWithCoords?.latitude ?: lastPhoneLocation?.latitude
                    val lon = lastEventWithCoords?.longitude ?: lastPhoneLocation?.longitude
                    val acc = lastEventWithCoords?.locationAccuracyMeters ?: lastPhoneLocation?.accuracyMeters

                    if (!hasLocationPermission) {
                        Button(
                            onClick = {
                                onHaptic(true)
                                onRequestLocationPermission()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("grant_location_btn")
                        ) {
                            Text("Grant Location Permission for Last-Known Phone Coordinates")
                        }
                    } else if (lat != null && lon != null) {
                        Text(
                            text = "Phone Coordinates: %.5f, %.5f (±%.0fm)".format(lat, lon, acc ?: 0f),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = tokens.accent
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onHaptic(false)
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Last+Known+Phone+Location)")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Map, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Last Phone Location in Maps")
                        }
                    } else {
                        Text(
                            text = "Phone location unavailable right now. Enable device Location Services and tap Refresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Play Sound & Left / Right Earbud Search
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = tokens.accent)
                        Text("Play Sound & Earbud Search", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (connectedEarbud != null) {
                            "Outputs high-visibility pulsed finder audio through connected earbuds (and BLE Immediate Alert 0x1802 if supported)."
                        } else {
                            "Connect earbuds first so audio routes to them; currently audio plays through phone speaker if disconnected."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onHaptic(true)
                                if (activeFinderTarget == AudioChannelTestTarget.LEFT) onStopFinderSound()
                                else onStartFinderSound(AudioChannelTestTarget.LEFT)
                            },
                            modifier = Modifier.weight(1f).testTag("find_left_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (activeFinderTarget == AudioChannelTestTarget.LEFT) "Stop Left" else "Left Bud")
                        }
                        Button(
                            onClick = {
                                onHaptic(true)
                                if (activeFinderTarget == AudioChannelTestTarget.RIGHT) onStopFinderSound()
                                else onStartFinderSound(AudioChannelTestTarget.RIGHT)
                            },
                            modifier = Modifier.weight(1f).testTag("find_right_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (activeFinderTarget == AudioChannelTestTarget.RIGHT) "Stop Right" else "Right Bud")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onHaptic(true)
                            if (activeFinderTarget != AudioChannelTestTarget.NONE) onStopFinderSound()
                            else onStartFinderSound(AudioChannelTestTarget.BOTH)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("find_both_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (activeFinderTarget != AudioChannelTestTarget.NONE) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (activeFinderTarget != AudioChannelTestTarget.NONE) "Stop Finder Alert" else "Play Sound on Both Earbuds")
                    }
                }
            }
        }

        // BLE Signal Strength (RSSI) Meter
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = tokens.accent)
                            Text("BLE Signal Strength (RSSI)", style = MaterialTheme.typography.titleLarge)
                        }
                        OutlinedButton(onClick = {
                            onHaptic(false)
                            onScanRssi()
                        }) {
                            Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isScanningBle) "Scanning..." else "Ping BLE")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val rssi = connectedEarbud?.rssiDbm
                    if (rssi != null) {
                        val norm = ((rssi + 100) / 60f).coerceIn(0.05f, 1f)
                        val proximity = when {
                            rssi >= -55 -> "Immediate (< 1.5m)"
                            rssi >= -72 -> "Near (1.5m – 5m)"
                            else -> "Far (> 5m)"
                        }
                        LinearProgressIndicator(
                            progress = { norm },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = StatusEmerald
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Live RSSI: $rssi dBm • Estimated Proximity: $proximity",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = StatusEmerald
                        )
                    } else {
                        Text(
                            text = "RSSI unavailable. Many classic A2DP earbuds do not broadcast BLE advertisements while connected; tap Ping BLE if your earbuds advertise BLE.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
