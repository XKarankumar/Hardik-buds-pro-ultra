package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AudioEngineState
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.EqPreset
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiChatUiState
import com.example.ui.viewmodel.AiEqTuneUiState

@Composable
fun AiAssistantScreen(
    connectedEarbud: ConnectedEarbudState?,
    audioState: AudioEngineState,
    chatState: AiChatUiState,
    messages: List<AiChatMessage>,
    eqTuneState: AiEqTuneUiState,
    onGenerateEq: (String) -> Unit,
    onApplyAndSaveEq: (EqPreset) -> Unit,
    onSendQuestion: (String) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    var eqGoalInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }

    val quickEqGoals = listOf(
        "Punchy Bollywood bass with clear vocals",
        "Competitive FPS footsteps & spatial clarity",
        "Warm late-night acoustic & jazz",
        "Crisp podcast & speech intelligibility"
    )

    val quickQuestions = listOf(
        "Diagnose my connected earbuds telemetry",
        "Why are vendor ANC controls marked pending?",
        "Best spatial & EQ tips for movies?"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_assistant_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Gemini AI Acoustic Studio", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = "Powered by Firebase AI Logic • gemini-3.8-flash",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.accent
                            )
                        }
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear AI chat")
                        }
                    }
                }
            }
        }

        // Structured JSON 10-Band EQ Tuning Card
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = tokens.accent)
                        Text("AI 10-Band EQ Designer", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Describe your sound signature goal and Gemini will generate a structured 10-band EQ curve + BassBoost + Spatial profile for your DSP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickEqGoals.forEach { goal ->
                            AssistChip(
                                onClick = {
                                    onHaptic(false)
                                    eqGoalInput = goal
                                    onGenerateEq(goal)
                                },
                                label = { Text(goal) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = eqGoalInput,
                        onValueChange = { eqGoalInput = it },
                        label = { Text("Describe desired EQ / music genre") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_eq_goal_input"),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onHaptic(true)
                            onGenerateEq(eqGoalInput)
                        },
                        enabled = eqGoalInput.isNotBlank() && eqTuneState !is AiEqTuneUiState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("ai_generate_eq_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (eqTuneState is AiEqTuneUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Tuning 10-Band Curve...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate EQ with Gemini")
                        }
                    }

                    when (eqTuneState) {
                        is AiEqTuneUiState.Success -> {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StatusEmerald.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${eqTuneState.preset.name} (${eqTuneState.preset.soundMode.label})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StatusEmerald
                                    )
                                    Text(
                                        text = eqTuneState.rationale,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Bands (dB): ${eqTuneState.preset.bands10Db.joinToString(", ")} | Bass: ${eqTuneState.preset.bassBoostStrength / 10}% | Spatial: ${eqTuneState.preset.virtualizerStrength / 10}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = tokens.accent
                                    )
                                    Button(
                                        onClick = {
                                            onHaptic(true)
                                            onApplyAndSaveEq(eqTuneState.preset)
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("ai_apply_eq_btn"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Apply to Hardware DSP & Save Preset")
                                    }
                                }
                            }
                        }
                        is AiEqTuneUiState.Error -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = eqTuneState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusRose
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        // Streaming Acoustic & Bluetooth Assistant Chat
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                LuxuryGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = tokens.accent)
                        Text("Earbuds & Acoustics Coach", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (connectedEarbud != null) {
                            "Grounded with ${connectedEarbud.deviceName} (${connectedEarbud.brand.displayName}) live telemetry."
                        } else {
                            "Ask anything about Bluetooth profiles, battery telemetry, or sound tuning."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickQuestions.forEach { q ->
                            AssistChip(
                                onClick = {
                                    onHaptic(false)
                                    onSendQuestion(q)
                                },
                                label = { Text(q) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            label = { Text("Ask Gemini about your earbuds") },
                            modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                onHaptic(false)
                                val q = chatInput
                                chatInput = ""
                                onSendQuestion(q)
                            },
                            enabled = chatInput.isNotBlank() && chatState !is AiChatUiState.Loading && chatState !is AiChatUiState.Streaming,
                            modifier = Modifier.height(54.dp).testTag("ai_send_chat_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                        }
                    }

                    if (chatState is AiChatUiState.Error) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = chatState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusRose
                        )
                    }
                }
            }
        }

        items(messages, key = { it.id }) { msg ->
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (msg.isUser) {
                        tokens.accent.copy(alpha = 0.16f)
                    } else {
                        tokens.glassSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (msg.isUser) tokens.accent.copy(alpha = 0.5f) else tokens.glassBorderTop.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (msg.isUser) "You" else "HARDIK BUDS AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isUser) tokens.accent else StatusEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        if (chatState is AiChatUiState.Loading || chatState is AiChatUiState.Streaming) {
            item {
                Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    LuxuryGlassCard(cornerRadius = 20.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = if (chatState is AiChatUiState.Streaming && chatState.partialText.isNotBlank()) {
                                    chatState.partialText
                                } else {
                                    "Gemini is analyzing acoustics & Bluetooth state..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
