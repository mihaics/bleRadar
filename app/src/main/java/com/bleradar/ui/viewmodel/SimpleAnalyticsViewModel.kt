package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bleradar.repository.SimpleAnalyticsRepository
import com.bleradar.repository.SimpleAnalytics
import com.bleradar.repository.RecentActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimpleAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: SimpleAnalyticsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SimpleAnalyticsUiState())
    val uiState: StateFlow<SimpleAnalyticsUiState> = _uiState.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(SimpleTimeRange.WEEK)
    val selectedTimeRange: StateFlow<SimpleTimeRange> = _selectedTimeRange.asStateFlow()
    
    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val timeRange = _selectedTimeRange.value
                val analytics = analyticsRepository.getSimpleAnalytics(timeRange.days)
                val recentActivity = analyticsRepository.getRecentActivity(24)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analytics = analytics,
                    recentActivity = recentActivity,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }
    
    fun onTimeRangeChanged(timeRange: SimpleTimeRange) {
        _selectedTimeRange.value = timeRange
        loadAnalytics()
    }
    
    fun refresh() {
        loadAnalytics()
    }
    
    init {
        loadAnalytics()
    }
}

data class SimpleAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val analytics: SimpleAnalytics = SimpleAnalytics(),
    val recentActivity: List<RecentActivity> = emptyList()
)

enum class SimpleTimeRange(val displayName: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30)
}