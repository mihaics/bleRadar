package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.BleDetection
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
    
    val devices = deviceRepository.getAllDevices()
    val currentLocation = locationTracker.currentLocation
    
    init {
        loadRecentDetections()
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
    
    fun getDetectionsForDevice(deviceAddress: String): List<BleDetection> {
        return _detections.value.filter { it.deviceAddress == deviceAddress }
    }
    
    fun getDeviceLocationHistory(deviceAddress: String, days: Int = 7): List<BleDetection> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return _detections.value.filter { detection ->
            detection.deviceAddress == deviceAddress && detection.timestamp > cutoffTime
        }.sortedByDescending { it.timestamp }
    }
    
    fun refreshDetections() {
        loadRecentDetections()
        viewModelScope.launch {
            locationTracker.getCurrentLocation()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationTracking()
    }
}