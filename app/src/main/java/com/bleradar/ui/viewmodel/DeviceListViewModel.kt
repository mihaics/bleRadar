package com.bleradar.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.analysis.FollowingDetector
import com.bleradar.data.database.BleDevice
import com.bleradar.repository.DeviceRepository
import com.bleradar.service.BleRadarService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeviceSortOption {
    LAST_SEEN,
    FIRST_SEEN,
    NAME,
    RSSI,
    THREAT_LEVEL
}

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val followingDetector: FollowingDetector
) : ViewModel() {
    
    private val _sortOption = MutableStateFlow(DeviceSortOption.LAST_SEEN)
    val sortOption: StateFlow<DeviceSortOption> = _sortOption.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showNewDevicesOnly = MutableStateFlow(false)
    val showNewDevicesOnly: StateFlow<Boolean> = _showNewDevicesOnly.asStateFlow()
    
    private val allDevices = deviceRepository.getAllDevices()
    
    val devices = combine(
        allDevices,
        _sortOption,
        _searchQuery,
        _showNewDevicesOnly
    ) { devices, sort, query, showNewOnly ->
        var filteredDevices = devices
        
        // Filter by search query
        if (query.isNotBlank()) {
            filteredDevices = filteredDevices.filter { device ->
                device.deviceName?.contains(query, ignoreCase = true) == true ||
                device.deviceAddress.contains(query, ignoreCase = true) ||
                device.label?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Filter for new devices (last 24 hours)
        if (showNewOnly) {
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            filteredDevices = filteredDevices.filter { device ->
                device.firstSeen > last24Hours
            }
        }
        
        // Sort devices
        when (sort) {
            DeviceSortOption.LAST_SEEN -> filteredDevices.sortedByDescending { it.lastSeen }
            DeviceSortOption.FIRST_SEEN -> filteredDevices.sortedByDescending { it.firstSeen }
            DeviceSortOption.NAME -> filteredDevices.sortedBy { it.deviceName ?: "Unknown" }
            DeviceSortOption.RSSI -> filteredDevices.sortedByDescending { it.rssi }
            DeviceSortOption.THREAT_LEVEL -> filteredDevices.sortedByDescending { it.followingScore }
        }
    }
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    init {
        // Start periodic following analysis
        viewModelScope.launch {
            devices.collect { deviceList ->
                deviceList.forEach { device ->
                    if (!device.isIgnored) {
                        followingDetector.analyzeDevice(device.deviceAddress)
                    }
                }
            }
        }
    }
    
    fun ignoreDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.ignoreDevice(deviceAddress)
        }
    }
    
    fun labelDevice(deviceAddress: String, label: String) {
        viewModelScope.launch {
            deviceRepository.labelDevice(deviceAddress, label)
        }
    }
    
    fun toggleTracking(deviceAddress: String) {
        viewModelScope.launch {
            val device = deviceRepository.getDevice(deviceAddress)
            device?.let {
                deviceRepository.setDeviceTracked(deviceAddress, !it.isTracked)
            }
        }
    }
    
    fun refreshDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            // Trigger manual scan if needed
            // This would typically communicate with the service
            kotlinx.coroutines.delay(2000) // Simulate scan duration
            _isScanning.value = false
        }
    }
    
    fun setSortOption(option: DeviceSortOption) {
        _sortOption.value = option
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleNewDevicesOnly() {
        _showNewDevicesOnly.value = !_showNewDevicesOnly.value
    }
    
    fun isDeviceNew(device: BleDevice): Boolean {
        val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return device.firstSeen > last24Hours
    }
    
    fun performEmergencyScan() {
        viewModelScope.launch {
            _isScanning.value = true
            
            // Start or restart the BLE radar service for emergency scan
            val serviceIntent = Intent(context, BleRadarService::class.java)
            context.startForegroundService(serviceIntent)
            
            // Wait for scan to complete (10 seconds as defined in service)
            kotlinx.coroutines.delay(12000) // 12 seconds to ensure scan completion
            
            // Perform emergency assessment on all recently detected devices
            performEmergencyAssessment()
            
            _isScanning.value = false
        }
    }
    
    private suspend fun performEmergencyAssessment() {
        try {
            // Get all devices detected in the last 30 minutes
            val recentDevices = deviceRepository.getRecentDevices(30 * 60 * 1000) // 30 minutes
            
            // Run advanced analysis on all recent devices
            recentDevices.forEach { device ->
                if (!device.isIgnored) {
                    followingDetector.analyzeDevice(device.deviceAddress)
                }
            }
            
            android.util.Log.d("DeviceListViewModel", "Emergency assessment completed for ${recentDevices.size} devices")
        } catch (e: Exception) {
            android.util.Log.e("DeviceListViewModel", "Error during emergency assessment: ${e.message}")
        }
    }
}