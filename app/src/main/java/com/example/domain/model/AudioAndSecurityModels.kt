package com.example.domain.model

enum class SoundMode(val label: String) {
    MUSIC("Music Mode"),
    GAMING("Gaming Mode"),
    MOVIE("Movie Mode"),
    CUSTOM("Custom Studio")
}

data class EqPreset(
    val id: String,
    val name: String,
    val bands10Db: List<Int>, // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz (-12 to +12 dB)
    val bassBoostStrength: Int, // 0..1000
    val virtualizerStrength: Int, // 0..1000
    val soundMode: SoundMode,
    val isCustom: Boolean = false
)

enum class AudioChannelTestTarget {
    NONE,
    LEFT,
    RIGHT,
    BOTH
}

data class AudioEngineState(
    val eqSupported: Boolean = false,
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val statusNote: String = "Probing device audio effects...",
    val hardwareBandCount: Int = 0,
    val hardwareMinDb: Int = -15,
    val hardwareMaxDb: Int = 15,
    val bands10Db: List<Int> = List(10) { 0 },
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val spatialEnabled: Boolean = false,
    val activePresetId: String = "flat",
    val soundMode: SoundMode = SoundMode.MUSIC,
    val activeChannelTest: AudioChannelTestTarget = AudioChannelTestTarget.NONE,
    val isMicTestRunning: Boolean = false,
    val micLevelDb: Float = -90f,
    val micPeakDb: Float = -90f,
    val micBluetoothScoActive: Boolean = false
)

data class SecurityState(
    val isPinConfigured: Boolean = false,
    val pinLength: Int = 4,
    val isBiometricHardwareAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isAppLocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val isRecoveryConfigured: Boolean = false,
    val maskMacAddressesInUi: Boolean = false,
    val alertOnUnauthorizedAccess: Boolean = true,
    val unauthorizedAttemptCount: Int = 0
)

data class UserProfile(
    val displayName: String = "Hardik Listener",
    val preferredBrandLabel: String = "Universal"
)

data class NotificationPreferences(
    val masterEnabled: Boolean = true,
    val onConnected: Boolean = true,
    val onDisconnected: Boolean = true,
    val onLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val onChargingChanged: Boolean = true,
    val onBluetoothDisabled: Boolean = true,
    val onConnectionLost: Boolean = true,
    val onUnauthorizedAccess: Boolean = true,
    val customSoundUri: String? = null,
    val customSoundName: String = "Default System Sound"
)
