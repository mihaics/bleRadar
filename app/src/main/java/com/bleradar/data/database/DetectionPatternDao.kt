package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionPatternDao {
    @Query("SELECT * FROM detection_patterns WHERE deviceAddress = :address ORDER BY timestamp DESC")
    fun getPatternsForDevice(address: String): Flow<List<DetectionPattern>>

    @Query("SELECT * FROM detection_patterns WHERE patternType = :type ORDER BY timestamp DESC")
    fun getPatternsByType(type: String): Flow<List<DetectionPattern>>

    @Query("SELECT * FROM detection_patterns WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getPatternsSince(startTime: Long): Flow<List<DetectionPattern>>

    @Query("SELECT * FROM detection_patterns WHERE deviceAddress = :address AND patternType = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPatternForDevice(address: String, type: String): DetectionPattern?

    @Insert
    suspend fun insertPattern(pattern: DetectionPattern)

    @Insert
    suspend fun insertPatterns(patterns: List<DetectionPattern>)

    @Query("DELETE FROM detection_patterns WHERE timestamp < :cutoffTime")
    suspend fun deleteOldPatterns(cutoffTime: Long)

    @Query("DELETE FROM detection_patterns WHERE deviceAddress = :address")
    suspend fun deletePatternsForDevice(address: String)
}