package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_records")
data class LocationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val provider: String
)