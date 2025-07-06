package com.bleradar.repository

import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleRadarDatabase
import com.bleradar.data.database.LocationRecord
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
}