package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryHistoryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentHistory(): Flow<List<BatteryHistoryEntity>>

    @Query("SELECT * FROM battery_history WHERE deviceAddress = :address ORDER BY timestamp DESC LIMIT 50")
    fun observeHistoryForDevice(address: String): Flow<List<BatteryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryHistoryEntity)

    @Query("DELETE FROM battery_history")
    suspend fun clearAll()
}

@Dao
interface ConnectionEventDao {
    @Query("SELECT * FROM connection_events ORDER BY timestamp DESC LIMIT 100")
    fun observeEvents(): Flow<List<ConnectionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ConnectionEventEntity)

    @Query("DELETE FROM connection_events")
    suspend fun clearAll()
}

@Dao
interface RegisteredDeviceDao {
    @Query("SELECT * FROM registered_devices ORDER BY lastConnectedAt DESC, registeredAt DESC")
    fun observeRegisteredDevices(): Flow<List<RegisteredDeviceEntity>>

    @Query("SELECT * FROM registered_devices WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): RegisteredDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: RegisteredDeviceEntity)

    @Query("DELETE FROM registered_devices WHERE address = :address")
    suspend fun deleteByAddress(address: String)

    @Query("DELETE FROM registered_devices")
    suspend fun clearAll()
}

@Dao
interface CustomEqPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY updatedAt DESC")
    fun observePresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_eq_presets")
    suspend fun clearAll()
}

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeLogs(): Flow<List<SecurityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SecurityLogEntity)

    @Query("DELETE FROM security_logs")
    suspend fun clearAll()
}
