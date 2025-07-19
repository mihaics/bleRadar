package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.repository.FingerprintRepository
import com.bleradar.ui.model.DeviceDisplayModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val fingerprintRepository: FingerprintRepository
) : ViewModel() {

    val suspiciousDevices = fingerprintRepository.getSuspiciousDevices(0.3f).map { devices ->
        devices.map { device ->
            val macAddresses = fingerprintRepository.getMacAddressesForDevice(device.deviceUuid)
            DeviceDisplayModel.fromFingerprint(device, macAddresses)
        }.filter { device ->
            !isLegitimateDevice(device.displayName) && !device.isTracked
        }
    }

    val trackedDevices = fingerprintRepository.getAllDeviceFingerprints().map { devices ->
        devices.filter { it.isTracked }.map { device ->
            val macAddresses = fingerprintRepository.getMacAddressesForDevice(device.deviceUuid)
            DeviceDisplayModel.fromFingerprint(device, macAddresses)
        }
    }
    
    val knownTrackers: Flow<List<DeviceDisplayModel>> = fingerprintRepository.getKnownTrackers().map { devices ->
        devices.map { device ->
            val macAddresses = fingerprintRepository.getMacAddressesForDevice(device.deviceUuid)
            DeviceDisplayModel.fromFingerprint(device, macAddresses)
        }.filter { device ->
            !device.isTracked
        }
    }

    private fun isKnownTrackerType(services: String?, deviceName: String?): Boolean {
        val knownTrackerServices = setOf(
            "0000FE9F-0000-1000-8000-00805F9B34FB", // Apple AirTag
            "0000FD5A-0000-1000-8000-00805F9B34FB", // Samsung SmartTag
            "0000FEED-0000-1000-8000-00805F9B34FB"  // Tile
        )

        val trackerNamePatterns = setOf(
            "AirTag", "SmartTag", "Tile", "Galaxy SmartTag", "SmartTag+", "SmartTag2"
        )

        // Check by service UUID
        if (services?.let { s ->
                knownTrackerServices.any { knownService ->
                    s.contains(knownService, ignoreCase = true)
                }
            } == true) {
            return true
        }

        // Check by device name
        val lowerDeviceName = deviceName?.lowercase() ?: ""
        return trackerNamePatterns.any { pattern ->
            lowerDeviceName.contains(pattern.lowercase())
        }
    }

    private fun isLegitimateDevice(deviceName: String?): Boolean {
        val lowerDeviceName = deviceName?.lowercase() ?: ""
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
            lowerDeviceName.contains(pattern)
        }
    }

    // Device management functions
    fun ignoreDevice(deviceUuid: String) {
        viewModelScope.launch {
            fingerprintRepository.updateTrackedStatus(deviceUuid, false)
        }
    }

    fun unignoreDevice(deviceUuid: String) {
        viewModelScope.launch {
            fingerprintRepository.updateTrackedStatus(deviceUuid, true)
        }
    }

    fun trackDevice(deviceUuid: String) {
        viewModelScope.launch {
            fingerprintRepository.updateTrackedStatus(deviceUuid, true)
        }
    }

    fun untrackDevice(deviceUuid: String) {
        viewModelScope.launch {
            fingerprintRepository.updateTrackedStatus(deviceUuid, false)
        }
    }

    fun markDeviceAsLegitimate(deviceUuid: String) {
        viewModelScope.launch {
            // Mark as ignored and reset suspicious score
            fingerprintRepository.updateTrackedStatus(deviceUuid, false)
            val device = fingerprintRepository.getDeviceFingerprint(deviceUuid)
            device?.let { deviceFingerprint ->
                fingerprintRepository.updateDeviceFingerprint(
                    deviceFingerprint.copy(
                        followingScore = 0f,
                        suspiciousScore = 0f,
                        notes = "Legitimate Device"
                    )
                )
            }
        }
    }
}
