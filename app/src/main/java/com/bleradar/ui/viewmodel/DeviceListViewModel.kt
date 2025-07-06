package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.analysis.FollowingDetector
import com.bleradar.data.database.BleDevice
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val followingDetector: FollowingDetector
) : ViewModel() {
    
    val devices = deviceRepository.getAllDevices()
    
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
}