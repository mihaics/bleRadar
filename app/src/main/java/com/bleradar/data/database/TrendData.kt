package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trend_data")
data class TrendData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricName: String,
    val date: String, // YYYY-MM-DD format
    val timeframe: String, // "hourly", "daily", "weekly", "monthly"
    val value: Float,
    val count: Int = 1,
    val category: String? = null, // device_type, location, etc.
    val subCategory: String? = null,
    val metadata: String? = null // JSON string for additional data
)