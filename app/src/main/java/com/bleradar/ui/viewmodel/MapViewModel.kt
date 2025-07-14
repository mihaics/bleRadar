package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.LocationRecord
import com.bleradar.location.LocationTracker
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {
    
    private val _detections = MutableStateFlow<List<BleDetection>>(emptyList())
    val detections: StateFlow<List<BleDetection>> = _detections.asStateFlow()
    
    private val _deviceLocations = MutableStateFlow<List<DeviceLocation>>(emptyList())
    val deviceLocations: StateFlow<List<DeviceLocation>> = _deviceLocations.asStateFlow()
    
    val devices = deviceRepository.getAllDevices()
    val currentLocation = locationTracker.currentLocation
    
    init {
        loadRecentDetections()
        loadDeviceLocations()
        locationTracker.startLocationTracking()
    }
    
    private fun loadRecentDetections() {
        viewModelScope.launch {
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            deviceRepository.getDetectionsSince(last24Hours).collect { detectionList ->
                // Filter out detections without valid location data
                _detections.value = detectionList.filter { detection ->
                    detection.latitude != 0.0 && detection.longitude != 0.0
                }
            }
        }
    }
    
    private fun loadDeviceLocations() {
        viewModelScope.launch {
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            deviceRepository.getDetectionsSince(last24Hours).collect { detectionList ->
                // Filter out detections without valid location data
                val validDetections = detectionList.filter { detection ->
                    detection.latitude != 0.0 && detection.longitude != 0.0
                }
                
                // Group by device address and get latest location for each device
                val deviceLocations = validDetections
                    .groupBy { it.deviceAddress }
                    .mapNotNull { (deviceAddress, detections) ->
                        // Get the latest detection for this device
                        val latestDetection = detections.maxByOrNull { it.timestamp }
                        latestDetection?.let { detection ->
                            DeviceLocation(
                                deviceAddress = deviceAddress,
                                latitude = detection.latitude,
                                longitude = detection.longitude,
                                timestamp = detection.timestamp,
                                rssi = detection.rssi,
                                accuracy = detection.accuracy,
                                altitude = detection.altitude,
                                speed = detection.speed,
                                bearing = detection.bearing,
                                detectionCount = detections.size
                            )
                        }
                    }
                    .sortedByDescending { it.timestamp }
                
                _deviceLocations.value = deviceLocations
            }
        }
    }
    
    fun loadDeviceLocationHistory(deviceAddress: String, days: Int = 7) {
        viewModelScope.launch {
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
            deviceRepository.getDetectionsSince(cutoffTime).collect { detectionList ->
                // Filter for specific device and valid location data
                val validDetections = detectionList.filter { detection ->
                    detection.deviceAddress == deviceAddress && 
                    detection.latitude != 0.0 && 
                    detection.longitude != 0.0
                }
                
                // Convert all detections to DeviceLocation objects for this device
                val deviceLocations = validDetections.map { detection ->
                    DeviceLocation(
                        deviceAddress = deviceAddress,
                        latitude = detection.latitude,
                        longitude = detection.longitude,
                        timestamp = detection.timestamp,
                        rssi = detection.rssi,
                        accuracy = detection.accuracy,
                        altitude = detection.altitude,
                        speed = detection.speed,
                        bearing = detection.bearing,
                        detectionCount = 1
                    )
                }.sortedByDescending { it.timestamp }
                
                _deviceLocations.value = deviceLocations
            }
        }
    }
    
    fun getDetectionsForDevice(deviceAddress: String): List<BleDetection> {
        return _detections.value.filter { it.deviceAddress == deviceAddress }
    }
    
    fun getDeviceLocation(deviceAddress: String): DeviceLocation? {
        return _deviceLocations.value.find { it.deviceAddress == deviceAddress }
    }
    
    fun getDeviceLocationHistory(deviceAddress: String, days: Int = 7): List<BleDetection> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return _detections.value.filter { detection ->
            detection.deviceAddress == deviceAddress && detection.timestamp > cutoffTime
        }.sortedByDescending { it.timestamp }
    }
    
    fun refreshDetections() {
        loadRecentDetections()
        loadDeviceLocations()
        viewModelScope.launch {
            locationTracker.getCurrentLocation()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
    
    data class DeviceLocation(
        val deviceAddress: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val rssi: Int,
        val accuracy: Float,
        val altitude: Double? = null,
        val speed: Float? = null,
        val bearing: Float? = null,
        val detectionCount: Int = 1
    )
}