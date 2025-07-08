package com.bleradar.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: AnalyticsSnapshot)

    @Query("SELECT * FROM analytics_snapshots WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getSnapshotsSince(startTime: Long): Flow<List<AnalyticsSnapshot>>

    @Query("SELECT * FROM analytics_snapshots WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getSnapshotsInRange(startTime: Long, endTime: Long): Flow<List<AnalyticsSnapshot>>

    @Query("SELECT * FROM analytics_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(limit: Int): Flow<List<AnalyticsSnapshot>>

    @Query("SELECT * FROM analytics_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): AnalyticsSnapshot?

    @Query("SELECT AVG(totalDevices) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getAverageDeviceCount(startTime: Long): Float

    @Query("SELECT AVG(averageRssi) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getAverageRssi(startTime: Long): Float

    @Query("SELECT AVG(averageFollowingScore) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getAverageFollowingScore(startTime: Long): Float

    @Query("SELECT COUNT(*) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getSnapshotCount(startTime: Long): Int

    @Query("DELETE FROM analytics_snapshots WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSnapshots(cutoffTime: Long)

    @Query("SELECT MAX(alertsTriggered) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getMaxAlertsInPeriod(startTime: Long): Int

    @Query("SELECT SUM(newDevicesDiscovered) FROM analytics_snapshots WHERE timestamp >= :startTime")
    suspend fun getTotalNewDevicesInPeriod(startTime: Long): Int

    @Query("SELECT * FROM analytics_snapshots WHERE timestamp >= :startTime GROUP BY DATE(timestamp/1000, 'unixepoch') ORDER BY timestamp DESC")
    fun getDailySnapshots(startTime: Long): Flow<List<AnalyticsSnapshot>>

    @Query("SELECT * FROM analytics_snapshots WHERE timestamp >= :startTime AND timestamp % :interval < 3600000 ORDER BY timestamp DESC")
    fun getHourlySnapshots(startTime: Long, interval: Long = 3600000): Flow<List<AnalyticsSnapshot>>
}