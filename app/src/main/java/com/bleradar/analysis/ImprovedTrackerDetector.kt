package com.bleradar.analysis

import com.bleradar.data.database.*
import com.bleradar.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class ImprovedTrackerDetector @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    
    companion object {
        private const val FOLLOWING_THRESHOLD = 0.7f
        private const val SUSPICIOUS_THRESHOLD = 0.6f
        private const val MIN_DETECTIONS_FOR_ANALYSIS = 3
        private const val TIME_WINDOW_HOURS = 6 // Reduced from 24 hours
        private const val SHORT_TIME_WINDOW_MINUTES = 15 // Reduced from 30 minutes
        
        private const val STATIONARY_RADIUS_METERS = 100.0 // Consider this as stationary
        private const val MIN_MOVEMENT_DISTANCE = 200.0 // Minimum distance to consider as movement
        private const val HOME_STATIONARY_TIME_THRESHOLD = 60 * 60 * 1000L // 1 hour in ms
        
        private val KNOWN_TRACKER_SERVICES = setOf(
            "0000FE9F-0000-1000-8000-00805F9B34FB", // Apple AirTag
            "0000FD5A-0000-1000-8000-00805F9B34FB", // Samsung SmartTag
            "0000FEED-0000-1000-8000-00805F9B34FB"  // Tile
        )
        
        private val TRACKER_NAME_PATTERNS = setOf(
            "AirTag", "SmartTag", "Tile", "Galaxy SmartTag", "SmartTag+", "SmartTag2"
        )
    }
    
    suspend fun analyzeDeviceForImprovedTracking(deviceAddress: String): ImprovedTrackerResult {
        val currentTime = System.currentTimeMillis()
        val timeWindow = currentTime - (TIME_WINDOW_HOURS * 60 * 60 * 1000)
        val shortTimeWindow = currentTime - (SHORT_TIME_WINDOW_MINUTES * 60 * 1000)
        
        val device = deviceRepository.getDevice(deviceAddress) ?: return ImprovedTrackerResult.notFound()
        val detections = deviceRepository.getDetectionsForDeviceSince(deviceAddress, timeWindow).first()
        val recentDetections = deviceRepository.getDetectionsForDeviceSince(deviceAddress, shortTimeWindow).first()
        val userLocations = deviceRepository.getLocationsSince(timeWindow).first()
        
        if (detections.size < MIN_DETECTIONS_FOR_ANALYSIS) {
            return ImprovedTrackerResult.insufficientData()
        }
        
        // Check if this is a known tracker type
        val isKnownTracker = isKnownTrackerType(device)
        
        // Analyze user movement patterns
        val userMovementAnalysis = analyzeUserMovement(userLocations)
        
        // Analyze device behavior based on user movement
        val trackingScore = if (userMovementAnalysis.isUserStationary) {
            analyzeStationaryScenario(device, detections, userLocations, userMovementAnalysis)
        } else {
            analyzeMobileScenario(device, detections, userLocations, userMovementAnalysis)
        }
        
        // Calculate final confidence with known tracker bonus
        val finalConfidence = if (isKnownTracker) {
            // Known trackers get lower threshold but still require movement correlation
            trackingScore * 0.7f // Reduce false positive sensitivity for known trackers
        } else {
            trackingScore
        }
        
        val riskLevel = when {
            finalConfidence >= FOLLOWING_THRESHOLD -> RiskLevel.HIGH
            finalConfidence >= SUSPICIOUS_THRESHOLD -> RiskLevel.MEDIUM
            finalConfidence >= 0.3f -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
        
        return ImprovedTrackerResult(
            deviceAddress = deviceAddress,
            riskLevel = riskLevel,
            confidence = finalConfidence,
            isKnownTracker = isKnownTracker,
            userMovementPattern = userMovementAnalysis,
            detectionCount = detections.size,
            recentDetectionCount = recentDetections.size,
            reasoning = generateReasoning(isKnownTracker, userMovementAnalysis, trackingScore, finalConfidence)
        )
    }
    
    private fun isKnownTrackerType(device: BleDevice): Boolean {
        // Check by service UUID
        if (device.services?.let { services ->
            KNOWN_TRACKER_SERVICES.any { knownService -> 
                services.contains(knownService, ignoreCase = true) 
            }
        } == true) {
            return true
        }
        
        // Check by device name
        val deviceName = device.deviceName?.lowercase() ?: ""
        return TRACKER_NAME_PATTERNS.any { pattern ->
            deviceName.contains(pattern.lowercase())
        }
    }
    
    private fun analyzeUserMovement(userLocations: List<LocationRecord>): UserMovementAnalysis {
        if (userLocations.size < 2) {
            return UserMovementAnalysis(
                isUserStationary = true,
                totalMovementDistance = 0.0,
                stationaryTime = 0L,
                movementCount = 0,
                averageStationaryDuration = 0L,
                homeLocation = userLocations.firstOrNull()
            )
        }
        
        val sortedLocations = userLocations.sortedBy { it.timestamp }
        var totalDistance = 0.0
        var movementCount = 0
        var totalStationaryTime = 0L
        var lastMovementTime: Long? = null
        
        for (i in 1 until sortedLocations.size) {
            val prev = sortedLocations[i - 1]
            val curr = sortedLocations[i]
            
            val distance = calculateDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            
            if (distance > MIN_MOVEMENT_DISTANCE) {
                totalDistance += distance
                movementCount++
                
                // Calculate stationary time between movements
                if (lastMovementTime != null) {
                    totalStationaryTime += prev.timestamp - lastMovementTime
                }
                lastMovementTime = curr.timestamp
            }
        }
        
        val isStationary = totalDistance < STATIONARY_RADIUS_METERS || movementCount == 0
        val averageStationaryDuration = if (movementCount > 0) totalStationaryTime / movementCount else 0L
        
        // Determine home location (most frequent location cluster)
        val homeLocation = findHomeLocation(sortedLocations)
        
        return UserMovementAnalysis(
            isUserStationary = isStationary,
            totalMovementDistance = totalDistance,
            stationaryTime = totalStationaryTime,
            movementCount = movementCount,
            averageStationaryDuration = averageStationaryDuration,
            homeLocation = homeLocation
        )
    }
    
    private fun analyzeStationaryScenario(
        device: BleDevice,
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>,
        userMovement: UserMovementAnalysis
    ): Float {
        // If user is stationary, check if this could be a legitimate tracker at home/work
        if (userMovement.stationaryTime > HOME_STATIONARY_TIME_THRESHOLD) {
            // User has been stationary for a long time, likely at home/work
            // Reduce suspicion for known trackers in this scenario
            return if (isKnownTrackerType(device)) {
                0.2f // Very low suspicion for known trackers at stationary locations
            } else {
                0.4f // Slightly higher for unknown devices
            }
        }
        
        // Short stationary period - could be legitimate
        return 0.3f
    }
    
    private fun analyzeMobileScenario(
        device: BleDevice,
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>,
        userMovement: UserMovementAnalysis
    ): Float {
        if (userLocations.isEmpty() || detections.isEmpty()) return 0f
        
        var correlationScore = 0f
        var correlationCount = 0
        
        // Analyze movement correlation
        val sortedDetections = detections.sortedBy { it.timestamp }
        val sortedLocations = userLocations.sortedBy { it.timestamp }
        
        for (detection in sortedDetections) {
            val nearestLocation = sortedLocations.minByOrNull { 
                abs(it.timestamp - detection.timestamp) 
            }
            
            if (nearestLocation != null) {
                val distance = calculateDistance(
                    detection.latitude, detection.longitude,
                    nearestLocation.latitude, nearestLocation.longitude
                )
                
                val timeDiff = abs(detection.timestamp - nearestLocation.timestamp) / (1000 * 60) // minutes
                
                // Score based on proximity and timing
                val score = when {
                    distance < 50 && timeDiff < 5 -> 1f // Very close in space and time
                    distance < 100 && timeDiff < 10 -> 0.8f
                    distance < 200 && timeDiff < 15 -> 0.6f
                    distance < 500 && timeDiff < 30 -> 0.4f
                    else -> 0.1f
                }
                
                correlationScore += score
                correlationCount++
            }
        }
        
        val averageCorrelation = if (correlationCount > 0) correlationScore / correlationCount else 0f
        
        // Boost score if device follows user across multiple locations
        val locationVariety = calculateLocationVariety(detections)
        val varietyBonus = if (locationVariety > 2) 0.2f else 0f
        
        return (averageCorrelation + varietyBonus).coerceAtMost(1f)
    }
    
    private fun findHomeLocation(locations: List<LocationRecord>): LocationRecord? {
        if (locations.isEmpty()) return null
        
        // Simple clustering - find location where user spends most time
        val locationClusters = mutableMapOf<String, Pair<LocationRecord, Long>>()
        
        for (location in locations) {
            val key = "${(location.latitude * 1000).toInt()}_${(location.longitude * 1000).toInt()}"
            val existing = locationClusters[key]
            
            if (existing == null) {
                locationClusters[key] = location to 1L
            } else {
                locationClusters[key] = existing.first to (existing.second + 1L)
            }
        }
        
        return locationClusters.values.maxByOrNull { it.second }?.first
    }
    
    private fun calculateLocationVariety(detections: List<BleDetection>): Int {
        val uniqueLocations = mutableSetOf<String>()
        
        for (detection in detections) {
            val locationKey = "${(detection.latitude * 100).toInt()}_${(detection.longitude * 100).toInt()}"
            uniqueLocations.add(locationKey)
        }
        
        return uniqueLocations.size
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    private fun generateReasoning(
        isKnownTracker: Boolean,
        userMovement: UserMovementAnalysis,
        trackingScore: Float,
        finalConfidence: Float
    ): String {
        return buildString {
            if (isKnownTracker) {
                append("Known tracker device detected. ")
            }
            
            if (userMovement.isUserStationary) {
                append("User appears stationary (possibly at home/work). ")
                if (userMovement.stationaryTime > HOME_STATIONARY_TIME_THRESHOLD) {
                    append("Extended stationary period suggests legitimate location. ")
                }
            } else {
                append("User movement detected. ")
                append("Movement correlation score: ${(trackingScore * 100).toInt()}%. ")
            }
            
            when (finalConfidence) {
                in 0f..0.3f -> append("Low tracking risk.")
                in 0.3f..0.6f -> append("Moderate tracking risk - monitor device.")
                in 0.6f..0.8f -> append("High tracking risk - investigate device.")
                else -> append("Very high tracking risk - take action!")
            }
        }
    }
}

data class ImprovedTrackerResult(
    val deviceAddress: String,
    val riskLevel: RiskLevel,
    val confidence: Float,
    val isKnownTracker: Boolean,
    val userMovementPattern: UserMovementAnalysis,
    val detectionCount: Int,
    val recentDetectionCount: Int,
    val reasoning: String
) {
    companion object {
        fun notFound() = ImprovedTrackerResult(
            deviceAddress = "",
            riskLevel = RiskLevel.SAFE,
            confidence = 0f,
            isKnownTracker = false,
            userMovementPattern = UserMovementAnalysis(),
            detectionCount = 0,
            recentDetectionCount = 0,
            reasoning = "Device not found"
        )
        
        fun insufficientData() = ImprovedTrackerResult(
            deviceAddress = "",
            riskLevel = RiskLevel.SAFE,
            confidence = 0f,
            isKnownTracker = false,
            userMovementPattern = UserMovementAnalysis(),
            detectionCount = 0,
            recentDetectionCount = 0,
            reasoning = "Insufficient detection data for analysis"
        )
    }
}

data class UserMovementAnalysis(
    val isUserStationary: Boolean = true,
    val totalMovementDistance: Double = 0.0,
    val stationaryTime: Long = 0L,
    val movementCount: Int = 0,
    val averageStationaryDuration: Long = 0L,
    val homeLocation: LocationRecord? = null
)