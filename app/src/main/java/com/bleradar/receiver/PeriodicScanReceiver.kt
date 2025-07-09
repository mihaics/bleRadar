package com.bleradar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.BleRadarService
import com.bleradar.worker.WorkManagerScheduler

class PeriodicScanReceiver : BroadcastReceiver() {
    private val tag = "PeriodicScanReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "Received action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "com.bleradar.START_SCANNING" -> {
                val settingsManager = SettingsManager(context)
                
                // Only start service if it was enabled
                if (settingsManager.isServiceEnabled) {
                    Log.d(tag, "Service was enabled, starting foreground service and scheduling periodic scans")
                    
                    // Start the foreground service
                    val serviceIntent = Intent(context, BleRadarService::class.java)
                    context.startForegroundService(serviceIntent)
                    
                    // Schedule periodic scans using WorkManager
                    val workManagerScheduler = WorkManagerScheduler(context, settingsManager)
                    workManagerScheduler.schedulePeriodicScans()
                    
                    // Also schedule data cleanup if enabled
                    if (settingsManager.autoCleanupEnabled) {
                        workManagerScheduler.scheduleDataCleanup()
                    }
                } else {
                    Log.d(tag, "Service was disabled, not starting")
                }
            }
        }
    }
}