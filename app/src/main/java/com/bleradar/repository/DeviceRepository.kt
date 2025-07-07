package com.bleradar.repository

import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleRadarDatabase
import com.bleradar.data.database.LocationRecord
import com.bleradar.data.database.DetectionPattern
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val database: BleRadarDatabase
) {
    fun getAllDevices(): Flow<List<BleDevice>> = database.bleDeviceDao().getAllDevices()
    
    fun getTrackedDevices(): Flow<List<BleDevice>> = database.bleDeviceDao().getTrackedDevices()
    
    fun getSuspiciousDevices(threshold: Float = 0.7f): Flow<List<BleDevice>> = 
        database.bleDeviceDao().getSuspiciousDevices(threshold)
    
    suspend fun getDevice(address: String): BleDevice? = database.bleDeviceDao().getDevice(address)
    
    suspend fun insertDevice(device: BleDevice) = database.bleDeviceDao().insertDevice(device)
    
    suspend fun updateDevice(device: BleDevice) = database.bleDeviceDao().updateDevice(device)
    
    suspend fun ignoreDevice(address: String) = database.bleDeviceDao().ignoreDevice(address)
    
    suspend fun labelDevice(address: String, label: String) = database.bleDeviceDao().labelDevice(address, label)
    
    suspend fun setDeviceTracked(address: String, tracked: Boolean) = 
        database.bleDeviceDao().setDeviceTracked(address, tracked)
    
    suspend fun updateFollowingScore(address: String, score: Float) = 
        database.bleDeviceDao().updateFollowingScore(address, score)
    
    suspend fun deleteDevice(address: String) = database.bleDeviceDao().deleteDevice(address)
    
    suspend fun getRecentDevices(sinceMillis: Long): List<BleDevice> {
        val cutoffTime = System.currentTimeMillis() - sinceMillis
        return database.bleDeviceDao().getRecentDevices(cutoffTime)
    }
    
    // Detection methods
    fun getDetectionsForDevice(address: String): Flow<List<BleDetection>> = 
        database.bleDetectionDao().getDetectionsForDevice(address)
    
    fun getDetectionsSince(startTime: Long): Flow<List<BleDetection>> = 
        database.bleDetectionDao().getDetectionsSince(startTime)
    
    fun getDetectionsForDeviceSince(address: String, startTime: Long): Flow<List<BleDetection>> = 
        database.bleDetectionDao().getDetectionsForDeviceSince(address, startTime)
    
    suspend fun getDetectionCount(address: String, startTime: Long): Int = 
        database.bleDetectionDao().getDetectionCount(address, startTime)
    
    suspend fun insertDetection(detection: BleDetection) = 
        database.bleDetectionDao().insertDetection(detection)
    
    suspend fun deleteOldDetections(cutoffTime: Long) = 
        database.bleDetectionDao().deleteOldDetections(cutoffTime)
    
    // Location methods
    suspend fun getLastLocation(): LocationRecord? = database.locationDao().getLastLocation()
    
    fun getLocationsSince(startTime: Long): Flow<List<LocationRecord>> = 
        database.locationDao().getLocationsSince(startTime)
    
    fun getRecentLocations(limit: Int = 100): Flow<List<LocationRecord>> = 
        database.locationDao().getRecentLocations(limit)
    
    suspend fun insertLocation(location: LocationRecord) = 
        database.locationDao().insertLocation(location)
    
    suspend fun deleteOldLocations(cutoffTime: Long) = 
        database.locationDao().deleteOldLocations(cutoffTime)
    
    // Enhanced device methods
    fun getKnownTrackers(): Flow<List<BleDevice>> = database.bleDeviceDao().getKnownTrackers()
    
    fun getSuspiciousDevicesByActivity(threshold: Float = 0.5f): Flow<List<BleDevice>> = 
        database.bleDeviceDao().getSuspiciousDevicesByActivity(threshold)
    
    suspend fun updateDeviceMetrics(
        address: String, 
        count: Int, 
        consecutive: Int, 
        maxConsecutive: Int, 
        avgRssi: Float, 
        rssiVar: Float, 
        lastMovement: Long, 
        stationary: Boolean, 
        suspiciousScore: Float, 
        knownTracker: Boolean, 
        trackerType: String?
    ) = database.bleDeviceDao().updateDeviceMetrics(
        address, count, consecutive, maxConsecutive, avgRssi, rssiVar, 
        lastMovement, stationary, suspiciousScore, knownTracker, trackerType
    )
    
    suspend fun updateLastAlertTime(address: String, alertTime: Long) = 
        database.bleDeviceDao().updateLastAlertTime(address, alertTime)
    
    suspend fun updateSuspiciousScore(address: String, score: Float) = 
        database.bleDeviceDao().updateSuspiciousScore(address, score)
    
    // Detection pattern methods
    fun getPatternsForDevice(address: String): Flow<List<DetectionPattern>> = 
        database.detectionPatternDao().getPatternsForDevice(address)
    
    fun getPatternsByType(type: String): Flow<List<DetectionPattern>> = 
        database.detectionPatternDao().getPatternsByType(type)
    
    fun getPatternsSince(startTime: Long): Flow<List<DetectionPattern>> = 
        database.detectionPatternDao().getPatternsSince(startTime)
    
    suspend fun getLastPatternForDevice(address: String, type: String): DetectionPattern? = 
        database.detectionPatternDao().getLastPatternForDevice(address, type)
    
    suspend fun insertPattern(pattern: DetectionPattern) = 
        database.detectionPatternDao().insertPattern(pattern)
    
    suspend fun deleteOldPatterns(cutoffTime: Long) = 
        database.detectionPatternDao().deleteOldPatterns(cutoffTime)
    
    // Synchronous methods for WorkManager
    suspend fun getAllDevicesSync(): List<BleDevice> = 
        database.bleDeviceDao().getAllDevicesSync()
    
    suspend fun getDetectionsForDeviceSync(address: String): List<BleDetection> = 
        database.bleDetectionDao().getDetectionsForDeviceSync(address)
}