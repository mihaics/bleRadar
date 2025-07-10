package com.bleradar.worker

import android.content.Context
import androidx.work.*
import com.bleradar.preferences.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    companion object {
        private const val TAG = "WorkManagerScheduler"
        private const val DATA_CLEANUP_WORK_NAME = "data_cleanup_periodic"
        private const val SCAN_WORK_NAME = "ble_scan_periodic"
    }

    private val workManager = WorkManager.getInstance(context)

    fun scheduleDataCleanup() {
        if (!settingsManager.autoCleanupEnabled) {
            Log.d(TAG, "Auto cleanup disabled, cancelling existing work")
            cancelDataCleanup()
            return
        }

        Log.d(TAG, "Scheduling periodic data cleanup")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            24, TimeUnit.HOURS, // Run every 24 hours
            6, TimeUnit.HOURS   // With a 6-hour flex period
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .addTag("data_cleanup")
            .build()

        workManager.enqueueUniquePeriodicWork(
            DATA_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )

        Log.d(TAG, "Periodic data cleanup scheduled")
    }

    fun cancelDataCleanup() {
        Log.d(TAG, "Cancelling periodic data cleanup")
        workManager.cancelUniqueWork(DATA_CLEANUP_WORK_NAME)
    }

    fun runCleanupNow() {
        Log.d(TAG, "Running immediate data cleanup")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val immediateCleanupRequest = OneTimeWorkRequestBuilder<DataCleanupWorker>()
            .setConstraints(constraints)
            .addTag("immediate_cleanup")
            .build()

        workManager.enqueue(immediateCleanupRequest)
    }

    fun getCleanupWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(DATA_CLEANUP_WORK_NAME)
    
    fun getLastCleanupTime(): Long {
        // This would require storing the last cleanup time in preferences
        // For now, return a default value
        return System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
    }
    
    fun schedulePeriodicScans() {
        if (!settingsManager.isServiceEnabled) {
            Log.d(TAG, "Service disabled, cancelling periodic scans")
            cancelPeriodicScans()
            return
        }

        val scanIntervalMinutes = settingsManager.scanIntervalMinutes.toLong()
        Log.d(TAG, "Scheduling periodic scans every $scanIntervalMinutes minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false) // Allow scanning even on low battery
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        val scanRequest = PeriodicWorkRequestBuilder<ScanWorker>(
            scanIntervalMinutes, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // 5-minute flex period
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .addTag("ble_scan")
            .build()

        workManager.enqueueUniquePeriodicWork(
            SCAN_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            scanRequest
        )

        Log.d(TAG, "Periodic scans scheduled")
    }
    
    fun cancelPeriodicScans() {
        Log.d(TAG, "Cancelling periodic scans")
        workManager.cancelUniqueWork(SCAN_WORK_NAME)
    }
    
    fun getScanWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(SCAN_WORK_NAME)
}