package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDeviceDao {
    @Query("SELECT * FROM ble_devices WHERE isIgnored = 0 ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<BleDevice>>
    
    @Query("SELECT * FROM ble_devices WHERE isIgnored = 0 ORDER BY lastSeen DESC")
    suspend fun getAllDevicesSync(): List<BleDevice>

    @Query("SELECT * FROM ble_devices WHERE isTracked = 1")
    fun getTrackedDevices(): Flow<List<BleDevice>>

    @Query("SELECT * FROM ble_devices WHERE followingScore > :threshold ORDER BY followingScore DESC")
    fun getSuspiciousDevices(threshold: Float = 0.7f): Flow<List<BleDevice>>

    @Query("SELECT * FROM ble_devices WHERE deviceAddress = :address")
    suspend fun getDevice(address: String): BleDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: BleDevice)

    @Update
    suspend fun updateDevice(device: BleDevice)

    @Query("UPDATE ble_devices SET isIgnored = 1 WHERE deviceAddress = :address")
    suspend fun ignoreDevice(address: String)

    @Query("UPDATE ble_devices SET label = :label WHERE deviceAddress = :address")
    suspend fun labelDevice(address: String, label: String)

    @Query("UPDATE ble_devices SET isTracked = :tracked WHERE deviceAddress = :address")
    suspend fun setDeviceTracked(address: String, tracked: Boolean)

    @Query("UPDATE ble_devices SET followingScore = :score WHERE deviceAddress = :address")
    suspend fun updateFollowingScore(address: String, score: Float)

    @Query("UPDATE ble_devices SET detectionCount = :count, consecutiveDetections = :consecutive, maxConsecutiveDetections = :maxConsecutive, averageRssi = :avgRssi, rssiVariation = :rssiVar, lastMovementTime = :lastMovement, isStationary = :stationary, suspiciousActivityScore = :suspiciousScore, isKnownTracker = :knownTracker, trackerType = :trackerType WHERE deviceAddress = :address")
    suspend fun updateDeviceMetrics(address: String, count: Int, consecutive: Int, maxConsecutive: Int, avgRssi: Float, rssiVar: Float, lastMovement: Long, stationary: Boolean, suspiciousScore: Float, knownTracker: Boolean, trackerType: String?)

    @Query("SELECT * FROM ble_devices WHERE isKnownTracker = 1 ORDER BY suspiciousActivityScore DESC")
    fun getKnownTrackers(): Flow<List<BleDevice>>

    @Query("SELECT * FROM ble_devices WHERE suspiciousActivityScore > :threshold ORDER BY suspiciousActivityScore DESC")
    fun getSuspiciousDevicesByActivity(threshold: Float = 0.5f): Flow<List<BleDevice>>

    @Query("UPDATE ble_devices SET lastAlertTime = :alertTime WHERE deviceAddress = :address")
    suspend fun updateLastAlertTime(address: String, alertTime: Long)

    @Query("UPDATE ble_devices SET suspiciousActivityScore = :score WHERE deviceAddress = :address")
    suspend fun updateSuspiciousScore(address: String, score: Float)

    @Query("DELETE FROM ble_devices WHERE deviceAddress = :address")
    suspend fun deleteDevice(address: String)
    
    @Query("SELECT * FROM ble_devices WHERE lastSeen > :since AND isIgnored = 0 ORDER BY lastSeen DESC")
    suspend fun getRecentDevices(since: Long): List<BleDevice>
}