package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.FingerprintDetection
import com.bleradar.data.database.DeviceFingerprint
import com.bleradar.data.database.LocationRecord
import com.bleradar.location.LocationTracker
import com.bleradar.repository.FingerprintRepository
import com.bleradar.ui.model.DeviceDisplayModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fingerprintRepository: FingerprintRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {
    
    private val _detections = MutableStateFlow<List<FingerprintDetection>>(emptyList())
    val detections: StateFlow<List<FingerprintDetection>> = _detections.asStateFlow()
    
    private val _deviceLocations = MutableStateFlow<List<DeviceLocation>>(emptyList())
    val deviceLocations: StateFlow<List<DeviceLocation>> = _deviceLocations.asStateFlow()
    
    private val _devices = MutableStateFlow<List<DeviceDisplayModel>>(emptyList())
    val devices: StateFlow<List<DeviceDisplayModel>> = _devices.asStateFlow()
    
    val currentLocation = locationTracker.currentLocation
    
    init {
        loadRecentDetections()
        loadDeviceLocations()
        loadDevices()
        locationTracker.startLocationTracking()
    }
    
    private fun loadRecentDetections() {
        viewModelScope.launch {
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            fingerprintRepository.getDetectionsSince(last24Hours).collect { detectionList ->
                // Filter out detections without valid location data
                _detections.value = detectionList.filter { detection ->
                    detection.latitude != 0.0 && detection.longitude != 0.0
                }
            }
        }
    }
    
    private fun loadDevices() {
        viewModelScope.launch {
            fingerprintRepository.getAllDeviceFingerprints().collect { fingerprints ->
                val deviceModels = fingerprints.map { fingerprint ->
                    val macAddresses = fingerprintRepository.getMacAddressesForDevice(fingerprint.deviceUuid)
                    DeviceDisplayModel.fromFingerprint(fingerprint, macAddresses)
                }
                _devices.value = deviceModels
            }
        }
    }
    
    private fun loadDeviceLocations() {
        viewModelScope.launch {
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            fingerprintRepository.getDetectionsSince(last24Hours).collect { detectionList ->
                // Filter out detections without valid location data
                val validDetections = detectionList.filter { detection ->
                    detection.latitude != 0.0 && detection.longitude != 0.0
                }
                
                // Group by device UUID and get latest location for each device
                val deviceLocations = validDetections
                    .groupBy { detection -> detection.deviceUuid }
                    .mapNotNull { (deviceUuid: String, detections: List<FingerprintDetection>) ->
                        // Get the latest detection for this device
                        val latestDetection = detections.maxByOrNull { detection -> detection.timestamp }
                        latestDetection?.let { detection ->
                            DeviceLocation(
                                deviceUuid = deviceUuid,
                                macAddress = detection.macAddress,
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
                    .sortedByDescending { deviceLocation -> deviceLocation.timestamp }
                
                _deviceLocations.value = deviceLocations
            }
        }
    }
    
    fun loadDeviceLocationHistory(deviceUuid: String, days: Int = 7) {
        viewModelScope.launch {
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
            fingerprintRepository.getDetectionsSince(cutoffTime).collect { detectionList ->
                // Filter for specific device and valid location data
                val validDetections = detectionList.filter { detection ->
                    detection.deviceUuid == deviceUuid && 
                    detection.latitude != 0.0 && 
                    detection.longitude != 0.0
                }
                
                // Convert all detections to DeviceLocation objects for this device
                val deviceLocations = validDetections.map { detection ->
                    DeviceLocation(
                        deviceUuid = deviceUuid,
                        macAddress = detection.macAddress,
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
                }.sortedByDescending { deviceLocation -> deviceLocation.timestamp }
                
                _deviceLocations.value = deviceLocations
            }
        }
    }
    
    fun getDetectionsForDevice(deviceUuid: String): List<FingerprintDetection> {
        return _detections.value.filter { detection -> detection.deviceUuid == deviceUuid }
    }
    
    fun getDeviceLocation(deviceUuid: String): DeviceLocation? {
        return _deviceLocations.value.find { deviceLocation -> deviceLocation.deviceUuid == deviceUuid }
    }
    
    fun getDeviceLocationHistory(deviceUuid: String, days: Int = 7): List<FingerprintDetection> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return _detections.value.filter { detection ->
            detection.deviceUuid == deviceUuid && detection.timestamp > cutoffTime
        }.sortedByDescending { detection -> detection.timestamp }
    }
    
    fun refreshDetections() {
        loadRecentDetections()
        loadDeviceLocations()
        viewModelScope.launch {
            locationTracker.getCurrentLocation()
        }
    }
    
    fun toggleDeviceTracking(deviceUuid: String) {
        viewModelScope.launch {
            try {
                val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
                device?.let { deviceFingerprint ->
                    fingerprintRepository.updateTrackedStatus(deviceUuid, !deviceFingerprint.isTracked)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapViewModel", "Error toggling device tracking: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
    
    data class DeviceLocation(
        val deviceUuid: String,
        val macAddress: String,
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