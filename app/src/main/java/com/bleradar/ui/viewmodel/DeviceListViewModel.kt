package com.bleradar.ui.viewmodel

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.repository.FingerprintRepository
import com.bleradar.service.BleRadarService
import com.bleradar.ui.model.DeviceDisplayModel
import com.bleradar.ui.model.DeviceDisplaySortOption
import com.bleradar.ui.model.DeviceStats
import com.bleradar.ui.model.ThreatLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fingerprintRepository: FingerprintRepository
) : ViewModel() {
    
    private val _sortOption = MutableStateFlow(DeviceDisplaySortOption.LAST_SEEN)
    val sortOption: StateFlow<DeviceDisplaySortOption> = _sortOption.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showNewDevicesOnly = MutableStateFlow(false)
    val showNewDevicesOnly: StateFlow<Boolean> = _showNewDevicesOnly.asStateFlow()
    
    private val _showTrackersOnly = MutableStateFlow(false)
    val showTrackersOnly: StateFlow<Boolean> = _showTrackersOnly.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Get all device fingerprints and convert to display models
    private val allDisplayDevices = fingerprintRepository.getAllDeviceFingerprints().map { fingerprints ->
        fingerprints.map { fingerprint ->
            val macAddresses = fingerprintRepository.getMacAddressesForDevice(fingerprint.deviceUuid)
            DeviceDisplayModel.fromFingerprint(fingerprint, macAddresses)
        }
    }
    
    // Combined flow for filtered and sorted devices
    val devices = combine(
        allDisplayDevices,
        _sortOption,
        _searchQuery,
        _showNewDevicesOnly,
        _showTrackersOnly
    ) { displayDevices, sort, query, showNewOnly, showTrackersOnly ->
        var filteredDevices = displayDevices.filter { !it.isIgnored }

        // Filter by search query
        if (query.isNotBlank()) {
            filteredDevices = filteredDevices.filter { device ->
                device.displayName.contains(query, ignoreCase = true) ||
                device.primaryMacAddress.contains(query, ignoreCase = true) ||
                device.macAddresses.any { it.macAddress.contains(query, ignoreCase = true) } ||
                device.manufacturer?.contains(query, ignoreCase = true) == true ||
                device.notes?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Filter for new devices (last 24 hours)
        if (showNewOnly) {
            filteredDevices = filteredDevices.filter { it.isNew() }
        }
        
        // Filter for trackers only
        if (showTrackersOnly) {
            filteredDevices = filteredDevices.filter { 
                it.isKnownTracker || it.getThreatLevel() != ThreatLevel.SAFE 
            }
        }
        
        // Sort devices
        when (sort) {
            DeviceDisplaySortOption.LAST_SEEN -> filteredDevices.sortedByDescending { it.lastSeen }
            DeviceDisplaySortOption.FIRST_SEEN -> filteredDevices.sortedByDescending { it.firstSeen }
            DeviceDisplaySortOption.NAME -> filteredDevices.sortedBy { it.displayName }
            DeviceDisplaySortOption.THREAT_LEVEL -> filteredDevices.sortedByDescending { it.suspiciousScore }
            DeviceDisplaySortOption.DEVICE_TYPE -> filteredDevices.sortedBy { it.deviceType.name }
            DeviceDisplaySortOption.MAC_COUNT -> filteredDevices.sortedByDescending { it.macAddressCount }
        }
    }
    
    // Statistics
    val deviceStats = allDisplayDevices.map { devices ->
        DeviceStats(
            totalDevices = devices.size,
            newDevices = devices.count { it.isNew() },
            knownTrackers = devices.count { it.isKnownTracker },
            suspiciousDevices = devices.count { it.getThreatLevel() in listOf(ThreatLevel.HIGH, ThreatLevel.CRITICAL) },
            trackedDevices = devices.count { it.isTracked }
        )
    }

    fun labelDevice(deviceUuid: String, label: String) {
        viewModelScope.launch {
            val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
            device?.let {
                val updatedDevice = it.copy(notes = label.ifBlank { null })
                fingerprintRepository.updateDeviceFingerprint(updatedDevice)
            }
        }
    }

    fun toggleTracking(deviceUuid: String) {
        viewModelScope.launch {
            val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
            device?.let {
                fingerprintRepository.updateTrackedStatus(deviceUuid, !it.isTracked)
            }
        }
    }
    
    fun ignoreDevice(deviceUuid: String) {
        viewModelScope.launch {
            val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
            device?.let {
                fingerprintRepository.updateIgnoredStatus(deviceUuid, !it.isIgnored)
            }
        }
    }

    fun deleteDevice(deviceUuid: String) {
        viewModelScope.launch {
            fingerprintRepository.deleteDeviceFingerprint(deviceUuid)
        }
    }
    
    fun refreshDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            _isLoading.value = true
            
            try {
                // Start single scan cycle
                val serviceIntent = Intent(context, BleRadarService::class.java).apply {
                    putExtra("action", "scan_once")
                }
                context.startForegroundService(serviceIntent)
                
                // Wait for scan to complete
                kotlinx.coroutines.delay(12000) // 12 seconds
                
            } catch (e: Exception) {
                android.util.Log.e("DeviceListViewModel", "Error during refresh: ${e.message}")
            } finally {
                _isScanning.value = false
                _isLoading.value = false
            }
        }
    }
    
    fun setSortOption(option: DeviceDisplaySortOption) {
        _sortOption.value = option
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleNewDevicesOnly() {
        _showNewDevicesOnly.value = !_showNewDevicesOnly.value
    }
    
    fun toggleTrackersOnly() {
        _showTrackersOnly.value = !_showTrackersOnly.value
    }
    
    fun isDeviceNew(device: DeviceDisplayModel): Boolean {
        return device.isNew()
    }
    
    fun performEmergencyScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _isLoading.value = true
            val deferred = CompletableDeferred<Unit>()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BleRadarService.ACTION_EMERGENCY_SCAN_COMPLETE) {
                        deferred.complete(Unit)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(BleRadarService.ACTION_EMERGENCY_SCAN_COMPLETE),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(BleRadarService.ACTION_EMERGENCY_SCAN_COMPLETE)
                )
            }
            try {
                android.util.Log.d("DeviceListViewModel", "Starting emergency scan")
                val wasServiceRunning = isServiceRunning()
                if (wasServiceRunning) {
                    val stopIntent = Intent(context, BleRadarService::class.java)
                    context.stopService(stopIntent)
                    kotlinx.coroutines.delay(1000)
                }
                val serviceIntent = Intent(context, BleRadarService::class.java).apply {
                    putExtra("action", "emergency_scan")
                }
                context.startForegroundService(serviceIntent)
                android.util.Log.d("DeviceListViewModel", "Emergency scan service started, waiting for completion")

                withTimeout(20000L) { // 20 seconds timeout
                    deferred.await()
                }

                android.util.Log.d("DeviceListViewModel", "Emergency scan completed, performing analysis")
                performEmergencyAnalysis()

                if (wasServiceRunning) {
                    kotlinx.coroutines.delay(1000)
                    val regularServiceIntent = Intent(context, BleRadarService::class.java)
                    context.startForegroundService(regularServiceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceListViewModel", "Error during emergency scan: ${e.message}")
            } finally {
                android.util.Log.d("DeviceListViewModel", "Emergency scan finished")
                context.unregisterReceiver(receiver)
                val stopIntent = Intent(context, BleRadarService::class.java)
                context.stopService(stopIntent)
                _isScanning.value = false
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun performEmergencyAnalysis() {
        try {
            // Get devices seen in the last 30 minutes
            val recentTime = System.currentTimeMillis() - (30 * 60 * 1000)
            val recentDevices = fingerprintRepository.getDevicesSeenSince(recentTime)
            
            recentDevices.collect { devices ->
                android.util.Log.d("DeviceListViewModel", "Emergency analysis for ${devices.size} recent devices")
                
                // Update suspicious scores for recent devices
                devices.forEach { device ->
                    if (!device.isTracked) {
                        // Calculate enhanced suspicious score
                        val enhancedScore = calculateEnhancedSuspiciousScore(device)
                        if (enhancedScore > device.suspiciousScore) {
                            fingerprintRepository.updateSuspiciousScore(device.deviceUuid, enhancedScore)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceListViewModel", "Error during emergency analysis: ${e.message}")
        }
    }
    
    private suspend fun calculateEnhancedSuspiciousScore(device: com.bleradar.data.database.DeviceFingerprint): Float {
        var score = device.suspiciousScore
        
        // Increase score for known trackers
        if (device.isKnownTracker) {
            score += 0.3f
        }
        
        // Increase score for devices with multiple MAC addresses
        if (device.macAddressCount > 1) {
            score += 0.2f
        }
        
        // Increase score for high detection frequency
        if (device.totalDetections > 50) {
            score += 0.1f
        }
        
        // Get unique location count
        val uniqueLocations = fingerprintRepository.getUniqueLocationCountForDevice(device.deviceUuid)
        if (uniqueLocations > 3) {
            score += 0.2f
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (BleRadarService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

// Legacy enum for backward compatibility with DeviceSortOption
enum class DeviceSortOption {
    LAST_SEEN,
    FIRST_SEEN,
    NAME,
    RSSI,
    THREAT_LEVEL
}