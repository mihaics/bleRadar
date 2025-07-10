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
import com.bleradar.ui.viewmodel.SimpleAnalyticsViewModel
import com.bleradar.ui.viewmodel.SimpleTimeRange
import com.bleradar.repository.SimpleAnalytics
import com.bleradar.repository.RecentActivity
import com.bleradar.repository.DeviceInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAnalyticsScreen(
    viewModel: SimpleAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    
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
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { viewModel.refresh() },
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
        
        if (uiState.error != null) {
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
                item {
                    SimpleTimeRangeSelector(
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = viewModel::onTimeRangeChanged
                    )
                }
                
                // Main stats
                item {
                    MainStatsSection(analytics = uiState.analytics)
                }
                
                // Device info
                item {
                    DeviceInfoSection(analytics = uiState.analytics)
                }
                
                // Recent activity
                if (uiState.recentActivity.isNotEmpty()) {
                    item {
                        RecentActivitySection(activity = uiState.recentActivity)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimeRangeSelector(
    selectedTimeRange: SimpleTimeRange,
    onTimeRangeSelected: (SimpleTimeRange) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SimpleTimeRange.entries) { timeRange ->
            FilterChip(
                selected = selectedTimeRange == timeRange,
                onClick = { onTimeRangeSelected(timeRange) },
                label = { Text(timeRange.displayName) }
            )
        }
    }
}

@Composable
fun MainStatsSection(analytics: SimpleAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = "Total Devices",
                        value = analytics.totalDevices.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    StatCard(
                        title = "Tracked",
                        value = analytics.trackedDevices.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                item {
                    StatCard(
                        title = "Suspicious",
                        value = analytics.suspiciousDevices.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                item {
                    StatCard(
                        title = "New",
                        value = analytics.newDevices.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Detections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analytics.totalDetections.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Avg RSSI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${analytics.averageRssi.toInt()} dBm",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoSection(analytics: SimpleAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notable Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            analytics.mostActiveDevice?.let { device ->
                DeviceCard(
                    title = "Most Active",
                    device = device,
                    valueLabel = "detections"
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            analytics.strongestSignalDevice?.let { device ->
                DeviceCard(
                    title = "Strongest Signal",
                    device = device,
                    valueLabel = "dBm"
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    title: String,
    device: DeviceInfo,
    valueLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = device.address.take(8) + "...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${device.value} $valueLabel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecentActivitySection(activity: List<RecentActivity>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Activity (24h)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            activity.take(5).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.deviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${item.detectionCount} detections",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${item.averageRssi.toInt()} dBm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatTime(item.lastSeen),
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
fun StatCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .widthIn(min = 80.dp, max = 120.dp)
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${(diff / (60 * 1000))}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${(diff / (60 * 60 * 1000))}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}