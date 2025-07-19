package com.bleradar.analysis

import com.bleradar.data.database.*
import com.bleradar.data.database.RiskLevel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SmartThreatDetector @Inject constructor(
    private val deviceDao: OptimizedDeviceDao,
    private val clusterDao: LocationClusterDao,
    private val detectionDao: DeviceClusterDetectionDao,
    private val evidenceDao: TrackingEvidenceDao
) {
    
    companion object {
        private const val CLUSTER_RADIUS_METERS = 50.0
        private const val FOLLOWING_LOCATION_THRESHOLD = 3 // Device seen in 3+ locations
        private const val PERSISTENCE_THRESHOLD_HOURS = 2.0 // Device persistent for 2+ hours
        private const val MULTI_LOCATION_CRITICAL_THRESHOLD = 5 // 5+ locations = critical
        private const val TIME_WINDOW_HOURS = 24
        
        // Known tracker signatures
        private val KNOWN_TRACKER_SERVICES = mapOf(
            "FE9F" to "AirTag",
            "FD5A" to "SmartTag",
            "FDCC" to "SmartTag2", 
            "FEED" to "Tile",
            "FE2C" to "Nordic"
        )
        
        private val KNOWN_TRACKER_MANUFACTURERS = mapOf(
            "Apple" to "AirTag",
            "Samsung" to "SmartTag",
            "Tile" to "Tile"
        )
    }
    
    suspend fun analyzeDevice(deviceAddress: String): ThreatAnalysisResult {
        val device = deviceDao.getDevice(deviceAddress) 
            ?: return ThreatAnalysisResult.notFound(deviceAddress)
        
        val detections = detectionDao.getDetectionsForDevice(deviceAddress)
        
        if (detections.isEmpty()) {
            return ThreatAnalysisResult.insufficientData(deviceAddress)
        }
        
        val results = mutableListOf<ThreatFactor>()
        
        // 1. Check for known tracker signatures (highest weight)
        val knownTrackerFactor = analyzeKnownTracker(device)
        results.add(knownTrackerFactor)
        
        // 2. Multi-location tracking (key indicator)
        val multiLocationFactor = analyzeMultiLocation(detections)
        results.add(multiLocationFactor)
        
        // 3. Persistence in single location (legitimate device check)
        val persistenceFactor = analyzePersistence(detections)
        results.add(persistenceFactor)
        
        // 4. Device classification (context)
        val classificationFactor = classifyDevice(device, detections)
        results.add(classificationFactor)
        
        return combineFactors(deviceAddress, results)
    }
    
    private suspend fun analyzeKnownTracker(device: OptimizedDevice): ThreatFactor {
        var confidence = 0f
        var trackerType = "Unknown"
        
        // Check manufacturer
        device.manufacturer?.let { manufacturer ->
            KNOWN_TRACKER_MANUFACTURERS[manufacturer]?.let { type ->
                confidence += 0.9f
                trackerType = type
            }
        }
        
        // Check service UUIDs
        device.services?.let { services ->
            KNOWN_TRACKER_SERVICES.forEach { (serviceId, type) ->
                if (services.contains(serviceId, ignoreCase = true)) {
                    confidence += 0.95f
                    trackerType = type
                }
            }
        }
        
        return ThreatFactor(
            type = ThreatType.KNOWN_TRACKER,
            confidence = confidence.coerceIn(0f, 1f),
            weight = 0.4f, // Highest weight
            details = "TrackerType: $trackerType, Manufacturer: ${device.manufacturer}",
            actionRequired = confidence > 0.7f
        )
    }
    
    private suspend fun analyzeMultiLocation(detections: List<DeviceClusterDetection>): ThreatFactor {
        if (detections.size < 2) {
            return ThreatFactor.empty(ThreatType.MULTI_LOCATION)
        }
        
        val locationCount = detections.size
        val timeSpan = (detections.maxOf { it.lastSeen } - detections.minOf { it.firstSeen }) / (1000 * 60 * 60).toDouble()
        
        // Calculate confidence based on location count and time span
        val confidence = when {
            locationCount >= MULTI_LOCATION_CRITICAL_THRESHOLD -> 0.95f
            locationCount >= FOLLOWING_LOCATION_THRESHOLD && timeSpan > 1.0 -> 0.8f
            locationCount >= 2 && timeSpan > 0.5 -> 0.6f
            else -> 0.2f
        }
        
        return ThreatFactor(
            type = ThreatType.MULTI_LOCATION,
            confidence = confidence,
            weight = 0.35f, // Second highest weight
            details = "Locations: $locationCount, TimeSpan: ${timeSpan.toInt()}h",
            actionRequired = confidence > 0.6f
        )
    }
    
    private suspend fun analyzePersistence(detections: List<DeviceClusterDetection>): ThreatFactor {
        if (detections.isEmpty()) {
            return ThreatFactor.empty(ThreatType.PERSISTENCE)
        }
        
        // Find the location with highest persistence
        val maxPersistence = detections.maxByOrNull { it.persistenceScore }
        val maxPersistenceHours = maxPersistence?.let { detection ->
            (detection.lastSeen - detection.firstSeen) / (1000 * 60 * 60).toDouble()
        } ?: 0.0
        
        // High persistence in single location suggests legitimate device (car, work equipment)
        val confidence = when {
            maxPersistenceHours > 8.0 -> 0.2f // Likely legitimate (low threat)
            maxPersistenceHours > 4.0 -> 0.4f // Possibly legitimate
            maxPersistenceHours > 2.0 -> 0.6f // Suspicious
            else -> 0.8f // Very suspicious if not persistent
        }
        
        return ThreatFactor(
            type = ThreatType.PERSISTENCE,
            confidence = confidence,
            weight = 0.15f,
            details = "MaxPersistence: ${maxPersistenceHours.toInt()}h",
            actionRequired = false // This is more for context
        )
    }
    
    private suspend fun classifyDevice(device: OptimizedDevice, detections: List<DeviceClusterDetection>): ThreatFactor {
        val classification = classifyDeviceType(device, detections)
        
        val confidence = when (classification) {
            DeviceType.AIRTAG, DeviceType.SMARTTAG, DeviceType.TILE -> 0.9f
            DeviceType.SUSPECTED_TRACKER -> 0.7f
            DeviceType.CAR_BLUETOOTH -> 0.1f // Low threat
            DeviceType.AIRPODS, DeviceType.EARBUDS -> 0.2f // Low threat
            DeviceType.TRACKED -> 0.0f // No threat
            else -> 0.5f // Unknown
        }
        
        return ThreatFactor(
            type = ThreatType.DEVICE_CLASSIFICATION,
            confidence = confidence,
            weight = 0.1f,
            details = "Classification: $classification",
            actionRequired = confidence > 0.7f
        )
    }
    
    private fun classifyDeviceType(device: OptimizedDevice, detections: List<DeviceClusterDetection>): DeviceType {
        // Check if already classified
        if (device.deviceType != DeviceType.UNKNOWN) {
            return device.deviceType
        }
        
        // Check known tracker signatures
        device.manufacturer?.let { manufacturer ->
            when (manufacturer) {
                "Apple" -> return DeviceType.AIRTAG
                "Samsung" -> return DeviceType.SMARTTAG
                "Tile" -> return DeviceType.TILE
                else -> { /* No match */ }
            }
        }
        
        // Check device name patterns
        device.deviceName?.let { name ->
            when {
                name.contains("AirTag", ignoreCase = true) -> return DeviceType.AIRTAG
                name.contains("SmartTag", ignoreCase = true) -> return DeviceType.SMARTTAG
                name.contains("Tile", ignoreCase = true) -> return DeviceType.TILE
                name.contains("AirPods", ignoreCase = true) -> return DeviceType.AIRPODS
                name.contains("Galaxy Buds", ignoreCase = true) -> return DeviceType.EARBUDS
                name.contains("Car", ignoreCase = true) -> return DeviceType.CAR_BLUETOOTH
                name.contains("BMW", ignoreCase = true) -> return DeviceType.CAR_BLUETOOTH
                name.contains("Audi", ignoreCase = true) -> return DeviceType.CAR_BLUETOOTH
                name.contains("Mercedes", ignoreCase = true) -> return DeviceType.CAR_BLUETOOTH
                else -> { /* No match */ }
            }
        }
        
        // Behavioral classification
        val locationCount = detections.size
        val avgRssi = detections.map { it.avgRssi }.average()
        
        return when {
            locationCount == 1 && avgRssi > -40 -> DeviceType.CAR_BLUETOOTH // Strong signal, single location
            locationCount > 3 && avgRssi in -70.0..-50.0 -> DeviceType.SUSPECTED_TRACKER // Multiple locations, medium signal
            else -> DeviceType.UNKNOWN
        }
    }
    
    private fun combineFactors(deviceAddress: String, factors: List<ThreatFactor>): ThreatAnalysisResult {
        val weightedScore = factors.sumOf { (it.confidence * it.weight).toDouble() }.toFloat()
        val maxPossibleScore = factors.sumOf { it.weight.toDouble() }.toFloat()
        val normalizedScore = if (maxPossibleScore > 0) weightedScore / maxPossibleScore else 0f
        
        val riskLevel = when {
            normalizedScore >= 0.8f -> RiskLevel.CRITICAL
            normalizedScore >= 0.6f -> RiskLevel.HIGH
            normalizedScore >= 0.4f -> RiskLevel.MEDIUM
            normalizedScore >= 0.2f -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
        
        val actionRequired = factors.any { it.actionRequired }
        
        return ThreatAnalysisResult(
            deviceAddress = deviceAddress,
            riskLevel = riskLevel,
            confidence = normalizedScore,
            factors = factors,
            actionRequired = actionRequired,
            timestamp = System.currentTimeMillis(),
            recommendation = generateRecommendation(riskLevel, factors)
        )
    }
    
    private fun generateRecommendation(riskLevel: RiskLevel, factors: List<ThreatFactor>): String {
        val knownTracker = factors.find { it.type == ThreatType.KNOWN_TRACKER && it.confidence > 0.7f }
        val multiLocation = factors.find { it.type == ThreatType.MULTI_LOCATION && it.confidence > 0.6f }
        
        return when {
            knownTracker != null && multiLocation != null -> 
                "⚠️ CRITICAL: Known tracker detected in multiple locations. This device may be tracking you."
            knownTracker != null -> 
                "⚠️ Known tracker detected. Monitor for location changes."
            multiLocation != null -> 
                "⚠️ Device following you across multiple locations. Could be tracking device."
            riskLevel == RiskLevel.MEDIUM -> 
                "⚠️ Potentially suspicious device. Continue monitoring."
            riskLevel == RiskLevel.LOW -> 
                "ℹ️ Low risk device. Likely legitimate (car, earbuds, etc.)."
            else -> 
                "✅ Safe device. No tracking behavior detected."
        }
    }
    
    // Utility function to generate cluster ID from coordinates
    fun generateClusterId(lat: Double, lon: Double): String {
        val roundedLat = (lat * 1000).toInt() / 1000.0 // Round to ~100m precision
        val roundedLon = (lon * 1000).toInt() / 1000.0
        return "${roundedLat}_${roundedLon}"
    }
    
    // Check if two coordinates are within clustering distance
    fun isWithinClusterRadius(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val distance = calculateDistance(lat1, lon1, lat2, lon2)
        return distance <= CLUSTER_RADIUS_METERS
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}

// Simplified threat analysis result
data class ThreatAnalysisResult(
    val deviceAddress: String,
    val riskLevel: RiskLevel,
    val confidence: Float,
    val factors: List<ThreatFactor>,
    val actionRequired: Boolean,
    val timestamp: Long,
    val recommendation: String
) {
    companion object {
        fun notFound(deviceAddress: String) = ThreatAnalysisResult(
            deviceAddress = deviceAddress,
            riskLevel = RiskLevel.SAFE,
            confidence = 0f,
            factors = emptyList(),
            actionRequired = false,
            timestamp = System.currentTimeMillis(),
            recommendation = "Device not found"
        )
        
        fun insufficientData(deviceAddress: String) = ThreatAnalysisResult(
            deviceAddress = deviceAddress,
            riskLevel = RiskLevel.SAFE,
            confidence = 0f,
            factors = emptyList(),
            actionRequired = false,
            timestamp = System.currentTimeMillis(),
            recommendation = "Insufficient data for analysis"
        )
    }
}

data class ThreatFactor(
    val type: ThreatType,
    val confidence: Float,
    val weight: Float,
    val details: String,
    val actionRequired: Boolean
) {
    companion object {
        fun empty(type: ThreatType) = ThreatFactor(
            type = type,
            confidence = 0f,
            weight = 0f,
            details = "No data",
            actionRequired = false
        )
    }
}

enum class ThreatType {
    KNOWN_TRACKER,
    MULTI_LOCATION,
    PERSISTENCE,
    DEVICE_CLASSIFICATION
}