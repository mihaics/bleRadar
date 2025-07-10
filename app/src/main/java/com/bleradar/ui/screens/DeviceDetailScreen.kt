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
import com.bleradar.data.database.BleDevice
import com.bleradar.data.database.BleDetection
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceAddress: String,
    onNavigateBack: () -> Unit,
    onShowOnMap: (String) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsStateWithLifecycle()
    val detections by viewModel.detections.collectAsStateWithLifecycle()
    val locationHistory by viewModel.locationHistory.collectAsStateWithLifecycle()
    
    LaunchedEffect(deviceAddress) {
        viewModel.loadDevice(deviceAddress)
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
                onClick = { onShowOnMap(deviceAddress) },
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
fun DeviceInfoCard(device: BleDevice) {
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
                value = device.deviceName ?: "Unknown Device"
            )
            
            InfoRow(
                label = "Address",
                value = device.deviceAddress
            )
            
            InfoRow(
                label = "Label",
                value = device.label ?: "No label"
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
                label = "RSSI",
                value = "${device.rssi} dBm"
            )
            
            InfoRow(
                label = "Manufacturer",
                value = device.manufacturer ?: "Unknown"
            )
            
            InfoRow(
                label = "Device Type",
                value = device.deviceType ?: "Unknown"
            )
            
            InfoRow(
                label = "Status",
                value = if (device.isTracked) "Tracked" else if (device.isIgnored) "Ignored" else "Normal"
            )
        }
    }
}

@Composable
fun ThreatAssessmentCard(device: BleDevice) {
    val threatLevel = when {
        device.followingScore > 0.7f -> "HIGH"
        device.followingScore > 0.5f -> "MEDIUM"
        device.followingScore > 0.3f -> "LOW"
        else -> "MINIMAL"
    }
    
    val threatColor = when {
        device.followingScore > 0.7f -> Color.Red
        device.followingScore > 0.5f -> Color(0xFFFFA500)
        device.followingScore > 0.3f -> Color.Yellow
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
                    text = "${(device.followingScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = threatColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = device.followingScore,
                modifier = Modifier.fillMaxWidth(),
                color = threatColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                label = "Detection Count",
                value = device.detectionCount.toString()
            )
            
            InfoRow(
                label = "Consecutive Detections",
                value = device.consecutiveDetections.toString()
            )
            
            InfoRow(
                label = "Suspicious Activity Score",
                value = "${(device.suspiciousActivityScore * 100).toInt()}%"
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
fun LocationHistoryItem(detection: BleDetection) {
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
    device: BleDevice,
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
                value = detectionCount.toString()
            )
            
            InfoRow(
                label = "Average RSSI",
                value = "${device.averageRssi} dBm"
            )
            
            InfoRow(
                label = "RSSI Variation",
                value = "${String.format("%.1f", device.rssiVariation)} dBm"
            )
            
            device.lastMovementTime.let { movementTime ->
                InfoRow(
                    label = "Last Movement",
                    value = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(movementTime))
                )
            }
            
            device.lastAlertTime.let { alertTime ->
                InfoRow(
                    label = "Last Alert",
                    value = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(alertTime))
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