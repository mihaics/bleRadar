package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceFingerprintDao {
    @Query("SELECT * FROM device_fingerprints ORDER BY lastSeen DESC")
    fun getAllDeviceFingerprints(): Flow<List<DeviceFingerprint>>

    @Query("SELECT * FROM device_fingerprints WHERE deviceUuid = :deviceUuid")
    suspend fun getDeviceFingerprint(deviceUuid: String): DeviceFingerprint?

    @Query("SELECT * FROM device_fingerprints WHERE fingerprintHash = :hash")
    suspend fun getDeviceByFingerprintHash(hash: String): DeviceFingerprint?

    @Query("SELECT * FROM device_fingerprints WHERE isKnownTracker = 1 ORDER BY suspiciousScore DESC")
    fun getKnownTrackers(): Flow<List<DeviceFingerprint>>

    @Query("SELECT * FROM device_fingerprints WHERE suspiciousScore > :threshold ORDER BY suspiciousScore DESC")
    fun getSuspiciousDevices(threshold: Float): Flow<List<DeviceFingerprint>>

    @Query("SELECT * FROM device_fingerprints WHERE lastSeen > :since ORDER BY lastSeen DESC")
    fun getDevicesSeenSince(since: Long): Flow<List<DeviceFingerprint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceFingerprint(device: DeviceFingerprint)

    @Update
    suspend fun updateDeviceFingerprint(device: DeviceFingerprint)

    @Delete
    suspend fun deleteDeviceFingerprint(device: DeviceFingerprint)

    @Query("DELETE FROM device_fingerprints WHERE deviceUuid = :deviceUuid")
    suspend fun deleteDeviceByUuid(deviceUuid: String)

    @Query("UPDATE device_fingerprints SET isTracked = :isTracked WHERE deviceUuid = :deviceUuid")
    suspend fun updateTrackedStatus(deviceUuid: String, isTracked: Boolean)

    @Query("UPDATE device_fingerprints SET suspiciousScore = :score WHERE deviceUuid = :deviceUuid")
    suspend fun updateSuspiciousScore(deviceUuid: String, score: Float)

    @Query("UPDATE device_fingerprints SET isIgnored = :isIgnored WHERE deviceUuid = :deviceUuid")
    suspend fun updateIgnoredStatus(deviceUuid: String, isIgnored: Boolean)
}

@Dao
interface DeviceMacAddressDao {
    @Query("SELECT * FROM device_mac_addresses WHERE deviceUuid = :deviceUuid ORDER BY lastSeen DESC")
    suspend fun getMacAddressesForDevice(deviceUuid: String): List<DeviceMacAddress>

    @Query("SELECT * FROM device_mac_addresses WHERE macAddress = :macAddress")
    suspend fun getDeviceByMacAddress(macAddress: String): DeviceMacAddress?

    @Query("SELECT deviceUuid FROM device_mac_addresses WHERE macAddress = :macAddress")
    suspend fun getDeviceUuidByMacAddress(macAddress: String): String?

    @Query("SELECT * FROM device_mac_addresses WHERE deviceUuid = :deviceUuid AND isActive = 1")
    suspend fun getActiveMacAddressesForDevice(deviceUuid: String): List<DeviceMacAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacAddress(macAddress: DeviceMacAddress)

    @Update
    suspend fun updateMacAddress(macAddress: DeviceMacAddress)

    @Query("UPDATE device_mac_addresses SET isActive = 0 WHERE deviceUuid = :deviceUuid AND macAddress != :currentMac")
    suspend fun deactivateOldMacAddresses(deviceUuid: String, currentMac: String)

    @Query("UPDATE device_mac_addresses SET lastSeen = :timestamp, detectionCount = detectionCount + 1 WHERE macAddress = :macAddress")
    suspend fun updateMacAddressActivity(macAddress: String, timestamp: Long)

    @Query("DELETE FROM device_mac_addresses WHERE deviceUuid = :deviceUuid")
    suspend fun deleteMacAddressesForDevice(deviceUuid: String)
}

@Dao
interface DeviceAdvertisingDataDao {
    @Query("SELECT * FROM device_advertising_data WHERE deviceUuid = :deviceUuid")
    suspend fun getAdvertisingDataForDevice(deviceUuid: String): List<DeviceAdvertisingData>

