package com.bleradar.analysis

import com.bleradar.data.database.*
import com.bleradar.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class AdvancedTrackerDetector @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    
    companion object {
        private const val AIRTAG_ADVERTISING_INTERVAL = 2000L // 2 seconds typical
        private const val SMARTTAG_ADVERTISING_INTERVAL = 1000L // 1 second typical
        private const val TILE_ADVERTISING_INTERVAL = 3000L // 3 seconds typical
        
        private const val FOLLOWING_THRESHOLD = 0.7f
        private const val SUSPICIOUS_THRESHOLD = 0.6f
        private const val MIN_DETECTIONS_FOR_ANALYSIS = 5
        private const val TIME_WINDOW_HOURS = 24
        private const val SHORT_TIME_WINDOW_MINUTES = 30
        
        private const val PROXIMITY_THRESHOLD_METERS = 50.0
        private const val STATIONARY_THRESHOLD_METERS = 10.0
        private const val HIGH_FREQUENCY_THRESHOLD = 10 // detections per hour
        
        private const val APPLE_COMPANY_ID = 0x004C
        private const val SAMSUNG_COMPANY_ID = 0x0075
        private const val TILE_COMPANY_ID = 0x00B3
        
        private val KNOWN_TRACKER_SERVICES = setOf(
            "0000FE9F-0000-1000-8000-00805F9B34FB", // Apple AirTag
            "0000FD5A-0000-1000-8000-00805F9B34FB", // Samsung SmartTag
            "0000FEED-0000-1000-8000-00805F9B34FB"  // Tile
        )
    }
    
    suspend fun analyzeDeviceForTracking(deviceAddress: String): TrackerAnalysisResult {
        val timeWindow = System.currentTimeMillis() - (TIME_WINDOW_HOURS * 60 * 60 * 1000)
        val shortTimeWindow = System.currentTimeMillis() - (SHORT_TIME_WINDOW_MINUTES * 60 * 1000)
        
        val device = deviceRepository.getDevice(deviceAddress) ?: return TrackerAnalysisResult.notFound()
        val detections = deviceRepository.getDetectionsForDeviceSince(deviceAddress, timeWindow).first()
        val recentDetections = deviceRepository.getDetectionsForDeviceSince(deviceAddress, shortTimeWindow).first()
        val userLocations = deviceRepository.getLocationsSince(timeWindow).first()
        
        if (detections.size < MIN_DETECTIONS_FOR_ANALYSIS) {
            return TrackerAnalysisResult.insufficientData()
        }
        
        val analysisResults = mutableListOf<PatternAnalysis>()
        
        // 1. Known tracker signature detection
        val knownTrackerResult = analyzeKnownTrackerSignature(device, detections)
        analysisResults.add(knownTrackerResult)
        
        // 2. Advertising pattern analysis
        val advertisingResult = analyzeAdvertisingPattern(detections)
        analysisResults.add(advertisingResult)
        
        // 3. Proximity correlation with enhanced algorithms
        val proximityResult = analyzeProximityCorrelation(detections, userLocations)
        analysisResults.add(proximityResult)
        
        // 4. Movement synchronization
        val movementResult = analyzeMovementSync(detections, userLocations)
        analysisResults.add(movementResult)
        
        // 5. Temporal pattern analysis
        val temporalResult = analyzeTemporalPatterns(detections, recentDetections)
        analysisResults.add(temporalResult)
        
        // 6. Signal strength pattern analysis
        val signalResult = analyzeSignalStrengthPattern(detections)
        analysisResults.add(signalResult)
        
        // 7. Persistence analysis
        val persistenceResult = analyzePersistence(detections, userLocations)
        analysisResults.add(persistenceResult)
        
        // 8. Cross-platform detection compatibility
        val crossPlatformResult = analyzeCrossPlatformSignature(device, detections)
        analysisResults.add(crossPlatformResult)
        
        return combineAnalysisResults(deviceAddress, analysisResults)
    }
    
    private fun analyzeKnownTrackerSignature(device: BleDevice, detections: List<BleDetection>): PatternAnalysis {
        var confidence = 0f
        var trackerType: String? = null
        
        // Check manufacturer
        when (device.manufacturer) {
            "Apple" -> {
                confidence += 0.8f
                trackerType = "AirTag"
            }
            "Samsung" -> {
                confidence += 0.7f
                trackerType = "SmartTag"
            }
            "Tile" -> {
                confidence += 0.6f
                trackerType = "Tile"
            }
        }
        
        // Check service UUIDs
        device.services?.let { services ->
            KNOWN_TRACKER_SERVICES.forEach { knownService ->
                if (services.contains(knownService, ignoreCase = true)) {
                    confidence += 0.9f
                    when {
                        knownService.contains("FE9F") -> trackerType = "AirTag"
                        knownService.contains("FD5A") -> trackerType = "SmartTag"
                        knownService.contains("FEED") -> trackerType = "Tile"
                    }
                }
            }
        }
        
        // Check advertising interval patterns
        if (detections.size >= 3) {
            val intervals = detections.zipWithNext { a, b -> 
                abs(a.timestamp - b.timestamp) 
            }
            val avgInterval = intervals.average()
            
            when {
                avgInterval in 1800.0..2200.0 -> { // ~2 seconds, typical AirTag
                    confidence += 0.3f
                    trackerType = trackerType ?: "AirTag"
                }
                avgInterval in 800.0..1200.0 -> { // ~1 second, typical SmartTag
                    confidence += 0.3f
                    trackerType = trackerType ?: "SmartTag"
                }
                avgInterval in 2800.0..3200.0 -> { // ~3 seconds, typical Tile
                    confidence += 0.3f
                    trackerType = trackerType ?: "Tile"
                }
            }
        }
        
        return PatternAnalysis(
            type = PatternType.KNOWN_TRACKER_SIGNATURE,
            confidence = confidence.coerceIn(0f, 1f),
            metadata = mapOf(
                "trackerType" to (trackerType ?: "Unknown"),
                "manufacturer" to (device.manufacturer ?: "Unknown"),
                "services" to (device.services ?: "None")
            )
        )
    }
    
    private fun analyzeAdvertisingPattern(detections: List<BleDetection>): PatternAnalysis {
        if (detections.size < 3) return PatternAnalysis.empty(PatternType.ADVERTISING_PATTERN)
        
        val intervals = detections.zipWithNext { a, b -> 
            abs(a.timestamp - b.timestamp) 
        }
        
        val avgInterval = intervals.average()
        val intervalVariation = intervals.map { abs(it - avgInterval) }.average()
        val variationCoefficient = intervalVariation / avgInterval
        
        // Low variation indicates consistent advertising (typical of trackers)
        val consistencyScore = when {
            variationCoefficient < 0.1 -> 0.9f
            variationCoefficient < 0.2 -> 0.7f
            variationCoefficient < 0.3 -> 0.5f
            else -> 0.2f
        }
        
        // Frequency analysis
        val frequencyScore = when {
            avgInterval < 1000 -> 0.8f // High frequency
            avgInterval < 3000 -> 0.6f // Medium frequency
            avgInterval < 5000 -> 0.4f // Low frequency
            else -> 0.2f
        }
        
        val confidence = (consistencyScore + frequencyScore) / 2
        
        return PatternAnalysis(
            type = PatternType.ADVERTISING_PATTERN,
            confidence = confidence,
            metadata = mapOf(
                "avgInterval" to avgInterval.toString(),
                "variationCoefficient" to variationCoefficient.toString(),
                "consistencyScore" to consistencyScore.toString()
            )
        )
    }
    
    private fun analyzeProximityCorrelation(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): PatternAnalysis {
        if (detections.isEmpty() || userLocations.isEmpty()) {
            return PatternAnalysis.empty(PatternType.PROXIMITY_CORRELATION)
        }
        
        var correlationSum = 0f
        var correlationCount = 0
        
        for (detection in detections) {
            val nearbyLocation = userLocations.minByOrNull { 
                abs(it.timestamp - detection.timestamp) 
            }
            
            if (nearbyLocation != null) {
                val distance = calculateDistance(
                    detection.latitude, detection.longitude,
                    nearbyLocation.latitude, nearbyLocation.longitude
                )
                
                val timeDiff = abs(detection.timestamp - nearbyLocation.timestamp) / (1000 * 60) // minutes
                
                // Enhanced proximity scoring
                val proximityScore = when {
                    distance < 10 && timeDiff < 5 -> 1f
                    distance < 25 && timeDiff < 10 -> 0.8f
                    distance < 50 && timeDiff < 15 -> 0.6f
                    distance < 100 && timeDiff < 30 -> 0.4f
                    else -> 0.1f
                }
                
                correlationSum += proximityScore
                correlationCount++
            }
        }
        
        val confidence = if (correlationCount > 0) correlationSum / correlationCount else 0f
        
        return PatternAnalysis(
            type = PatternType.PROXIMITY_CORRELATION,
            confidence = confidence,
            metadata = mapOf(
                "correlationCount" to correlationCount.toString(),
                "avgProximity" to (correlationSum / correlationCount.coerceAtLeast(1)).toString()
            )
        )
    }
    
    private fun analyzeMovementSync(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): PatternAnalysis {
        if (detections.size < 3 || userLocations.size < 3) {
            return PatternAnalysis.empty(PatternType.MOVEMENT_SYNC)
        }
        
        val deviceMovements = calculateMovements(detections.map { 
            LocationPoint(it.latitude, it.longitude, it.timestamp) 
        })
        val userMovements = calculateMovements(userLocations.map { 
            LocationPoint(it.latitude, it.longitude, it.timestamp) 
        })
        
        if (deviceMovements.isEmpty() || userMovements.isEmpty()) {
            return PatternAnalysis.empty(PatternType.MOVEMENT_SYNC)
        }
        
        var syncScore = 0f
        var syncCount = 0
        
        for (deviceMove in deviceMovements) {
            val nearestUserMove = userMovements.minByOrNull { 
                abs(it.timestamp - deviceMove.timestamp) 
            }
            
            if (nearestUserMove != null) {
                val timeDiff = abs(deviceMove.timestamp - nearestUserMove.timestamp) / (1000 * 60) // minutes
                
                if (timeDiff < 30) { // Within 30 minutes
                    val directionSimilarity = calculateDirectionSimilarity(
                        deviceMove.bearing, nearestUserMove.bearing
                    )
                    val speedSimilarity = calculateSpeedSimilarity(
                        deviceMove.speed, nearestUserMove.speed
                    )
                    
                    syncScore += (directionSimilarity + speedSimilarity) / 2
                    syncCount++
                }
            }
        }
        
        val confidence = if (syncCount > 0) syncScore / syncCount else 0f
        
        return PatternAnalysis(
            type = PatternType.MOVEMENT_SYNC,
            confidence = confidence,
            metadata = mapOf(
                "syncCount" to syncCount.toString(),
                "avgSync" to (syncScore / syncCount.coerceAtLeast(1)).toString()
            )
        )
    }
    
    private fun analyzeTemporalPatterns(
        detections: List<BleDetection>,
        recentDetections: List<BleDetection>
    ): PatternAnalysis {
        if (detections.isEmpty()) return PatternAnalysis.empty(PatternType.SUSPICIOUS_TIMING)
        
        val hourlyDistribution = IntArray(24)
        val daylyDistribution = IntArray(7)
        
        detections.forEach { detection ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = detection.timestamp
            
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            
            hourlyDistribution[hour]++
            daylyDistribution[dayOfWeek - 1]++
        }
        
        // Calculate pattern regularity
        val hourlyVariance = calculateVariance(hourlyDistribution.map { it.toFloat() })
        val dailyVariance = calculateVariance(daylyDistribution.map { it.toFloat() })
        
        // High variance indicates irregular patterns (more suspicious)
        val irregularityScore = (hourlyVariance + dailyVariance) / 2
        
        // Recent activity burst detection
        val recentActivityRatio = if (detections.isNotEmpty()) {
            recentDetections.size.toFloat() / detections.size
        } else 0f
        
        val burstScore = when {
            recentActivityRatio > 0.5 -> 0.8f // High recent activity
            recentActivityRatio > 0.3 -> 0.6f
            recentActivityRatio > 0.1 -> 0.4f
            else -> 0.2f
        }
        
        val confidence = (irregularityScore + burstScore) / 2
        
        return PatternAnalysis(
            type = PatternType.SUSPICIOUS_TIMING,
            confidence = confidence.coerceIn(0f, 1f),
            metadata = mapOf(
                "recentActivityRatio" to recentActivityRatio.toString(),
                "hourlyVariance" to hourlyVariance.toString(),
                "dailyVariance" to dailyVariance.toString()
            )
        )
    }
    
    private fun analyzeSignalStrengthPattern(detections: List<BleDetection>): PatternAnalysis {
        if (detections.isEmpty()) return PatternAnalysis.empty(PatternType.SIGNAL_STRENGTH_PATTERN)
        
        val rssiValues = detections.map { it.rssi.toFloat() }
        val avgRssi = rssiValues.average()
        val rssiVariance = calculateVariance(rssiValues)
        
        // Analyze RSSI trend
        val rssiTrend = if (detections.size >= 2) {
            val first = detections.takeLast(detections.size / 2).map { it.rssi }.average()
            val second = detections.take(detections.size / 2).map { it.rssi }.average()
            first - second
        } else 0.0
        
        // Consistent signal strength indicates potential tracking
        val consistencyScore = when {
            rssiVariance < 25 -> 0.8f // Very consistent
            rssiVariance < 50 -> 0.6f // Moderate consistency
            rssiVariance < 100 -> 0.4f // Low consistency
            else -> 0.2f
        }
        
        // Improving signal strength over time is suspicious
        val trendScore = when {
            rssiTrend > 10 -> 0.8f // Getting stronger
            rssiTrend > 5 -> 0.6f
            rssiTrend > 0 -> 0.4f
            else -> 0.2f
        }
        
        val confidence = (consistencyScore + trendScore) / 2
        
        return PatternAnalysis(
            type = PatternType.SIGNAL_STRENGTH_PATTERN,
            confidence = confidence,
            metadata = mapOf(
                "avgRssi" to avgRssi.toString(),
                "rssiVariance" to rssiVariance.toString(),
                "rssiTrend" to rssiTrend.toString()
            )
        )
    }
    
    private fun analyzePersistence(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): PatternAnalysis {
        if (detections.isEmpty()) return PatternAnalysis.empty(PatternType.FOLLOWING)
        
        val timeSpan = (detections.maxOfOrNull { it.timestamp } ?: 0) - 
                      (detections.minOfOrNull { it.timestamp } ?: 0)
        val hours = timeSpan / (1000 * 60 * 60).toDouble()
        
        val detectionFreq = if (hours > 0) detections.size / hours else 0.0
        
        // Analyze location changes
        val userLocationChanges = if (userLocations.size >= 2) {
            userLocations.zipWithNext { a, b ->
                calculateDistance(a.latitude, a.longitude, b.latitude, b.longitude)
            }.count { it > 100 } // Significant location changes
        } else 0
        
        val deviceLocationChanges = if (detections.size >= 2) {
            detections.zipWithNext { a, b ->
                calculateDistance(a.latitude, a.longitude, b.latitude, b.longitude)
            }.count { it > 100 }
        } else 0
        
        // High correlation between user and device location changes
        val locationCorrelation = if (userLocationChanges > 0) {
            deviceLocationChanges.toFloat() / userLocationChanges
        } else 0f
        
        val persistenceScore = when {
            hours > 12 && detectionFreq > 0.5 -> 0.8f // Long-term following
            hours > 6 && detectionFreq > 1.0 -> 0.7f
            hours > 3 && detectionFreq > 2.0 -> 0.6f
            else -> 0.3f
        }
        
        val confidence = (persistenceScore + locationCorrelation.coerceIn(0f, 1f)) / 2
        
        return PatternAnalysis(
            type = PatternType.FOLLOWING,
            confidence = confidence,
            metadata = mapOf(
                "timeSpanHours" to hours.toString(),
                "detectionFreq" to detectionFreq.toString(),
                "locationCorrelation" to locationCorrelation.toString()
            )
        )
    }
    
    private fun analyzeCrossPlatformSignature(device: BleDevice, detections: List<BleDetection>): PatternAnalysis {
        // Cross-platform detection based on 2024 standards
        var confidence = 0f
        
        // Check for rotating identifiers (common in privacy-focused trackers)
        val addressVariations = detections.groupBy { it.deviceAddress }.size
        if (addressVariations > 1) {
            confidence += 0.7f // Rotating MAC addresses
        }
        
        // Check for standard service UUIDs that trigger cross-platform alerts
        device.services?.let { services ->
            if (services.contains("FE9F") || services.contains("FD5A")) {
                confidence += 0.8f // Standard tracker services
            }
        }
        
        // Check for manufacturer data patterns
        if (device.manufacturer in listOf("Apple", "Samsung", "Tile")) {
            confidence += 0.6f
        }
        
        return PatternAnalysis(
            type = PatternType.KNOWN_TRACKER_SIGNATURE,
            confidence = confidence.coerceIn(0f, 1f),
            metadata = mapOf(
                "crossPlatformCompatible" to (confidence > 0.5).toString(),
                "addressVariations" to addressVariations.toString()
            )
        )
    }
    
    private fun combineAnalysisResults(
        deviceAddress: String,
        analyses: List<PatternAnalysis>
    ): TrackerAnalysisResult {
        val weightedScores = analyses.map { analysis ->
            val weight = when (analysis.type) {
                PatternType.KNOWN_TRACKER_SIGNATURE -> 0.25f
                PatternType.PROXIMITY_CORRELATION -> 0.20f
                PatternType.FOLLOWING -> 0.15f
                PatternType.MOVEMENT_SYNC -> 0.15f
                PatternType.ADVERTISING_PATTERN -> 0.10f
                PatternType.SIGNAL_STRENGTH_PATTERN -> 0.05f
                PatternType.SUSPICIOUS_TIMING -> 0.05f
                else -> 0.05f
            }
            analysis.confidence * weight
        }
        
        val overallScore = weightedScores.sum()
        val riskLevel = when {
            overallScore >= 0.8f -> RiskLevel.CRITICAL
            overallScore >= 0.6f -> RiskLevel.HIGH
            overallScore >= 0.4f -> RiskLevel.MEDIUM
            overallScore >= 0.2f -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
        
        return TrackerAnalysisResult(
            deviceAddress = deviceAddress,
            overallScore = overallScore,
            riskLevel = riskLevel,
            analyses = analyses,
            timestamp = System.currentTimeMillis(),
            recommendedAction = determineRecommendedAction(riskLevel, analyses)
        )
    }
    
    private fun determineRecommendedAction(
        riskLevel: RiskLevel,
        @Suppress("UNUSED_PARAMETER") analyses: List<PatternAnalysis>
    ): RecommendedAction {
        return when (riskLevel) {
            RiskLevel.CRITICAL -> RecommendedAction.IMMEDIATE_ALERT
            RiskLevel.HIGH -> RecommendedAction.STRONG_WARNING
            RiskLevel.MEDIUM -> RecommendedAction.MONITOR_CLOSELY
            RiskLevel.LOW -> RecommendedAction.CONTINUE_MONITORING
            RiskLevel.MINIMAL -> RecommendedAction.NO_ACTION
        }
    }
    
    // Helper functions
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    private fun calculateMovements(locations: List<LocationPoint>): List<Movement> {
        if (locations.size < 2) return emptyList()
        
        val movements = mutableListOf<Movement>()
        
        for (i in 1 until locations.size) {
            val prev = locations[i - 1]
            val curr = locations[i]
            
            val distance = calculateDistance(prev.lat, prev.lon, curr.lat, curr.lon)
            val timeDiff = (curr.timestamp - prev.timestamp) / 1000.0
            
            if (timeDiff > 0 && distance > 10) {
                val speed = distance / timeDiff
                val bearing = calculateBearing(prev.lat, prev.lon, curr.lat, curr.lon)
                movements.add(Movement(curr.timestamp, speed, bearing))
            }
        }
        
        return movements
    }
    
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        return Math.toDegrees(atan2(y, x))
    }
    
    private fun calculateDirectionSimilarity(bearing1: Double, bearing2: Double): Float {
        val diff = abs(bearing1 - bearing2)
        val normalizedDiff = minOf(diff, 360 - diff)
        return (1 - normalizedDiff / 180).toFloat()
    }
    
    private fun calculateSpeedSimilarity(speed1: Double, speed2: Double): Float {
        val maxSpeed = maxOf(speed1, speed2)
        if (maxSpeed == 0.0) return 1f
        val diff = abs(speed1 - speed2)
        return (1 - diff / maxSpeed).toFloat().coerceIn(0f, 1f)
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average().toFloat()
    }
    
    data class LocationPoint(val lat: Double, val lon: Double, val timestamp: Long)
    data class Movement(val timestamp: Long, val speed: Double, val bearing: Double)
}

