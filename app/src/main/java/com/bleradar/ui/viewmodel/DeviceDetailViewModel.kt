package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.BleDetection
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val _device = MutableStateFlow<BleDevice?>(null)
    val device: StateFlow<BleDevice?> = _device.asStateFlow()
    
    private val _detections = MutableStateFlow<List<BleDetection>>(emptyList())
    val detections: StateFlow<List<BleDetection>> = _detections.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<BleDetection>>(emptyList())
    val locationHistory: StateFlow<List<BleDetection>> = _locationHistory.asStateFlow()
    
    fun loadDevice(deviceAddress: String) {
        viewModelScope.launch {
            // Load device info
            val deviceInfo = deviceRepository.getDevice(deviceAddress)
            _device.value = deviceInfo
            
            // Load all detections for this device
            deviceRepository.getDetectionsForDevice(deviceAddress).collect { detectionList ->
                _detections.value = detectionList
                // Filter detections with valid location data for location history
                _locationHistory.value = detectionList.filter { detection ->
                    detection.latitude != 0.0 && detection.longitude != 0.0
                }.sortedByDescending { it.timestamp }
            }
        }
    }
    
    fun updateDeviceLabel(deviceAddress: String, label: String) {
        viewModelScope.launch {
            deviceRepository.labelDevice(deviceAddress, label)
            // Reload device to update UI
            loadDevice(deviceAddress)
        }
    }
    
    fun toggleDeviceTracking(deviceAddress: String) {
        viewModelScope.launch {
            val currentDevice = _device.value
            currentDevice?.let { device ->
                deviceRepository.setDeviceTracked(deviceAddress, !device.isTracked)
                // Reload device to update UI
                loadDevice(deviceAddress)
            }
        }
    }
    
    fun ignoreDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.ignoreDevice(deviceAddress)
            // Reload device to update UI
            loadDevice(deviceAddress)
        }
    }
    
    fun getLocationHistoryForDateRange(startTime: Long, endTime: Long): List<BleDetection> {
        return _locationHistory.value.filter { detection ->
            detection.timestamp in startTime..endTime
        }
    }
    
    fun getDetectionsByDay(): Map<String, List<BleDetection>> {
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

data class DetectionStats(
    val totalDetections: Int,
    val uniqueDays: Int,
    val averageRssi: Double,
    val maxRssi: Int,
    val minRssi: Int,
    val locationCount: Int
)