    @Query("SELECT * FROM device_advertising_data WHERE deviceUuid = :deviceUuid AND dataType = :dataType")
    suspend fun getAdvertisingDataByType(deviceUuid: String, dataType: AdvertisingDataType): DeviceAdvertisingData?

    @Query("SELECT * FROM device_advertising_data WHERE dataType = :dataType AND isStable = 1")
    suspend fun getStableAdvertisingDataByType(dataType: AdvertisingDataType): List<DeviceAdvertisingData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvertisingData(data: DeviceAdvertisingData)

    @Update
    suspend fun updateAdvertisingData(data: DeviceAdvertisingData)

    @Query("UPDATE device_advertising_data SET lastSeen = :timestamp, occurrenceCount = occurrenceCount + 1 WHERE deviceUuid = :deviceUuid AND dataType = :dataType")
    suspend fun updateAdvertisingDataActivity(deviceUuid: String, dataType: AdvertisingDataType, timestamp: Long)

    @Query("DELETE FROM device_advertising_data WHERE deviceUuid = :deviceUuid")
    suspend fun deleteAdvertisingDataForDevice(deviceUuid: String)
}

@Dao
interface FingerprintPatternDao {
    @Query("SELECT * FROM fingerprint_patterns WHERE deviceUuid = :deviceUuid")
    suspend fun getPatternsForDevice(deviceUuid: String): List<FingerprintPattern>

    @Query("SELECT * FROM fingerprint_patterns WHERE deviceUuid = :deviceUuid AND patternType = :patternType")
    suspend fun getPatternByType(deviceUuid: String, patternType: FingerprintPatternType): FingerprintPattern?

    @Query("SELECT * FROM fingerprint_patterns WHERE patternType = :patternType AND isMatchable = 1")
    suspend fun getMatchablePatternsByType(patternType: FingerprintPatternType): List<FingerprintPattern>

    @Query("SELECT * FROM fingerprint_patterns WHERE confidence > :threshold AND isMatchable = 1")
    suspend fun getHighConfidencePatterns(threshold: Float): List<FingerprintPattern>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: FingerprintPattern)

    @Update
    suspend fun updatePattern(pattern: FingerprintPattern)

    @Query("DELETE FROM fingerprint_patterns WHERE deviceUuid = :deviceUuid")
    suspend fun deletePatternsForDevice(deviceUuid: String)

    @Query("DELETE FROM fingerprint_patterns WHERE deviceUuid = :deviceUuid AND patternType = :patternType")
    suspend fun deletePatternByType(deviceUuid: String, patternType: FingerprintPatternType)
}

@Dao
interface FingerprintDetectionDao {
    @Query("SELECT * FROM fingerprint_detections WHERE deviceUuid = :deviceUuid ORDER BY timestamp DESC")
    fun getDetectionsForDevice(deviceUuid: String): Flow<List<FingerprintDetection>>

    @Query("SELECT * FROM fingerprint_detections WHERE deviceUuid = :deviceUuid AND timestamp > :since ORDER BY timestamp DESC")
    fun getDetectionsForDeviceSince(deviceUuid: String, since: Long): Flow<List<FingerprintDetection>>

    @Query("SELECT * FROM fingerprint_detections WHERE macAddress = :macAddress ORDER BY timestamp DESC")
    suspend fun getDetectionsByMacAddress(macAddress: String): List<FingerprintDetection>

