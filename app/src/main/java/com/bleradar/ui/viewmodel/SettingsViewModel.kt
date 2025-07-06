package com.bleradar.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.preferences.SettingsManager
import com.bleradar.repository.DeviceRepository
import com.bleradar.service.BleRadarService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    
    private val _scanInterval = MutableStateFlow(settingsManager.scanIntervalMinutes)
    val scanInterval: StateFlow<Int> = _scanInterval.asStateFlow()
    
    private val _isServiceRunning = MutableStateFlow(isServiceActuallyRunning())
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    private val _databaseSize = MutableStateFlow("Calculating...")
    val databaseSize: StateFlow<String> = _databaseSize.asStateFlow()
    
    init {
        refreshDatabaseSize()
        _isServiceRunning.value = isServiceActuallyRunning()
    }
    
    fun setScanInterval(minutes: Int) {
        _scanInterval.value = minutes
        settingsManager.scanIntervalMinutes = minutes
        
        // If service is running, restart it with new interval
        if (_isServiceRunning.value) {
            restartServiceWithNewSettings()
        }
    }
    
    fun startService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.startForegroundService(intent)
        settingsManager.isServiceEnabled = true
        _isServiceRunning.value = true
    }
    
    fun stopService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.stopService(intent)
        settingsManager.isServiceEnabled = false
        _isServiceRunning.value = false
    }
    
    private fun restartServiceWithNewSettings() {
        val intent = Intent(context, BleRadarService::class.java)
        context.stopService(intent)
        
        // Small delay to ensure service is stopped before restarting
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            context.startForegroundService(intent)
        }
    }
    
    private fun isServiceActuallyRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (BleRadarService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
    
    fun refreshDatabaseSize() {
        viewModelScope.launch {
            // Calculate database size
            val devices = deviceRepository.getAllDevices()
            val detections = deviceRepository.getDetectionsSince(0)
            val locations = deviceRepository.getLocationsSince(0)
            
            // This is a simplified calculation
            _databaseSize.value = "~${(devices.hashCode() + detections.hashCode() + locations.hashCode()) / 1000} KB"
        }
    }
    
    fun clearDatabase() {
        viewModelScope.launch {
            val oldestTime = System.currentTimeMillis()
            deviceRepository.deleteOldDetections(oldestTime)
            deviceRepository.deleteOldLocations(oldestTime)
            refreshDatabaseSize()
        }
    }
}