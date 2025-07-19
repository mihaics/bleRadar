package com.bleradar.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

// Optimized Device table - stores device characteristics only
@Entity(
    tableName = "optimized_devices",
    indices = [
        Index(value = ["deviceAddress"], unique = true),
        Index(value = ["manufacturer"]),
        Index(value = ["deviceType"]),
        Index(value = ["suspiciousScore"]),
        Index(value = ["lastSeen"])
    ]
)
data class OptimizedDevice(
    @PrimaryKey val deviceAddress: String,
    val deviceName: String? = null,
    val manufacturer: String? = null,
    val services: String? = null,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val isKnownTracker: Boolean = false,
    val trackerType: String? = null,
    val firstSeen: Long,
    val lastSeen: Long,
    val suspiciousScore: Float = 0f,
    val followingScore: Float = 0f,
    val multiLocationScore: Float = 0f,
    val totalDetections: Int = 0,
    val lastAlertTime: Long = 0L,
    val isTracked: Boolean = false,
    val notes: String? = null
)

// Location clusters - groups nearby detections (50m radius)
@Entity(
    tableName = "location_clusters",
    indices = [
        Index(value = ["clusterId"], unique = true),
        Index(value = ["centerLat", "centerLon"]),
        Index(value = ["firstSeen"]),
        Index(value = ["lastSeen"])
    ]
)
data class LocationCluster(
    @PrimaryKey val clusterId: String, // Generated hash of lat/lon rounded to 50m
    val centerLat: Double,
    val centerLon: Double,
    val radiusMeters: Float = 50f,
    val firstSeen: Long,
    val lastSeen: Long,
    val deviceCount: Int = 0,
    val detectionCount: Int = 0,
    val description: String? = null, // e.g., "Home", "Work", "Mall"
    val isUserLocation: Boolean = false
)

// Device detections at specific clusters - much more efficient
@Entity(
    tableName = "device_cluster_detections",
    primaryKeys = ["deviceAddress", "clusterId"],
    foreignKeys = [
        ForeignKey(
            entity = OptimizedDevice::class,
            parentColumns = ["deviceAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationCluster::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceAddress"]),
        Index(value = ["clusterId"]),
        Index(value = ["firstSeen"]),
        Index(value = ["lastSeen"]),
        Index(value = ["detectionCount"])
    ]
)
data class DeviceClusterDetection(
    val deviceAddress: String,
    val clusterId: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val detectionCount: Int,
    val avgRssi: Float,
    val minRssi: Int,
    val maxRssi: Int,
    val rssiVariance: Float,
    val persistenceScore: Float = 0f, // How long device stayed in this cluster
    val isCurrentLocation: Boolean = false // Is user currently at this location
)

// Multi-location tracking evidence
@Entity(
    tableName = "tracking_evidence",
    primaryKeys = ["deviceAddress", "evidenceType"],
    foreignKeys = [
        ForeignKey(
            entity = OptimizedDevice::class,
            parentColumns = ["deviceAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceAddress"]),
        Index(value = ["evidenceType"]),
        Index(value = ["confidence"]),
        Index(value = ["lastUpdated"])
    ]
)
data class TrackingEvidence(
    val deviceAddress: String,
    val evidenceType: EvidenceType,
    val confidence: Float,
    val lastUpdated: Long,
    val locationCount: Int = 0,
    val timeSpanHours: Float = 0f,
    val metadata: String? = null
)

// Simplified device analytics - only key metrics
@Entity(
    tableName = "device_summary",
    primaryKeys = ["deviceAddress", "date"],
    foreignKeys = [
        ForeignKey(
            entity = OptimizedDevice::class,
            parentColumns = ["deviceAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceAddress"]),
        Index(value = ["date"]),
        Index(value = ["riskLevel"])
    ]
)
data class DeviceSummary(
    val deviceAddress: String,
    val date: String, // YYYY-MM-DD format
    val detectionsCount: Int,
    val locationsCount: Int,
    val riskLevel: RiskLevel,
    val avgRssi: Float,
    val followingScore: Float,
    val multiLocationScore: Float,
    val alertsTriggered: Int,
    val notes: String? = null
)

// Enums for better type safety
enum class DeviceType {
    UNKNOWN,
    AIRTAG,
    SMARTTAG,
    TILE,
    AIRPODS,
    EARBUDS,
    PHONE,
    WATCH,
    CAR_BLUETOOTH,
    FITNESS_TRACKER,
    SMART_SPEAKER,
    BEACON,
    SUSPECTED_TRACKER,
    TRACKED
}

enum class EvidenceType {
    MULTI_LOCATION,
    PERSISTENT_FOLLOWING,
    KNOWN_TRACKER_SIGNATURE,
    SUSPICIOUS_TIMING,
    SIGNAL_PATTERN,
    MOVEMENT_CORRELATION,
    RAPID_LOCATION_CHANGE
}

enum class RiskLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}