    @Query("SELECT * FROM fingerprint_detections WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getRecentDetections(since: Long): Flow<List<FingerprintDetection>>

    @Query("SELECT * FROM fingerprint_detections WHERE clusterId = :clusterId ORDER BY timestamp DESC")
    suspend fun getDetectionsInCluster(clusterId: String): List<FingerprintDetection>

    @Query("SELECT COUNT(*) FROM fingerprint_detections WHERE deviceUuid = :deviceUuid")
    suspend fun getDetectionCountForDevice(deviceUuid: String): Int

    @Insert
    suspend fun insertDetection(detection: FingerprintDetection)

    @Insert
    suspend fun insertDetections(detections: List<FingerprintDetection>)

    @Query("DELETE FROM fingerprint_detections WHERE timestamp < :before")
    suspend fun deleteOldDetections(before: Long)

    @Query("DELETE FROM fingerprint_detections WHERE deviceUuid = :deviceUuid")
    suspend fun deleteDetectionsForDevice(deviceUuid: String)

    @Query("SELECT AVG(rssi) FROM fingerprint_detections WHERE deviceUuid = :deviceUuid AND timestamp > :since")
    suspend fun getAverageRssiForDevice(deviceUuid: String, since: Long): Float?

    @Query("SELECT COUNT(DISTINCT clusterId) FROM fingerprint_detections WHERE deviceUuid = :deviceUuid AND clusterId IS NOT NULL")
    suspend fun getUniqueLocationCountForDevice(deviceUuid: String): Int
}

@Dao
interface DeviceCorrelationDao {
    @Query("SELECT * FROM device_correlations WHERE deviceUuid1 = :deviceUuid OR deviceUuid2 = :deviceUuid ORDER BY correlation DESC")
    suspend fun getCorrelationsForDevice(deviceUuid: String): List<DeviceCorrelation>

    @Query("SELECT * FROM device_correlations WHERE correlation > :threshold ORDER BY correlation DESC")
    suspend fun getHighCorrelations(threshold: Float): List<DeviceCorrelation>

    @Query("SELECT * FROM device_correlations WHERE (deviceUuid1 = :uuid1 AND deviceUuid2 = :uuid2) OR (deviceUuid1 = :uuid2 AND deviceUuid2 = :uuid1)")
    suspend fun getCorrelationBetweenDevices(uuid1: String, uuid2: String): DeviceCorrelation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrelation(correlation: DeviceCorrelation)

    @Update
    suspend fun updateCorrelation(correlation: DeviceCorrelation)

    @Query("DELETE FROM device_correlations WHERE deviceUuid1 = :deviceUuid OR deviceUuid2 = :deviceUuid")
    suspend fun deleteCorrelationsForDevice(deviceUuid: String)

    @Query("UPDATE device_correlations SET cooccurrenceCount = cooccurrenceCount + 1, lastSeen = :timestamp WHERE (deviceUuid1 = :uuid1 AND deviceUuid2 = :uuid2) OR (deviceUuid1 = :uuid2 AND deviceUuid2 = :uuid1)")
    suspend fun updateCorrelationActivity(uuid1: String, uuid2: String, timestamp: Long)
}

@Dao
interface FingerprintMatchDao {
    @Query("SELECT * FROM fingerprint_matches WHERE candidateDeviceUuid = :candidateUuid ORDER BY matchScore DESC")
    suspend fun getMatchesForCandidate(candidateUuid: String): List<FingerprintMatch>

    @Query("SELECT * FROM fingerprint_matches WHERE existingDeviceUuid = :existingUuid ORDER BY matchScore DESC")
    suspend fun getMatchesForExisting(existingUuid: String): List<FingerprintMatch>

    @Query("SELECT * FROM fingerprint_matches WHERE matchScore > :threshold AND isConfirmed = 0 ORDER BY matchScore DESC")
    suspend fun getPendingMatches(threshold: Float): List<FingerprintMatch>

    @Query("SELECT * FROM fingerprint_matches WHERE candidateDeviceUuid = :candidateUuid AND existingDeviceUuid = :existingUuid")
    suspend fun getSpecificMatch(candidateUuid: String, existingUuid: String): FingerprintMatch?

    @Insert
    suspend fun insertMatch(match: FingerprintMatch)

    @Update
    suspend fun updateMatch(match: FingerprintMatch)

    @Query("UPDATE fingerprint_matches SET isConfirmed = :confirmed WHERE candidateDeviceUuid = :candidateUuid AND existingDeviceUuid = :existingUuid")
    suspend fun confirmMatch(candidateUuid: String, existingUuid: String, confirmed: Boolean)

    @Query("DELETE FROM fingerprint_matches WHERE candidateDeviceUuid = :candidateUuid OR existingDeviceUuid = :existingUuid")
    suspend fun deleteMatchesForDevice(candidateUuid: String, existingUuid: String)
}