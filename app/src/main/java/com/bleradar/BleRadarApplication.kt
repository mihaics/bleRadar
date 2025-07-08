package com.bleradar

import android.app.Application
import android.content.Intent
import androidx.work.WorkManager
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.AnalyticsCollectionService
import com.bleradar.service.BleRadarService
import com.bleradar.worker.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BleRadarApplication : Application() {
    
    @Inject
    lateinit var settingsManager: SettingsManager
    
    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule data cleanup if enabled
        if (settingsManager.autoCleanupEnabled) {
            workManagerScheduler.scheduleDataCleanup()
        }
        
        // Note: Service startup moved to MainActivity to comply with modern Android restrictions
    }
}