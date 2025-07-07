package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDetectionDao {
    @Query("SELECT * FROM ble_detections WHERE deviceAddress = :address ORDER BY timestamp DESC")
    fun getDetectionsForDevice(address: String): Flow<List<BleDetection>>
    
    @Query("SELECT * FROM ble_detections WHERE deviceAddress = :address ORDER BY timestamp DESC")
    suspend fun getDetectionsForDeviceSync(address: String): List<BleDetection>

    @Query("SELECT * FROM ble_detections WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getDetectionsSince(startTime: Long): Flow<List<BleDetection>>

    @Query("SELECT * FROM ble_detections WHERE deviceAddress = :address AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getDetectionsForDeviceSince(address: String, startTime: Long): Flow<List<BleDetection>>

    @Query("SELECT COUNT(*) FROM ble_detections WHERE deviceAddress = :address AND timestamp >= :startTime")
    suspend fun getDetectionCount(address: String, startTime: Long): Int

    @Insert
    suspend fun insertDetection(detection: BleDetection)

    @Insert
    suspend fun insertDetections(detections: List<BleDetection>)

    @Query("DELETE FROM ble_detections WHERE timestamp < :cutoffTime")
    suspend fun deleteOldDetections(cutoffTime: Long)

    @Query("DELETE FROM ble_detections WHERE deviceAddress = :address")
    suspend fun deleteDetectionsForDevice(address: String)
}