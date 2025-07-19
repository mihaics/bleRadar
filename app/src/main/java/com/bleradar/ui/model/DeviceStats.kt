package com.bleradar.ui.model

/**
 * Statistics model for device list displays
 */
data class DeviceStats(
    val totalDevices: Int,
    val newDevices: Int,
    val knownTrackers: Int,
    val suspiciousDevices: Int,
    val trackedDevices: Int
)