package com.bleradar.ui.model

import com.bleradar.data.database.DeviceFingerprint
import com.bleradar.data.database.DeviceMacAddress
import com.bleradar.data.database.DeviceType

/**
 * UI model for displaying device information with fingerprinting data
 */
data class DeviceDisplayModel(
    val deviceUuid: String,
    val displayName: String,
    val deviceType: DeviceType,
    val manufacturer: String?,
    val isKnownTracker: Boolean,
    val trackerType: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val suspiciousScore: Float,
    val followingScore: Float,
    val totalDetections: Int,
    val macAddresses: List<DeviceMacAddress>,
    val isTracked: Boolean,
    val isIgnored: Boolean,
    val notes: String?,
    val confidence: Float,
    val primaryMacAddress: String, // Most recently used MAC
    val macAddressCount: Int
) {
    
    companion object {
        fun fromFingerprint(
            fingerprint: DeviceFingerprint,
            macAddresses: List<DeviceMacAddress>
        ): DeviceDisplayModel {
            val activeMac = macAddresses.firstOrNull { it.isActive }
            val primaryMac = activeMac?.macAddress ?: macAddresses.maxByOrNull { it.lastSeen }?.macAddress ?: "Unknown"
            val displayName = when {
                !fingerprint.deviceName.isNullOrBlank() -> fingerprint.deviceName
                fingerprint.isKnownTracker -> "${fingerprint.trackerType} Tracker"
                !fingerprint.manufacturer.isNullOrBlank() -> "${fingerprint.manufacturer} Device"
                else -> "Unknown Device"
            }
            
            return DeviceDisplayModel(
                deviceUuid = fingerprint.deviceUuid,
                displayName = displayName,
                deviceType = fingerprint.deviceType,
                manufacturer = fingerprint.manufacturer,
                isKnownTracker = fingerprint.isKnownTracker,
                trackerType = fingerprint.trackerType,
                firstSeen = fingerprint.firstSeen,
                lastSeen = fingerprint.lastSeen,
                suspiciousScore = fingerprint.suspiciousScore,
                followingScore = fingerprint.followingScore,
                totalDetections = fingerprint.totalDetections,
                macAddresses = macAddresses,
                isTracked = fingerprint.isTracked,
                isIgnored = fingerprint.isIgnored,
                notes = fingerprint.notes,
                confidence = fingerprint.confidence,
                primaryMacAddress = primaryMac,
                macAddressCount = fingerprint.macAddressCount
            )
        }
    }
    
    fun isNew(): Boolean {
        val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return firstSeen > last24Hours
    }
    
    fun getThreatLevel(): ThreatLevel {
        return when {
            suspiciousScore >= 0.8f -> ThreatLevel.CRITICAL
            suspiciousScore >= 0.6f -> ThreatLevel.HIGH
            suspiciousScore >= 0.4f -> ThreatLevel.MEDIUM
            suspiciousScore >= 0.2f -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
    }
    
    fun getDisplayIcon(): String {
        return when (deviceType) {
            DeviceType.AIRTAG -> "🏷️"
            DeviceType.SMARTTAG -> "🏷️"
            DeviceType.TILE -> "🏷️"
            DeviceType.AIRPODS -> "🎧"
            DeviceType.EARBUDS -> "🎧"
            DeviceType.PHONE -> "📱"
            DeviceType.WATCH -> "⌚"
            DeviceType.CAR_BLUETOOTH -> "🚗"
            DeviceType.FITNESS_TRACKER -> "🏃"
            DeviceType.SMART_SPEAKER -> "🔊"
            DeviceType.BEACON -> "📡"
            DeviceType.SUSPECTED_TRACKER -> "⚠️"
            DeviceType.TRACKED -> "✅"
            DeviceType.UNKNOWN -> "❓"
        }
    }
}

enum class ThreatLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class DeviceDisplaySortOption {
    LAST_SEEN,
    FIRST_SEEN,
    NAME,
    THREAT_LEVEL,
    DEVICE_TYPE,
    MAC_COUNT
}