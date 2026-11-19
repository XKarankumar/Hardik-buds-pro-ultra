package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HardikBudsApplication
import com.example.data.audio.AudioEffectsManager
import com.example.data.bluetooth.BluetoothEvent
import com.example.data.local.AppPreferencesState
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.ConnectionEventEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityLogEntity
import com.example.data.location.PhoneLocationSnapshot
import com.example.data.security.KeystoreSecurityManager
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.AudioChannelTestTarget
import com.example.domain.model.AudioEngineState
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.DiscoveredBluetoothDevice
import com.example.domain.model.EqPreset
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.SecurityState
import com.example.domain.model.SoundMode
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DynamicIslandStatus(
    val title: String,
    val subtitle: String,
    val isAlert: Boolean = false
)

data class ConnectionPopupState(
    val visible: Boolean = false,
    val deviceName: String = "",
    val brandName: String = "",
    val combinedBatteryPercent: Int? = null,
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val batteryNote: String = "",
    val isUiPreviewOnly: Boolean = false
)

class BudsCompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HardikBudsApplication
    private val bt = app.bluetoothManager
    private val audio = app.audioEffectsManager
    private val repo = app.localRepository
    private val prefsRepo = app.preferencesRepository
    private val security = app.securityManager
    private val location = app.locationProvider
    private val notifier = app.notificationManager

    val isBluetoothHardwareSupported: StateFlow<Boolean> = bt.isHardwareSupported
    val isBluetoothEnabled: StateFlow<Boolean> = bt.isBluetoothEnabled
    val isScanning: StateFlow<Boolean> = bt.isScanning
    val scanStatusMessage: StateFlow<String?> = bt.scanStatusMessage
    val bondedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = bt.bondedDevices
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = bt.discoveredDevices
    val connectedEarbud: StateFlow<ConnectedEarbudState?> = bt.connectedEarbud

    val audioState: StateFlow<AudioEngineState> = audio.state
    val securityState: StateFlow<SecurityState> = security.state

    val preferences: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    val batteryHistory: StateFlow<List<BatteryHistoryEntity>> = repo.batteryHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionEvents: StateFlow<List<ConnectionEventEntity>> = repo.connectionEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredDevices: StateFlow<List<RegisteredDeviceEntity>> = repo.registeredDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEqPresets: StateFlow<List<EqPreset>> = repo.customEqPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs: StateFlow<List<SecurityLogEntity>> = repo.securityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastPhoneLocation = MutableStateFlow<PhoneLocationSnapshot?>(null)
    val lastPhoneLocation: StateFlow<PhoneLocationSnapshot?> = _lastPhoneLocation.asStateFlow()

    private val _dynamicIsland = MutableStateFlow<DynamicIslandStatus?>(null)
    val dynamicIsland: StateFlow<DynamicIslandStatus?> = _dynamicIsland.asStateFlow()

    private val _connectionPopup = MutableStateFlow(ConnectionPopupState())
    val connectionPopup: StateFlow<ConnectionPopupState> = _connectionPopup.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _pinFeedback = MutableStateFlow<String?>(null)
    val pinFeedback: StateFlow<String?> = _pinFeedback.asStateFlow()

    init {
        bt.startMonitoring()
        refreshPhoneLocation()
        observeBluetoothEvents()
    }

    private fun observeBluetoothEvents() {
        viewModelScope.launch {
            bt.events.collect { event ->
                val currentPrefs = prefsRepo.preferencesFlow.first()
                when (event) {
                    is BluetoothEvent.DeviceConnected -> {
                        val dev = event.device
                        val loc = location.getLastKnownPhoneLocation()
                        if (loc != null) _lastPhoneLocation.value = loc

                        repo.recordConnectionEvent(
                            ConnectionEventEntity(
                                deviceAddress = dev.address,
                                deviceName = dev.deviceName,
                                brandName = dev.brand.displayName,
                                eventType = "CONNECTED",
                                latitude = loc?.latitude,
                                longitude = loc?.longitude,
                                locationAccuracyMeters = loc?.accuracyMeters,
                                details = "Connected (${dev.batterySourceNote})"
                            )
                        )

                        repo.getRegisteredDevice(dev.address)?.let { existing ->
                            repo.registerOwnerDevice(
                                existing.copy(
                                    lastConnectedAt = System.currentTimeMillis(),
                                    lastLatitude = loc?.latitude ?: existing.lastLatitude,
                                    lastLongitude = loc?.longitude ?: existing.lastLongitude,
                                    lastLocationAccuracyMeters = loc?.accuracyMeters ?: existing.lastLocationAccuracyMeters,
                                    lastLocationTimestamp = loc?.timestampMillis ?: existing.lastLocationTimestamp
                                )
                            )
                        }

                        if (dev.combinedBatteryPercent != null) {
                            repo.recordBatterySample(
                                BatteryHistoryEntity(
                                    deviceAddress = dev.address,
                                    deviceName = dev.deviceName,
                                    combinedPercent = dev.combinedBatteryPercent,
                                    leftPercent = dev.leftBatteryPercent,
                                    rightPercent = dev.rightBatteryPercent,
                                    casePercent = dev.caseBatteryPercent,
                                    isCharging = dev.isCharging,
                                    sourceLabel = dev.batterySourceNote
                                )
                            )
                            notifier.notifyLowBattery(dev.deviceName, dev.combinedBatteryPercent, currentPrefs.notifications)
                        }

                        notifier.notifyConnected(dev.deviceName, dev.combinedBatteryPercent, currentPrefs.notifications)

                        if (currentPrefs.dynamicIslandBannerEnabled) {
                            val bat = dev.combinedBatteryPercent?.let { "$it%" } ?: "Battery N/A"
                            _dynamicIsland.value = DynamicIslandStatus(
                                title = dev.deviceName,
                                subtitle = "Connected • $bat"
                            )
                        }

                        if (currentPrefs.connectionPopupEnabled) {
                            _connectionPopup.value = ConnectionPopupState(
                                visible = true,
                                deviceName = dev.deviceName,
                                brandName = dev.brand.displayName,
                                combinedBatteryPercent = dev.combinedBatteryPercent,
                                leftBatteryPercent = dev.leftBatteryPercent,
                                rightBatteryPercent = dev.rightBatteryPercent,
                                caseBatteryPercent = dev.caseBatteryPercent,
                                batteryNote = dev.batterySourceNote,
                                isUiPreviewOnly = false
                            )
                        }
                    }

                    is BluetoothEvent.DeviceDisconnected -> {
                        val loc = location.getLastKnownPhoneLocation()
                        if (loc != null) _lastPhoneLocation.value = loc
                        val type = if (event.unexpected) "CONNECTION_LOST" else "DISCONNECTED"
                        repo.recordConnectionEvent(
                            ConnectionEventEntity(
                                deviceAddress = event.address,
                                deviceName = event.name,
                                brandName = "Bluetooth",
                                eventType = type,
                                latitude = loc?.latitude,
                                longitude = loc?.longitude,
                                locationAccuracyMeters = loc?.accuracyMeters,
                                details = if (event.unexpected) "Unexpected disconnection" else "Disconnected"
                            )
                        )
                        notifier.notifyDisconnected(event.name, event.unexpected, currentPrefs.notifications)
                        if (currentPrefs.dynamicIslandBannerEnabled) {
                            _dynamicIsland.value = DynamicIslandStatus(
                                title = event.name,
                                subtitle = if (event.unexpected) "Connection Lost" else "Disconnected",
                                isAlert = event.unexpected
                            )
                        }
                    }

                    is BluetoothEvent.BatteryUpdated -> {
                        repo.recordBatterySample(
                            BatteryHistoryEntity(
                                deviceAddress = event.address,
                                deviceName = event.name,
                                combinedPercent = event.percent,
                                leftPercent = null,
                                rightPercent = null,
                                casePercent = null,
                                isCharging = null,
                                sourceLabel = "Bluetooth Battery Level Broadcast"
                            )
                        )
                        notifier.notifyLowBattery(event.name, event.percent, currentPrefs.notifications)
                    }

                    is BluetoothEvent.RadioStateChanged -> {
                        if (!event.enabled) {
                            notifier.notifyBluetoothDisabled(currentPrefs.notifications)
                            _dynamicIsland.value = DynamicIslandStatus(
                                title = "Bluetooth Off",
                                subtitle = "Enable radio to monitor earbuds",
                                isAlert = true
                            )
                        } else {
                            _dynamicIsland.value = DynamicIslandStatus(
                                title = "Bluetooth Ready",
                                subtitle = "Scanning bonded audio profiles"
                            )
                        }
                    }

                    is BluetoothEvent.PairingFailed -> {
                        _uiMessage.value = "Pairing failed for ${event.deviceName}: ${event.reason}"
                    }

                    is BluetoothEvent.ErrorMessage -> {
                        _uiMessage.value = event.message
                    }
                }
            }
        }
    }

    // Bluetooth operations
    fun startScan() = bt.startScan()
    fun stopScan() = bt.stopScan()
    fun refreshBluetooth() = bt.refreshAllStates()
    fun pairDevice(address: String) = bt.pairDevice(address)
    fun connectToDevice(address: String) = bt.connectToDevice(address)
    fun disconnectCurrent() = bt.disconnectCurrentDevice()
    fun openBluetoothSettings() = bt.openSystemBluetoothSettings()

    // Connection popup & Dynamic Island
    fun dismissConnectionPopup() {
        _connectionPopup.value = _connectionPopup.value.copy(visible = false)
    }

    fun dismissDynamicIsland() {
        _dynamicIsland.value = null
    }

    fun openAnimationPreviewPopup() {
        val connected = connectedEarbud.value
        _connectionPopup.value = ConnectionPopupState(
            visible = true,
            deviceName = connected?.deviceName ?: "Animation Preview (No Hardware Connected)",
            brandName = connected?.brand?.displayName ?: "UI Design Preview Only",
            combinedBatteryPercent = connected?.combinedBatteryPercent,
            leftBatteryPercent = connected?.leftBatteryPercent,
            rightBatteryPercent = connected?.rightBatteryPercent,
            caseBatteryPercent = connected?.caseBatteryPercent,
            batteryNote = connected?.batterySourceNote
                ?: "Previewing connection animation layout. Real telemetry populates only from connected Bluetooth earbuds.",
            isUiPreviewOnly = connected == null
        )
    }

    // Audio Studio actions
    fun applyPreset(preset: EqPreset) = audio.applyPreset(preset)
    fun setEqBand(index: Int, db: Int) = audio.setBandDb(index, db)
    fun setBassBoost(strength: Int) = audio.setBassBoost(strength)
    fun setSpatialAudio(enabled: Boolean, strength: Int = 650) = audio.setSpatialAudio(enabled, strength)
    fun setSoundMode(mode: SoundMode) = audio.setSoundMode(mode)
    fun startChannelTest(target: AudioChannelTestTarget) = audio.startChannelTest(target)
    fun stopChannelTest() = audio.stopChannelTest()
    fun startMicTest() = audio.startMicTest()
    fun stopMicTest() = audio.stopMicTest()

    fun saveCustomPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val s = audio.state.value
        viewModelScope.launch {
            repo.saveCustomEqPreset(
                EqPreset(
                    id = "custom_${UUID.randomUUID()}",
                    name = trimmed,
                    bands10Db = s.bands10Db,
                    bassBoostStrength = s.bassBoostStrength,
                    virtualizerStrength = s.virtualizerStrength,
                    soundMode = s.soundMode,
                    isCustom = true
                )
            )
            _uiMessage.value = "Saved custom preset \"$trimmed\""
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch { repo.deleteCustomEqPreset(id) }
    }

    fun applyAndSaveAiPreset(preset: EqPreset) {
        audio.applyPreset(preset)
        viewModelScope.launch {
            repo.saveCustomEqPreset(preset)
            _uiMessage.value = "Applied & saved Gemini preset \"${preset.name}\""
        }
    }

    // Find My Earbuds
    fun refreshPhoneLocation() {
        _lastPhoneLocation.value = location.getLastKnownPhoneLocation()
    }

    fun startFinderSound(target: AudioChannelTestTarget) {
        bt.triggerBleImmediateAlert(true)
        audio.startFinderChime(target)
    }

    fun stopFinderSound() {
        bt.triggerBleImmediateAlert(false)
        audio.stopFinderChime()
    }

    // Security & Owner Registration
    fun registerCurrentAsOwner(alias: String) {
        val dev = connectedEarbud.value ?: return
        val loc = location.getLastKnownPhoneLocation()
        viewModelScope.launch {
            repo.registerOwnerDevice(
                RegisteredDeviceEntity(
                    address = dev.address,
                    deviceName = dev.deviceName,
                    brandName = dev.brand.displayName,
                    customAlias = alias.trim().ifEmpty { dev.deviceName },
                    lastConnectedAt = System.currentTimeMillis(),
                    lastLatitude = loc?.latitude,
                    lastLongitude = loc?.longitude,
                    lastLocationAccuracyMeters = loc?.accuracyMeters,
                    lastLocationTimestamp = loc?.timestampMillis
                )
            )
            repo.addSecurityLog(
                eventType = "OWNER_REGISTERED",
                description = "Registered ${dev.deviceName} (${dev.address}) as owner device.",
                isAlert = false
            )
            _uiMessage.value = "Owner device registered."
        }
    }

    fun unregisterOwnerDevice(address: String) {
        viewModelScope.launch {
            repo.unregisterOwnerDevice(address)
            repo.addSecurityLog("OWNER_REMOVED", "Removed owner registration for $address", false)
        }
    }

    fun configurePin(pin: String, recoveryPhrase: String?) {
        val ok = security.configurePin(pin, recoveryPhrase)
        if (ok) {
            viewModelScope.launch {
                repo.addSecurityLog("PIN_CONFIGURED", "${pin.length}-digit Keystore-backed PIN enabled.", false)
            }
            _uiMessage.value = "${pin.length}-digit PIN configured securely."
        } else {
            _uiMessage.value = "PIN must be 4 or 6 numeric digits."
        }
    }

    fun disablePin() {
        security.disablePin()
        viewModelScope.launch {
            repo.addSecurityLog("PIN_DISABLED", "Companion PIN protection disabled by user.", false)
        }
        _uiMessage.value = "Companion PIN disabled."
    }

    fun verifyPin(pin: String) {
        when (val res = security.verifyPin(pin)) {
            is KeystoreSecurityManager.PinVerifyResult.Success -> {
                _pinFeedback.value = null
            }
            is KeystoreSecurityManager.PinVerifyResult.Invalid -> {
                _pinFeedback.value = "Incorrect PIN. ${res.remainingBeforeLockout} attempts remaining before lockout."
                handleUnauthorizedAttempt(security.state.value.failedAttempts)
            }
            is KeystoreSecurityManager.PinVerifyResult.LockedOut -> {
                _pinFeedback.value = "Too many attempts. Locked out for ${res.remainingSeconds}s."
                handleUnauthorizedAttempt(security.state.value.failedAttempts)
            }
        }
    }

    private fun handleUnauthorizedAttempt(attempts: Int) {
        viewModelScope.launch {
            repo.addSecurityLog(
                eventType = "UNAUTHORIZED_ACCESS",
                description = "Failed PIN attempt #$attempts detected.",
                isAlert = true
            )
            val currentPrefs = prefsRepo.preferencesFlow.first()
            if (security.state.value.alertOnUnauthorizedAccess) {
                notifier.notifyUnauthorizedAccess(attempts, currentPrefs.notifications)
            }
        }
    }

    fun recoverPinWithPhrase(phrase: String, newPin: String): Boolean {
        val ok = security.recoverWithPhrase(phrase, newPin)
        if (ok) {
            _pinFeedback.value = null
            viewModelScope.launch {
                repo.addSecurityLog("PIN_RECOVERED", "PIN reset via secure recovery phrase.", false)
            }
        } else {
            _pinFeedback.value = "Invalid recovery phrase or PIN format."
        }
        return ok
    }

    fun onBiometricSuccess() {
        security.unlockViaBiometric()
        _pinFeedback.value = null
    }

    fun setBiometricEnabled(enabled: Boolean) = security.setBiometricEnabled(enabled)
    fun lockAppNow() = security.lockAppNow()
    fun setAlertOnUnauthorized(enabled: Boolean) = security.setAlertOnUnauthorizedAccess(enabled)

    // Preferences & Settings
    fun setThemeStyle(style: AppThemeStyle) = viewModelScope.launch { prefsRepo.setThemeStyle(style) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setAccentPalette(accent: AccentPalette) = viewModelScope.launch { prefsRepo.setAccentPalette(accent) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { prefsRepo.setLanguage(language) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    fun setConnectionPopupEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setConnectionPopupEnabled(enabled) }
    fun setDynamicIslandEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDynamicIslandEnabled(enabled) }
    fun setMaskMacAddresses(masked: Boolean) = viewModelScope.launch { prefsRepo.setMaskMacAddresses(masked) }
    fun updateProfile(profile: UserProfile) = viewModelScope.launch { prefsRepo.updateUserProfile(profile) }
    fun updateNotifications(notif: NotificationPreferences) = viewModelScope.launch {
        prefsRepo.updateNotifications(notif)
        notifier.ensureChannelsCreated(notif.customSoundUri)
    }

    fun clearLogs() = viewModelScope.launch {
        repo.clearAllLocalLogs()
        _uiMessage.value = "History logs cleared."
    }

    fun resetAllSettings() = viewModelScope.launch {
        prefsRepo.resetAll()
        repo.resetEverything()
        security.disablePin()
        audio.applyPreset(AudioEffectsManager.BUILT_IN_PRESETS.first())
        _uiMessage.value = "All settings and local records reset."
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun formatMacForDisplay(address: String, mask: Boolean): String {
        if (!mask || address.length < 8) return address
        return "XX:XX:XX:XX:${address.takeLast(5)}"
    }

    override fun onCleared() {
        super.onCleared()
        bt.stopMonitoring()
        audio.releaseEffects()
    }
}
