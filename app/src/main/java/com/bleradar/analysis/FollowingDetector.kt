package com.bleradar.analysis

import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.LocationRecord
import com.bleradar.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class FollowingDetector @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    
    companion object {
        private const val FOLLOWING_THRESHOLD = 0.7f
        private const val MIN_DETECTIONS_FOR_ANALYSIS = 10
        private const val TIME_WINDOW_HOURS = 24
        private const val MIN_DISTANCE_THRESHOLD = 100.0 // meters
        private const val CORRELATION_THRESHOLD = 0.6f
    }
    
    suspend fun analyzeDevice(deviceAddress: String): Float {
        val timeWindow = System.currentTimeMillis() - (TIME_WINDOW_HOURS * 60 * 60 * 1000)
        
        val detections = deviceRepository.getDetectionsForDeviceSince(deviceAddress, timeWindow).first()
        val userLocations = deviceRepository.getLocationsSince(timeWindow).first()
        
        if (detections.size < MIN_DETECTIONS_FOR_ANALYSIS) {
            return 0f
        }
        
        val score = calculateFollowingScore(detections, userLocations)
        
        // Update the device's following score
        deviceRepository.updateFollowingScore(deviceAddress, score)
        
        return score
    }
    
    private fun calculateFollowingScore(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): Float {
        var totalScore = 0f
        var scoreCount = 0
        
        // Factor 1: Frequency of detections
        val frequencyScore = calculateFrequencyScore(detections)
        totalScore += frequencyScore
        scoreCount++
        
        // Factor 2: Location correlation
        val correlationScore = calculateLocationCorrelation(detections, userLocations)
        totalScore += correlationScore
        scoreCount++
        
        // Factor 3: Movement pattern similarity
        val movementScore = calculateMovementSimilarity(detections, userLocations)
        totalScore += movementScore
        scoreCount++
        
        // Factor 4: Temporal proximity
        val temporalScore = calculateTemporalProximity(detections, userLocations)
        totalScore += temporalScore
        scoreCount++
        
        return if (scoreCount > 0) totalScore / scoreCount else 0f
    }
    
    private fun calculateFrequencyScore(detections: List<BleDetection>): Float {
        if (detections.isEmpty()) return 0f
        
        val timeSpan = detections.maxOf { it.timestamp } - detections.minOf { it.timestamp }
        val hoursSpan = timeSpan / (1000 * 60 * 60).toDouble()
        
        if (hoursSpan < 1) return 0f
        
        val detectionsPerHour = detections.size / hoursSpan
        
        // High frequency detection indicates potential following
        return when {
            detectionsPerHour > 5 -> 1f
            detectionsPerHour > 3 -> 0.8f
            detectionsPerHour > 1 -> 0.6f
            detectionsPerHour > 0.5 -> 0.4f
            else -> 0.2f
        }
    }
    
    private fun calculateLocationCorrelation(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): Float {
        if (detections.isEmpty() || userLocations.isEmpty()) return 0f
        
        var correlationSum = 0f
        var correlationCount = 0
        
        for (detection in detections) {
            val nearbyUserLocation = userLocations.minByOrNull { 
                abs(it.timestamp - detection.timestamp) 
            }
            
            if (nearbyUserLocation != null) {
                val distance = calculateDistance(
                    detection.latitude, detection.longitude,
                    nearbyUserLocation.latitude, nearbyUserLocation.longitude
                )
                
                // Close proximity indicates potential following
                val proximityScore = when {
                    distance < 10 -> 1f
                    distance < 50 -> 0.8f
                    distance < 100 -> 0.6f
                    distance < 500 -> 0.4f
                    else -> 0.2f
                }
                
                correlationSum += proximityScore
                correlationCount++
            }
        }
        
        return if (correlationCount > 0) correlationSum / correlationCount else 0f
    }
    
    private fun calculateMovementSimilarity(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): Float {
        if (detections.size < 3 || userLocations.size < 3) return 0f
        
        val deviceMoves = calculateMovements(detections.map { 
            LocationPoint(it.latitude, it.longitude, it.timestamp) 
        })
        val userMoves = calculateMovements(userLocations.map { 
            LocationPoint(it.latitude, it.longitude, it.timestamp) 
        })
        
        if (deviceMoves.isEmpty() || userMoves.isEmpty()) return 0f
        
        var similaritySum = 0f
        var comparisonCount = 0
        
        for (deviceMove in deviceMoves) {
            val nearestUserMove = userMoves.minByOrNull { 
                abs(it.timestamp - deviceMove.timestamp) 
            }
            
            if (nearestUserMove != null) {
                val directionSimilarity = calculateDirectionSimilarity(
                    deviceMove.bearing, nearestUserMove.bearing
                )
                val speedSimilarity = calculateSpeedSimilarity(
                    deviceMove.speed, nearestUserMove.speed
                )
                
                similaritySum += (directionSimilarity + speedSimilarity) / 2
                comparisonCount++
            }
        }
        
        return if (comparisonCount > 0) similaritySum / comparisonCount else 0f
    }
    
    private fun calculateTemporalProximity(
        detections: List<BleDetection>,
        userLocations: List<LocationRecord>
    ): Float {
        if (detections.isEmpty() || userLocations.isEmpty()) return 0f
        
        var proximitySum = 0f
        var proximityCount = 0
        
        for (detection in detections) {
            val nearestTime = userLocations.minOfOrNull { 
                abs(it.timestamp - detection.timestamp) 
            }
            
            if (nearestTime != null) {
                val timeDiff = nearestTime / (1000 * 60) // minutes
                val proximityScore = when {
                    timeDiff < 5 -> 1f
                    timeDiff < 15 -> 0.8f
                    timeDiff < 30 -> 0.6f
                    timeDiff < 60 -> 0.4f
                    else -> 0.2f
                }
                
                proximitySum += proximityScore
                proximityCount++
            }
        }
        
        return if (proximityCount > 0) proximitySum / proximityCount else 0f
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
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
            val timeDiff = (curr.timestamp - prev.timestamp) / 1000.0 // seconds
            
            if (timeDiff > 0 && distance > MIN_DISTANCE_THRESHOLD) {
                val speed = distance / timeDiff // m/s
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
    
    data class LocationPoint(val lat: Double, val lon: Double, val timestamp: Long)
    data class Movement(val timestamp: Long, val speed: Double, val bearing: Double)
}