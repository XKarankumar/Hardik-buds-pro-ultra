package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.bluetooth.BrandIntegrationRegistry
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EarbudBrand
import com.example.ui.components.CapabilityStatusBadge
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.LocalLuxuryTokens

@Composable
fun DeviceCompatibilityScreen(
    onBack: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val brands = BrandIntegrationRegistry.allSupportedBrandDocs()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text("Device Compatibility Matrix", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.height(6.dp))
                LuxuryGlassCard {
                    Text(
                        text = "Modular Brand Integration Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.accent
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "All 11 device profiles support standard Bluetooth Classic A2DP/HFP, BLE GATT (0x180F Battery, 0x180A Device Info, 0x1802 Alert), AudioFX 10-band EQ, Stereo Channel Test, and Find My Phone Location. Proprietary vendor controls (ANC, Touch Remap, In-Ear Sensors) are strictly gated pending official OEM SDK integration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(brands) { brand ->
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(brand.displayName, style = MaterialTheme.typography.titleLarge)
                        CapabilityStatusBadge(
                            status = if (brand == EarbudBrand.STANDARD) {
                                CapabilityStatus.SUPPORTED
                            } else {
                                CapabilityStatus.PENDING_INTEGRATION
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = brand.protocolSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
