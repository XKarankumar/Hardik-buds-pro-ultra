package com.example.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.model.AudioChannelTestTarget
import com.example.domain.model.AudioEngineState
import com.example.domain.model.EqPreset
import com.example.domain.model.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class AudioEffectsManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "AudioEffectsManager"
        val BAND_FREQUENCIES_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        val BUILT_IN_PRESETS = listOf(
            EqPreset("flat", "Reference Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 0, 0, SoundMode.MUSIC),
            EqPreset("bass_boost", "Bass Boost", listOf(7, 6, 5, 3, 1, 0, 0, 1, 2, 2), 650, 150, SoundMode.MUSIC),
            EqPreset("deep_bass", "Deep Bass", listOf(10, 9, 6, 2, -1, -1, 0, 1, 2, 3), 900, 200, SoundMode.MUSIC),
            EqPreset("vocal_boost", "Vocal Boost", listOf(-2, -2, -1, 1, 4, 6, 6, 4, 2, 0), 150, 250, SoundMode.MUSIC),
            EqPreset("treble_boost", "Treble Boost", listOf(-1, -1, 0, 0, 1, 2, 4, 6, 8, 9), 100, 300, SoundMode.MUSIC),
            EqPreset("gaming", "Gaming Mode", listOf(2, 1, 0, 1, 3, 4, 5, 6, 5, 3), 350, 650, SoundMode.GAMING),
            EqPreset("movie", "Movie Mode", listOf(5, 4, 2, 1, 2, 3, 2, 3, 4, 4), 550, 850, SoundMode.MOVIE)
        )
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var toneJob: Job? = null
    private var micJob: Job? = null
    private var finderJob: Job? = null

    private val _state = MutableStateFlow(AudioEngineState())
    val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    fun initializeEffects() {
        releaseEffects()
        var eqOk = false
        var bassOk = false
        var virtOk = false
        var hwBands = 0
        var minDb = -15
        var maxDb = 15

        // Attach to session 0 (device global mix). Catch cleanly if OEM/platform restricts session 0.
        try {
            val eq = Equalizer(0, 0).apply { enabled = true }
            equalizer = eq
            eqOk = true
            hwBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            minDb = (range[0] / 100)
            maxDb = (range[1] / 100)
        } catch (e: Exception) {
            Log.w(TAG, "Global Equalizer unavailable: ${e.message}")
        }

        try {
            val bb = BassBoost(0, 0).apply { enabled = true }
            bassBoost = bb
            bassOk = bb.strengthSupported
        } catch (e: Exception) {
            Log.w(TAG, "Global BassBoost unavailable: ${e.message}")
        }

        try {
            val virt = Virtualizer(0, 0).apply { enabled = true }
            virtualizer = virt
            virtOk = virt.strengthSupported
        } catch (e: Exception) {
            Log.w(TAG, "Global Virtualizer unavailable: ${e.message}")
        }

        val note = when {
            eqOk && bassOk && virtOk -> "Android Hardware AudioFX Active ($hwBands-band DSP mapped to 10-band studio EQ)"
            eqOk -> "Android Equalizer Active ($hwBands-band DSP); some secondary effects unsupported on this chipset"
            else -> "Global AudioFX session 0 restricted on this Android build; effects apply during built-in Audio Channel Test"
        }

        _state.value = _state.value.copy(
            eqSupported = eqOk,
            bassBoostSupported = bassOk,
            virtualizerSupported = virtOk,
            hardwareBandCount = hwBands,
            hardwareMinDb = minDb,
            hardwareMaxDb = maxDb,
            statusNote = note
        )
        applyBandsToHardware(_state.value.bands10Db)
    }

    fun applyPreset(preset: EqPreset) {
        _state.value = _state.value.copy(
            bands10Db = preset.bands10Db,
            bassBoostStrength = preset.bassBoostStrength,
            virtualizerStrength = preset.virtualizerStrength,
            spatialEnabled = preset.virtualizerStrength > 0,
            activePresetId = preset.id,
            soundMode = preset.soundMode
        )
        applyBandsToHardware(preset.bands10Db)
        applyBassStrength(preset.bassBoostStrength)
        applyVirtualizerStrength(preset.virtualizerStrength)
    }

    fun setBandDb(index: Int, db: Int) {
        if (index !in 0..9) return
        val clamped = db.coerceIn(-12, 12)
        val updated = _state.value.bands10Db.toMutableList().apply { this[index] = clamped }
        _state.value = _state.value.copy(
            bands10Db = updated,
            activePresetId = "custom_unsaved",
            soundMode = SoundMode.CUSTOM
        )
        applyBandsToHardware(updated)
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoostStrength = clamped, activePresetId = "custom_unsaved")
        applyBassStrength(clamped)
    }

    fun setSpatialAudio(enabled: Boolean, strength: Int = _state.value.virtualizerStrength.takeIf { it > 0 } ?: 650) {
        val applied = if (enabled) strength.coerceIn(0, 1000) else 0
        _state.value = _state.value.copy(
            spatialEnabled = enabled,
            virtualizerStrength = applied,
            activePresetId = "custom_unsaved"
        )
        applyVirtualizerStrength(applied)
    }

    fun setSoundMode(mode: SoundMode) {
        val matchingPreset = when (mode) {
            SoundMode.MUSIC -> BUILT_IN_PRESETS.first { it.id == "flat" }
            SoundMode.GAMING -> BUILT_IN_PRESETS.first { it.id == "gaming" }
            SoundMode.MOVIE -> BUILT_IN_PRESETS.first { it.id == "movie" }
            SoundMode.CUSTOM -> null
        }
        if (matchingPreset != null) {
            applyPreset(matchingPreset)
        } else {
            _state.value = _state.value.copy(soundMode = SoundMode.CUSTOM)
        }
    }

    fun startChannelTest(target: AudioChannelTestTarget) {
        stopChannelTest()
        if (target == AudioChannelTestTarget.NONE) return
        _state.value = _state.value.copy(activeChannelTest = target)

        toneJob = scope.launch(Dispatchers.Default) {
            playStereoToneLoop(
                target = target,
                frequencyHz = when (target) {
                    AudioChannelTestTarget.LEFT -> 440.0
                    AudioChannelTestTarget.RIGHT -> 554.37
                    AudioChannelTestTarget.BOTH -> 523.25
                    AudioChannelTestTarget.NONE -> 440.0
                },
                pulsed = false,
                durationMs = 4500L
            )
            _state.value = _state.value.copy(activeChannelTest = AudioChannelTestTarget.NONE)
        }
    }

    fun stopChannelTest() {
        toneJob?.cancel()
        toneJob = null
        _state.value = _state.value.copy(activeChannelTest = AudioChannelTestTarget.NONE)
    }

    fun startFinderChime(target: AudioChannelTestTarget) {
        stopFinderChime()
        if (target == AudioChannelTestTarget.NONE) return
        _state.value = _state.value.copy(activeChannelTest = target)
        finderJob = scope.launch(Dispatchers.Default) {
            playStereoToneLoop(
                target = target,
                frequencyHz = 1760.0,
                pulsed = true,
                durationMs = 15_000L
            )
            _state.value = _state.value.copy(activeChannelTest = AudioChannelTestTarget.NONE)
        }
    }

    fun stopFinderChime() {
        finderJob?.cancel()
        finderJob = null
        _state.value = _state.value.copy(activeChannelTest = AudioChannelTestTarget.NONE)
    }

    @SuppressLint("MissingPermission")
    fun startMicTest() {
        stopMicTest()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        micJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var scoActivated = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val btDevice = audioManager?.availableCommunicationDevices?.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    }
                    if (btDevice != null) {
                        scoActivated = audioManager?.setCommunicationDevice(btDevice) == true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (audioManager?.isBluetoothScoAvailableOffCall == true) {
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true
                        scoActivated = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BT Mic routing fallback: ${e.message}")
            }

            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord init failed: ${e.message}")
                null
            }

            if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder?.release()
                _state.value = _state.value.copy(isMicTestRunning = false, micBluetoothScoActive = false)
                return@launch
            }

            _state.value = _state.value.copy(
                isMicTestRunning = true,
                micBluetoothScoActive = scoActivated,
                micPeakDb = -90f
            )

            val buffer = ShortArray(minBuf / 2)
            try {
                recorder.startRecording()
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val norm = buffer[i] / 32768.0
                            sumSq += norm * norm
                        }
                        val rms = sqrt(sumSq / read).coerceAtLeast(1e-6)
                        val db = (20.0 * log10(rms)).toFloat().coerceIn(-90f, 0f)
                        val currentPeak = maxOf(_state.value.micPeakDb, db)
                        _state.value = _state.value.copy(micLevelDb = db, micPeakDb = currentPeak)
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mic loop ended: ${e.message}")
            } finally {
                try {
                    recorder.stop()
                    recorder.release()
                } catch (_: Exception) {
                }
                cleanupBluetoothMicRouting()
                _state.value = _state.value.copy(
                    isMicTestRunning = false,
                    micLevelDb = -90f,
                    micBluetoothScoActive = false
                )
            }
        }
    }

    fun stopMicTest() {
        micJob?.cancel()
        micJob = null
        cleanupBluetoothMicRouting()
        _state.value = _state.value.copy(
            isMicTestRunning = false,
            micLevelDb = -90f,
            micBluetoothScoActive = false
        )
    }

    private fun cleanupBluetoothMicRouting() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager?.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager?.isBluetoothScoOn = false
            }
        } catch (_: Exception) {
        }
    }

    private fun applyBandsToHardware(bands10Db: List<Int>) {
        val eq = equalizer ?: return
        try {
            val hwBands = eq.numberOfBands.toInt()
            val minMb = eq.bandLevelRange[0].toInt()
            val maxMb = eq.bandLevelRange[1].toInt()
            for (b in 0 until hwBands) {
                val centerHz = eq.getCenterFreq(b.toShort()) / 1000
                // Find closest of the 10 studio bands
                val closestIdx = BAND_FREQUENCIES_HZ.indices.minByOrNull { idx ->
                    kotlin.math.abs(BAND_FREQUENCIES_HZ[idx] - centerHz)
                } ?: 0
                val targetMb = (bands10Db[closestIdx] * 100).coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(b.toShort(), targetMb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "EQ apply failed: ${e.message}")
        }
    }

    private fun applyBassStrength(strength: Int) {
        val bb = bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost apply failed: ${e.message}")
        }
    }

    private fun applyVirtualizerStrength(strength: Int) {
        val virt = virtualizer ?: return
        try {
            if (virt.strengthSupported) {
                virt.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer apply failed: ${e.message}")
        }
    }

    private suspend fun playStereoToneLoop(
        target: AudioChannelTestTarget,
        frequencyHz: Double,
        pulsed: Boolean,
        durationMs: Long
    ) {
        val sampleRate = 44100
        val chunkFrames = sampleRate / 10 // 100ms chunks
        val pcm = ShortArray(chunkFrames * 2) // Stereo interleaved L, R
        val bufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack init failed: ${e.message}")
            return
        }

        // Attach session-scoped Equalizer fallback if global session 0 was restricted
        var localEq: Equalizer? = null
        if (equalizer == null) {
            try {
                localEq = Equalizer(0, track.audioSessionId).apply {
                    enabled = true
                    val hwBands = numberOfBands.toInt()
                    val minMb = bandLevelRange[0].toInt()
                    val maxMb = bandLevelRange[1].toInt()
                    val bands10 = _state.value.bands10Db
                    for (b in 0 until hwBands) {
                        val centerHz = getCenterFreq(b.toShort()) / 1000
                        val idx = BAND_FREQUENCIES_HZ.indices.minByOrNull { kotlin.math.abs(BAND_FREQUENCIES_HZ[it] - centerHz) } ?: 0
                        setBandLevel(b.toShort(), (bands10[idx] * 100).coerceIn(minMb, maxMb).toShort())
                    }
                }
            } catch (_: Exception) {
            }
        }

        try {
            track.play()
            var phase = 0.0
            val phaseStep = 2.0 * PI * frequencyHz / sampleRate
            val startTime = System.currentTimeMillis()
            var chunkIndex = 0
            while (coroutineContextActive() && System.currentTimeMillis() - startTime < durationMs) {
                val activePulse = !pulsed || (chunkIndex % 4 < 2) // 200ms on, 200ms off
                val leftGain = if (target == AudioChannelTestTarget.LEFT || target == AudioChannelTestTarget.BOTH) 0.45 else 0.0
                val rightGain = if (target == AudioChannelTestTarget.RIGHT || target == AudioChannelTestTarget.BOTH) 0.45 else 0.0
                for (i in 0 until chunkFrames) {
                    val sample = if (activePulse) sin(phase) else 0.0
                    phase += phaseStep
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    pcm[i * 2] = (sample * leftGain * Short.MAX_VALUE).roundToInt().toShort()
                    pcm[i * 2 + 1] = (sample * rightGain * Short.MAX_VALUE).roundToInt().toShort()
                }
                track.write(pcm, 0, pcm.size)
                chunkIndex++
                delay(5)
            }
        } catch (_: Exception) {
        } finally {
            try {
                localEq?.release()
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun coroutineContextActive(): Boolean =
        (toneJob?.isActive == true) || (finderJob?.isActive == true)

    fun releaseEffects() {
        stopChannelTest()
        stopFinderChime()
        stopMicTest()
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
