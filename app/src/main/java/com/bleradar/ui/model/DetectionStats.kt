package com.bleradar.ui.model

/**
 * Statistics model for device detection data
 */
data class DetectionStats(
    val totalDetections: Int,
    val uniqueDays: Int,
    val averageRssi: Double,
    val maxRssi: Int,
    val minRssi: Int,
    val locationCount: Int
)