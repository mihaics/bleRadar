package com.bleradar.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val _scanInterval = MutableStateFlow(5)
    val scanInterval: StateFlow<Int> = _scanInterval.asStateFlow()
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    private val _databaseSize = MutableStateFlow("Calculating...")
    val databaseSize: StateFlow<String> = _databaseSize.asStateFlow()
    
    init {
        refreshDatabaseSize()
    }
    
    fun setScanInterval(minutes: Int) {
        _scanInterval.value = minutes
        // In a real app, you'd save this to preferences and update the service
    }
    
    fun startService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.startForegroundService(intent)
        _isServiceRunning.value = true
    }
    
    fun stopService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.stopService(intent)
        _isServiceRunning.value = false
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