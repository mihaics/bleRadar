package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
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
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val timeRange = _selectedTimeRange.value
                val daysBack = timeRange.days
                
                // Load summary data with error handling
                val summary = try {
                    analyticsRepository.getAnalyticsSummary(daysBack)
                } catch (e: Exception) {
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
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load analytics: ${e.message}"
                        )
                    }
                    .collect { snapshots ->
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