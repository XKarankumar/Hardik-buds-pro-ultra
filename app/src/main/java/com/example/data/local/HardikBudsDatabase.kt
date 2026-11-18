package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BatteryHistoryEntity::class,
        ConnectionEventEntity::class,
        RegisteredDeviceEntity::class,
        CustomEqPresetEntity::class,
        SecurityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HardikBudsDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun connectionEventDao(): ConnectionEventDao
    abstract fun registeredDeviceDao(): RegisteredDeviceDao
    abstract fun customEqPresetDao(): CustomEqPresetDao
    abstract fun securityLogDao(): SecurityLogDao

    companion object {
        @Volatile
        private var INSTANCE: HardikBudsDatabase? = null

        fun getInstance(context: Context): HardikBudsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HardikBudsDatabase::class.java,
                    "hardik_buds_pro.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
