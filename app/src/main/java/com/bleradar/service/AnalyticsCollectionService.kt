package com.bleradar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bleradar.data.database.AnalyticsSnapshot
import com.bleradar.data.database.DeviceAnalytics
import com.bleradar.data.database.TrendData
import com.bleradar.repository.AnalyticsRepository
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

@AndroidEntryPoint
class AnalyticsCollectionService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 2
        private const val NOTIFICATION_CHANNEL_ID = "analytics_service"
    }
    
    @Inject
    lateinit var analyticsRepository: AnalyticsRepository
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var snapshotJob: Job? = null
    private var analyticsJob: Job? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Check if this is a manual refresh request
            if (intent?.action == "COLLECT_NOW") {
                serviceScope.launch {
                    try {
                        collectAnalyticsSnapshot()
                        android.util.Log.d("AnalyticsService", "Manual analytics collection completed")
                    } catch (e: Exception) {
                        android.util.Log.e("AnalyticsService", "Manual collection failed", e)
                    }
                }
            } else {
                startAnalyticsCollection()
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsService", "Failed to start analytics collection", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        snapshotJob?.cancel()
        analyticsJob?.cancel()
    }
    
    private fun startAnalyticsCollection() {
        // Collect snapshots every 15 minutes
        snapshotJob = serviceScope.launch {
            while (true) {
                try {
                    collectAnalyticsSnapshot()
                    delay(TimeUnit.MINUTES.toMillis(15))
                } catch (e: Exception) {
                    // Log error and continue
                    delay(TimeUnit.MINUTES.toMillis(5)) // Retry sooner on error
                }
            }
        }
        
        // Process daily analytics at 2 AM
        analyticsJob = serviceScope.launch {
            while (true) {
                try {
                    val now = System.currentTimeMillis()
                    val nextRun = getNext2AMTime()
                    val delayTime = nextRun - now
                    
                    if (delayTime > 0) {
                        delay(delayTime)
                    }
                    
                    processDailyAnalytics()
                    
                    // Wait until next day
                    delay(TimeUnit.HOURS.toMillis(23))
                } catch (e: Exception) {
                    // Log error and continue
                    delay(TimeUnit.HOURS.toMillis(1)) // Retry in 1 hour on error
                }
            }
        }
    }
    
    suspend fun collectAnalyticsSnapshot() {
        try {
            val currentTime = System.currentTimeMillis()
            val dayAgo = currentTime - TimeUnit.DAYS.toMillis(1)
        
        // Try both sync and flow-based approaches to get devices
        val allDevices = deviceRepository.getAllDevicesSync()
        android.util.Log.d("AnalyticsService", "Found ${allDevices.size} devices in database (sync method)")
        
        // If sync method returns empty, try getting the latest from flow
        val finalDevices = if (allDevices.isEmpty()) {
            android.util.Log.d("AnalyticsService", "Sync method returned empty, trying flow method...")
            try {
                // Collect the latest value from the flow
                val flowDevices = mutableListOf<com.bleradar.data.database.BleDevice>()
                deviceRepository.getAllDevices().collect { devices ->
                    flowDevices.addAll(devices)
                    return@collect // Exit after first emission
                }
                android.util.Log.d("AnalyticsService", "Flow method found ${flowDevices.size} devices")
                flowDevices
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsService", "Flow method failed: ${e.message}")
                allDevices // Fall back to sync result (empty)
            }
        } else {
            allDevices
        }
        
        // Also check all devices including ignored ones for debugging
        val allDevicesRaw = analyticsRepository.getAllDevicesIncludingIgnored()
        android.util.Log.d("AnalyticsService", "Total devices in DB (including ignored): ${allDevicesRaw.size}")
        finalDevices.forEach { device ->
            android.util.Log.d("AnalyticsService", "Device: ${device.deviceAddress} - ${device.deviceName} - Ignored: ${device.isIgnored}")
        }
        
        val recentDevices = deviceRepository.getRecentDevices(TimeUnit.HOURS.toMillis(24))
        android.util.Log.d("AnalyticsService", "Found ${recentDevices.size} recent devices")
        
        // If no devices found, let's also check if we can access device list data directly
        if (finalDevices.isEmpty()) {
            android.util.Log.w("AnalyticsService", "No devices found! This might indicate an issue with data access or BLE scanning not running.")
            // Check if scanning service is running
            android.util.Log.d("AnalyticsService", "Checking if BLE scanning service might have data...")
        }
        
        val trackedDevices = finalDevices.filter { it.isTracked }
        val suspiciousDevices = finalDevices.filter { it.suspiciousActivityScore > 0.5f }
        val knownTrackers = finalDevices.filter { it.isKnownTracker }
        
        val recentDetections = deviceRepository.getDetectionsSince(dayAgo)
        var activeDetections = 0
        recentDetections.collect { detections ->
            activeDetections = detections.size
        }
        
        val averageRssi = if (finalDevices.isNotEmpty()) {
            finalDevices.map { it.averageRssi }.average().toFloat()
        } else 0f
        
        val averageFollowingScore = if (finalDevices.isNotEmpty()) {
            finalDevices.map { it.followingScore }.average().toFloat()
        } else 0f
        
        val averageSuspiciousScore = if (finalDevices.isNotEmpty()) {
            finalDevices.map { it.suspiciousActivityScore }.average().toFloat()
        } else 0f
        
        // Calculate detection distances
        val distances = calculateDetectionDistances(finalDevices)
        
        val snapshot = AnalyticsSnapshot(
            timestamp = currentTime,
            totalDevices = finalDevices.size,
            trackedDevices = trackedDevices.size,
            suspiciousDevices = suspiciousDevices.size,
            knownTrackers = knownTrackers.size,
            activeDetections = activeDetections,
            averageRssi = averageRssi,
            averageFollowingScore = averageFollowingScore,
            averageSuspiciousScore = averageSuspiciousScore,
            locationUpdates = 0, // Could be enhanced with location tracking
            alertsTriggered = suspiciousDevices.size,
            newDevicesDiscovered = recentDevices.size,
            devicesTurnedOff = 0, // Could be calculated from last seen times
            averageDetectionDistance = distances.average,
            maxDetectionDistance = distances.max,
            minDetectionDistance = distances.min,
            scanningDuration = TimeUnit.MINUTES.toMillis(15), // Approximate
            backgroundTime = 0,
            foregroundTime = 0
        )
        
        analyticsRepository.insertSnapshot(snapshot)
        
        // Update trend data
        updateTrendData(snapshot)
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsService", "Failed to collect analytics snapshot", e)
        }
    }
    
    private suspend fun processDailyAnalytics() {
        try {
            val currentTime = System.currentTimeMillis()
            val dayAgo = currentTime - TimeUnit.DAYS.toMillis(1)
            val today = dateFormat.format(Date(currentTime))
            
            val allDevices = deviceRepository.getAllDevicesSync()
            
            for (device in allDevices) {
                val detections = deviceRepository.getDetectionsForDeviceSync(device.deviceAddress)
                val dayDetections = detections.filter { it.timestamp >= dayAgo }
                
                if (dayDetections.isNotEmpty()) {
                    val deviceAnalytics = calculateDeviceAnalytics(device, dayDetections, today)
                    analyticsRepository.insertDeviceAnalytics(deviceAnalytics)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsService", "Failed to process daily analytics", e)
        }
    }
    
    private fun calculateDeviceAnalytics(device: com.bleradar.data.database.BleDevice, detections: List<com.bleradar.data.database.BleDetection>, date: String): DeviceAnalytics {
        val rssiValues = detections.map { it.rssi }
        val averageRssi = rssiValues.average().toFloat()
        val minRssi = rssiValues.minOrNull() ?: 0
        val maxRssi = rssiValues.maxOrNull() ?: 0
        val rssiVariance = calculateVariance(rssiValues.map { it.toFloat() })
        
        val firstSeen = detections.minByOrNull { it.timestamp }?.timestamp ?: 0L
        val lastSeen = detections.maxByOrNull { it.timestamp }?.timestamp ?: 0L
        val activeDuration = lastSeen - firstSeen
        
        val distanceTraveled = calculateDistanceTraveled(detections)
        val averageSpeed = if (activeDuration > 0) {
            distanceTraveled / (activeDuration / 1000f) // m/s
        } else 0f
        
        val consecutiveDetections = calculateConsecutiveDetections(detections)
        val detectionGaps = calculateDetectionGaps(detections)
        val averageDetectionInterval = if (detections.size > 1) {
            activeDuration / (detections.size - 1)
        } else 0L
        
        return DeviceAnalytics(
            deviceAddress = device.deviceAddress,
            date = date,
            detectionsCount = detections.size,
            averageRssi = averageRssi,
            minRssi = minRssi,
            maxRssi = maxRssi,
            rssiVariance = rssiVariance,
            followingScore = device.followingScore,
            suspiciousScore = device.suspiciousActivityScore,
            firstSeenTime = firstSeen,
            lastSeenTime = lastSeen,
            activeDuration = activeDuration,
            distanceTraveled = distanceTraveled,
            averageSpeed = averageSpeed,
            maxSpeed = calculateMaxSpeed(detections),
            locationsVisited = calculateUniqueLocations(detections),
            timeStationary = calculateStationaryTime(detections),
            timeMoving = activeDuration - calculateStationaryTime(detections),
            consecutiveDetections = consecutiveDetections,
            detectionGaps = detectionGaps,
            averageDetectionInterval = averageDetectionInterval,
            advertisingChanges = 0, // Could be enhanced with identifier tracking
            alertsTriggered = if (device.suspiciousActivityScore > 0.5f) 1 else 0,
            userInteractions = 0 // Could be enhanced with user interaction tracking
        )
    }
    
    private suspend fun updateTrendData(snapshot: AnalyticsSnapshot) {
        val date = dateFormat.format(Date(snapshot.timestamp))
        
        val trendMetrics = mapOf(
            "total_devices" to snapshot.totalDevices.toFloat(),
            "tracked_devices" to snapshot.trackedDevices.toFloat(),
            "suspicious_devices" to snapshot.suspiciousDevices.toFloat(),
            "known_trackers" to snapshot.knownTrackers.toFloat(),
            "active_detections" to snapshot.activeDetections.toFloat(),
            "average_rssi" to snapshot.averageRssi,
            "average_following_score" to snapshot.averageFollowingScore,
            "average_suspicious_score" to snapshot.averageSuspiciousScore,
            "new_devices_discovered" to snapshot.newDevicesDiscovered.toFloat(),
            "alerts_triggered" to snapshot.alertsTriggered.toFloat()
        )
        
        for ((metricName, value) in trendMetrics) {
            val trendData = TrendData(
                metricName = metricName,
                date = date,
                timeframe = "daily",
                value = value
            )
            analyticsRepository.insertTrendData(trendData)
        }
    }
    
    private fun calculateDetectionDistances(devices: List<com.bleradar.data.database.BleDevice>): DetectionDistances {
        val distances = devices.map { rssiToDistance(it.averageRssi) }
        return DetectionDistances(
            average = if (distances.isNotEmpty()) distances.average().toFloat() else 0f,
            max = distances.maxOrNull() ?: 0f,
            min = distances.minOrNull() ?: 0f
        )
    }
    
    private fun rssiToDistance(rssi: Float): Float {
        // Rough approximation: distance = 10^((Tx Power - RSSI) / (10 * n))
        // Using typical values: Tx Power = 0 dBm, n = 2
        return 10.0.pow((0 - rssi) / 20.0).toFloat()
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val squaredDifferences = values.map { (it - mean) * (it - mean) }
        return squaredDifferences.average().toFloat()
    }
    
    private fun calculateDistanceTraveled(detections: List<com.bleradar.data.database.BleDetection>): Float {
        if (detections.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 1 until detections.size) {
            val prev = detections[i-1]
            val curr = detections[i]
            totalDistance += haversineDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
        }
        return totalDistance
    }
    
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000f // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c.toFloat()
    }
    
    private fun calculateMaxSpeed(detections: List<com.bleradar.data.database.BleDetection>): Float {
        if (detections.size < 2) return 0f
        
        var maxSpeed = 0f
        for (i in 1 until detections.size) {
            val prev = detections[i-1]
            val curr = detections[i]
            val distance = haversineDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            val timeDiff = (curr.timestamp - prev.timestamp) / 1000f // seconds
            if (timeDiff > 0) {
                val speed = distance / timeDiff
                maxSpeed = maxOf(maxSpeed, speed)
            }
        }
        return maxSpeed
    }
    
    private fun calculateUniqueLocations(detections: List<com.bleradar.data.database.BleDetection>): Int {
        val locations = detections.map { "${it.latitude}_${it.longitude}" }.toSet()
        return locations.size
    }
    
    private fun calculateStationaryTime(detections: List<com.bleradar.data.database.BleDetection>): Long {
        if (detections.size < 2) return 0L
        
        var stationaryTime = 0L
        var stationaryStart: Long? = null
        
        for (i in 1 until detections.size) {
            val prev = detections[i-1]
            val curr = detections[i]
            val distance = haversineDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            
            if (distance < 10f) { // Less than 10 meters = stationary
                if (stationaryStart == null) {
                    stationaryStart = prev.timestamp
                }
            } else {
                if (stationaryStart != null) {
                    stationaryTime += prev.timestamp - stationaryStart
                    stationaryStart = null
                }
            }
        }
        
        return stationaryTime
    }
    
    private fun calculateConsecutiveDetections(detections: List<com.bleradar.data.database.BleDetection>): Int {
        if (detections.isEmpty()) return 0
        
        var maxConsecutive = 1
        var currentConsecutive = 1
        
        for (i in 1 until detections.size) {
            val timeDiff = detections[i].timestamp - detections[i-1].timestamp
            if (timeDiff <= TimeUnit.MINUTES.toMillis(10)) { // Within 10 minutes
                currentConsecutive++
            } else {
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
                currentConsecutive = 1
            }
        }
        
        return maxOf(maxConsecutive, currentConsecutive)
    }
    
    private fun calculateDetectionGaps(detections: List<com.bleradar.data.database.BleDetection>): Int {
        if (detections.size < 2) return 0
        
        var gaps = 0
        for (i in 1 until detections.size) {
            val timeDiff = detections[i].timestamp - detections[i-1].timestamp
            if (timeDiff > TimeUnit.MINUTES.toMillis(30)) { // Gap if more than 30 minutes
                gaps++
            }
        }
        return gaps
    }
    
    private fun getNext2AMTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 2)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Analytics Collection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BLE Radar analytics data collection"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BLE Radar Analytics")
            .setContentText("Collecting analytics data...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private data class DetectionDistances(
        val average: Float,
        val max: Float,
        val min: Float
    )
}