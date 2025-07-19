package com.bleradar.repository

import com.bleradar.data.database.BleRadarDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleAnalyticsRepository @Inject constructor(
    private val database: BleRadarDatabase
) {
    
    suspend fun getSimpleAnalytics(daysBack: Int = 7): SimpleAnalytics {
        return try {
            val allDevices = database.deviceFingerprintDao().getAllDeviceFingerprints().first()
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
            
            val totalDevices = allDevices.size
            val trackedDevices = allDevices.count { it.isTracked }
            val suspiciousDevices = allDevices.count { it.suspiciousScore > 0.5f }
            val newDevices = allDevices.count { it.firstSeen >= cutoffTime }
            
            // Calculate average RSSI from recent detections
            val recentDetections = database.fingerprintDetectionDao().getRecentDetections(cutoffTime).first()
            val averageRssi = if (recentDetections.isNotEmpty()) {
                recentDetections.map { it.rssi }.average().toFloat()
            } else 0f
            
            val totalDetections = allDevices.sumOf { it.totalDetections }
            val mostActiveDevice = allDevices.maxByOrNull { it.totalDetections }
            
            // Find strongest signal from recent detections
            val strongestSignalDetection = recentDetections.maxByOrNull { it.rssi }
            val strongestSignalDevice = strongestSignalDetection?.let { detection ->
                allDevices.find { it.deviceUuid == detection.deviceUuid }
            }
            
            SimpleAnalytics(
                totalDevices = totalDevices,
                trackedDevices = trackedDevices,
                suspiciousDevices = suspiciousDevices,
                newDevices = newDevices,
                averageRssi = averageRssi,
                totalDetections = totalDetections,
                mostActiveDevice = mostActiveDevice?.let { 
                    DeviceInfo(it.deviceUuid, it.deviceName ?: "Unknown", it.totalDetections)
                },
                strongestSignalDevice = strongestSignalDevice?.let {
                    DeviceInfo(it.deviceUuid, it.deviceName ?: "Unknown", strongestSignalDetection.rssi)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("SimpleAnalyticsRepository", "Failed to get analytics", e)
            SimpleAnalytics()
        }
    }
    
    suspend fun getRecentActivity(hoursBack: Int = 24): List<RecentActivity> {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursBack.toLong())
            val detections = database.fingerprintDetectionDao().getRecentDetections(cutoffTime).first()
            
            detections.groupBy { it.deviceUuid }
                .map { (deviceUuid, detectionList) ->
                    val device = database.deviceFingerprintDao().getDeviceFingerprint(deviceUuid)
                    RecentActivity(
                        deviceUuid = deviceUuid,
                        deviceName = device?.deviceName ?: "Unknown",
                        detectionCount = detectionList.size,
                        lastSeen = detectionList.maxOfOrNull { it.timestamp } ?: 0L,
                        averageRssi = detectionList.map { it.rssi }.average().toFloat()
                    )
                }
                .sortedByDescending { it.lastSeen }
                .take(10)
        } catch (e: Exception) {
            android.util.Log.e("SimpleAnalyticsRepository", "Failed to get recent activity", e)
            emptyList()
        }
    }
}

data class SimpleAnalytics(
    val totalDevices: Int = 0,
    val trackedDevices: Int = 0,
    val suspiciousDevices: Int = 0,
    val newDevices: Int = 0,
    val averageRssi: Float = 0f,
    val totalDetections: Int = 0,
    val mostActiveDevice: DeviceInfo? = null,
    val strongestSignalDevice: DeviceInfo? = null
)

data class DeviceInfo(
    val deviceUuid: String,
    val name: String,
    val value: Int
)

data class RecentActivity(
    val deviceUuid: String,
    val deviceName: String,
    val detectionCount: Int,
    val lastSeen: Long,
    val averageRssi: Float
)