package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    // Show devices with lower threshold to include more potentially suspicious devices
    val suspiciousDevices = deviceRepository.getSuspiciousDevices(0.3f)
    val trackedDevices = deviceRepository.getTrackedDevices()
    
    // Also show known tracker types even if not flagged as suspicious
    val knownTrackers: Flow<List<com.bleradar.data.database.BleDevice>> = deviceRepository.getAllDevices().map { devices ->
        devices.filter { device ->
            isKnownTrackerType(device) && !device.isIgnored
        }
    }
    
    private fun isKnownTrackerType(device: com.bleradar.data.database.BleDevice): Boolean {
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
}