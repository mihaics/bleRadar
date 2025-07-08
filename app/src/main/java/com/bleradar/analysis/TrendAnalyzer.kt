package com.bleradar.analysis

import com.bleradar.data.database.AnalyticsSnapshot
import com.bleradar.data.database.DeviceAnalytics
import com.bleradar.data.database.TrendData
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TrendAnalyzer {
    
    fun analyzeDeviceCountTrend(snapshots: List<AnalyticsSnapshot>): TrendAnalysis {
        val values = snapshots.map { it.totalDevices.toFloat() }
        return analyzeTrend(values, "Device Count")
    }
    
    fun analyzeRssiTrend(snapshots: List<AnalyticsSnapshot>): TrendAnalysis {
        val values = snapshots.map { it.averageRssi }
        return analyzeTrend(values, "Average RSSI")
    }
    
    fun analyzeSuspiciousTrend(snapshots: List<AnalyticsSnapshot>): TrendAnalysis {
        val values = snapshots.map { it.averageSuspiciousScore }
        return analyzeTrend(values, "Suspicious Score")
    }
    
    fun analyzeFollowingTrend(snapshots: List<AnalyticsSnapshot>): TrendAnalysis {
        val values = snapshots.map { it.averageFollowingScore }
        return analyzeTrend(values, "Following Score")
    }
    
    fun analyzeAlertsTrend(snapshots: List<AnalyticsSnapshot>): TrendAnalysis {
        val values = snapshots.map { it.alertsTriggered.toFloat() }
        return analyzeTrend(values, "Alerts")
    }
    
    private fun analyzeTrend(values: List<Float>, metricName: String): TrendAnalysis {
        if (values.size < 2) {
            return TrendAnalysis(
                metricName = metricName,
                trendDirection = TrendDirection.STABLE,
                slope = 0.0,
                correlation = 0.0,
                volatility = 0.0,
                confidence = 0.0,
                prediction = emptyList(),
                anomalies = emptyList(),
                seasonality = SeasonalityAnalysis(),
                summary = "Insufficient data for trend analysis"
            )
        }
        
        val slope = calculateSlope(values)
        val correlation = calculateCorrelation(values)
        val volatility = calculateVolatility(values)
        val confidence = calculateConfidence(values, slope)
        val trendDirection = determineTrendDirection(slope, confidence)
        val prediction = predictNextValues(values, 5)
        val anomalies = detectAnomalies(values)
        val seasonality = analyzeSeasonality(values)
        
        return TrendAnalysis(
            metricName = metricName,
            trendDirection = trendDirection,
            slope = slope,
            correlation = correlation,
            volatility = volatility,
            confidence = confidence,
            prediction = prediction,
            anomalies = anomalies,
            seasonality = seasonality,
            summary = generateSummary(metricName, trendDirection, slope, volatility, confidence)
        )
    }
    
    private fun calculateSlope(values: List<Float>): Double {
        val n = values.size
        val xSum = (0 until n).sum()
        val ySum = values.sum()
        val xySum = values.mapIndexed { index, value -> index * value }.sum()
        val xSquaredSum = (0 until n).map { it * it }.sum()
        
        val numerator = n * xySum - xSum * ySum
        val denominator = n * xSquaredSum - xSum * xSum
        
        return if (denominator != 0) numerator.toDouble() / denominator.toDouble() else 0.0
    }
    
    private fun calculateCorrelation(values: List<Float>): Double {
        val n = values.size
        val xIndices = (0 until n).map { it.toFloat() }
        
        val xMean = xIndices.average()
        val yMean = values.average()
        
        val numerator = xIndices.zip(values).sumOf { (x, y) -> (x - xMean) * (y - yMean) }
        val xVariance = xIndices.sumOf { (it - xMean).pow(2) }
        val yVariance = values.sumOf { (it - yMean).pow(2) }
        
        return if (xVariance > 0 && yVariance > 0) {
            numerator / sqrt(xVariance * yVariance)
        } else 0.0
    }
    
    private fun calculateVolatility(values: List<Float>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    private fun calculateConfidence(values: List<Float>, slope: Double): Double {
        val correlation = calculateCorrelation(values)
        val volatility = calculateVolatility(values)
        val dataPoints = values.size
        
        // Confidence based on correlation strength, data points, and volatility
        val correlationConfidence = abs(correlation)
        val dataConfidence = minOf(dataPoints / 30.0, 1.0) // More data = more confidence
        val volatilityConfidence = 1.0 / (1.0 + volatility / 10.0) // Lower volatility = more confidence
        
        return (correlationConfidence + dataConfidence + volatilityConfidence) / 3.0
    }
    
    private fun determineTrendDirection(slope: Double, confidence: Double): TrendDirection {
        val threshold = 0.01 // Minimum slope to consider a trend
        return when {
            confidence < 0.3 -> TrendDirection.STABLE
            slope > threshold -> TrendDirection.INCREASING
            slope < -threshold -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun predictNextValues(values: List<Float>, count: Int): List<Float> {
        if (values.size < 2) return emptyList()
        
        val slope = calculateSlope(values)
        val lastIndex = values.size - 1
        val lastValue = values.last()
        
        return (1..count).map { i ->
            (lastValue + slope * i).toFloat()
        }
    }
    
    private fun detectAnomalies(values: List<Float>): List<Anomaly> {
        if (values.size < 5) return emptyList()
        
        val mean = values.average()
        val stdDev = sqrt(values.map { (it - mean).pow(2) }.average())
        val threshold = 2.0 * stdDev // 2 standard deviations
        
        return values.mapIndexedNotNull { index, value ->
            val deviation = abs(value - mean)
            if (deviation > threshold) {
                Anomaly(
                    index = index,
                    value = value,
                    expectedValue = mean.toFloat(),
                    deviation = deviation.toFloat(),
                    severity = when {
                        deviation > 3 * stdDev -> AnomalySeverity.HIGH
                        deviation > 2.5 * stdDev -> AnomalySeverity.MEDIUM
                        else -> AnomalySeverity.LOW
                    }
                )
            } else null
        }
    }
    
    private fun analyzeSeasonality(values: List<Float>): SeasonalityAnalysis {
        if (values.size < 24) { // Need at least 24 points for meaningful seasonality
            return SeasonalityAnalysis()
        }
        
        // Simple seasonality detection using autocorrelation
        val periods = listOf(7, 24, 168) // Daily, hourly, weekly patterns
        val seasonalityStrength = mutableMapOf<Int, Double>()
        
        for (period in periods) {
            if (values.size >= period * 2) {
                val autocorrelation = calculateAutocorrelation(values, period)
                seasonalityStrength[period] = autocorrelation
            }
        }
        
        val strongestPeriod = seasonalityStrength.maxByOrNull { it.value }
        
        return SeasonalityAnalysis(
            hasSeasonality = (strongestPeriod?.value ?: 0.0) > 0.5,
            period = strongestPeriod?.key ?: 0,
            strength = strongestPeriod?.value ?: 0.0,
            patterns = seasonalityStrength
        )
    }
    
    private fun calculateAutocorrelation(values: List<Float>, lag: Int): Double {
        if (values.size <= lag) return 0.0
        
        val n = values.size - lag
        val mean = values.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in 0 until n) {
            numerator += (values[i] - mean) * (values[i + lag] - mean)
        }
        
        for (value in values) {
            denominator += (value - mean).pow(2)
        }
        
        return if (denominator > 0) numerator / denominator else 0.0
    }
    
    private fun generateSummary(
        metricName: String,
        direction: TrendDirection,
        slope: Double,
        volatility: Double,
        confidence: Double
    ): String {
        val directionText = when (direction) {
            TrendDirection.INCREASING -> "increasing"
            TrendDirection.DECREASING -> "decreasing"
            TrendDirection.STABLE -> "stable"
        }
        
        val volatilityText = when {
            volatility < 1.0 -> "low"
            volatility < 3.0 -> "moderate"
            else -> "high"
        }
        
        val confidenceText = when {
            confidence > 0.7 -> "high"
            confidence > 0.4 -> "moderate"
            else -> "low"
        }
        
        return "$metricName shows a $directionText trend with $volatilityText volatility. " +
               "Confidence in this analysis is $confidenceText (${String.format("%.1f", confidence * 100)}%)."
    }
    
    fun comparePerformanceMetrics(
        currentPeriod: List<AnalyticsSnapshot>,
        previousPeriod: List<AnalyticsSnapshot>
    ): PerformanceComparison {
        val currentMetrics = calculatePeriodMetrics(currentPeriod)
        val previousMetrics = calculatePeriodMetrics(previousPeriod)
        
        return PerformanceComparison(
            deviceCountChange = calculatePercentageChange(
                previousMetrics.averageDeviceCount,
                currentMetrics.averageDeviceCount
            ),
            rssiChange = calculatePercentageChange(
                previousMetrics.averageRssi,
                currentMetrics.averageRssi
            ),
            suspiciousScoreChange = calculatePercentageChange(
                previousMetrics.averageSuspiciousScore,
                currentMetrics.averageSuspiciousScore
            ),
            followingScoreChange = calculatePercentageChange(
                previousMetrics.averageFollowingScore,
                currentMetrics.averageFollowingScore
            ),
            alertsChange = calculatePercentageChange(
                previousMetrics.averageAlerts,
                currentMetrics.averageAlerts
            ),
            currentMetrics = currentMetrics,
            previousMetrics = previousMetrics
        )
    }
    
    private fun calculatePeriodMetrics(snapshots: List<AnalyticsSnapshot>): PeriodMetrics {
        if (snapshots.isEmpty()) {
            return PeriodMetrics()
        }
        
        return PeriodMetrics(
            averageDeviceCount = snapshots.map { it.totalDevices }.average().toFloat(),
            averageRssi = snapshots.map { it.averageRssi }.average().toFloat(),
            averageSuspiciousScore = snapshots.map { it.averageSuspiciousScore }.average().toFloat(),
            averageFollowingScore = snapshots.map { it.averageFollowingScore }.average().toFloat(),
            averageAlerts = snapshots.map { it.alertsTriggered }.average().toFloat(),
            totalNewDevices = snapshots.sumOf { it.newDevicesDiscovered },
            totalDetections = snapshots.sumOf { it.activeDetections }
        )
    }
    
    private fun calculatePercentageChange(previous: Float, current: Float): Float {
        return if (previous != 0f) {
            ((current - previous) / previous) * 100
        } else if (current != 0f) {
            100f // 100% increase from zero
        } else {
            0f // No change
        }
    }
}

data class TrendAnalysis(
    val metricName: String,
    val trendDirection: TrendDirection,
    val slope: Double,
    val correlation: Double,
    val volatility: Double,
    val confidence: Double,
    val prediction: List<Float>,
    val anomalies: List<Anomaly>,
    val seasonality: SeasonalityAnalysis,
    val summary: String
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

data class Anomaly(
    val index: Int,
    val value: Float,
    val expectedValue: Float,
    val deviation: Float,
    val severity: AnomalySeverity
)

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH
}

data class SeasonalityAnalysis(
    val hasSeasonality: Boolean = false,
    val period: Int = 0,
    val strength: Double = 0.0,
    val patterns: Map<Int, Double> = emptyMap()
)

data class PerformanceComparison(
    val deviceCountChange: Float,
    val rssiChange: Float,
    val suspiciousScoreChange: Float,
    val followingScoreChange: Float,
    val alertsChange: Float,
    val currentMetrics: PeriodMetrics,
    val previousMetrics: PeriodMetrics
)

data class PeriodMetrics(
    val averageDeviceCount: Float = 0f,
    val averageRssi: Float = 0f,
    val averageSuspiciousScore: Float = 0f,
    val averageFollowingScore: Float = 0f,
    val averageAlerts: Float = 0f,
    val totalNewDevices: Int = 0,
    val totalDetections: Int = 0
)