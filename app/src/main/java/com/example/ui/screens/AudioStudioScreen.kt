package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.data.audio.AudioEffectsManager
import com.example.domain.model.AudioChannelTestTarget
import com.example.domain.model.AudioEngineState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EqPreset
import com.example.domain.model.SoundMode
import com.example.ui.components.CapabilityStatusBadge
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import kotlin.math.roundToInt

@Composable
fun AudioStudioScreen(
    audioState: AudioEngineState,
    customPresets: List<EqPreset>,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onSetBandDb: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetSpatialAudio: (Boolean, Int) -> Unit,
    onSetSoundMode: (SoundMode) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onStartChannelTest: (AudioChannelTestTarget) -> Unit,
    onStopChannelTest: () -> Unit,
    onStartMicTest: () -> Unit,
    onStopMicTest: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    var showSaveDialog by remember { mutableStateOf(false) }

    val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sound Modes & Hardware Status
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = tokens.accent)
                            Text("Audio Studio DSP", style = MaterialTheme.typography.titleLarge)
                        }
                        CapabilityStatusBadge(
                            status = if (audioState.eqSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = audioState.statusNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Acoustic Sound Mode", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundMode.entries.forEach { mode ->
                            FilterChip(
                                selected = audioState.soundMode == mode,
                                onClick = {
                                    onHaptic(false)
                                    onSetSoundMode(mode)
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }

        // 10-Band Equalizer & Presets
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("10-Band Equalizer", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onHaptic(false)
                                    onOpenAiAssistant()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("audio_ai_tune_btn")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI Tune")
                            }
                            OutlinedButton(
                                onClick = {
                                    onHaptic(false)
                                    showSaveDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_eq_preset_btn")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudioEffectsManager.BUILT_IN_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = audioState.activePresetId == preset.id,
                                onClick = {
                                    onHaptic(false)
                                    onApplyPreset(preset)
                                },
                                label = { Text(preset.name) }
                            )
                        }
                    }

                    if (customPresets.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Saved Custom Presets", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        customPresets.forEach { preset ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilterChip(
                                    selected = audioState.activePresetId == preset.id,
                                    onClick = {
                                        onHaptic(false)
                                        onApplyPreset(preset)
                                    },
                                    label = { Text(preset.name) }
                                )
                                IconButton(onClick = { onDeleteCustomPreset(preset.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete preset ${preset.name}",
                                        tint = StatusRose
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 10-band vertical sliders row (scrollable on compact screens)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        audioState.bands10Db.forEachIndexed { idx, db ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(54.dp)
                            ) {
                                Text(
                                    text = if (db > 0) "+$db" else "$db",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = tokens.accent
                                )
                                VerticalEqSlider(
                                    value = db.toFloat(),
                                    valueRange = -12f..12f,
                                    onValueChange = { newVal ->
                                        onSetBandDb(idx, newVal.roundToInt())
                                    },
                                    modifier = Modifier.height(148.dp)
                                )
                                Text(
                                    text = bandLabels[idx],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bass Boost & Spatial Audio
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hardware Bass Boost", style = MaterialTheme.typography.titleMedium)
                        CapabilityStatusBadge(
                            status = if (audioState.bassBoostSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = audioState.bassBoostStrength.toFloat(),
                            onValueChange = { onSetBassBoost(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = audioState.bassBoostSupported,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${(audioState.bassBoostStrength / 10)}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = tokens.accent
                        )
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
                            Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = tokens.accent)
                            Column {
                                Text("Spatial Audio Virtualizer", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (audioState.virtualizerSupported) {
                                        "Android AudioFX Virtualizer DSP"
                                    } else {
                                        "Not Supported on This Device's global mix"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = audioState.spatialEnabled,
                            enabled = audioState.virtualizerSupported,
                            onCheckedChange = { enabled ->
                                onHaptic(false)
                                onSetSpatialAudio(enabled, audioState.virtualizerStrength.takeIf { it > 0 } ?: 650)
                            }
                        )
                    }
                    if (audioState.spatialEnabled && audioState.virtualizerSupported) {
                        Slider(
                            value = audioState.virtualizerStrength.toFloat(),
                            onValueChange = { onSetSpatialAudio(true, it.roundToInt()) },
                            valueRange = 0f..1000f
                        )
                    }
                }
            }
        }

        // Audio Channel Test & Microphone Test
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, tint = tokens.accent)
                        Text("Stereo Audio Channel Test", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Generates real stereo PCM tones through AudioTrack panned strictly to Left, Right, or Both channels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelTestBtn(
                            label = "Left (440Hz)",
                            active = audioState.activeChannelTest == AudioChannelTestTarget.LEFT,
                            modifier = Modifier.weight(1f).testTag("test_channel_left"),
                            onClick = {
                                onHaptic(false)
                                if (audioState.activeChannelTest == AudioChannelTestTarget.LEFT) onStopChannelTest()
                                else onStartChannelTest(AudioChannelTestTarget.LEFT)
                            }
                        )
                        ChannelTestBtn(
                            label = "Right (554Hz)",
                            active = audioState.activeChannelTest == AudioChannelTestTarget.RIGHT,
                            modifier = Modifier.weight(1f).testTag("test_channel_right"),
                            onClick = {
                                onHaptic(false)
                                if (audioState.activeChannelTest == AudioChannelTestTarget.RIGHT) onStopChannelTest()
                                else onStartChannelTest(AudioChannelTestTarget.RIGHT)
                            }
                        )
                        ChannelTestBtn(
                            label = "Stereo",
                            active = audioState.activeChannelTest == AudioChannelTestTarget.BOTH,
                            modifier = Modifier.weight(1f).testTag("test_channel_both"),
                            onClick = {
                                onHaptic(false)
                                if (audioState.activeChannelTest == AudioChannelTestTarget.BOTH) onStopChannelTest()
                                else onStartChannelTest(AudioChannelTestTarget.BOTH)
                            }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = tokens.accent)
                            Text("Live Microphone Test", style = MaterialTheme.typography.titleLarge)
                        }
                        if (audioState.micBluetoothScoActive) {
                            Surface(
                                shape = CircleShape,
                                color = StatusEmerald.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "BT MIC ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val normalizedMic = ((audioState.micLevelDb + 90f) / 90f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { normalizedMic },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (normalizedMic > 0.75f) StatusRose else tokens.accent
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Input Level: ${audioState.micLevelDb.roundToInt()} dBFS • Peak: ${audioState.micPeakDb.roundToInt()} dBFS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    if (!hasMicPermission) {
                        Button(
                            onClick = {
                                onHaptic(true)
                                onRequestMicPermission()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Microphone Permission")
                        }
                    } else {
                        Button(
                            onClick = {
                                onHaptic(true)
                                if (audioState.isMicTestRunning) onStopMicTest() else onStartMicTest()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("mic_test_toggle_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (audioState.isMicTestRunning) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (audioState.isMicTestRunning) "Stop Microphone Monitor" else "Start Microphone Test")
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom EQ Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomPreset(presetName)
                        showSaveDialog = false
                    },
                    enabled = presetName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChannelTestBtn(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (active) tokens.accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (active) tokens.accent else MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = if (active) "Stop" else label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) tokens.accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VerticalEqSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}
