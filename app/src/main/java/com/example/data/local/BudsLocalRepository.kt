package com.example.data.local

import com.example.domain.model.EqPreset
import com.example.domain.model.SoundMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudsLocalRepository(private val db: HardikBudsDatabase) {

    val batteryHistory: Flow<List<BatteryHistoryEntity>> =
        db.batteryHistoryDao().observeRecentHistory()

    val connectionEvents: Flow<List<ConnectionEventEntity>> =
        db.connectionEventDao().observeEvents()

    val registeredDevices: Flow<List<RegisteredDeviceEntity>> =
        db.registeredDeviceDao().observeRegisteredDevices()

    val customEqPresets: Flow<List<EqPreset>> =
        db.customEqPresetDao().observePresets().map { list ->
            list.map { entity ->
                val bands = entity.bandsCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .let { parsed ->
                        if (parsed.size == 10) parsed else List(10) { 0 }
                    }
                val mode = SoundMode.entries.firstOrNull { it.name == entity.soundModeName }
                    ?: SoundMode.CUSTOM
                EqPreset(
                    id = entity.id,
                    name = entity.name,
                    bands10Db = bands,
                    bassBoostStrength = entity.bassBoostStrength,
                    virtualizerStrength = entity.virtualizerStrength,
                    soundMode = mode,
                    isCustom = true
                )
            }
        }

    val securityLogs: Flow<List<SecurityLogEntity>> =
        db.securityLogDao().observeLogs()

    suspend fun recordBatterySample(entry: BatteryHistoryEntity) {
        db.batteryHistoryDao().insert(entry)
    }

    suspend fun recordConnectionEvent(event: ConnectionEventEntity) {
        db.connectionEventDao().insert(event)
    }

    suspend fun registerOwnerDevice(device: RegisteredDeviceEntity) {
        db.registeredDeviceDao().upsert(device)
    }

    suspend fun getRegisteredDevice(address: String): RegisteredDeviceEntity? =
        db.registeredDeviceDao().getByAddress(address)

    suspend fun unregisterOwnerDevice(address: String) {
        db.registeredDeviceDao().deleteByAddress(address)
    }

    suspend fun saveCustomEqPreset(preset: EqPreset) {
        db.customEqPresetDao().upsert(
            CustomEqPresetEntity(
                id = preset.id,
                name = preset.name,
                bandsCsv = preset.bands10Db.joinToString(","),
                bassBoostStrength = preset.bassBoostStrength,
                virtualizerStrength = preset.virtualizerStrength,
                soundModeName = preset.soundMode.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCustomEqPreset(id: String) {
        db.customEqPresetDao().deleteById(id)
    }

    suspend fun addSecurityLog(eventType: String, description: String, isAlert: Boolean) {
        db.securityLogDao().insert(
            SecurityLogEntity(
                eventType = eventType,
                description = description,
                isAlert = isAlert
            )
        )
    }

    suspend fun clearAllLocalLogs() {
        db.batteryHistoryDao().clearAll()
        db.connectionEventDao().clearAll()
        db.securityLogDao().clearAll()
    }

    suspend fun resetEverything() {
        db.batteryHistoryDao().clearAll()
        db.connectionEventDao().clearAll()
        db.registeredDeviceDao().clearAll()
        db.customEqPresetDao().clearAll()
        db.securityLogDao().clearAll()
    }
}
