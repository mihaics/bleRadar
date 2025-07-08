package com.bleradar.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.data.database.AnalyticsSnapshot
import com.bleradar.data.database.DeviceAnalytics
import com.bleradar.data.database.TrendData
import com.bleradar.data.database.DeviceActivityResult
import com.bleradar.data.database.DeviceScoreResult
import com.bleradar.data.database.RssiTrendResult
import com.bleradar.data.database.DeviceCountTrendResult
import com.bleradar.data.database.AlertsTrendResult
import com.bleradar.repository.AnalyticsRepository
import com.bleradar.repository.AnalyticsSummary
import com.bleradar.repository.WeeklyTrendComparison
import com.bleradar.service.AnalyticsCollectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val application: Application
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    private val _selectedMetric = MutableStateFlow(MetricType.DEVICE_COUNT)
    val selectedMetric: StateFlow<MetricType> = _selectedMetric.asStateFlow()
    
    init {
        // Delay initial load to allow dependencies to initialize
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Small delay
            loadAnalytics()
        }
    }
    
    fun onTimeRangeChanged(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
        loadAnalytics()
    }
    
    fun onMetricChanged(metric: MetricType) {
        _selectedMetric.value = metric
        loadTrendData()
    }
    
    fun refreshAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                android.util.Log.d("AnalyticsViewModel", "Triggered analytics collection, waiting for data...")
                
                // Get current device data directly from repository instead of waiting for snapshot
                loadCurrentDeviceData()
                
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Failed to refresh analytics", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadCurrentDeviceData() {
        try {
            // Get device data directly using the analytics repository's database access
            val allDevices = analyticsRepository.getAllDevicesIncludingIgnored().filter { !it.isIgnored }
            android.util.Log.d("AnalyticsViewModel", "Direct device query found: ${allDevices.size} devices")
            
            if (allDevices.isNotEmpty()) {
                // Create summary from current device data
                val trackedDevices = allDevices.filter { it.isTracked }
                val suspiciousDevices = allDevices.filter { it.suspiciousActivityScore > 0.5f }
                val averageRssi = allDevices.map { it.averageRssi }.average().toFloat()
                val averageFollowing = allDevices.map { it.followingScore }.average().toFloat()
                val averageSuspicious = allDevices.map { it.suspiciousActivityScore }.average().toFloat()
                
                val currentSummary = com.bleradar.repository.AnalyticsSummary(
                    totalDevices = allDevices.size,
                    totalDetections = allDevices.sumOf { it.detectionCount },
                    averageRssi = averageRssi,
                    averageFollowingScore = averageFollowing,
                    averageSuspiciousScore = averageSuspicious,
                    totalDistanceTraveled = 0f, // Could be calculated if needed
                    maxAlertsInPeriod = suspiciousDevices.size,
                    newDevicesDiscovered = allDevices.filter { 
                        (System.currentTimeMillis() - it.firstSeen) < (24 * 60 * 60 * 1000) 
                    }.size,
                    daysAnalyzed = 7
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = currentSummary,
                    error = null
                )
                
                android.util.Log.d("AnalyticsViewModel", "Created current summary with ${currentSummary.totalDevices} devices")
            } else {
                // Trigger service collection and then try loading again
                triggerAnalyticsCollection()
                delay(2000)
                loadAnalytics()
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Failed to load current device data", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load device data: ${e.message}"
            )
        }
    }
    
    private fun triggerAnalyticsCollection() {
        try {
            val intent = Intent(application, AnalyticsCollectionService::class.java)
            intent.action = "COLLECT_NOW"
            application.startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Failed to trigger analytics collection", e)
        }
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val timeRange = _selectedTimeRange.value
                val daysBack = timeRange.days
                
                // Load summary data with error handling
                val summary = try {
                    val result = analyticsRepository.getAnalyticsSummary(daysBack)
                    android.util.Log.d("AnalyticsViewModel", "Loaded summary: ${result?.totalDevices ?: 0} total devices")
                    result
                } catch (e: Exception) {
                    android.util.Log.e("AnalyticsViewModel", "Failed to load summary", e)
                    null
                }
                
                val weeklyComparison = try {
                    analyticsRepository.getWeeklyTrendComparison()
                } catch (e: Exception) {
                    null
                }
                
                // Load recent snapshots
                analyticsRepository.getRecentSnapshots(50)
                    .catch { e -> 
                        android.util.Log.e("AnalyticsViewModel", "Failed to load snapshots", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load analytics: ${e.message}"
                        )
                    }
                    .collect { snapshots ->
                        android.util.Log.d("AnalyticsViewModel", "Loaded ${snapshots?.size ?: 0} snapshots")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            summary = summary,
                            weeklyComparison = weeklyComparison,
                            recentSnapshots = snapshots ?: emptyList(),
                            error = null
                        )
                    }
                
                // Load top devices with error handling
                try {
                    loadTopDevices(daysBack)
                } catch (e: Exception) {
                    // Continue without top devices data
                }
                
                // Load trend data with error handling
                try {
                    loadTrendData()
                } catch (e: Exception) {
                    // Continue without trend data
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }
    
    private fun loadTopDevices(daysBack: Int) {
        viewModelScope.launch {
            try {
                val topFollowing = analyticsRepository.getTopFollowingDevices(daysBack, 5)
                val topSuspicious = analyticsRepository.getTopSuspiciousDevices(daysBack, 5)
                val mostActive = analyticsRepository.getMostActiveDevices(daysBack, 5)
                
                _uiState.value = _uiState.value.copy(
                    topFollowingDevices = topFollowing,
                    topSuspiciousDevices = topSuspicious,
                    mostActiveDevices = mostActive
                )
            } catch (e: Exception) {
                // Handle error silently for secondary data
            }
        }
    }
    
    private fun loadTrendData() {
        viewModelScope.launch {
            try {
                val timeRange = _selectedTimeRange.value
                val metric = _selectedMetric.value
                val daysBack = timeRange.days
                
                when (metric) {
                    MetricType.DEVICE_COUNT -> {
                        analyticsRepository.getDeviceCountTrendByDate(daysBack)
                            .catch { /* Handle error */ }
                            .collect { trends ->
                                _uiState.value = _uiState.value.copy(
                                    deviceCountTrend = trends
                                )
                            }
                    }
                    MetricType.RSSI -> {
                        analyticsRepository.getRssiTrendByDate(daysBack)
                            .catch { /* Handle error */ }
                            .collect { trends ->
                                _uiState.value = _uiState.value.copy(
                                    rssiTrend = trends
                                )
                            }
                    }
                    MetricType.ALERTS -> {
                        analyticsRepository.getAlertsTrendByDate(daysBack)
                            .catch { /* Handle error */ }
                            .collect { trends ->
                                _uiState.value = _uiState.value.copy(
                                    alertsTrend = trends
                                )
                            }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently for trend data
            }
        }
    }
    
    fun getDeviceAnalytics(deviceAddress: String) {
        viewModelScope.launch {
            try {
                val timeRange = _selectedTimeRange.value
                val deviceSummary = analyticsRepository.getDeviceAnalyticsSummary(deviceAddress, timeRange.days)
                
                analyticsRepository.getAnalyticsForDevice(deviceAddress)
                    .catch { /* Handle error */ }
                    .collect { analytics ->
                        _uiState.value = _uiState.value.copy(
                            selectedDeviceAnalytics = analytics,
                            selectedDeviceSummary = deviceSummary
                        )
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun clearDeviceAnalytics() {
        _uiState.value = _uiState.value.copy(
            selectedDeviceAnalytics = emptyList(),
            selectedDeviceSummary = null
        )
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val summary: AnalyticsSummary? = null,
    val weeklyComparison: WeeklyTrendComparison? = null,
    val recentSnapshots: List<AnalyticsSnapshot> = emptyList(),
    val topFollowingDevices: List<DeviceScoreResult> = emptyList(),
    val topSuspiciousDevices: List<DeviceScoreResult> = emptyList(),
    val mostActiveDevices: List<DeviceActivityResult> = emptyList(),
    val deviceCountTrend: List<DeviceCountTrendResult> = emptyList(),
    val rssiTrend: List<RssiTrendResult> = emptyList(),
    val alertsTrend: List<AlertsTrendResult> = emptyList(),
    val selectedDeviceAnalytics: List<DeviceAnalytics> = emptyList(),
    val selectedDeviceSummary: com.bleradar.repository.DeviceAnalyticsSummary? = null
)

enum class TimeRange(val displayName: String, val days: Int) {
    DAY("24 Hours", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    QUARTER("90 Days", 90)
}

enum class MetricType(val displayName: String) {
    DEVICE_COUNT("Device Count"),
    RSSI("Signal Strength"),
    ALERTS("Alerts")
}