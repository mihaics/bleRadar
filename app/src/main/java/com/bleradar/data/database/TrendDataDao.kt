package com.bleradar.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrendDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendData(trendData: TrendData)

    @Query("SELECT * FROM trend_data WHERE metricName = :metricName AND timeframe = :timeframe AND date >= :startDate ORDER BY date ASC")
    fun getTrendData(metricName: String, timeframe: String, startDate: String): Flow<List<TrendData>>

    @Query("SELECT * FROM trend_data WHERE metricName = :metricName AND timeframe = :timeframe AND category = :category AND date >= :startDate ORDER BY date ASC")
    fun getTrendDataByCategory(metricName: String, timeframe: String, category: String, startDate: String): Flow<List<TrendData>>

    @Query("SELECT DISTINCT category FROM trend_data WHERE metricName = :metricName AND category IS NOT NULL")
    suspend fun getCategoriesForMetric(metricName: String): List<String>

    @Query("SELECT DISTINCT metricName FROM trend_data")
    suspend fun getAllMetricNames(): List<String>

    @Query("SELECT * FROM trend_data WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTrendDataInRange(startDate: String, endDate: String): Flow<List<TrendData>>

    @Query("SELECT AVG(value) FROM trend_data WHERE metricName = :metricName AND date >= :startDate")
    suspend fun getAverageValue(metricName: String, startDate: String): Float

    @Query("SELECT MAX(value) FROM trend_data WHERE metricName = :metricName AND date >= :startDate")
    suspend fun getMaxValue(metricName: String, startDate: String): Float

    @Query("SELECT MIN(value) FROM trend_data WHERE metricName = :metricName AND date >= :startDate")
    suspend fun getMinValue(metricName: String, startDate: String): Float

    @Query("SELECT SUM(value) FROM trend_data WHERE metricName = :metricName AND date >= :startDate")
    suspend fun getSumValue(metricName: String, startDate: String): Float

    @Query("SELECT * FROM trend_data WHERE metricName = :metricName AND timeframe = :timeframe ORDER BY date DESC LIMIT :limit")
    fun getRecentTrendData(metricName: String, timeframe: String, limit: Int): Flow<List<TrendData>>

    @Query("SELECT category, AVG(value) as avgValue FROM trend_data WHERE metricName = :metricName AND date >= :startDate AND category IS NOT NULL GROUP BY category ORDER BY avgValue DESC")
    suspend fun getAverageValuesByCategory(metricName: String, startDate: String): List<CategoryAverageResult>

    @Query("SELECT date, AVG(value) as avgValue FROM trend_data WHERE metricName = :metricName AND timeframe = :timeframe AND date >= :startDate GROUP BY date ORDER BY date")
    fun getValueTrendByDate(metricName: String, timeframe: String, startDate: String): Flow<List<ValueTrendResult>>

    @Query("DELETE FROM trend_data WHERE date < :cutoffDate")
    suspend fun deleteOldTrendData(cutoffDate: String)

    @Query("SELECT * FROM trend_data WHERE metricName = :metricName AND date = :date AND timeframe = :timeframe AND category = :category")
    suspend fun getTrendDataForDateAndCategory(metricName: String, date: String, timeframe: String, category: String?): TrendData?

    @Query("SELECT COUNT(*) FROM trend_data WHERE metricName = :metricName AND date >= :startDate")
    suspend fun getTrendDataCount(metricName: String, startDate: String): Int
}

data class CategoryAverageResult(
    val category: String,
    val avgValue: Float
)

data class ValueTrendResult(
    val date: String,
    val avgValue: Float
)