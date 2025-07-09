package com.bleradar.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.BleRadarService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val settingsManager: SettingsManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ScanWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic scan work")
        
        // Check if service is still enabled
        if (!settingsManager.isServiceEnabled) {
            Log.d(TAG, "Service disabled, cancelling scan work")
            return Result.failure()
        }

        try {
            // Start the service to perform a scan
            val serviceIntent = Intent(context, BleRadarService::class.java)
            serviceIntent.putExtra("action", "scan_once")
            context.startForegroundService(serviceIntent)
            
            Log.d(TAG, "Scan work completed successfully")
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in scan work: ${e.message}")
            return Result.retry()
        }
    }
}