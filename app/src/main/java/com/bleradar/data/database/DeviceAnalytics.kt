package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_analytics",
    foreignKeys = [
        ForeignKey(
            entity = BleDevice::class,
            parentColumns = ["deviceAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deviceAddress", "date"])]
)
data class DeviceAnalytics(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val date: String, // YYYY-MM-DD format
    val detectionsCount: Int,
    val averageRssi: Float,
    val minRssi: Int,
    val maxRssi: Int,
    val rssiVariance: Float,
    val followingScore: Float,
    val suspiciousScore: Float,
    val firstSeenTime: Long,
    val lastSeenTime: Long,
    val activeDuration: Long, // milliseconds
    val distanceTraveled: Float, // meters
    val averageSpeed: Float, // m/s
    val maxSpeed: Float,
    val locationsVisited: Int,
    val timeStationary: Long, // milliseconds
    val timeMoving: Long, // milliseconds
    val consecutiveDetections: Int,
    val detectionGaps: Int, // number of gaps in detection
    val averageDetectionInterval: Long, // milliseconds
    val advertisingChanges: Int, // identifier rotation count
    val alertsTriggered: Int,
    val userInteractions: Int // ignore, label, track actions
)