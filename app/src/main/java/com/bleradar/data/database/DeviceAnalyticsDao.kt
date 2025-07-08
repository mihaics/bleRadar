package com.bleradar.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceAnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: DeviceAnalytics)

    @Query("SELECT * FROM device_analytics WHERE deviceAddress = :address ORDER BY date DESC")
    fun getAnalyticsForDevice(address: String): Flow<List<DeviceAnalytics>>

    @Query("SELECT * FROM device_analytics WHERE date >= :startDate ORDER BY date DESC")
    fun getAnalyticsSince(startDate: String): Flow<List<DeviceAnalytics>>

    @Query("SELECT * FROM device_analytics WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAnalyticsInRange(startDate: String, endDate: String): Flow<List<DeviceAnalytics>>

    @Query("SELECT * FROM device_analytics WHERE deviceAddress = :address AND date >= :startDate ORDER BY date DESC")
    fun getDeviceAnalyticsInRange(address: String, startDate: String): Flow<List<DeviceAnalytics>>

    @Query("SELECT deviceAddress, MAX(followingScore) as maxScore FROM device_analytics WHERE date >= :startDate GROUP BY deviceAddress ORDER BY maxScore DESC LIMIT :limit")
    suspend fun getTopFollowingDevices(startDate: String, limit: Int): List<DeviceScoreResult>

    @Query("SELECT deviceAddress, MAX(suspiciousScore) as maxScore FROM device_analytics WHERE date >= :startDate GROUP BY deviceAddress ORDER BY maxScore DESC LIMIT :limit")
    suspend fun getTopSuspiciousDevices(startDate: String, limit: Int): List<DeviceScoreResult>

    @Query("SELECT deviceAddress, SUM(detectionsCount) as totalDetections FROM device_analytics WHERE date >= :startDate GROUP BY deviceAddress ORDER BY totalDetections DESC LIMIT :limit")
    suspend fun getMostActiveDevices(startDate: String, limit: Int): List<DeviceActivityResult>

    @Query("SELECT AVG(averageRssi) FROM device_analytics WHERE date >= :startDate")
    suspend fun getAverageRssiTrend(startDate: String): Float

    @Query("SELECT AVG(followingScore) FROM device_analytics WHERE date >= :startDate")
    suspend fun getAverageFollowingScoreTrend(startDate: String): Float

    @Query("SELECT AVG(suspiciousScore) FROM device_analytics WHERE date >= :startDate")
    suspend fun getAverageSuspiciousScoreTrend(startDate: String): Float

    @Query("SELECT COUNT(DISTINCT deviceAddress) FROM device_analytics WHERE date >= :startDate")
    suspend fun getUniqueDeviceCount(startDate: String): Int

    @Query("SELECT SUM(distanceTraveled) FROM device_analytics WHERE date >= :startDate")
    suspend fun getTotalDistanceTraveled(startDate: String): Float

    @Query("SELECT date, AVG(averageRssi) as avgRssi FROM device_analytics WHERE date >= :startDate GROUP BY date ORDER BY date")
    fun getRssiTrendByDate(startDate: String): Flow<List<RssiTrendResult>>

    @Query("SELECT date, COUNT(DISTINCT deviceAddress) as deviceCount FROM device_analytics WHERE date >= :startDate GROUP BY date ORDER BY date")
    fun getDeviceCountTrendByDate(startDate: String): Flow<List<DeviceCountTrendResult>>

    @Query("SELECT date, SUM(alertsTriggered) as totalAlerts FROM device_analytics WHERE date >= :startDate GROUP BY date ORDER BY date")
    fun getAlertsTrendByDate(startDate: String): Flow<List<AlertsTrendResult>>

    @Query("DELETE FROM device_analytics WHERE date < :cutoffDate")
    suspend fun deleteOldAnalytics(cutoffDate: String)

    @Query("SELECT * FROM device_analytics WHERE deviceAddress = :address AND date = :date")
    suspend fun getAnalyticsForDeviceAndDate(address: String, date: String): DeviceAnalytics?
}

data class DeviceScoreResult(
    val deviceAddress: String,
    val maxScore: Float
)

data class DeviceActivityResult(
    val deviceAddress: String,
    val totalDetections: Int
)

data class RssiTrendResult(
    val date: String,
    val avgRssi: Float
)

data class DeviceCountTrendResult(
    val date: String,
    val deviceCount: Int
)

data class AlertsTrendResult(
    val date: String,
    val totalAlerts: Int
)