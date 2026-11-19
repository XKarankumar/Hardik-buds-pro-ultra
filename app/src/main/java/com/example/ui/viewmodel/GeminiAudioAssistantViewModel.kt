package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AudioEngineState
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.EqPreset
import com.example.domain.model.SoundMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiAudioAssistantVM"

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String
)

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Loading : AiChatUiState
    data class Streaming(val partialText: String) : AiChatUiState
    data class Success(val text: String) : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

sealed interface AiEqTuneUiState {
    data object Idle : AiEqTuneUiState
    data object Loading : AiEqTuneUiState
    data class Success(val preset: EqPreset, val rationale: String) : AiEqTuneUiState
    data class Error(val message: String) : AiEqTuneUiState
}

class GeminiAudioAssistantViewModel : ViewModel() {

    private val eqTuningSchema = Schema.obj(
        properties = mapOf(
            "presetName" to Schema.string(description = "Short 2-4 word luxury EQ preset title"),
            "rationale" to Schema.string(description = "1-2 sentence acoustic explanation for these band adjustments"),
            "soundMode" to Schema.enumeration(
                values = listOf("MUSIC", "GAMING", "MOVIE", "CUSTOM"),
                description = "Recommended acoustic sound mode"
            ),
            "bassBoost" to Schema.integer(description = "Hardware bass boost strength between 0 and 1000"),
            "spatialStrength" to Schema.integer(description = "Spatial virtualizer strength between 0 and 1000"),
            "bands10Db" to Schema.array(
                items = Schema.integer(description = "Gain in dB from -12 to 12"),
                description = "Exactly 10 integer dB values for 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz"
            )
        )
    )

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are the HARDIK BUDS PRO ULTRA acoustic & Bluetooth engineering assistant. " +
                        "Be concise, accurate, and honest. Never claim proprietary vendor features " +
                        "(ANC, touch remapping, per-bud battery) work if the Android device only exposes " +
                        "standard A2DP/HFP/GATT profiles. Help users tune EQ, diagnose Bluetooth audio, " +
                        "understand battery/GATT telemetry, and locate lost earbuds safely."
                )
            }
        )
    }

    private val eqSchemaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = eqTuningSchema
            }
        )
    }

    private val _chatState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _eqTuneState = MutableStateFlow<AiEqTuneUiState>(AiEqTuneUiState.Idle)
    val eqTuneState: StateFlow<AiEqTuneUiState> = _eqTuneState.asStateFlow()

    fun sendQuestion(
        userPrompt: String,
        connectedEarbud: ConnectedEarbudState?,
        audioState: AudioEngineState
    ) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        _messages.value = _messages.value + AiChatMessage(isUser = true, text = trimmed)

        val deviceContext = buildString {
            appendLine("Live Device Telemetry Context:")
            if (connectedEarbud != null) {
                appendLine("- Connected Earbuds: ${connectedEarbud.deviceName} (${connectedEarbud.brand.displayName})")
                appendLine("- Profiles: A2DP=${connectedEarbud.a2dpConnected}, HFP=${connectedEarbud.hfpConnected}, GATT=${connectedEarbud.gattConnected}")
                appendLine("- Battery: ${connectedEarbud.combinedBatteryPercent?.let { "$it%" } ?: "Unavailable"} (${connectedEarbud.batterySourceNote})")
                appendLine("- Firmware (0x2A26): ${connectedEarbud.firmwareRevision ?: "Unavailable"}")
            } else {
                appendLine("- Connected Earbuds: None currently connected")
            }
            appendLine("- Android AudioFX Equalizer supported: ${audioState.eqSupported}, BassBoost: ${audioState.bassBoostSupported}, Virtualizer: ${audioState.virtualizerSupported}")
            appendLine("- Active EQ bands (31Hz..16kHz dB): ${audioState.bands10Db}")
            appendLine()
            append("User question: ")
            append(trimmed)
        }

        viewModelScope.launch {
            _chatState.value = AiChatUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    chatModel.generateContentStream(deviceContext).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        accumulated.append(part)
                        _chatState.value = AiChatUiState.Streaming(accumulated.toString())
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _messages.value = _messages.value + AiChatMessage(isUser = false, text = finalText)
                    _chatState.value = AiChatUiState.Success(finalText)
                } else {
                    _chatState.value = AiChatUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat timed out", e)
                _chatState.value = AiChatUiState.Error("Request timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat failed", e)
                _chatState.value = AiChatUiState.Error(e.localizedMessage ?: "Gemini request failed.")
            }
        }
    }

    fun generateEqPreset(
        acousticGoal: String,
        connectedEarbud: ConnectedEarbudState?
    ) {
        val trimmed = acousticGoal.trim()
        if (trimmed.isEmpty()) return

        val prompt = buildString {
            appendLine("Design an Android 10-band parametric/graphic EQ curve for true wireless earbuds.")
            appendLine("Frequencies in order: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.")
            appendLine("Each band gain must be an integer between -12 and 12 dB.")
            appendLine("bassBoost and spatialStrength must be integers between 0 and 1000.")
            if (connectedEarbud != null) {
                appendLine("Target earbuds: ${connectedEarbud.deviceName} (${connectedEarbud.brand.displayName}).")
            }
            append("Listener request: ")
            append(trimmed)
        }

        viewModelScope.launch {
            _eqTuneState.value = AiEqTuneUiState.Loading
            try {
                val response = withTimeout(60_000L) {
                    eqSchemaModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty JSON.")
                val json = JSONObject(jsonText)
                val name = json.optString("presetName", "Gemini Studio").ifBlank { "Gemini Studio" }
                val rationale = json.optString("rationale", "Custom acoustic profile tuned by Gemini.")
                val modeStr = json.optString("soundMode", "CUSTOM")
                val mode = SoundMode.entries.firstOrNull { it.name == modeStr } ?: SoundMode.CUSTOM
                val bass = json.optInt("bassBoost", 250).coerceIn(0, 1000)
                val spatial = json.optInt("spatialStrength", 200).coerceIn(0, 1000)
                val bandsArray = json.getJSONArray("bands10Db")
                val bands = (0 until 10).map { idx ->
                    if (idx < bandsArray.length()) bandsArray.getInt(idx).coerceIn(-12, 12) else 0
                }

                val preset = EqPreset(
                    id = "ai_${UUID.randomUUID()}",
                    name = name,
                    bands10Db = bands,
                    bassBoostStrength = bass,
                    virtualizerStrength = spatial,
                    soundMode = mode,
                    isCustom = true
                )
                _eqTuneState.value = AiEqTuneUiState.Success(preset, rationale)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini EQ tuning timed out", e)
                _eqTuneState.value = AiEqTuneUiState.Error("EQ generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini EQ tuning failed", e)
                _eqTuneState.value = AiEqTuneUiState.Error(e.localizedMessage ?: "Failed to generate EQ profile.")
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _chatState.value = AiChatUiState.Idle
    }
}
