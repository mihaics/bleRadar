package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import java.util.UUID

/**
 * Device fingerprint entity - represents a unique device regardless of MAC address changes
 * Uses UUID as primary key to handle MAC randomization
 */
@Entity(
    tableName = "device_fingerprints",
    indices = [
        Index(value = ["deviceUuid"], unique = true),
        Index(value = ["deviceType"]),
        Index(value = ["manufacturer"]),
        Index(value = ["isKnownTracker"]),
        Index(value = ["suspiciousScore"]),
        Index(value = ["lastSeen"]),
        Index(value = ["fingerprintHash"])
    ]
)
data class DeviceFingerprint(
    @PrimaryKey val deviceUuid: String, // Generated UUID for this device
    val deviceName: String? = null,
    val manufacturer: String? = null,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val isKnownTracker: Boolean = false,
    val trackerType: String? = null,
    val firstSeen: Long,
    val lastSeen: Long,
    val suspiciousScore: Float = 0f,
    val followingScore: Float = 0f,
    val totalDetections: Int = 0,
    val macAddressCount: Int = 0, // Number of different MAC addresses seen
    val lastAlertTime: Long = 0L,
    val isTracked: Boolean = false,
    val isIgnored: Boolean = false,
    val notes: String? = null,
    val fingerprintHash: String, // Hash of identifying characteristics
    val confidence: Float = 0f // How confident we are in this fingerprint
)

/**
 * MAC address entity - tracks all MAC addresses seen for a device
 * Multiple MACs can belong to the same device fingerprint
 */
@Entity(
    tableName = "device_mac_addresses",
    primaryKeys = ["deviceUuid", "macAddress"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceFingerprint::class,
            parentColumns = ["deviceUuid"],
            childColumns = ["deviceUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["macAddress"]),
        Index(value = ["firstSeen"]),
        Index(value = ["lastSeen"]),
        Index(value = ["isActive"])
    ]
)
data class DeviceMacAddress(
    val deviceUuid: String,
    val macAddress: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val detectionCount: Int = 0,
    val isActive: Boolean = true, // Is this MAC currently being used
    val addressType: MacAddressType = MacAddressType.UNKNOWN
)

/**
 * Device advertising data - stores fingerprinting characteristics
 */
@Entity(
    tableName = "device_advertising_data",
    primaryKeys = ["deviceUuid", "dataType"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceFingerprint::class,
            parentColumns = ["deviceUuid"],
            childColumns = ["deviceUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["dataType"]),
        Index(value = ["lastSeen"])
    ]
)
data class DeviceAdvertisingData(
    val deviceUuid: String,
    val dataType: AdvertisingDataType,
    val dataValue: String, // JSON or hex string
    val firstSeen: Long,
    val lastSeen: Long,
    val occurrenceCount: Int = 1,
    val isStable: Boolean = false // Does this data remain constant over time
)

/**
 * Fingerprint patterns - stores the actual fingerprinting algorithm data
 */
@Entity(
    tableName = "fingerprint_patterns",
    primaryKeys = ["deviceUuid", "patternType"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceFingerprint::class,
            parentColumns = ["deviceUuid"],
            childColumns = ["deviceUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceUuid"]),
        Index(value = ["patternType"]),
        Index(value = ["confidence"]),
        Index(value = ["lastUpdated"])
    ]
)
data class FingerprintPattern(
    val deviceUuid: String,
    val patternType: FingerprintPatternType,
    val patternData: String, // JSON data specific to pattern type
    val confidence: Float,
    val lastUpdated: Long,
    val isMatchable: Boolean = true // Can this pattern be used for matching
)

/**
 * Device detections - now references UUID instead of MAC
 */
@Entity(
    tableName = "fingerprint_detections",
    foreignKeys = [
        ForeignKey(
            entity = DeviceFingerprint::class,
            parentColumns = ["deviceUuid"],
            childColumns = ["deviceUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceUuid", "timestamp"]),
        Index(value = ["macAddress"]),
        Index(value = ["timestamp"]),
        Index(value = ["clusterId"])
    ]
)
data class FingerprintDetection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceUuid: String,
    val macAddress: String, // The MAC address used in this detection
    val timestamp: Long,
    val rssi: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val clusterId: String? = null, // Reference to location cluster
    val advertisingInterval: Long? = null, // Time since last detection
    val txPower: Int? = null // If available from advertising data
)

/**
 * Device clustering correlation - tracks which devices appear together
 */
@Entity(
    tableName = "device_correlations",
    primaryKeys = ["deviceUuid1", "deviceUuid2"],
    indices = [
        Index(value = ["deviceUuid1"]),
        Index(value = ["deviceUuid2"]),
        Index(value = ["correlation"]),
        Index(value = ["lastSeen"])
    ]
)
data class DeviceCorrelation(
    val deviceUuid1: String,
    val deviceUuid2: String,
    val correlation: Float, // 0.0 to 1.0 correlation strength
    val cooccurrenceCount: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val correlationType: CorrelationType
)

// Enums for better type safety
enum class MacAddressType {
    UNKNOWN,
    STATIC,
    RANDOM_STATIC,
    RANDOM_RESOLVABLE,
    RANDOM_NON_RESOLVABLE
}

enum class AdvertisingDataType {
    SERVICE_UUIDS,
    MANUFACTURER_DATA,
    SERVICE_DATA,
    DEVICE_NAME,
    TX_POWER,
    APPEARANCE,
    FLAGS,
    ADVERTISING_INTERVAL,
    SOLICITATION_UUIDS
}

enum class FingerprintPatternType {
    SERVICE_UUID_PATTERN,
    MANUFACTURER_DATA_PATTERN,
    ADVERTISING_TIMING_PATTERN,
    SIGNAL_STRENGTH_PATTERN,
    MOVEMENT_PATTERN,
    LOCATION_PATTERN,
    COMBINED_SIGNATURE
}

enum class CorrelationType {
    TEMPORAL, // Devices seen at same time
    SPATIAL,  // Devices seen at same location
    MOVEMENT, // Devices moving together
    UNKNOWN
}

/**
 * Fingerprint matching results - stores results of fingerprint matching
 */
@Entity(
    tableName = "fingerprint_matches",
    indices = [
        Index(value = ["candidateDeviceUuid"]),
        Index(value = ["existingDeviceUuid"]),
        Index(value = ["matchScore"]),
        Index(value = ["timestamp"])
    ]
)
data class FingerprintMatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidateDeviceUuid: String, // New device being evaluated
    val existingDeviceUuid: String,  // Existing device it might match
    val matchScore: Float,           // 0.0 to 1.0 match confidence
    val timestamp: Long,
    val matchingPatterns: String,    // JSON list of patterns that matched
    val isConfirmed: Boolean = false // Has this match been confirmed
)