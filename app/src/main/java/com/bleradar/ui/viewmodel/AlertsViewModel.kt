package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.repository.DeviceRepository
import com.bleradar.data.database.BleDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    // Show devices with lower threshold to include more potentially suspicious devices, but filter out known legitimate devices
    val suspiciousDevices = deviceRepository.getSuspiciousDevices(0.3f).map { devices ->
        devices.filter { device ->
            !isLegitimateDevice(device) && !device.isIgnored
        }
    }
    val trackedDevices = deviceRepository.getTrackedDevices()
    
    // Also show known tracker types even if not flagged as suspicious
    val knownTrackers: Flow<List<BleDevice>> = deviceRepository.getAllDevices().map { devices ->
        devices.filter { device ->
            isKnownTrackerType(device) && !device.isIgnored
        }
    }
    
    private fun isKnownTrackerType(device: BleDevice): Boolean {
        val knownTrackerServices = setOf(
            "0000FE9F-0000-1000-8000-00805F9B34FB", // Apple AirTag
            "0000FD5A-0000-1000-8000-00805F9B34FB", // Samsung SmartTag
            "0000FEED-0000-1000-8000-00805F9B34FB"  // Tile
        )
        
        val trackerNamePatterns = setOf(
            "AirTag", "SmartTag", "Tile", "Galaxy SmartTag", "SmartTag+", "SmartTag2"
        )
        
        // Check by service UUID
        if (device.services?.let { services ->
            knownTrackerServices.any { knownService -> 
                services.contains(knownService, ignoreCase = true) 
            }
        } == true) {
            return true
        }
        
        // Check by device name
        val deviceName = device.deviceName?.lowercase() ?: ""
        return trackerNamePatterns.any { pattern ->
            deviceName.contains(pattern.lowercase())
        }
    }
    
    private fun isLegitimateDevice(device: BleDevice): Boolean {
        val deviceName = device.deviceName?.lowercase() ?: ""
        val legitimateDevicePatterns = setOf(
            // Entertainment devices
            "tv", "television", "samsung tv", "lg tv", "sony tv", "smart tv",
            "soundbar", "sound bar", "speaker", "bose", "sonos", "jbl",
            "home theatre", "home theater", "yamaha", "denon", "onkyo",
            
            // Home automation
            "philips hue", "nest", "alexa", "echo", "google home", "smart bulb",
            "ring", "doorbell", "camera", "thermostat", "smart switch",
            
            // Gaming & Computing
            "playstation", "xbox", "nintendo", "controller", "gaming",
            "keyboard", "mouse", "laptop", "desktop", "printer",
            
            // Audio devices
            "headphones", "earbuds", "airpods", "galaxy buds", "beats",
            "wireless headset", "bluetooth speaker",
            
            // Car & Transportation
            "car", "bmw", "mercedes", "audi", "toyota", "honda", "ford",
            "carplay", "android auto", "hands-free"
        )
        
        return legitimateDevicePatterns.any { pattern ->
            deviceName.contains(pattern)
        }
    }
    
    // Device management functions
    fun ignoreDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceIgnoreStatus(deviceAddress, true)
        }
    }
    
    fun unignoreDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceIgnoreStatus(deviceAddress, false)
        }
    }
    
    fun trackDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceTrackingStatus(deviceAddress, true)
        }
    }
    
    fun untrackDevice(deviceAddress: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceTrackingStatus(deviceAddress, false)
        }
    }
    
    fun markDeviceAsLegitimate(deviceAddress: String) {
        viewModelScope.launch {
            // Mark as ignored and reset suspicious score
            deviceRepository.updateDeviceIgnoreStatus(deviceAddress, true)
            deviceRepository.updateDevice(
                deviceRepository.getDevice(deviceAddress)?.copy(
                    followingScore = 0f,
                    label = "Legitimate Device"
                ) ?: return@launch
            )
        }
    }
}