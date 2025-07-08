package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bleradar.ui.components.BarChart
import com.bleradar.ui.components.ChartData
import com.bleradar.ui.components.LineChart
import com.bleradar.ui.components.PieChart
import com.bleradar.ui.components.PieChartData
import com.bleradar.ui.viewmodel.AnalyticsViewModel
import com.bleradar.ui.viewmodel.MetricType
import com.bleradar.ui.viewmodel.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val selectedMetric by viewModel.selectedMetric.collectAsState()
    
    // Add safety check to prevent early rendering issues
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Small delay to ensure proper initialization
    }
    
    // Add state validation - ensure lists are not empty before using them
    val hasValidData = uiState.recentSnapshots.isNotEmpty() || 
                      uiState.deviceCountTrend.isNotEmpty() || 
                      uiState.rssiTrend.isNotEmpty() || 
                      uiState.alertsTrend.isNotEmpty() || 
                      uiState.isLoading
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refreshAnalytics() },
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = uiState.error!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time range selector
                item(key = "time_range_selector") {
                    TimeRangeSelector(
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = viewModel::onTimeRangeChanged
                    )
                }
                
                // Summary cards
                uiState.summary?.let { summary ->
                    item(key = "summary_cards") {
                        SummaryCards(summary = summary)
                    }
                }
                
                // Weekly comparison
                uiState.weeklyComparison?.let { comparison ->
                    item(key = "weekly_comparison") {
                        WeeklyComparisonCard(comparison = comparison)
                    }
                }
                
                // Metric selector
                item(key = "metric_selector") {
                    MetricSelector(
                        selectedMetric = selectedMetric,
                        onMetricSelected = viewModel::onMetricChanged
                    )
                }
                
                // Trend chart
                item(key = "trend_chart") {
                    TrendChart(
                        uiState = uiState,
                        selectedMetric = selectedMetric
                    )
                }
                
                // Top devices section
                if (uiState.topFollowingDevices.isNotEmpty() || 
                    uiState.topSuspiciousDevices.isNotEmpty() || 
                    uiState.mostActiveDevices.isNotEmpty()) {
                    item(key = "top_devices") {
                        TopDevicesSection(
                            topFollowing = uiState.topFollowingDevices,
                            topSuspicious = uiState.topSuspiciousDevices,
                            mostActive = uiState.mostActiveDevices
                        )
                    }
                }
                
                // Device distribution pie chart
                uiState.summary?.let { 
                    item(key = "device_distribution") {
                        DeviceDistributionChart(uiState = uiState)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    val timeRanges = TimeRange.entries
    if (timeRanges.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeRanges, key = { it.name }) { timeRange ->
                FilterChip(
                    selected = selectedTimeRange == timeRange,
                    onClick = { onTimeRangeSelected(timeRange) },
                    label = { Text(timeRange.displayName) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricSelector(
    selectedMetric: MetricType,
    onMetricSelected: (MetricType) -> Unit
) {
    val metrics = MetricType.entries
    if (metrics.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(metrics, key = { it.name }) { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { onMetricSelected(metric) },
                    label = { Text(metric.displayName) }
                )
            }
        }
    }
}

@Composable
fun SummaryCards(summary: com.bleradar.repository.AnalyticsSummary) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "total_devices") {
            SummaryCard(
                title = "Total",
                value = summary.totalDevices.toString(),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item(key = "new_devices") {
            SummaryCard(
                title = "New",
                value = summary.newDevicesDiscovered.toString(),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item(key = "avg_rssi") {
            SummaryCard(
                title = "Avg RSSI",
                value = "${summary.averageRssi.toInt()} dBm",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        item(key = "max_alerts") {
            SummaryCard(
                title = "Alerts",
                value = summary.maxAlertsInPeriod.toString(),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .widthIn(min = 100.dp, max = 160.dp)
            .heightIn(min = 80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WeeklyComparisonCard(comparison: com.bleradar.repository.WeeklyTrendComparison) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonItem(
                    label = "Devices",
                    change = comparison.deviceCountChange.toFloat(),
                    current = comparison.thisWeekDevices
                )
                ComparisonItem(
                    label = "RSSI",
                    change = comparison.rssiChange,
                    current = null,
                    isFloat = true
                )
                ComparisonItem(
                    label = "Suspicious",
                    change = comparison.suspiciousScoreChange,
                    current = null,
                    isFloat = true
                )
            }
        }
    }
}

@Composable
fun ComparisonItem(
    label: String,
    change: Float,
    current: Int?,
    isFloat: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (current != null) {
            Text(
                text = current.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        val changeText = if (isFloat) {
            "${if (change > 0) "+" else ""}${String.format("%.2f", change)}"
        } else {
            "${if (change > 0) "+" else ""}${change.toInt()}"
        }
        
        Text(
            text = changeText,
            style = MaterialTheme.typography.bodySmall,
            color = if (change > 0) Color.Green else if (change < 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrendChart(
    uiState: com.bleradar.ui.viewmodel.AnalyticsUiState,
    selectedMetric: MetricType
) {
    when (selectedMetric) {
        MetricType.DEVICE_COUNT -> {
            if (uiState.deviceCountTrend.isNotEmpty()) {
                LineChart(
                    data = uiState.deviceCountTrend.map { 
                        ChartData(it.date, it.deviceCount.toFloat()) 
                    },
                    title = "Device Count Trend",
                    lineColor = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "No device count data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MetricType.RSSI -> {
            if (uiState.rssiTrend.isNotEmpty()) {
                LineChart(
                    data = uiState.rssiTrend.map { 
                        ChartData(it.date, it.avgRssi) 
                    },
                    title = "Average RSSI Trend",
                    lineColor = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Text(
                    text = "No RSSI data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MetricType.ALERTS -> {
            if (uiState.alertsTrend.isNotEmpty()) {
                BarChart(
                    data = uiState.alertsTrend.map { 
                        ChartData(it.date, it.totalAlerts.toFloat()) 
                    },
                    title = "Alerts Trend",
                    barColor = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "No alerts data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopDevicesSection(
    topFollowing: List<com.bleradar.data.database.DeviceScoreResult>,
    topSuspicious: List<com.bleradar.data.database.DeviceScoreResult>,
    mostActive: List<com.bleradar.data.database.DeviceActivityResult>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Top Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Following",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (topFollowing.isNotEmpty()) {
                        topFollowing.take(3).forEach { device ->
                            Text(
                                text = "${device.deviceAddress.take(6)}... (${String.format("%.2f", device.maxScore)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Suspicious",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (topSuspicious.isNotEmpty()) {
                        topSuspicious.take(3).forEach { device ->
                            Text(
                                text = "${device.deviceAddress.take(6)}... (${String.format("%.2f", device.maxScore)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Most Active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (mostActive.isNotEmpty()) {
                        mostActive.take(3).forEach { device ->
                            Text(
                                text = "${device.deviceAddress.take(6)}... (${device.totalDetections})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceDistributionChart(uiState: com.bleradar.ui.viewmodel.AnalyticsUiState) {
    val summary = uiState.summary ?: return
    
    val pieData = listOf(
        PieChartData(
            label = "Tracked",
            value = summary.totalDevices.toFloat(),
            color = MaterialTheme.colorScheme.primary
        ),
        PieChartData(
            label = "Suspicious",
            value = (summary.averageSuspiciousScore * 100).toInt().toFloat(),
            color = MaterialTheme.colorScheme.error
        ),
        PieChartData(
            label = "Following",
            value = (summary.averageFollowingScore * 100).toInt().toFloat(),
            color = MaterialTheme.colorScheme.secondary
        )
    )
    
    PieChart(
        data = pieData,
        title = "Device Distribution"
    )
}