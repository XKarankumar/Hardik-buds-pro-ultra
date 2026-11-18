package com.example

import android.app.Application
import com.example.data.audio.AudioEffectsManager
import com.example.data.bluetooth.BluetoothHardwareManager
import com.example.data.local.BudsLocalRepository
import com.example.data.local.HardikBudsDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.location.LastKnownLocationProvider
import com.example.data.notifications.SmartNotificationManager
import com.example.data.security.KeystoreSecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HardikBudsApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: HardikBudsDatabase by lazy { HardikBudsDatabase.getInstance(this) }
    val localRepository: BudsLocalRepository by lazy { BudsLocalRepository(database) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val bluetoothManager: BluetoothHardwareManager by lazy { BluetoothHardwareManager(this) }
    val audioEffectsManager: AudioEffectsManager by lazy { AudioEffectsManager(this, applicationScope) }
    val securityManager: KeystoreSecurityManager by lazy { KeystoreSecurityManager(this) }
    val locationProvider: LastKnownLocationProvider by lazy { LastKnownLocationProvider(this) }
    val notificationManager: SmartNotificationManager by lazy { SmartNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
        notificationManager.ensureChannelsCreated()
        audioEffectsManager.initializeEffects()
        bluetoothManager.updateAudioEffectSupport(audioEffectsManager.state.value.eqSupported)
    }
}
