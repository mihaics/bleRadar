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
import com.bleradar.MainActivity
import com.bleradar.R
import com.bleradar.data.database.*
import com.bleradar.fingerprint.BleDeviceFingerprinter
import com.bleradar.fingerprint.DeviceProcessingResult
import com.bleradar.location.LocationTracker
import com.bleradar.analysis.AdvancedTrackerDetector
import com.bleradar.analysis.TrackerAnalysisResult
import com.bleradar.analysis.RiskLevel
import com.bleradar.repository.DeviceRepository
import com.bleradar.preferences.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
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
    
    @Inject
    lateinit var deviceFingerprinter: BleDeviceFingerprinter

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastAnalysisTime = 0L
    private val deviceDetectionCounts = mutableMapOf<String, Int>()
    private val deviceFirstSeen = mutableMapOf<String, Long>()
    private val processedDevicesThisScan = ConcurrentHashMap.newKeySet<String>()
    private var currentScanStartTime = 0L

    companion object {
        const val ACTION_EMERGENCY_SCAN_COMPLETE = "com.bleradar.EMERGENCY_SCAN_COMPLETE"
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
            android.util.Log.d("BleRadarService", "Service started with action: $action")
            
            when (action) {
                "scan_once" -> {
                    // Perform a single scan cycle triggered by WorkManager
                    serviceScope.launch {
                        performScan()
                        // Keep service running but don't start continuous scanning
                    }
                }
                "emergency_scan" -> {
                    // Perform emergency scan - single intensive scan cycle
                    android.util.Log.d("BleRadarService", "Starting emergency scan")
                    serviceScope.launch {
                        performEmergencyScan()
                        // Broadcast completion
                        val intent = Intent(ACTION_EMERGENCY_SCAN_COMPLETE)
                        sendBroadcast(intent)
                        android.util.Log.d("BleRadarService", "Emergency scan completed, broadcast sent.")
                        // The service will be stopped by the ViewModel
                    }
                }
                else -> {
                    // Default: start continuous scanning for immediate use
                    android.util.Log.d("BleRadarService", "Starting continuous scanning")
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
                "BLE Guardian Service",
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Guardian Active")
            .setContentText("Scanning for BLE devices every ${settingsManager.scanIntervalMinutes} minutes")
            .setSmallIcon(R.drawable.ic_radar)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Make notification persistent
            .setAutoCancel(false) // Prevent user from dismissing
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createErrorNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Guardian - Permission Required")
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

    private suspend fun performEmergencyScan() {
        if (!hasPermissions()) {
            android.util.Log.e("BleRadarService", "Cannot perform emergency scan: missing permissions")
            return
        }

        android.util.Log.d("BleRadarService", "Starting emergency scan cycle...")
        startScanning()
        delay(15000L) // 15 seconds intensive scan
        stopScanning()
        android.util.Log.d("BleRadarService", "Emergency scan cycle completed")
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
            
            // Reset deduplication for new scan cycle
            processedDevicesThisScan.clear()
            currentScanStartTime = System.currentTimeMillis()

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
            val macAddress = device.address

            // Atomically check and add the MAC address to the set for this scan cycle.
            // If add() returns false, it means the MAC was already in the set, so we skip it.
            if (!processedDevicesThisScan.add(macAddress)) {
                android.util.Log.d("BleRadarService", "Skipping duplicate device in same scan cycle: $macAddress")
                return
            }
            
            android.util.Log.d("BleRadarService", "Processing device: $macAddress with RSSI: $rssi")
            
            val currentLocation = locationTracker.getCurrentLocation()
            
            // Check if this is a known MAC address first (fast path)
            val existingDeviceUuid = database.deviceMacAddressDao().getDeviceUuidByMacAddress(macAddress)
            android.util.Log.d("BleRadarService", "MAC address lookup: $macAddress -> UUID: $existingDeviceUuid")
            
            val processingResult = if (existingDeviceUuid != null) {
                // Fast path: Known MAC address - skip expensive fingerprinting
                android.util.Log.d("BleRadarService", "Fast path: Known MAC address $macAddress -> UUID $existingDeviceUuid")
                deviceFingerprinter.processKnownDevice(existingDeviceUuid, result, timestamp)
            } else {
                // Slow path: Unknown MAC address - use fingerprinting
                android.util.Log.d("BleRadarService", "Slow path: Unknown MAC address $macAddress - running fingerprinting")
                deviceFingerprinter.processScanResult(result, timestamp)
            }
            
            android.util.Log.d("BleRadarService", "Device processed: UUID=${processingResult.deviceUuid}, isNew=${processingResult.isNewDevice}, MAC=${processingResult.macAddress}")
            
            // Store fingerprint detection
            val fingerprintDetection = FingerprintDetection(
                deviceUuid = processingResult.deviceUuid,
                macAddress = processingResult.macAddress,
                timestamp = timestamp,
                rssi = rssi,
                latitude = currentLocation?.latitude ?: 0.0,
                longitude = currentLocation?.longitude ?: 0.0,
                accuracy = currentLocation?.accuracy ?: 0.0f,
                altitude = if (currentLocation?.hasAltitude() == true) currentLocation.altitude else null,
                speed = if (currentLocation?.hasSpeed() == true) currentLocation.speed else null,
                bearing = if (currentLocation?.hasBearing() == true) currentLocation.bearing else null,
                advertisingInterval = calculateAdvertisingInterval(processingResult.deviceUuid),
                txPower = result.scanRecord?.txPowerLevel
            )
            
            try {
                database.fingerprintDetectionDao().insertDetection(fingerprintDetection)
                android.util.Log.d("BleRadarService", "Fingerprint detection stored for: ${processingResult.deviceUuid}")
            } catch (e: Exception) {
                android.util.Log.e("BleRadarService", "Error storing fingerprint detection: ${e.message}")
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
            
            // Enhanced tracking analysis using fingerprints
            performFingerprintAnalysis(processingResult)
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error processing scan result: ${e.message}")
        }
    }
    
    private suspend fun performFingerprintAnalysis(processingResult: DeviceProcessingResult) {
        try {
            val currentTime = System.currentTimeMillis()
            val deviceUuid = processingResult.deviceUuid
            
            // Update detection counts based on UUID instead of MAC
            val currentCount = deviceDetectionCounts.getOrDefault(deviceUuid, 0) + 1
            deviceDetectionCounts[deviceUuid] = currentCount
            
            // Track first seen
            if (!deviceFirstSeen.containsKey(deviceUuid)) {
                deviceFirstSeen[deviceUuid] = currentTime
            }
            
            // Get fingerprint detections for analysis
            val detections = try {
                database.fingerprintDetectionDao().getDetectionsForDeviceSince(
                    deviceUuid, 
                    currentTime - (24 * 60 * 60 * 1000) // Last 24 hours
                ).first()
            } catch (e: Exception) {
                emptyList<FingerprintDetection>()
            }
            
            if (detections.isNotEmpty()) {
                val rssiValues = detections.map { it.rssi.toFloat() }
                @Suppress("UNUSED_VARIABLE")
                val avgRssi = rssiValues.average().toFloat()
                @Suppress("UNUSED_VARIABLE")
                val rssiVariation = calculateVariance(rssiValues)
                
                // Detect movement patterns
                @Suppress("UNUSED_VARIABLE")
                val lastMovement = detectLastMovementFromFingerprints(detections)
                @Suppress("UNUSED_VARIABLE")
                val isStationary = isDeviceStationaryFromFingerprints(detections)
                
                // Get device fingerprint for analysis
                val deviceFingerprint = database.deviceFingerprintDao().getDeviceFingerprint(deviceUuid)
                
                if (deviceFingerprint != null) {
                    // Update device fingerprint metrics
                    val updatedFingerprint = deviceFingerprint.copy(
                        totalDetections = currentCount,
                        lastSeen = currentTime,
                        suspiciousScore = calculateSuspiciousScore(detections, deviceFingerprint)
                    )
                    database.deviceFingerprintDao().updateDeviceFingerprint(updatedFingerprint)
                    
                    // Periodic advanced analysis
                    if (currentTime - lastAnalysisTime > ANALYSIS_INTERVAL_MS) {
                        lastAnalysisTime = currentTime
                        performAdvancedFingerprintAnalysis(deviceUuid)
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error in fingerprint analysis: ${e.message}")
        }
    }
    
    private suspend fun performAdvancedFingerprintAnalysis(deviceUuid: String) {
        try {
            // Get device fingerprint
            val deviceFingerprint = database.deviceFingerprintDao().getDeviceFingerprint(deviceUuid)
            if (deviceFingerprint == null) {
                android.util.Log.e("BleRadarService", "Device fingerprint not found for UUID: $deviceUuid")
                return
            }
            
            // Analyze fingerprint patterns
            val patterns = database.fingerprintPatternDao().getPatternsForDevice(deviceUuid)
            val detections = database.fingerprintDetectionDao().getDetectionsForDeviceSince(
                deviceUuid, 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            ).first()
            
            // Calculate advanced metrics
            val uniqueLocationCount = database.fingerprintDetectionDao().getUniqueLocationCountForDevice(deviceUuid)
            val avgRssi = database.fingerprintDetectionDao().getAverageRssiForDevice(deviceUuid, System.currentTimeMillis() - (24 * 60 * 60 * 1000)) ?: 0f
            
            // Calculate suspicious score based on multiple factors
            val suspiciousScore = calculateAdvancedSuspiciousScore(
                deviceFingerprint,
                patterns,
                detections,
                uniqueLocationCount,
                avgRssi
            )
            
            // Update device suspicious score
            database.deviceFingerprintDao().updateSuspiciousScore(deviceUuid, suspiciousScore)
            
            // Generate alerts for high-risk devices
            if (suspiciousScore > 0.7f) {
                handleFingerprintAlert(deviceUuid, suspiciousScore)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error in advanced fingerprint analysis: ${e.message}")
        }
    }
    
    private suspend fun handleFingerprintAlert(deviceUuid: String, suspiciousScore: Float) {
        try {
            val deviceFingerprint = database.deviceFingerprintDao().getDeviceFingerprint(deviceUuid) ?: return
            val currentTime = System.currentTimeMillis()
            
            // Check alert cooldown
            if (currentTime - deviceFingerprint.lastAlertTime < ALERT_COOLDOWN_MS) {
                return
            }
            
            // Update last alert time
            val updatedFingerprint = deviceFingerprint.copy(lastAlertTime = currentTime)
            database.deviceFingerprintDao().updateDeviceFingerprint(updatedFingerprint)
            
            // Create alert notification
            val alertNotification = createFingerprintAlertNotification(deviceFingerprint, suspiciousScore)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(deviceUuid.hashCode(), alertNotification)
            
            android.util.Log.w("BleRadarService", "Fingerprint alert for device: $deviceUuid, Score: $suspiciousScore")
            
        } catch (e: Exception) {
            android.util.Log.e("BleRadarService", "Error handling fingerprint alert: ${e.message}")
        }
    }
    
    private fun createFingerprintAlertNotification(deviceFingerprint: DeviceFingerprint, suspiciousScore: Float): Notification {
        val title = when {
            suspiciousScore > 0.9f -> "🚨 Critical Tracker Alert"
            suspiciousScore > 0.7f -> "⚠️ Suspicious Tracker Detected"
            else -> "📍 Tracker Activity"
        }
        
        val description = when {
            deviceFingerprint.isKnownTracker -> "Known ${deviceFingerprint.trackerType} tracker detected"
            else -> "Suspicious BLE device showing tracking behavior"
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("device_uuid", deviceFingerprint.deviceUuid)
            putExtra("open_device_details", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_radar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
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
    
    private fun detectLastMovementFromFingerprints(detections: List<FingerprintDetection>): Long {
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
    
    private fun isDeviceStationaryFromFingerprints(detections: List<FingerprintDetection>): Boolean {
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
    
    private fun calculateAdvertisingInterval(@Suppress("UNUSED_PARAMETER") deviceUuid: String): Long? {
        // Calculate advertising interval based on previous detections
        // This is a simplified implementation - in production, you'd track timing patterns
        return null
    }
    
    private fun calculateSuspiciousScore(detections: List<FingerprintDetection>, deviceFingerprint: DeviceFingerprint): Float {
        var score = 0f
        
        // Higher score for known trackers
        if (deviceFingerprint.isKnownTracker) {
            score += 0.5f
        }
        
        // Higher score for devices with multiple MAC addresses
        if (deviceFingerprint.macAddressCount > 1) {
            score += 0.3f
        }
        
        // Higher score for devices detected in multiple locations
        val uniqueLocations = detections.groupBy { "${it.latitude.toInt()},${it.longitude.toInt()}" }.size
        if (uniqueLocations > 3) {
            score += 0.2f
        }
        
        // Higher score for consistent RSSI (indicating following)
        val rssiValues = detections.map { it.rssi.toFloat() }
        if (rssiValues.isNotEmpty()) {
            val rssiVariance = calculateVariance(rssiValues)
            if (rssiVariance < 25) { // Low variance indicates consistent distance
                score += 0.2f
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun calculateAdvancedSuspiciousScore(
        deviceFingerprint: DeviceFingerprint,
        patterns: List<FingerprintPattern>,
        detections: List<FingerprintDetection>,
        uniqueLocationCount: Int,
        @Suppress("UNUSED_PARAMETER") avgRssi: Float
    ): Float {
        var score = 0f
        
        // Base score from existing fingerprint
        score += deviceFingerprint.suspiciousScore * 0.4f
        
        // Score based on patterns
        patterns.forEach { pattern ->
            when (pattern.patternType) {
                FingerprintPatternType.SERVICE_UUID_PATTERN -> score += pattern.confidence * 0.2f
                FingerprintPatternType.MANUFACTURER_DATA_PATTERN -> score += pattern.confidence * 0.2f
                FingerprintPatternType.COMBINED_SIGNATURE -> score += pattern.confidence * 0.3f
                else -> score += pattern.confidence * 0.1f
            }
        }
        
        // Score based on location diversity
        if (uniqueLocationCount > 5) {
            score += 0.3f
        } else if (uniqueLocationCount > 3) {
            score += 0.2f
        }
        
        // Score based on detection frequency
        if (detections.size > 50) {
            score += 0.2f
        } else if (detections.size > 20) {
            score += 0.1f
        }
        
        return score.coerceIn(0f, 1f)
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