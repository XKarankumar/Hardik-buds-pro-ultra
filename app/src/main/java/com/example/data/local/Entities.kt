package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val combinedPercent: Int?,
    val leftPercent: Int?,
    val rightPercent: Int?,
    val casePercent: Int?,
    val isCharging: Boolean?,
    val sourceLabel: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "connection_events")
data class ConnectionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val brandName: String,
    val eventType: String, // CONNECTED, DISCONNECTED, CONNECTION_LOST, REGISTERED, BONDED
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracyMeters: Float?,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "registered_devices")
data class RegisteredDeviceEntity(
    @PrimaryKey val address: String,
    val deviceName: String,
    val brandName: String,
    val customAlias: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationAccuracyMeters: Float? = null,
    val lastLocationTimestamp: Long? = null
)

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandsCsv: String,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val soundModeName: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val description: String,
    val isAlert: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
