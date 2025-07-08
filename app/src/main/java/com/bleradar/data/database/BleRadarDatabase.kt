package com.bleradar.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [BleDevice::class, BleDetection::class, LocationRecord::class, DetectionPattern::class, AnalyticsSnapshot::class, DeviceAnalytics::class, TrendData::class],
    version = 3,
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

    companion object {
        @Volatile
        private var INSTANCE: BleRadarDatabase? = null
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create analytics_snapshots table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `analytics_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `totalDevices` INTEGER NOT NULL,
                        `trackedDevices` INTEGER NOT NULL,
                        `suspiciousDevices` INTEGER NOT NULL,
                        `knownTrackers` INTEGER NOT NULL,
                        `activeDetections` INTEGER NOT NULL,
                        `averageRssi` REAL NOT NULL,
                        `averageFollowingScore` REAL NOT NULL,
                        `averageSuspiciousScore` REAL NOT NULL,
                        `locationUpdates` INTEGER NOT NULL,
                        `alertsTriggered` INTEGER NOT NULL,
                        `newDevicesDiscovered` INTEGER NOT NULL,
                        `devicesTurnedOff` INTEGER NOT NULL,
                        `averageDetectionDistance` REAL NOT NULL,
                        `maxDetectionDistance` REAL NOT NULL,
                        `minDetectionDistance` REAL NOT NULL,
                        `scanningDuration` INTEGER NOT NULL,
                        `backgroundTime` INTEGER NOT NULL,
                        `foregroundTime` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create device_analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `device_analytics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `deviceAddress` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `detectionsCount` INTEGER NOT NULL,
                        `averageRssi` REAL NOT NULL,
                        `minRssi` INTEGER NOT NULL,
                        `maxRssi` INTEGER NOT NULL,
                        `rssiVariance` REAL NOT NULL,
                        `followingScore` REAL NOT NULL,
                        `suspiciousScore` REAL NOT NULL,
                        `firstSeenTime` INTEGER NOT NULL,
                        `lastSeenTime` INTEGER NOT NULL,
                        `activeDuration` INTEGER NOT NULL,
                        `distanceTraveled` REAL NOT NULL,
                        `averageSpeed` REAL NOT NULL,
                        `maxSpeed` REAL NOT NULL,
                        `locationsVisited` INTEGER NOT NULL,
                        `timeStationary` INTEGER NOT NULL,
                        `timeMoving` INTEGER NOT NULL,
                        `consecutiveDetections` INTEGER NOT NULL,
                        `detectionGaps` INTEGER NOT NULL,
                        `averageDetectionInterval` INTEGER NOT NULL,
                        `advertisingChanges` INTEGER NOT NULL,
                        `alertsTriggered` INTEGER NOT NULL,
                        `userInteractions` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create trend_data table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `trend_data` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `metricName` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `timeframe` TEXT NOT NULL,
                        `value` REAL NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): BleRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BleRadarDatabase::class.java,
                    "ble_radar_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}