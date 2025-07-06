package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detection_patterns",
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
data class DetectionPattern(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val timestamp: Long,
    val patternType: String,
    val confidence: Float,
    val metadata: String? = null
)

enum class PatternType(val value: String) {
    FOLLOWING("following"),
    STATIONARY("stationary"),
    PERIODIC("periodic"),
    SUDDEN_APPEARANCE("sudden_appearance"),
    PROXIMITY_CORRELATION("proximity_correlation"),
    MOVEMENT_SYNC("movement_sync"),
    SIGNAL_STRENGTH_PATTERN("signal_strength_pattern"),
    ADVERTISING_PATTERN("advertising_pattern"),
    KNOWN_TRACKER_SIGNATURE("known_tracker_signature"),
    SUSPICIOUS_TIMING("suspicious_timing")
}