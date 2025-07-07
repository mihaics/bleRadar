package com.bleradar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bleradar.preferences.SettingsManager
import com.bleradar.repository.DeviceRepository
import com.bleradar.data.database.BleRadarDatabase
import android.util.Log

class DataCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = BleRadarDatabase.getDatabase(context)
    private val deviceRepository = DeviceRepository(database)
    private val settingsManager = SettingsManager(context)

    companion object {
        const val WORK_NAME = "data_cleanup_work"
        private const val TAG = "DataCleanupWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            if (!settingsManager.autoCleanupEnabled) {
                Log.d(TAG, "Auto cleanup is disabled, skipping")
                return Result.success()
            }

            Log.d(TAG, "Starting data cleanup")

            // Calculate cutoff times based on retention settings
            val detectionCutoff = System.currentTimeMillis() - settingsManager.getDetectionRetentionMillis()
            val locationCutoff = System.currentTimeMillis() - settingsManager.getLocationRetentionMillis()

            Log.d(TAG, "Detection cutoff: $detectionCutoff, Location cutoff: $locationCutoff")

            // Clean up old detection data
            val deletedDetections = deviceRepository.deleteOldDetections(detectionCutoff)
            Log.d(TAG, "Deleted $deletedDetections old detections")

            // Clean up old location data
            val deletedLocations = deviceRepository.deleteOldLocations(locationCutoff)
            Log.d(TAG, "Deleted $deletedLocations old location records")

            // Clean up old detection patterns (use same cutoff as detections)
            val deletedPatterns = deviceRepository.deleteOldPatterns(detectionCutoff)
            Log.d(TAG, "Deleted $deletedPatterns old detection patterns")

            // Clean up devices that have no recent detections and are not tracked/labeled
            cleanupOrphanedDevices(detectionCutoff)

            Log.d(TAG, "Data cleanup completed successfully")
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Data cleanup failed", exception)
            Result.retry()
        }
    }

    private suspend fun cleanupOrphanedDevices(cutoffTime: Long) {
        try {
            // Get all devices
            val allDevices = deviceRepository.getAllDevicesSync()
            
            var orphanedCount = 0
            allDevices.forEach { device ->
                // Don't delete tracked devices or labeled devices
                if (device.isTracked || !device.label.isNullOrBlank()) {
                    return@forEach
                }
                
                // Check if device has any recent detections
                val recentDetections = deviceRepository.getDetectionsForDeviceSync(device.deviceAddress)
                    .filter { it.timestamp > cutoffTime }
                
                // If no recent detections and device is not important, delete it
                if (recentDetections.isEmpty()) {
                    deviceRepository.deleteDevice(device.deviceAddress)
                    orphanedCount++
                    Log.d(TAG, "Deleted orphaned device: ${device.deviceAddress}")
                }
            }
            
            Log.d(TAG, "Cleaned up $orphanedCount orphaned devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up orphaned devices", e)
        }
    }
}