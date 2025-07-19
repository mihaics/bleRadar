package com.bleradar.repository

import com.bleradar.data.database.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FingerprintRepository @Inject constructor(
    private val database: BleRadarDatabase
) {
    
    // Device Fingerprint methods
    fun getAllDeviceFingerprints(): Flow<List<DeviceFingerprint>> = 
        database.deviceFingerprintDao().getAllDeviceFingerprints()
    
    fun getKnownTrackers(): Flow<List<DeviceFingerprint>> = 
        database.deviceFingerprintDao().getKnownTrackers()
    
    fun getSuspiciousDevices(threshold: Float = 0.7f): Flow<List<DeviceFingerprint>> = 
        database.deviceFingerprintDao().getSuspiciousDevices(threshold)
    
    fun getDevicesSeenSince(since: Long): Flow<List<DeviceFingerprint>> = 
        database.deviceFingerprintDao().getDevicesSeenSince(since)
    
    suspend fun getDeviceFingerprint(deviceUuid: String): DeviceFingerprint? = 
        database.deviceFingerprintDao().getDeviceFingerprint(deviceUuid)
    
    suspend fun getDeviceByFingerprintHash(hash: String): DeviceFingerprint? = 
        database.deviceFingerprintDao().getDeviceByFingerprintHash(hash)
    
    suspend fun insertDeviceFingerprint(device: DeviceFingerprint) = 
        database.deviceFingerprintDao().insertDeviceFingerprint(device)
    
    suspend fun updateDeviceFingerprint(device: DeviceFingerprint) = 
        database.deviceFingerprintDao().updateDeviceFingerprint(device)
    
    suspend fun deleteDeviceFingerprint(deviceUuid: String) = 
        database.deviceFingerprintDao().deleteDeviceByUuid(deviceUuid)
    
    suspend fun updateTrackedStatus(deviceUuid: String, isTracked: Boolean) =
        database.deviceFingerprintDao().updateTrackedStatus(deviceUuid, isTracked)
    
    suspend fun updateSuspiciousScore(deviceUuid: String, score: Float) = 
        database.deviceFingerprintDao().updateSuspiciousScore(deviceUuid, score)

    suspend fun updateIgnoredStatus(deviceUuid: String, isIgnored: Boolean) =
        database.deviceFingerprintDao().updateIgnoredStatus(deviceUuid, isIgnored)
    
    // MAC Address methods
    suspend fun getMacAddressesForDevice(deviceUuid: String): List<DeviceMacAddress> = 
        database.deviceMacAddressDao().getMacAddressesForDevice(deviceUuid)
    
    suspend fun getDeviceByMacAddress(macAddress: String): DeviceMacAddress? = 
        database.deviceMacAddressDao().getDeviceByMacAddress(macAddress)
    
    suspend fun getMacAddressByAddress(address: String): DeviceMacAddress? = 
        database.deviceMacAddressDao().getDeviceByMacAddress(address)
    
    suspend fun getDeviceUuidByMacAddress(macAddress: String): String? = 
        database.deviceMacAddressDao().getDeviceUuidByMacAddress(macAddress)
    
    suspend fun getActiveMacAddressesForDevice(deviceUuid: String): List<DeviceMacAddress> = 
        database.deviceMacAddressDao().getActiveMacAddressesForDevice(deviceUuid)
    
    suspend fun insertMacAddress(macAddress: DeviceMacAddress) = 
        database.deviceMacAddressDao().insertMacAddress(macAddress)
    
    suspend fun updateMacAddress(macAddress: DeviceMacAddress) = 
        database.deviceMacAddressDao().updateMacAddress(macAddress)
    
    suspend fun deactivateOldMacAddresses(deviceUuid: String, currentMac: String) = 
        database.deviceMacAddressDao().deactivateOldMacAddresses(deviceUuid, currentMac)
    
    suspend fun updateMacAddressActivity(macAddress: String, timestamp: Long) = 
        database.deviceMacAddressDao().updateMacAddressActivity(macAddress, timestamp)
    
    // Advertising Data methods
    suspend fun getAdvertisingDataForDevice(deviceUuid: String): List<DeviceAdvertisingData> = 
        database.deviceAdvertisingDataDao().getAdvertisingDataForDevice(deviceUuid)
    
    suspend fun getAdvertisingDataByType(deviceUuid: String, dataType: AdvertisingDataType): DeviceAdvertisingData? = 
        database.deviceAdvertisingDataDao().getAdvertisingDataByType(deviceUuid, dataType)
    
    suspend fun getStableAdvertisingDataByType(dataType: AdvertisingDataType): List<DeviceAdvertisingData> = 
        database.deviceAdvertisingDataDao().getStableAdvertisingDataByType(dataType)
    
    suspend fun insertAdvertisingData(data: DeviceAdvertisingData) = 
        database.deviceAdvertisingDataDao().insertAdvertisingData(data)
    
    suspend fun updateAdvertisingData(data: DeviceAdvertisingData) = 
        database.deviceAdvertisingDataDao().updateAdvertisingData(data)
    
    suspend fun updateAdvertisingDataActivity(deviceUuid: String, dataType: AdvertisingDataType, timestamp: Long) = 
        database.deviceAdvertisingDataDao().updateAdvertisingDataActivity(deviceUuid, dataType, timestamp)
    
    // Fingerprint Pattern methods
    suspend fun getPatternsForDevice(deviceUuid: String): List<FingerprintPattern> = 
        database.fingerprintPatternDao().getPatternsForDevice(deviceUuid)
    
    suspend fun getPatternByType(deviceUuid: String, patternType: FingerprintPatternType): FingerprintPattern? = 
        database.fingerprintPatternDao().getPatternByType(deviceUuid, patternType)
    
    suspend fun getMatchablePatternsByType(patternType: FingerprintPatternType): List<FingerprintPattern> = 
        database.fingerprintPatternDao().getMatchablePatternsByType(patternType)
    
    suspend fun getHighConfidencePatterns(threshold: Float): List<FingerprintPattern> = 
        database.fingerprintPatternDao().getHighConfidencePatterns(threshold)
    
    suspend fun insertPattern(pattern: FingerprintPattern) = 
        database.fingerprintPatternDao().insertPattern(pattern)
    
    suspend fun updatePattern(pattern: FingerprintPattern) = 
        database.fingerprintPatternDao().updatePattern(pattern)
    
    suspend fun deletePatternByType(deviceUuid: String, patternType: FingerprintPatternType) = 
        database.fingerprintPatternDao().deletePatternByType(deviceUuid, patternType)
    
    // Fingerprint Detection methods
    fun getDetectionsForDevice(deviceUuid: String): Flow<List<FingerprintDetection>> = 
        database.fingerprintDetectionDao().getDetectionsForDevice(deviceUuid)
    
    fun getDetectionsForDeviceSince(deviceUuid: String, since: Long): Flow<List<FingerprintDetection>> = 
        database.fingerprintDetectionDao().getDetectionsForDeviceSince(deviceUuid, since)
    
    suspend fun getDetectionsByMacAddress(macAddress: String): List<FingerprintDetection> = 
        database.fingerprintDetectionDao().getDetectionsByMacAddress(macAddress)
    
    fun getRecentDetections(since: Long): Flow<List<FingerprintDetection>> = 
        database.fingerprintDetectionDao().getRecentDetections(since)
    
    fun getDetectionsSince(since: Long): Flow<List<FingerprintDetection>> = 
        database.fingerprintDetectionDao().getRecentDetections(since)
    
    suspend fun getDetectionsInCluster(clusterId: String): List<FingerprintDetection> = 
        database.fingerprintDetectionDao().getDetectionsInCluster(clusterId)
    
    suspend fun getDetectionCountForDevice(deviceUuid: String): Int = 
        database.fingerprintDetectionDao().getDetectionCountForDevice(deviceUuid)
    
    suspend fun insertDetection(detection: FingerprintDetection) = 
        database.fingerprintDetectionDao().insertDetection(detection)
    
    suspend fun insertDetections(detections: List<FingerprintDetection>) = 
        database.fingerprintDetectionDao().insertDetections(detections)
    
    suspend fun deleteOldDetections(before: Long) = 
        database.fingerprintDetectionDao().deleteOldDetections(before)
    
    suspend fun getAverageRssiForDevice(deviceUuid: String, since: Long): Float? = 
        database.fingerprintDetectionDao().getAverageRssiForDevice(deviceUuid, since)
    
    suspend fun getUniqueLocationCountForDevice(deviceUuid: String): Int = 
        database.fingerprintDetectionDao().getUniqueLocationCountForDevice(deviceUuid)
    
    // Device Correlation methods
    suspend fun getCorrelationsForDevice(deviceUuid: String): List<DeviceCorrelation> = 
        database.deviceCorrelationDao().getCorrelationsForDevice(deviceUuid)
    
    suspend fun getHighCorrelations(threshold: Float): List<DeviceCorrelation> = 
        database.deviceCorrelationDao().getHighCorrelations(threshold)
    
    suspend fun getCorrelationBetweenDevices(uuid1: String, uuid2: String): DeviceCorrelation? = 
        database.deviceCorrelationDao().getCorrelationBetweenDevices(uuid1, uuid2)
    
    suspend fun insertCorrelation(correlation: DeviceCorrelation) = 
        database.deviceCorrelationDao().insertCorrelation(correlation)
    
    suspend fun updateCorrelation(correlation: DeviceCorrelation) = 
        database.deviceCorrelationDao().updateCorrelation(correlation)
    
    suspend fun updateCorrelationActivity(uuid1: String, uuid2: String, timestamp: Long) = 
        database.deviceCorrelationDao().updateCorrelationActivity(uuid1, uuid2, timestamp)
    
    // Fingerprint Match methods
    suspend fun getMatchesForCandidate(candidateUuid: String): List<FingerprintMatch> = 
        database.fingerprintMatchDao().getMatchesForCandidate(candidateUuid)
    
    suspend fun getMatchesForExisting(existingUuid: String): List<FingerprintMatch> = 
        database.fingerprintMatchDao().getMatchesForExisting(existingUuid)
    
    suspend fun getPendingMatches(threshold: Float): List<FingerprintMatch> = 
        database.fingerprintMatchDao().getPendingMatches(threshold)
    
    suspend fun getSpecificMatch(candidateUuid: String, existingUuid: String): FingerprintMatch? = 
        database.fingerprintMatchDao().getSpecificMatch(candidateUuid, existingUuid)
    
    suspend fun insertMatch(match: FingerprintMatch) = 
        database.fingerprintMatchDao().insertMatch(match)
    
    suspend fun updateMatch(match: FingerprintMatch) = 
        database.fingerprintMatchDao().updateMatch(match)
    
    suspend fun confirmMatch(candidateUuid: String, existingUuid: String, confirmed: Boolean) = 
        database.fingerprintMatchDao().confirmMatch(candidateUuid, existingUuid, confirmed)
    
    // Convenience methods
    suspend fun getDeviceWithMacAddresses(deviceUuid: String): DeviceFingerprintWithMacs? {
        val device = getDeviceFingerprint(deviceUuid)
        val macs = getMacAddressesForDevice(deviceUuid)
        return device?.let { DeviceFingerprintWithMacs(it, macs) }
    }
    
    suspend fun getDeviceWithAllData(deviceUuid: String): DeviceFingerprintFullData? {
        val device = getDeviceFingerprint(deviceUuid)
        val macs = getMacAddressesForDevice(deviceUuid)
        val advertisingData = getAdvertisingDataForDevice(deviceUuid)
        val patterns = getPatternsForDevice(deviceUuid)
        val correlations = getCorrelationsForDevice(deviceUuid)
        
        return device?.let { 
            DeviceFingerprintFullData(it, macs, advertisingData, patterns, correlations) 
        }
    }
    
    suspend fun searchDevicesByManufacturer(@Suppress("UNUSED_PARAMETER") manufacturer: String): List<DeviceFingerprint> {
        return getAllDeviceFingerprints().toString()
            .let { emptyList<DeviceFingerprint>() } // Placeholder - would need proper search in DB
    }
    
    suspend fun getDeviceStatistics(deviceUuid: String): DeviceStatistics? {
        val device = getDeviceFingerprint(deviceUuid) ?: return null
        val detectionCount = getDetectionCountForDevice(deviceUuid)
        val macCount = getMacAddressesForDevice(deviceUuid).size
        val uniqueLocationCount = getUniqueLocationCountForDevice(deviceUuid)
        val avgRssi = getAverageRssiForDevice(deviceUuid, System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        
        return DeviceStatistics(
            deviceUuid = deviceUuid,
            detectionCount = detectionCount,
            macAddressCount = macCount,
            uniqueLocationCount = uniqueLocationCount,
            averageRssi = avgRssi ?: 0f,
            suspiciousScore = device.suspiciousScore,
            firstSeen = device.firstSeen,
            lastSeen = device.lastSeen
        )
    }
    
    // Cleanup methods
    suspend fun cleanupOldData(cutoffTime: Long) {
        deleteOldDetections(cutoffTime)
        // Additional cleanup logic can be added here
    }
}

// Data classes for convenience
data class DeviceFingerprintWithMacs(
    val device: DeviceFingerprint,
    val macAddresses: List<DeviceMacAddress>
)

data class DeviceFingerprintFullData(
    val device: DeviceFingerprint,
    val macAddresses: List<DeviceMacAddress>,
    val advertisingData: List<DeviceAdvertisingData>,
    val patterns: List<FingerprintPattern>,
    val correlations: List<DeviceCorrelation>
)

data class DeviceStatistics(
    val deviceUuid: String,
    val detectionCount: Int,
    val macAddressCount: Int,
    val uniqueLocationCount: Int,
    val averageRssi: Float,
    val suspiciousScore: Float,
    val firstSeen: Long,
    val lastSeen: Long
)