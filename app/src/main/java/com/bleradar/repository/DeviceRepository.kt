package com.bleradar.repository

import com.bleradar.data.database.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val database: BleRadarDatabase,
    private val fingerprintRepository: FingerprintRepository
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
    
    // Additional methods for AlertsViewModel
    suspend fun updateDeviceIgnoreStatus(address: String, ignored: Boolean) {
        if (ignored) {
            database.bleDeviceDao().ignoreDevice(address)
        } else {
            // We need to update the device to unignore it
            val device = database.bleDeviceDao().getDevice(address)
            device?.let {
                database.bleDeviceDao().updateDevice(it.copy(isIgnored = false))
            }
        }
    }
    
    suspend fun updateDeviceTrackingStatus(address: String, tracked: Boolean) = 
        database.bleDeviceDao().setDeviceTracked(address, tracked)
    
    suspend fun toggleDeviceTracking(address: String) {
        val device = database.bleDeviceDao().getDevice(address)
        device?.let {
            database.bleDeviceDao().updateDevice(it.copy(isTracked = !it.isTracked))
        }
    }
    
    suspend fun toggleDeviceIgnore(address: String) {
        val device = database.bleDeviceDao().getDevice(address)
        device?.let {
            database.bleDeviceDao().updateDevice(it.copy(isIgnored = !it.isIgnored))
        }
    }
    
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
    
    // Bridge methods to fingerprint system
    suspend fun getDeviceByMacAddress(macAddress: String): DeviceFingerprint? {
        val deviceMac = fingerprintRepository.getDeviceByMacAddress(macAddress)
        return deviceMac?.let { fingerprintRepository.getDeviceFingerprint(it.deviceUuid) }
    }
    
    suspend fun getDeviceUuidByMacAddress(macAddress: String): String? {
        return fingerprintRepository.getDeviceUuidByMacAddress(macAddress)
    }
    
    suspend fun getMacAddressesForDevice(deviceUuid: String): List<DeviceMacAddress> {
        return fingerprintRepository.getMacAddressesForDevice(deviceUuid)
    }
    
    fun getAllDeviceFingerprints(): Flow<List<DeviceFingerprint>> {
        return fingerprintRepository.getAllDeviceFingerprints()
    }
    
    suspend fun getDeviceFingerprint(deviceUuid: String): DeviceFingerprint? {
        return fingerprintRepository.getDeviceFingerprint(deviceUuid)
    }
    
    suspend fun insertDeviceFingerprint(device: DeviceFingerprint) {
        return fingerprintRepository.insertDeviceFingerprint(device)
    }
    
    suspend fun updateDeviceFingerprint(device: DeviceFingerprint) {
        return fingerprintRepository.updateDeviceFingerprint(device)
    }
    
    // Fingerprint detection methods
    fun getFingerprintDetectionsForDevice(deviceUuid: String): Flow<List<FingerprintDetection>> {
        return fingerprintRepository.getDetectionsForDevice(deviceUuid)
    }
    
    fun getFingerprintDetectionsForDeviceSince(deviceUuid: String, since: Long): Flow<List<FingerprintDetection>> {
        return fingerprintRepository.getDetectionsForDeviceSince(deviceUuid, since)
    }
    
    suspend fun insertFingerprintDetection(detection: FingerprintDetection) {
        return fingerprintRepository.insertDetection(detection)
    }
    
    suspend fun getFingerprintDetectionCount(deviceUuid: String): Int {
        return fingerprintRepository.getDetectionCountForDevice(deviceUuid)
    }
    
    suspend fun getUniqueLocationCountForDevice(deviceUuid: String): Int {
        return fingerprintRepository.getUniqueLocationCountForDevice(deviceUuid)
    }
    
    suspend fun getAverageRssiForDevice(deviceUuid: String, since: Long): Float? {
        return fingerprintRepository.getAverageRssiForDevice(deviceUuid, since)
    }
    
    // Device statistics and analytics
    suspend fun getDeviceStatistics(deviceUuid: String): DeviceStatistics? {
        return fingerprintRepository.getDeviceStatistics(deviceUuid)
    }
    
    suspend fun getDeviceWithMacAddresses(deviceUuid: String): DeviceFingerprintWithMacs? {
        return fingerprintRepository.getDeviceWithMacAddresses(deviceUuid)
    }
    
    suspend fun getDeviceWithAllData(deviceUuid: String): DeviceFingerprintFullData? {
        return fingerprintRepository.getDeviceWithAllData(deviceUuid)
    }
    
    // Cleanup methods
    suspend fun cleanupOldFingerprintData(cutoffTime: Long) {
        fingerprintRepository.cleanupOldData(cutoffTime)
    }
}