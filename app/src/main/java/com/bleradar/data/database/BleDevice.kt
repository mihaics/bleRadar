package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ble_devices")
data class BleDevice(
    @PrimaryKey val deviceAddress: String,
    val deviceName: String?,
    val rssi: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val isIgnored: Boolean = false,
    val label: String? = null,
    val isTracked: Boolean = false,
    val followingScore: Float = 0f,
    val deviceType: String? = null,
    val manufacturer: String? = null,
    val services: String? = null,
    val detectionCount: Int = 0,
    val consecutiveDetections: Int = 0,
    val maxConsecutiveDetections: Int = 0,
    val averageRssi: Float = 0f,
    val rssiVariation: Float = 0f,
    val lastMovementTime: Long = 0,
    val isStationary: Boolean = false,
    val detectionPattern: String? = null,
    val suspiciousActivityScore: Float = 0f,
    val lastAlertTime: Long = 0,
    val isKnownTracker: Boolean = false,
    val trackerType: String? = null,
    val advertisingInterval: Long = 0,
    val rotatingIdentifier: Boolean = false
)