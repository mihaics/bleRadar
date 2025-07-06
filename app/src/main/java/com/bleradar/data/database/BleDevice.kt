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
    val services: String? = null
)