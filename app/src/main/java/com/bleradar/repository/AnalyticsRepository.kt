package com.bleradar.repository

import com.bleradar.data.database.AnalyticsSnapshot
import com.bleradar.data.database.BleRadarDatabase
import com.bleradar.data.database.DeviceAnalytics
import com.bleradar.data.database.TrendData
import com.bleradar.data.database.CategoryAverageResult
import com.bleradar.data.database.DeviceActivityResult
import com.bleradar.data.database.DeviceScoreResult
import com.bleradar.data.database.RssiTrendResult
import com.bleradar.data.database.DeviceCountTrendResult
import com.bleradar.data.database.AlertsTrendResult
import com.bleradar.data.database.ValueTrendResult
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val database: BleRadarDatabase
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Debug method to get all devices including ignored ones
    suspend fun getAllDevicesIncludingIgnored() = database.bleDeviceDao().getAllDevicesIncludingIgnored()
    
    // Analytics Snapshot methods
    suspend fun insertSnapshot(snapshot: AnalyticsSnapshot) = 
        database.analyticsSnapshotDao().insertSnapshot(snapshot)
    
    fun getSnapshotsSince(startTime: Long): Flow<List<AnalyticsSnapshot>> = 
        database.analyticsSnapshotDao().getSnapshotsSince(startTime)
    
    fun getSnapshotsInRange(startTime: Long, endTime: Long): Flow<List<AnalyticsSnapshot>> = 
        database.analyticsSnapshotDao().getSnapshotsInRange(startTime, endTime)
    
    fun getRecentSnapshots(limit: Int = 100): Flow<List<AnalyticsSnapshot>> = 
        database.analyticsSnapshotDao().getRecentSnapshots(limit)
    
    suspend fun getLatestSnapshot(): AnalyticsSnapshot? = 
        database.analyticsSnapshotDao().getLatestSnapshot()
    
    fun getDailySnapshots(daysBack: Int = 30): Flow<List<AnalyticsSnapshot>> {
        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
        return database.analyticsSnapshotDao().getDailySnapshots(startTime)
    }
    
    fun getHourlySnapshots(hoursBack: Int = 24): Flow<List<AnalyticsSnapshot>> {
        val startTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursBack.toLong())
        return database.analyticsSnapshotDao().getHourlySnapshots(startTime)
    }
    
    // Device Analytics methods
    suspend fun insertDeviceAnalytics(analytics: DeviceAnalytics) = 
        database.deviceAnalyticsDao().insertAnalytics(analytics)
    
    fun getAnalyticsForDevice(address: String): Flow<List<DeviceAnalytics>> = 
        database.deviceAnalyticsDao().getAnalyticsForDevice(address)
    
    fun getAnalyticsSince(daysBack: Int = 30): Flow<List<DeviceAnalytics>> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getAnalyticsSince(startDate)
    }
    
    fun getAnalyticsInRange(startDays: Int, endDays: Int): Flow<List<DeviceAnalytics>> {
        val startDate = getDateString(startDays)
        val endDate = getDateString(endDays)
        return database.deviceAnalyticsDao().getAnalyticsInRange(startDate, endDate)
    }
    
    suspend fun getTopFollowingDevices(daysBack: Int = 7, limit: Int = 10): List<DeviceScoreResult> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getTopFollowingDevices(startDate, limit)
    }
    
    suspend fun getTopSuspiciousDevices(daysBack: Int = 7, limit: Int = 10): List<DeviceScoreResult> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getTopSuspiciousDevices(startDate, limit)
    }
    
    suspend fun getMostActiveDevices(daysBack: Int = 7, limit: Int = 10): List<DeviceActivityResult> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getMostActiveDevices(startDate, limit)
    }
    
    fun getRssiTrendByDate(daysBack: Int = 30): Flow<List<RssiTrendResult>> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getRssiTrendByDate(startDate)
    }
    
    fun getDeviceCountTrendByDate(daysBack: Int = 30): Flow<List<DeviceCountTrendResult>> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getDeviceCountTrendByDate(startDate)
    }
    
    fun getAlertsTrendByDate(daysBack: Int = 30): Flow<List<AlertsTrendResult>> {
        val startDate = getDateString(daysBack)
        return database.deviceAnalyticsDao().getAlertsTrendByDate(startDate)
    }
    
    // Trend Data methods
    suspend fun insertTrendData(trendData: TrendData) = 
        database.trendDataDao().insertTrendData(trendData)
    
    fun getTrendData(metricName: String, timeframe: String, daysBack: Int = 30): Flow<List<TrendData>> {
        val startDate = getDateString(daysBack)
        return database.trendDataDao().getTrendData(metricName, timeframe, startDate)
    }
    
    fun getTrendDataByCategory(metricName: String, timeframe: String, category: String, daysBack: Int = 30): Flow<List<TrendData>> {
        val startDate = getDateString(daysBack)
        return database.trendDataDao().getTrendDataByCategory(metricName, timeframe, category, startDate)
    }
    
    suspend fun getCategoriesForMetric(metricName: String): List<String> = 
        database.trendDataDao().getCategoriesForMetric(metricName)
    
    suspend fun getAllMetricNames(): List<String> = 
        database.trendDataDao().getAllMetricNames()
    
    fun getValueTrendByDate(metricName: String, timeframe: String, daysBack: Int = 30): Flow<List<ValueTrendResult>> {
        val startDate = getDateString(daysBack)
        return database.trendDataDao().getValueTrendByDate(metricName, timeframe, startDate)
    }
    
    suspend fun getAverageValuesByCategory(metricName: String, daysBack: Int = 30): List<CategoryAverageResult> {
        val startDate = getDateString(daysBack)
        return database.trendDataDao().getAverageValuesByCategory(metricName, startDate)
    }
    
    // Summary and aggregation methods
    suspend fun getAnalyticsSummary(daysBack: Int = 7): AnalyticsSummary {
        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
        val startDate = getDateString(daysBack)
        
        val totalDevices = database.deviceAnalyticsDao().getUniqueDeviceCount(startDate)
        val totalDetections = database.analyticsSnapshotDao().getSnapshotCount(startTime)
        val avgRssi = database.deviceAnalyticsDao().getAverageRssiTrend(startDate)
        val avgFollowingScore = database.deviceAnalyticsDao().getAverageFollowingScoreTrend(startDate)
        val avgSuspiciousScore = database.deviceAnalyticsDao().getAverageSuspiciousScoreTrend(startDate)
        val totalDistance = database.deviceAnalyticsDao().getTotalDistanceTraveled(startDate)
        val maxAlerts = database.analyticsSnapshotDao().getMaxAlertsInPeriod(startTime)
        val newDevices = database.analyticsSnapshotDao().getTotalNewDevicesInPeriod(startTime)
        
        return AnalyticsSummary(
            totalDevices = totalDevices,
            totalDetections = totalDetections,
            averageRssi = avgRssi,
            averageFollowingScore = avgFollowingScore,
            averageSuspiciousScore = avgSuspiciousScore,
            totalDistanceTraveled = totalDistance,
            maxAlertsInPeriod = maxAlerts,
            newDevicesDiscovered = newDevices,
            daysAnalyzed = daysBack
        )
    }
    
    suspend fun getWeeklyTrendComparison(): WeeklyTrendComparison {
        val thisWeekStart = getDateString(7)
        val lastWeekStart = getDateString(14)
        val lastWeekEnd = getDateString(7)
        
        val thisWeekDevices = database.deviceAnalyticsDao().getUniqueDeviceCount(thisWeekStart)
        val lastWeekDevices = database.deviceAnalyticsDao().getUniqueDeviceCount(lastWeekStart) - 
                             database.deviceAnalyticsDao().getUniqueDeviceCount(lastWeekEnd)
        
        val thisWeekRssi = database.deviceAnalyticsDao().getAverageRssiTrend(thisWeekStart)
        val lastWeekRssi = database.trendDataDao().getAverageValue("average_rssi", lastWeekStart) - 
                          database.trendDataDao().getAverageValue("average_rssi", lastWeekEnd)
        
        val thisWeekSuspicious = database.deviceAnalyticsDao().getAverageSuspiciousScoreTrend(thisWeekStart)
        val lastWeekSuspicious = database.trendDataDao().getAverageValue("average_suspicious_score", lastWeekStart) - 
                                database.trendDataDao().getAverageValue("average_suspicious_score", lastWeekEnd)
        
        return WeeklyTrendComparison(
            deviceCountChange = thisWeekDevices - lastWeekDevices,
            rssiChange = thisWeekRssi - lastWeekRssi,
            suspiciousScoreChange = thisWeekSuspicious - lastWeekSuspicious,
            thisWeekDevices = thisWeekDevices,
            lastWeekDevices = lastWeekDevices
        )
    }
    
    // Device-specific analytics
    suspend fun getDeviceAnalyticsSummary(deviceAddress: String, daysBack: Int = 30): DeviceAnalyticsSummary? {
        val startDate = getDateString(daysBack)
        val analytics = database.deviceAnalyticsDao().getDeviceAnalyticsInRange(deviceAddress, startDate)
        
        var analyticsList: List<DeviceAnalytics> = emptyList()
        analytics.collect { analyticsList = it }
        
        if (analyticsList.isEmpty()) return null
        
        val totalDetections = analyticsList.sumOf { it.detectionsCount }
        val avgRssi = analyticsList.map { it.averageRssi }.average().toFloat()
        val maxFollowingScore = analyticsList.maxOfOrNull { it.followingScore } ?: 0f
        val maxSuspiciousScore = analyticsList.maxOfOrNull { it.suspiciousScore } ?: 0f
        val totalDistance = analyticsList.sumOf { it.distanceTraveled.toDouble() }.toFloat()
        val totalAlerts = analyticsList.sumOf { it.alertsTriggered }
        val avgSpeed = analyticsList.map { it.averageSpeed }.average().toFloat()
        val maxSpeed = analyticsList.maxOfOrNull { it.maxSpeed } ?: 0f
        
        return DeviceAnalyticsSummary(
            deviceAddress = deviceAddress,
            totalDetections = totalDetections,
            averageRssi = avgRssi,
            maxFollowingScore = maxFollowingScore,
            maxSuspiciousScore = maxSuspiciousScore,
            totalDistanceTraveled = totalDistance,
            totalAlerts = totalAlerts,
            averageSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            daysAnalyzed = daysBack
        )
    }
    
    // Cleanup methods
    suspend fun cleanupOldData(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        val cutoffDate = getDateString(daysToKeep)
        
        database.analyticsSnapshotDao().deleteOldSnapshots(cutoffTime)
        database.deviceAnalyticsDao().deleteOldAnalytics(cutoffDate)
        database.trendDataDao().deleteOldTrendData(cutoffDate)
    }
    
    private fun getDateString(daysBack: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -daysBack)
        return dateFormat.format(calendar.time)
    }
}

data class AnalyticsSummary(
    val totalDevices: Int,
    val totalDetections: Int,
    val averageRssi: Float,
    val averageFollowingScore: Float,
    val averageSuspiciousScore: Float,
    val totalDistanceTraveled: Float,
    val maxAlertsInPeriod: Int,
    val newDevicesDiscovered: Int,
    val daysAnalyzed: Int
)

data class WeeklyTrendComparison(
    val deviceCountChange: Int,
    val rssiChange: Float,
    val suspiciousScoreChange: Float,
    val thisWeekDevices: Int,
    val lastWeekDevices: Int
)

data class DeviceAnalyticsSummary(
    val deviceAddress: String,
    val totalDetections: Int,
    val averageRssi: Float,
    val maxFollowingScore: Float,
    val maxSuspiciousScore: Float,
    val totalDistanceTraveled: Float,
    val totalAlerts: Int,
    val averageSpeed: Float,
    val maxSpeed: Float,
    val daysAnalyzed: Int
)