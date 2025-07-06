package com.bleradar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.BleRadarService

class PeriodicScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "com.bleradar.START_SCANNING" -> {
                val settingsManager = SettingsManager(context)
                // Only start service if it was enabled
                if (settingsManager.isServiceEnabled) {
                    val serviceIntent = Intent(context, BleRadarService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}