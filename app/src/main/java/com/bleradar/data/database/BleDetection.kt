package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ble_detections",
    foreignKeys = [
        ForeignKey(
            entity = BleDevice::class,
            parentColumns = ["deviceAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deviceAddress", "timestamp"])]
)
data class BleDetection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val timestamp: Long,
    val rssi: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null
)