data class TrackerAnalysisResult(
    val deviceAddress: String,
    val overallScore: Float,
    val riskLevel: RiskLevel,
    val analyses: List<PatternAnalysis>,
    val timestamp: Long,
    val recommendedAction: RecommendedAction
) {
    companion object {
        fun notFound() = TrackerAnalysisResult(
            deviceAddress = "",
            overallScore = 0f,
            riskLevel = RiskLevel.MINIMAL,
            analyses = emptyList(),
            timestamp = System.currentTimeMillis(),
            recommendedAction = RecommendedAction.NO_ACTION
        )
        
        fun insufficientData() = TrackerAnalysisResult(
            deviceAddress = "",
            overallScore = 0f,
            riskLevel = RiskLevel.MINIMAL,
            analyses = emptyList(),
            timestamp = System.currentTimeMillis(),
            recommendedAction = RecommendedAction.CONTINUE_MONITORING
        )
    }
}

data class PatternAnalysis(
    val type: PatternType,
    val confidence: Float,
    val metadata: Map<String, String>
) {
    companion object {
        fun empty(type: PatternType) = PatternAnalysis(
            type = type,
            confidence = 0f,
            metadata = emptyMap()
        )
    }
}

enum class RiskLevel {
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class RecommendedAction {
    NO_ACTION,
    CONTINUE_MONITORING,
    MONITOR_CLOSELY,
    STRONG_WARNING,
    IMMEDIATE_ALERT
}