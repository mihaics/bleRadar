package com.bleradar.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [BleDevice::class, BleDetection::class, LocationRecord::class],
    version = 1,
    exportSchema = false
)
abstract class BleRadarDatabase : RoomDatabase() {
    abstract fun bleDeviceDao(): BleDeviceDao
    abstract fun bleDetectionDao(): BleDetectionDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: BleRadarDatabase? = null

        fun getDatabase(context: Context): BleRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BleRadarDatabase::class.java,
                    "ble_radar_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}