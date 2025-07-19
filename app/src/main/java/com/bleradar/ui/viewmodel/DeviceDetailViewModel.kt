package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.DeviceFingerprint
import com.bleradar.data.database.DeviceMacAddress
import com.bleradar.data.database.FingerprintDetection
import com.bleradar.repository.FingerprintRepository
import com.bleradar.ui.model.DeviceDisplayModel
import com.bleradar.ui.model.DetectionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val fingerprintRepository: FingerprintRepository
) : ViewModel() {
    
    private val _device = MutableStateFlow<DeviceDisplayModel?>(null)
    val device: StateFlow<DeviceDisplayModel?> = _device.asStateFlow()
    
    private val _detections = MutableStateFlow<List<FingerprintDetection>>(emptyList())
    val detections: StateFlow<List<FingerprintDetection>> = _detections.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<FingerprintDetection>>(emptyList())
    val locationHistory: StateFlow<List<FingerprintDetection>> = _locationHistory.asStateFlow()
    
    private val _macAddresses = MutableStateFlow<List<DeviceMacAddress>>(emptyList())
    val macAddresses: StateFlow<List<DeviceMacAddress>> = _macAddresses.asStateFlow()
    
    fun loadDevice(deviceIdentifier: String) {
        viewModelScope.launch {
            try {
                // First try to load by UUID
                var deviceFingerprint = fingerprintRepository.getDeviceFingerprint(deviceIdentifier)
                
                // If not found by UUID, try to find by MAC address
                if (deviceFingerprint == null) {
                    val macAddress = fingerprintRepository.getMacAddressByAddress(deviceIdentifier)
                    if (macAddress != null) {
                        deviceFingerprint = fingerprintRepository.getDeviceFingerprint(macAddress.deviceUuid)
                    }
                }
                
                if (deviceFingerprint != null) {
                    // Load MAC addresses for this device
                    val macAddresses = fingerprintRepository.getMacAddressesForDevice(deviceFingerprint.deviceUuid)
                    _macAddresses.value = macAddresses
                    
                    // Create display model
                    _device.value = DeviceDisplayModel.fromFingerprint(deviceFingerprint, macAddresses)
                    
                    // Load all detections for this device
                    fingerprintRepository.getDetectionsForDevice(deviceFingerprint.deviceUuid).collect { detectionList ->
                        _detections.value = detectionList
                        // Filter detections with valid location data for location history
                        _locationHistory.value = detectionList.filter { detection ->
                            detection.latitude != 0.0 && detection.longitude != 0.0
                        }.sortedByDescending { it.timestamp }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceDetailViewModel", "Error loading device: ${e.message}")
            }
        }
    }
    
    fun updateDeviceLabel(deviceUuid: String, label: String) {
        viewModelScope.launch {
            try {
                val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
                device?.let {
                    val updatedDevice = it.copy(notes = label.ifBlank { null })
                    fingerprintRepository.updateDeviceFingerprint(updatedDevice)
                    // Reload device to update UI
                    loadDevice(deviceUuid)
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceDetailViewModel", "Error updating device label: ${e.message}")
            }
        }
    }
    
    fun toggleDeviceTracking(deviceUuid: String) {
        viewModelScope.launch {
            try {
                val currentDevice = _device.value
                currentDevice?.let { device ->
                    fingerprintRepository.updateTrackedStatus(deviceUuid, !device.isTracked)
                    // Reload device to update UI
                    loadDevice(deviceUuid)
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceDetailViewModel", "Error toggling device tracking: ${e.message}")
            }
        }
    }
    
    fun getLocationHistoryForDateRange(startTime: Long, endTime: Long): List<FingerprintDetection> {
        return _locationHistory.value.filter { detection ->
            detection.timestamp in startTime..endTime
        }
    }
    
    fun getDetectionsByDay(): Map<String, List<FingerprintDetection>> {
        return _locationHistory.value.groupBy { detection ->
            val date = java.util.Date(detection.timestamp)
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
        }
    }
    
    fun getDetectionStatsForPeriod(days: Int): DetectionStats {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        val recentDetections = _detections.value.filter { it.timestamp > cutoffTime }
        
        return DetectionStats(
            totalDetections = recentDetections.size,
            uniqueDays = recentDetections.groupBy { detection ->
                val date = java.util.Date(detection.timestamp)
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
            }.size,
            averageRssi = if (recentDetections.isNotEmpty()) {
                recentDetections.map { it.rssi }.average()
            } else 0.0,
            maxRssi = recentDetections.maxOfOrNull { it.rssi } ?: 0,
            minRssi = recentDetections.minOfOrNull { it.rssi } ?: 0,
            locationCount = recentDetections.filter { it.latitude != 0.0 && it.longitude != 0.0 }.size
        )
    }
}

