package com.bleradar.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.preferences.SettingsManager
import com.bleradar.repository.FingerprintRepository
import com.bleradar.service.BleRadarService
import com.bleradar.worker.WorkManagerScheduler
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
    private val fingerprintRepository: FingerprintRepository,
    private val settingsManager: SettingsManager,
    private val workManagerScheduler: WorkManagerScheduler
) : ViewModel() {
    
    private val _scanInterval = MutableStateFlow(settingsManager.scanIntervalMinutes)
    val scanInterval: StateFlow<Int> = _scanInterval.asStateFlow()
    
    private val _isServiceRunning = MutableStateFlow(isServiceActuallyRunning())
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    private val _databaseSize = MutableStateFlow("Calculating...")
    val databaseSize: StateFlow<String> = _databaseSize.asStateFlow()
    
    private val _dataRetentionDays = MutableStateFlow(settingsManager.dataRetentionDays)
    val dataRetentionDays: StateFlow<Int> = _dataRetentionDays.asStateFlow()
    
    private val _autoCleanupEnabled = MutableStateFlow(settingsManager.autoCleanupEnabled)
    val autoCleanupEnabled: StateFlow<Boolean> = _autoCleanupEnabled.asStateFlow()
    
    private val _detectionRetentionDays = MutableStateFlow(settingsManager.detectionRetentionDays)
    val detectionRetentionDays: StateFlow<Int> = _detectionRetentionDays.asStateFlow()
    
    private val _locationRetentionDays = MutableStateFlow(settingsManager.locationRetentionDays)
    val locationRetentionDays: StateFlow<Int> = _locationRetentionDays.asStateFlow()
    
    init {
        refreshDatabaseSize()
        _isServiceRunning.value = isServiceActuallyRunning()
        
        // Schedule cleanup based on current settings
        if (settingsManager.autoCleanupEnabled) {
            workManagerScheduler.scheduleDataCleanup()
        }
        
        // Schedule periodic scans if service is enabled
        if (settingsManager.isServiceEnabled) {
            workManagerScheduler.schedulePeriodicScans()
        }
    }
    
    fun setScanInterval(minutes: Int) {
        _scanInterval.value = minutes
        settingsManager.scanIntervalMinutes = minutes
        
        // Update WorkManager schedule with new interval
        if (settingsManager.isServiceEnabled) {
            workManagerScheduler.schedulePeriodicScans()
        }
    }
    
    fun startService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.startForegroundService(intent)
        settingsManager.isServiceEnabled = true
        _isServiceRunning.value = true
        
        // Schedule periodic scans using WorkManager
        workManagerScheduler.schedulePeriodicScans()
    }
    
    fun stopService() {
        val intent = Intent(context, BleRadarService::class.java)
        context.stopService(intent)
        settingsManager.isServiceEnabled = false
        _isServiceRunning.value = false
        
        // Cancel periodic scans
        workManagerScheduler.cancelPeriodicScans()
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
            // Calculate database size using fingerprint repository
            val devices = fingerprintRepository.getAllDeviceFingerprints()
            
            // This is a simplified calculation - just use device count for now
            devices.collect { deviceList ->
                _databaseSize.value = "~${deviceList.size * 5} KB"
            }
        }
    }
    
    fun setDataRetentionDays(days: Int) {
        _dataRetentionDays.value = days
        settingsManager.dataRetentionDays = days
    }
    
    fun setAutoCleanupEnabled(enabled: Boolean) {
        _autoCleanupEnabled.value = enabled
        settingsManager.autoCleanupEnabled = enabled
        
        // Schedule or cancel cleanup based on setting
        if (enabled) {
            workManagerScheduler.scheduleDataCleanup()
        } else {
            workManagerScheduler.cancelDataCleanup()
        }
    }
    
    fun setDetectionRetentionDays(days: Int) {
        _detectionRetentionDays.value = days
        settingsManager.detectionRetentionDays = days
    }
    
    fun setLocationRetentionDays(days: Int) {
        _locationRetentionDays.value = days
        settingsManager.locationRetentionDays = days
    }
    
    fun getRetentionOptions(): List<Int> {
        return settingsManager.getRetentionOptions()
    }
    
    fun getRetentionDisplayName(days: Int): String {
        return settingsManager.getRetentionDisplayName(days)
    }
    
    fun runCleanup() {
        workManagerScheduler.runCleanupNow()
        
        // Refresh database size after a delay to allow cleanup to complete
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds
            refreshDatabaseSize()
        }
    }
    
    fun clearDatabase() {
        viewModelScope.launch {
            // Clear old data using existing method
            val currentTime = System.currentTimeMillis()
            fingerprintRepository.deleteOldDetections(currentTime)
            refreshDatabaseSize()
        }
    }
}