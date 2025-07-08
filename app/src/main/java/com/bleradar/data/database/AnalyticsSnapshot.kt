package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_snapshots")
data class AnalyticsSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalDevices: Int,
    val trackedDevices: Int,
    val suspiciousDevices: Int,
    val knownTrackers: Int,
    val activeDetections: Int,
    val averageRssi: Float,
    val averageFollowingScore: Float,
    val averageSuspiciousScore: Float,
    val locationUpdates: Int,
    val alertsTriggered: Int,
    val newDevicesDiscovered: Int,
    val devicesTurnedOff: Int,
    val averageDetectionDistance: Float,
    val maxDetectionDistance: Float,
    val minDetectionDistance: Float,
    val batteryLevel: Int? = null,
    val scanningDuration: Long = 0,
    val backgroundTime: Long = 0,
    val foregroundTime: Long = 0
)