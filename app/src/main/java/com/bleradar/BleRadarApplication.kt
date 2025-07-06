package com.bleradar

import android.app.Application
import android.content.Intent
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.BleRadarService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BleRadarApplication : Application() {
    
    @Inject
    lateinit var settingsManager: SettingsManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Auto-start service if it was enabled previously
        if (settingsManager.isServiceEnabled) {
            val intent = Intent(this, BleRadarService::class.java)
            startForegroundService(intent)
        }
    }
}