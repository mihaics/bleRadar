package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocation(): LocationRecord?

    @Query("SELECT * FROM location_records WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLocationsSince(startTime: Long): Flow<List<LocationRecord>>

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLocations(limit: Int = 100): Flow<List<LocationRecord>>

    @Insert
    suspend fun insertLocation(location: LocationRecord)

    @Query("DELETE FROM location_records WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLocations(cutoffTime: Long)
}