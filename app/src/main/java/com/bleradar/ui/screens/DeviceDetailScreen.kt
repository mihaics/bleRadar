package com.bleradar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bleradar.ui.viewmodel.DeviceDetailViewModel
import com.bleradar.ui.model.DeviceDisplayModel
import com.bleradar.data.database.FingerprintDetection
import com.bleradar.data.database.DeviceMacAddress
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceUuid: String,
    onNavigateBack: () -> Unit,
    onShowOnMap: (String) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsStateWithLifecycle()
    val detections by viewModel.detections.collectAsStateWithLifecycle()
    val locationHistory by viewModel.locationHistory.collectAsStateWithLifecycle()
    val macAddresses by viewModel.macAddresses.collectAsStateWithLifecycle()
    
    LaunchedEffect(deviceUuid) {
        viewModel.loadDevice(deviceUuid)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Text(
                text = "Device Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Show on map button
            IconButton(
                onClick = { onShowOnMap(deviceUuid) },
                enabled = locationHistory.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "Show on Map",
                    tint = if (locationHistory.isNotEmpty()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        device?.let { dev ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device info card
                item {
                    DeviceInfoCard(device = dev)
                }
                
                // Threat assessment card
                item {
                    ThreatAssessmentCard(device = dev)
                }
                
                // Location history header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Location History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${locationHistory.size} locations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Location history items
                items(locationHistory) { detection ->
                    LocationHistoryItem(detection = detection)
                }
                
                // Detection statistics
                item {
                    DetectionStatsCard(
                        device = dev,
                        detectionCount = detections.size
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DeviceInfoCard(device: DeviceDisplayModel) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = "Name",
                value = device.displayName
            )
            
            InfoRow(
                label = "Primary Address",
                value = device.primaryMacAddress
            )
            
            InfoRow(
                label = "MAC Address Count",
                value = device.macAddressCount.toString()
            )
            
            InfoRow(
                label = "Label",
                value = device.notes ?: "No label"
            )
            
            InfoRow(
                label = "First Seen",
                value = dateFormat.format(Date(device.firstSeen))
            )
            
            InfoRow(
                label = "Last Seen",
                value = dateFormat.format(Date(device.lastSeen))
            )
            
            InfoRow(
                label = "Device UUID",
                value = device.deviceUuid
            )
            
            InfoRow(
                label = "Manufacturer",
                value = device.manufacturer ?: "Unknown"
            )
            
            InfoRow(
                label = "Device Type",
                value = device.deviceType.name
            )
            
            InfoRow(
                label = "Status",
                value = if (device.isTracked) "Tracked" else "Normal"
            )
            
            InfoRow(
                label = "Confidence",
                value = "${(device.confidence * 100).toInt()}%"
            )
        }
    }
}

@Composable
fun ThreatAssessmentCard(device: DeviceDisplayModel) {
    val threatLevel = device.getThreatLevel().name
    
    val threatColor = when {
        device.suspiciousScore > 0.7f -> Color.Red
        device.suspiciousScore > 0.5f -> Color(0xFFFFA500)
        device.suspiciousScore > 0.3f -> Color.Yellow
        else -> Color.Green
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = threatColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Threat Level",
                    tint = threatColor
                )
                Text(
                    text = "Threat Assessment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Threat Level: $threatLevel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = threatColor
                )
                
                Text(
                    text = "${(device.suspiciousScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = threatColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = device.suspiciousScore,
                modifier = Modifier.fillMaxWidth(),
                color = threatColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = "Total Detections",
                value = device.totalDetections.toString()
            )
            
            InfoRow(
                label = "Suspicious Score",
                value = "${(device.suspiciousScore * 100).toInt()}%"
            )
            
            InfoRow(
                label = "Following Score",
                value = "${(device.followingScore * 100).toInt()}%"
            )
            
            if (device.isKnownTracker) {
                InfoRow(
                    label = "Known Tracker",
                    value = device.trackerType ?: "Yes"
                )
            }
        }
    }
}

@Composable
fun LocationHistoryItem(detection: FingerprintDetection) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${String.format("%.6f", detection.latitude)}, ${String.format("%.6f", detection.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = dateFormat.format(Date(detection.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "RSSI: ${detection.rssi} dBm, Accuracy: ±${String.format("%.0f", detection.accuracy)}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DetectionStatsCard(
    device: DeviceDisplayModel,
    detectionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Statistics",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Detection Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = "Total Detections",
                value = device.totalDetections.toString()
            )
            
            InfoRow(
                label = "Confidence Level",
                value = "${(device.confidence * 100).toInt()}%"
            )
            
            if (device.isKnownTracker) {
                InfoRow(
                    label = "Tracker Type",
                    value = device.trackerType ?: "Unknown"
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}