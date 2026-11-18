package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Earbuds
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.bluetooth.BrandIntegrationRegistry
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EarbudFeatureId
import com.example.domain.model.FeatureCapability
import com.example.ui.components.CapabilityStatusBadge
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusMuted

@Composable
fun SmartControlsScreen(
    connectedEarbud: ConnectedEarbudState?,
    onOpenCompatibilityMatrix: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    var inspectedCapability by remember { mutableStateOf<FeatureCapability?>(null) }

    val activeMatrix = connectedEarbud?.capabilityMatrix ?: BrandIntegrationRegistry
        .getAdapter(EarbudBrand.STANDARD)
        .buildCapabilities(
            hasGattBattery = false,
            hasCombinedBattery = false,
            hasFirmwareRevision = false,
            hasImmediateAlert = false,
            eqSupported = true
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Earbud Controls", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (connectedEarbud != null) {
                                    "${connectedEarbud.deviceName} • ${connectedEarbud.brand.displayName}"
                                } else {
                                    "No earbuds connected — showing protocol readiness"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.accent
                            )
                        }
                        Button(
                            onClick = {
                                onHaptic(false)
                                onOpenCompatibilityMatrix()
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Brands")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Controls only activate when the connected hardware exposes a documented standard GATT profile or authorized vendor SDK. Unsupported or proprietary features are never faked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Noise Control Section (ANC / Transparency / Normal)
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Text("Noise Control Modes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        EarbudFeatureId.ANC_CONTROL to Icons.Rounded.NoiseControlOff,
                        EarbudFeatureId.TRANSPARENCY_MODE to Icons.Rounded.Hearing,
                        EarbudFeatureId.NORMAL_MODE to Icons.Rounded.Tune
                    ).forEach { (feature, icon) ->
                        val cap = activeMatrix.capabilities[feature]
                            ?: FeatureCapability(feature, CapabilityStatus.UNSUPPORTED, "Not Supported on This Device")
                        ControlCapabilityRow(
                            icon = icon,
                            capability = cap,
                            onClick = {
                                onHaptic(false)
                                inspectedCapability = cap
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Touch Controls Customization
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Text("Touch Control Customization", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        EarbudFeatureId.TOUCH_SINGLE_TAP,
                        EarbudFeatureId.TOUCH_DOUBLE_TAP,
                        EarbudFeatureId.TOUCH_TRIPLE_TAP,
                        EarbudFeatureId.TOUCH_LONG_PRESS
                    ).forEach { feature ->
                        val cap = activeMatrix.capabilities[feature]
                            ?: FeatureCapability(feature, CapabilityStatus.UNSUPPORTED, "Not Supported on This Device")
                        ControlCapabilityRow(
                            icon = Icons.Rounded.TouchApp,
                            capability = cap,
                            onClick = {
                                onHaptic(false)
                                inspectedCapability = cap
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Hardware Sensors & Firmware
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Text("Sensors, Latency & Firmware", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))

                    val inEar = activeMatrix.capabilities[EarbudFeatureId.IN_EAR_DETECTION]!!
                    val lowLat = activeMatrix.capabilities[EarbudFeatureId.LOW_LATENCY_MODE]!!
                    val fwInfo = activeMatrix.capabilities[EarbudFeatureId.FIRMWARE_INFO]!!
                    val fwUpdate = activeMatrix.capabilities[EarbudFeatureId.FIRMWARE_UPDATE]!!

                    ControlCapabilityRow(Icons.Rounded.Earbuds, inEar) { inspectedCapability = inEar }
                    Spacer(Modifier.height(8.dp))
                    ControlCapabilityRow(Icons.Rounded.Speed, lowLat) { inspectedCapability = lowLat }
                    Spacer(Modifier.height(8.dp))
                    ControlCapabilityRow(
                        icon = Icons.Rounded.Info,
                        capability = fwInfo,
                        subtitleOverride = connectedEarbud?.firmwareRevision?.let { "Firmware Revision (0x2A26): $it" }
                    ) { inspectedCapability = fwInfo }
                    Spacer(Modifier.height(8.dp))
                    ControlCapabilityRow(Icons.Rounded.SystemUpdate, fwUpdate) { inspectedCapability = fwUpdate }
                }
            }
        }
    }

    inspectedCapability?.let { cap ->
        AlertDialog(
            onDismissRequest = { inspectedCapability = null },
            title = { Text(cap.feature.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CapabilityStatusBadge(status = cap.status)
                    Text(
                        text = cap.detail,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { inspectedCapability = null }) { Text("Understood") }
            }
        )
    }
}

@Composable
private fun ControlCapabilityRow(
    icon: ImageVector,
    capability: FeatureCapability,
    subtitleOverride: String? = null,
    onClick: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val tint = when (capability.status) {
        CapabilityStatus.SUPPORTED -> tokens.accent
        CapabilityStatus.PENDING_INTEGRATION -> StatusAmber
        CapabilityStatus.UNSUPPORTED -> StatusMuted
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Column {
                    Text(capability.feature.title, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = subtitleOverride ?: when (capability.status) {
                            CapabilityStatus.SUPPORTED -> "Active via documented protocol"
                            CapabilityStatus.PENDING_INTEGRATION -> "Pending Vendor SDK Integration"
                            CapabilityStatus.UNSUPPORTED -> "Not Supported on This Device"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CapabilityStatusBadge(status = capability.status)
        }
    }
}
