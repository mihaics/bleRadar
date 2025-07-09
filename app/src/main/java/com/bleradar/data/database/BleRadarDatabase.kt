package com.bleradar.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        BleDevice::class, BleDetection::class, LocationRecord::class, DetectionPattern::class, 
        AnalyticsSnapshot::class, DeviceAnalytics::class, TrendData::class,
        OptimizedDevice::class, LocationCluster::class, DeviceClusterDetection::class,
        TrackingEvidence::class, DeviceSummary::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BleRadarDatabase : RoomDatabase() {
    abstract fun bleDeviceDao(): BleDeviceDao
    abstract fun bleDetectionDao(): BleDetectionDao
    abstract fun locationDao(): LocationDao
    abstract fun detectionPatternDao(): DetectionPatternDao
    abstract fun analyticsSnapshotDao(): AnalyticsSnapshotDao
    abstract fun deviceAnalyticsDao(): DeviceAnalyticsDao
    abstract fun trendDataDao(): TrendDataDao
    
    // New optimized DAOs
    abstract fun optimizedDeviceDao(): OptimizedDeviceDao
    abstract fun locationClusterDao(): LocationClusterDao
    abstract fun deviceClusterDetectionDao(): DeviceClusterDetectionDao
    abstract fun trackingEvidenceDao(): TrackingEvidenceDao
    abstract fun deviceSummaryDao(): DeviceSummaryDao

    companion object {
        @Volatile
        private var INSTANCE: BleRadarDatabase? = null

        fun getDatabase(context: Context): BleRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BleRadarDatabase::class.java,
                    "ble_radar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}