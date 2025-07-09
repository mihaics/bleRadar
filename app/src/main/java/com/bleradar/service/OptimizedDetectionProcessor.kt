package com.bleradar.service

import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.bleradar.data.database.*
import com.bleradar.analysis.SmartThreatDetector
import com.bleradar.location.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class OptimizedDetectionProcessor @Inject constructor(
    private val deviceDao: OptimizedDeviceDao,
    private val clusterDao: LocationClusterDao,
    private val detectionDao: DeviceClusterDetectionDao,
    private val evidenceDao: TrackingEvidenceDao,
    private val threatDetector: SmartThreatDetector,
    private val locationTracker: LocationTracker,
    private val context: Context
) {
    private val tag = "OptimizedDetectionProcessor"
    private val processingScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val MIN_RSSI_CHANGE = 5 // Only update if RSSI changes by 5dBm or more
        private const val MIN_TIME_BETWEEN_UPDATES = 30 * 1000L // 30 seconds
        private const val CLUSTER_RADIUS_METERS = 50.0
    }
    
    suspend fun processDetection(scanResult: ScanResult) {
        try {
            val deviceAddress = scanResult.device.address
            val rssi = scanResult.rssi
            val timestamp = System.currentTimeMillis()
            
            // Get current location
            val currentLocation = locationTracker.getCurrentLocation()
            if (currentLocation == null) {
                Log.w(tag, "No location available, skipping detection")
                return
            }
            
            // Find or create location cluster
            val clusterId = findOrCreateCluster(currentLocation.latitude, currentLocation.longitude)
            
            // Get or create device
            val device = getOrCreateDevice(scanResult, timestamp)
            
            // Check if we should update detection data (avoid spam)
            val existingDetection = detectionDao.getDetection(deviceAddress, clusterId)
            if (shouldUpdateDetection(existingDetection, rssi, timestamp)) {
                updateDetectionData(device, clusterId, rssi, timestamp, existingDetection)
            }
            
            // Periodic threat analysis (not every detection)
            if (shouldRunThreatAnalysis(device, timestamp)) {
                analyzeThreatLevel(deviceAddress)
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing detection: ${e.message}")
        }
    }
    
    private suspend fun findOrCreateCluster(lat: Double, lon: Double): String {
        val clusterId = threatDetector.generateClusterId(lat, lon)
        
        var cluster = clusterDao.getCluster(clusterId)
        if (cluster == null) {
            // Create new cluster
            cluster = LocationCluster(
                clusterId = clusterId,
                centerLat = lat,
                centerLon = lon,
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                deviceCount = 0,
                detectionCount = 0,
                isUserLocation = true // Assume user is at this location
            )
            clusterDao.insertCluster(cluster)
            Log.d(tag, "Created new location cluster: $clusterId")
        } else {
            // Update existing cluster
            clusterDao.updateClusterStats(
                clusterId = clusterId,
                timestamp = System.currentTimeMillis(),
                deviceCount = cluster.deviceCount,
                detectionCount = cluster.detectionCount + 1
            )
        }
        
        return clusterId
    }
    
    private suspend fun getOrCreateDevice(scanResult: ScanResult, timestamp: Long): OptimizedDevice {
        val deviceAddress = scanResult.device.address
        val deviceName = try {
            scanResult.device.name
        } catch (e: SecurityException) {
            null
        }
        
        var device = deviceDao.getDevice(deviceAddress)
        if (device == null) {
            // Create new device
            device = OptimizedDevice(
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                manufacturer = getManufacturerName(scanResult),
                services = getServiceUuids(scanResult),
                deviceType = DeviceType.UNKNOWN,
                firstSeen = timestamp,
                lastSeen = timestamp,
                totalDetections = 1
            )
            deviceDao.insertDevice(device)
            Log.d(tag, "Created new device: $deviceAddress")
        } else {
            // Update existing device
            deviceDao.updateLastSeen(deviceAddress, timestamp)
            deviceDao.updateDetectionCount(deviceAddress, device.totalDetections + 1)
        }
        
        return device
    }
    
    private fun shouldUpdateDetection(
        existingDetection: DeviceClusterDetection?,
        newRssi: Int,
        timestamp: Long
    ): Boolean {
        if (existingDetection == null) return true
        
        // Check time threshold
        val timeDiff = timestamp - existingDetection.lastSeen
        if (timeDiff < MIN_TIME_BETWEEN_UPDATES) return false
        
        // Check RSSI change threshold
        val rssiDiff = abs(newRssi - existingDetection.avgRssi)
        if (rssiDiff < MIN_RSSI_CHANGE && timeDiff < MIN_TIME_BETWEEN_UPDATES * 2) return false
        
        return true
    }
    
    private suspend fun updateDetectionData(
        device: OptimizedDevice,
        clusterId: String,
        rssi: Int,
        timestamp: Long,
        existingDetection: DeviceClusterDetection?
    ) {
        if (existingDetection == null) {
            // Create new detection
            val detection = DeviceClusterDetection(
                deviceAddress = device.deviceAddress,
                clusterId = clusterId,
                firstSeen = timestamp,
                lastSeen = timestamp,
                detectionCount = 1,
                avgRssi = rssi.toFloat(),
                minRssi = rssi,
                maxRssi = rssi,
                rssiVariance = 0f,
                isCurrentLocation = true
            )
            detectionDao.insertDetection(detection)
            Log.d(tag, "Created new detection: ${device.deviceAddress} at $clusterId")
        } else {
            // Update existing detection with smart averaging
            val newCount = existingDetection.detectionCount + 1
            val newAvgRssi = ((existingDetection.avgRssi * existingDetection.detectionCount) + rssi) / newCount
            val newMinRssi = minOf(existingDetection.minRssi, rssi)
            val newMaxRssi = maxOf(existingDetection.maxRssi, rssi)
            val newVariance = calculateRssiVariance(existingDetection.avgRssi, newAvgRssi, rssi)
            
            val updatedDetection = existingDetection.copy(
                lastSeen = timestamp,
                detectionCount = newCount,
                avgRssi = newAvgRssi,
                minRssi = newMinRssi,
                maxRssi = newMaxRssi,
                rssiVariance = newVariance,
                isCurrentLocation = true
            )
            
            detectionDao.updateDetection(updatedDetection)
            Log.d(tag, "Updated detection: ${device.deviceAddress} at $clusterId (count: $newCount)")
        }
    }
    
    private fun shouldRunThreatAnalysis(device: OptimizedDevice, timestamp: Long): Boolean {
        // Run analysis if:
        // 1. Device is new (first time seen)
        // 2. Every 10 detections
        // 3. Every 30 minutes
        val timeSinceFirstSeen = timestamp - device.firstSeen
        val isNewDevice = timeSinceFirstSeen < 60000 // Less than 1 minute old
        val isPeriodicCheck = device.totalDetections % 10 == 0
        val isTimeBasedCheck = timeSinceFirstSeen % (30 * 60 * 1000) < 60000 // Every 30 minutes
        
        return isNewDevice || isPeriodicCheck || isTimeBasedCheck
    }
    
    private fun analyzeThreatLevel(deviceAddress: String) {
        processingScope.launch {
            try {
                val analysisResult = threatDetector.analyzeDevice(deviceAddress)
                
                // Update device threat scores
                deviceDao.updateSuspiciousScore(deviceAddress, analysisResult.confidence)
                
                // Update multi-location score specifically
                val multiLocationFactor = analysisResult.factors.find { it.type == com.bleradar.analysis.ThreatType.MULTI_LOCATION }
                multiLocationFactor?.let {
                    deviceDao.updateMultiLocationScore(deviceAddress, it.confidence)
                }
                
                // Store evidence
                analysisResult.factors.forEach { factor ->
                    if (factor.confidence > 0.5f) {
                        val evidenceType = when (factor.type) {
                            com.bleradar.analysis.ThreatType.KNOWN_TRACKER -> EvidenceType.KNOWN_TRACKER_SIGNATURE
                            com.bleradar.analysis.ThreatType.MULTI_LOCATION -> EvidenceType.MULTI_LOCATION
                            com.bleradar.analysis.ThreatType.PERSISTENCE -> EvidenceType.PERSISTENT_FOLLOWING
                            else -> EvidenceType.SIGNAL_PATTERN
                        }
                        
                        val evidence = TrackingEvidence(
                            deviceAddress = deviceAddress,
                            evidenceType = evidenceType,
                            confidence = factor.confidence,
                            lastUpdated = System.currentTimeMillis(),
                            metadata = factor.details
                        )
                        evidenceDao.insertEvidence(evidence)
                    }
                }
                
                // Trigger alert if necessary
                if (analysisResult.actionRequired) {
                    triggerThreatAlert(deviceAddress, analysisResult)
                }
                
                Log.d(tag, "Threat analysis completed for $deviceAddress: ${analysisResult.riskLevel}")
                
            } catch (e: Exception) {
                Log.e(tag, "Error in threat analysis: ${e.message}")
            }
        }
    }
    
    private fun triggerThreatAlert(deviceAddress: String, analysisResult: com.bleradar.analysis.ThreatAnalysisResult) {
        // This would trigger a notification or alert
        Log.w(tag, "THREAT ALERT: $deviceAddress - ${analysisResult.recommendation}")
        // TODO: Implement notification system
    }
    
    private fun calculateRssiVariance(oldAvg: Float, newAvg: Float, newRssi: Int): Float {
        // Simple variance calculation
        val diff = abs(newRssi - newAvg)
        return sqrt(diff.pow(2))
    }
    
    private fun getManufacturerName(scanResult: ScanResult): String? {
        val manufacturerData = scanResult.scanRecord?.manufacturerSpecificData
        return if (manufacturerData != null && manufacturerData.size() > 0) {
            val manufacturerId = manufacturerData.keyAt(0)
            when (manufacturerId) {
                0x004C -> "Apple"
                0x0006 -> "Microsoft"
                0x00E0 -> "Google"
                0x0075 -> "Samsung"
                else -> "Unknown ($manufacturerId)"
            }
        } else null
    }
    
    private fun getServiceUuids(scanResult: ScanResult): String? {
        val serviceUuids = scanResult.scanRecord?.serviceUuids
        return serviceUuids?.joinToString(",") { it.toString() }
    }
    
    suspend fun cleanupOldData() {
        try {
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
            
            deviceDao.deleteOldDevices(cutoffTime)
            clusterDao.deleteOldClusters(cutoffTime)
            detectionDao.deleteOldDetections(cutoffTime)
            evidenceDao.deleteOldEvidence(cutoffTime)
            
            Log.d(tag, "Cleaned up old data")
        } catch (e: Exception) {
            Log.e(tag, "Error cleaning up old data: ${e.message}")
        }
    }
    
    suspend fun getMultiLocationDevices(): List<MultiLocationResult> {
        val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Last 24 hours
        return detectionDao.getMultiLocationDevices(since)
    }
}