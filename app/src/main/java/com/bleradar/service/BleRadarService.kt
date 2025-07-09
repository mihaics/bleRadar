package com.bleradar.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.bleradar.R
import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleRadarDatabase
import com.bleradar.data.database.LocationRecord
import com.bleradar.data.database.DetectionPattern
import com.bleradar.data.database.PatternType
import com.bleradar.location.LocationTracker
import com.bleradar.analysis.AdvancedTrackerDetector
import com.bleradar.analysis.TrackerAnalysisResult
import com.bleradar.analysis.RiskLevel
import com.bleradar.repository.DeviceRepository
import com.bleradar.preferences.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BleRadarService : Service() {

    @Inject
    lateinit var database: BleRadarDatabase
    
    @Inject
    lateinit var locationTracker: LocationTracker
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var advancedTrackerDetector: AdvancedTrackerDetector
    
    @Inject
    lateinit var settingsManager: SettingsManager

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastAnalysisTime = 0L
    private val deviceDetectionCounts = mutableMapOf<String, Int>()
    private val deviceFirstSeen = mutableMapOf<String, Long>()

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "BLE_RADAR_CHANNEL"
        const val SCAN_DURATION_MS = 10000L // 10 seconds
        const val SCAN_INTERVAL_MS = 30000L // 30 seconds between scans
        const val ANALYSIS_INTERVAL_MS = 300000L // 5 minutes between analyses
        const val ALERT_COOLDOWN_MS = 1800000L // 30 minutes between alerts for same device
    }

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
        initializeBluetooth()
        acquireWakeLock()
        // Start location tracking for continuous GPS monitoring
        locationTracker.startLocationTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (!hasPermissions()) {
                android.util.Log.e("BleRadarService", "Cannot start scanning: missing required permissions")
                startForeground(NOTIFICATION_ID, createErrorNotification())
                // Schedule service stop after a short delay to allow user to see the error
                serviceScope.launch {
                    kotlinx.coroutines.delay(3000) // 3 seconds
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Handle different actions
            val action = intent?.getStringExtra("action")
            when (action) {
                "scan_once" -> {
                    // Perform a single scan cycle triggered by WorkManager
                    serviceScope.launch {
                        performScan()
                        // Keep service running but don't start continuous scanning
                    }
                }
                else -> {
                    // Default: start continuous scanning for immediate use
                    startPeriodicScanning()
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("BleRadarService", "SecurityException starting foreground service: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, createErrorNotification())
                serviceScope.launch {
                    kotlinx.coroutines.delay(3000)
                    stopSelf()
                }
            } catch (e2: Exception) {
                android.util.Log.e("BleRadarService", "Failed to start foreground service even with error notification: ${e2.message}")
                stopSelf()
            }
            return START_NOT_STICKY
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        locationTracker.stopLocationTracking()
        releaseWakeLock()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter == null) {
            android.util.Log.e("BleRadarService", "Bluetooth adapter not available")
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            android.util.Log.e("BleRadarService", "Bluetooth is not enabled")
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            android.util.Log.e("BleRadarService", "BLE scanner not available")
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE Radar Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background BLE scanning service"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Radar Active")
            .setContentText("Scanning for BLE devices every ${settingsManager.scanIntervalMinutes} minutes")
            .setSmallIcon(R.drawable.ic_radar)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Make notification persistent
            .setAutoCancel(false) // Prevent user from dismissing
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createErrorNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Radar - Permission Required")
            .setContentText("Please grant all required permissions in Settings")
            .setSmallIcon(R.drawable.ic_radar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BleRadar::ScanWakeLock"
        )
        // Don't specify timeout to prevent premature release
        // The wake lock will be released when the service stops
        wakeLock?.acquire()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun startPeriodicScanning() {
        serviceScope.launch {
            // Wait a bit before starting first scan to avoid conflicts
            delay(5000) // 5 seconds
            
            while (isActive) {
                performScan()
                // Use configurable scan interval from settings
                val scanIntervalMs = settingsManager.getScanIntervalMillis()
                delay(scanIntervalMs)
            }
        }
    }
    
    private fun updateNotification() {
        try {
            val notification = createNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error updating notification: ${e.message}")
        }
    }

    private suspend fun performScan() {
        if (!hasPermissions()) {
            android.util.Log.e("BleRadarService", "Cannot scan: missing permissions")
            return
        }

        android.util.Log.d("BleRadarService", "Starting scan cycle...")
        startScanning()
        delay(SCAN_DURATION_MS)
        stopScanning()
        android.util.Log.d("BleRadarService", "Scan cycle completed")
    }

    private fun startScanning() {
        synchronized(this) {
            if (isScanning || bluetoothLeScanner == null) {
                android.util.Log.d("BleRadarService", "Scan already running or scanner null. isScanning: $isScanning")
                return
            }
            
            if (!hasPermissions()) {
                android.util.Log.e("BleRadarService", "Missing required permissions for scanning")
                return
            }

            if (bluetoothAdapter?.isEnabled != true) {
                android.util.Log.e("BleRadarService", "Bluetooth is not enabled")
                return
            }

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(0) // Report immediately
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) // More aggressive matching
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT) // Max advertisements
                .build()

            val scanFilters = listOf<ScanFilter>()

            try {
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
                isScanning = true
                android.util.Log.d("BleRadarService", "BLE scanning started successfully")
            } catch (e: SecurityException) {
                android.util.Log.e("BleRadarService", "Security exception during scanning: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Exception during scanning: ${e.message}")
            }
        }
    }

    private fun stopScanning() {
        synchronized(this) {
            if (!isScanning || bluetoothLeScanner == null) {
                android.util.Log.d("BleRadarService", "Scan not running or scanner null. isScanning: $isScanning")
                return
            }
            
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                android.util.Log.d("BleRadarService", "BLE scanning stopped successfully")
            } catch (e: SecurityException) {
                android.util.Log.e("BleRadarService", "Security exception stopping scan: ${e.message}")
                isScanning = false
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Exception stopping scan: ${e.message}")
                isScanning = false
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            try {
                serviceScope.launch {
                    processScanResult(result)
                }
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error in onScanResult: ${e.message}")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            try {
                serviceScope.launch {
                    results.forEach { result ->
                        processScanResult(result)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error in onBatchScanResults: ${e.message}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            android.util.Log.e("BleRadarService", "BLE scan failed with error code: $errorCode")
            when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> android.util.Log.e("BleRadarService", "Scan already started")
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> android.util.Log.e("BleRadarService", "App registration failed")
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> android.util.Log.e("BleRadarService", "Feature unsupported")
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> android.util.Log.e("BleRadarService", "Internal error")
                else -> android.util.Log.e("BleRadarService", "Unknown scan failure")
            }
        }
    }

    private suspend fun processScanResult(result: ScanResult) {
        try {
            val device = result.device
            val rssi = result.rssi
            val timestamp = System.currentTimeMillis()
            
            android.util.Log.d("BleRadarService", "Processing device: ${device.address} with RSSI: $rssi")
            
            val currentLocation = locationTracker.getCurrentLocation()
            
            // Store or update device
            val existingDevice = try {
                database.bleDeviceDao().getDevice(device.address)
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error getting existing device: ${e.message}")
                null
            }
            
            // Safely get device name
            val deviceName = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(this@BleRadarService, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.name
                    } else null
                } else {
                    device.name
                }
            } catch (e: SecurityException) {
                android.util.Log.w("BleRadarService", "No permission to get device name: ${e.message}")
                null
            }
            
            val bleDevice = if (existingDevice != null) {
                existingDevice.copy(
                    deviceName = deviceName ?: existingDevice.deviceName,
                    rssi = rssi,
                    lastSeen = timestamp
                )
            } else {
                BleDevice(
                    deviceAddress = device.address,
                    deviceName = deviceName,
                    rssi = rssi,
                    firstSeen = timestamp,
                    lastSeen = timestamp,
                    manufacturer = getManufacturerName(result),
                    services = getServiceUuids(result)
                )
            }
            
            try {
                database.bleDeviceDao().insertDevice(bleDevice)
                android.util.Log.d("BleRadarService", "Device stored: ${device.address}")
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error storing device: ${e.message}")
                return
            }
        
        // Store detection with or without location
        val detection = if (currentLocation != null) {
            BleDetection(
                deviceAddress = device.address,
                timestamp = timestamp,
                rssi = rssi,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                accuracy = currentLocation.accuracy,
                altitude = if (currentLocation.hasAltitude()) currentLocation.altitude else null,
                speed = if (currentLocation.hasSpeed()) currentLocation.speed else null,
                bearing = if (currentLocation.hasBearing()) currentLocation.bearing else null
            )
        } else {
            // Store without location data (use default coordinates)
            BleDetection(
                deviceAddress = device.address,
                timestamp = timestamp,
                rssi = rssi,
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0.0f,
                altitude = null,
                speed = null,
                bearing = null
            )
        }
        
            try {
                database.bleDetectionDao().insertDetection(detection)
                android.util.Log.d("BleRadarService", "Detection stored for: ${device.address}")
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error storing detection: ${e.message}")
            }
            
            // Store location record only if we have location
            currentLocation?.let { location ->
                try {
                    val locationRecord = LocationRecord(
                        timestamp = timestamp,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        provider = location.provider ?: "unknown"
                    )
                    database.locationDao().insertLocation(locationRecord)
                } catch (e: Exception) {
                    android.util.Log.e("BleRadarService", "Error storing location: ${e.message}")
                }
            }
            
            // Enhanced tracking analysis
            performEnhancedAnalysis(device.address, bleDevice)
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error processing scan result: ${e.message}")
        }
    }
    
    private suspend fun performEnhancedAnalysis(deviceAddress: String, bleDevice: BleDevice) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Update detection counts
            val currentCount = deviceDetectionCounts.getOrDefault(deviceAddress, 0) + 1
            deviceDetectionCounts[deviceAddress] = currentCount
            
            // Track first seen
            if (!deviceFirstSeen.containsKey(deviceAddress)) {
                deviceFirstSeen[deviceAddress] = currentTime
            }
            
            // Calculate enhanced metrics
            val detections = try {
                deviceRepository.getDetectionsForDeviceSince(
                    deviceAddress, 
                    currentTime - (24 * 60 * 60 * 1000) // Last 24 hours
                ).first()
            } catch (e: Exception) {
                emptyList<BleDetection>()
            }
            
            if (detections.isNotEmpty()) {
                val rssiValues = detections.map { it.rssi.toFloat() }
                val avgRssi = rssiValues.average().toFloat()
                val rssiVariation = calculateVariance(rssiValues)
                
                // Detect movement patterns
                val lastMovement = detectLastMovement(detections)
                val isStationary = isDeviceStationary(detections)
                
                // Check for known tracker signatures
                val trackerInfo = identifyTrackerType(bleDevice)
                
                // Update device metrics
                deviceRepository.updateDeviceMetrics(
                    address = deviceAddress,
                    count = currentCount,
                    consecutive = calculateConsecutiveDetections(deviceAddress),
                    maxConsecutive = getMaxConsecutiveDetections(deviceAddress),
                    avgRssi = avgRssi,
                    rssiVar = rssiVariation,
                    lastMovement = lastMovement,
                    stationary = isStationary,
                    suspiciousScore = 0f, // Will be updated by analysis
                    knownTracker = trackerInfo.first,
                    trackerType = trackerInfo.second
                )
                
                // Periodic advanced analysis
                if (currentTime - lastAnalysisTime > ANALYSIS_INTERVAL_MS) {
                    lastAnalysisTime = currentTime
                    performAdvancedTrackerAnalysis(deviceAddress)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error in enhanced analysis: ${e.message}")
        }
    }
    
    private suspend fun performAdvancedTrackerAnalysis(deviceAddress: String) {
        try {
            val analysisResult = advancedTrackerDetector.analyzeDeviceForTracking(deviceAddress)
            
            // Update suspicious activity score
            deviceRepository.updateSuspiciousScore(deviceAddress, analysisResult.overallScore)
            
            // Store detection patterns
            analysisResult.analyses.forEach { analysis ->
                val pattern = DetectionPattern(
                    deviceAddress = deviceAddress,
                    timestamp = System.currentTimeMillis(),
                    patternType = analysis.type.value,
                    confidence = analysis.confidence,
                    metadata = analysis.metadata.entries.joinToString(";") { "${it.key}=${it.value}" }
                )
                deviceRepository.insertPattern(pattern)
            }
            
            // Generate alerts for high-risk devices
            if (analysisResult.riskLevel == RiskLevel.HIGH || analysisResult.riskLevel == RiskLevel.CRITICAL) {
                handleTrackerAlert(deviceAddress, analysisResult)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error in advanced tracker analysis: ${e.message}")
        }
    }
    
    private suspend fun handleTrackerAlert(deviceAddress: String, analysisResult: TrackerAnalysisResult) {
        try {
            val device = deviceRepository.getDevice(deviceAddress) ?: return
            val currentTime = System.currentTimeMillis()
            
            // Check alert cooldown
            if (currentTime - device.lastAlertTime < ALERT_COOLDOWN_MS) {
                return
            }
            
            // Update last alert time
            deviceRepository.updateLastAlertTime(deviceAddress, currentTime)
            
            // Create alert notification
            val alertNotification = createTrackerAlertNotification(device, analysisResult)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(deviceAddress.hashCode(), alertNotification)
            
            android.util.Log.w("BleRadarService", "Tracker alert for device: $deviceAddress, Risk: ${analysisResult.riskLevel}")
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error handling tracker alert: ${e.message}")
        }
    }
    
    private fun createTrackerAlertNotification(device: BleDevice, analysisResult: TrackerAnalysisResult): Notification {
        val title = when (analysisResult.riskLevel) {
            RiskLevel.CRITICAL -> "🚨 Critical Tracker Alert"
            RiskLevel.HIGH -> "⚠️ Suspicious Tracker Detected"
            else -> "📍 Tracker Activity"
        }
        
        val description = when {
            device.isKnownTracker -> "Known ${device.trackerType} tracker detected"
            else -> "Suspicious BLE device showing tracking behavior"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_radar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }
    
    private fun detectLastMovement(detections: List<BleDetection>): Long {
        if (detections.size < 2) return 0L
        
        val sorted = detections.sortedBy { it.timestamp }
        for (i in sorted.size - 2 downTo 0) {
            val curr = sorted[i + 1]
            val prev = sorted[i]
            
            val distance = calculateDistance(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude
            )
            
            if (distance > 10.0) { // 10 meters movement threshold
                return curr.timestamp
            }
        }
        return sorted.firstOrNull()?.timestamp ?: 0L
    }
    
    private fun isDeviceStationary(detections: List<BleDetection>): Boolean {
        if (detections.size < 3) return false
        
        val recent = detections.takeLast(5)
        val distances = recent.zipWithNext { a, b ->
            calculateDistance(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        
        return distances.all { it < 20.0 } // All movements under 20 meters
    }
    
    private fun identifyTrackerType(device: BleDevice): Pair<Boolean, String?> {
        // Check known tracker signatures
        device.manufacturer?.let { manufacturer ->
            when (manufacturer) {
                "Apple" -> return Pair(true, "AirTag")
                "Samsung" -> return Pair(true, "SmartTag")
                "Tile" -> return Pair(true, "Tile")
                else -> { /* No match */ }
            }
        }
        
        device.services?.let { services ->
            when {
                services.contains("FE9F", ignoreCase = true) -> return Pair(true, "AirTag")
                services.contains("FD5A", ignoreCase = true) -> return Pair(true, "SmartTag") // Samsung SmartTag+
                services.contains("FDCC", ignoreCase = true) -> return Pair(true, "SmartTag") // Samsung SmartTag2
                services.contains("FEED", ignoreCase = true) -> return Pair(true, "Tile")
                services.contains("FE2C", ignoreCase = true) -> return Pair(true, "Nordic") // Nordic tracking devices
                else -> { /* No match */ }
            }
        }
        
        // Check for Samsung tracker patterns by device name
        device.deviceName?.let { name ->
            when {
                name.contains("Galaxy SmartTag", ignoreCase = true) -> return Pair(true, "SmartTag")
                name.contains("SmartTag", ignoreCase = true) -> return Pair(true, "SmartTag")
                name.contains("SM-", ignoreCase = true) -> return Pair(true, "Samsung Device")
                else -> { /* No match */ }
            }
        }
        
        return Pair(false, null)
    }
    
    private fun calculateConsecutiveDetections(deviceAddress: String): Int {
        // This would need more sophisticated logic to track actual consecutive detections
        return deviceDetectionCounts.getOrDefault(deviceAddress, 0).coerceAtMost(50)
    }
    
    private fun getMaxConsecutiveDetections(deviceAddress: String): Int {
        // This would track the maximum consecutive detections over time
        return calculateConsecutiveDetections(deviceAddress)
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun getManufacturerName(result: ScanResult): String? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
        return if (manufacturerData != null && manufacturerData.size() > 0) {
            val manufacturerId = manufacturerData.keyAt(0)
            getManufacturerName(manufacturerId)
        } else null
    }

    private fun getManufacturerName(id: Int): String? {
        return when (id) {
            0x004C -> "Apple"
            0x0006 -> "Microsoft"
            0x00E0 -> "Google"
            0x0075 -> "Samsung"
            else -> "Unknown ($id)"
        }
    }

    private fun getServiceUuids(result: ScanResult): String? {
        val serviceUuids = result.scanRecord?.serviceUuids
        return serviceUuids?.joinToString(",") { it.toString() }
    }

    private fun hasPermissions(): Boolean {
        val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
        
        // For dataSync service type, we only need location permission for actual location tracking
        // Bluetooth scanning can work without location for basic device detection
        val locationPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("BleRadarService", "Bluetooth permission: $bluetoothPermission, Location permission: $locationPermission")
        
        return bluetoothPermission && locationPermission
    }
}