package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bleradar.ui.viewmodel.AlertsViewModel
import com.bleradar.ui.model.DeviceDisplayModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onNavigateToMap: (deviceUuid: String) -> Unit = {}
) {
    val suspiciousDevices by viewModel.suspiciousDevices.collectAsStateWithLifecycle(initialValue = emptyList())
    val trackedDevices by viewModel.trackedDevices.collectAsStateWithLifecycle(initialValue = emptyList())
    val knownTrackers by viewModel.knownTrackers.collectAsStateWithLifecycle(initialValue = emptyList())
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (suspiciousDevices.isNotEmpty()) Color.Red.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Security Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (suspiciousDevices.isNotEmpty()) 
                                "${suspiciousDevices.size} suspicious devices detected" 
                            else "No threats detected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (suspiciousDevices.isNotEmpty()) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Suspicious devices section
        if (suspiciousDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Suspicious Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(suspiciousDevices) { device ->
                SuspiciousDeviceCard(device = device, viewModel = viewModel, onNavigateToMap = onNavigateToMap)
            }
        }
        
        // Known trackers section
        if (knownTrackers.isNotEmpty()) {
            item {
                Text(
                    text = "Known Trackers Detected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(knownTrackers) { device ->
                KnownTrackerCard(device = device, viewModel = viewModel, onNavigateToMap = onNavigateToMap)
            }
        }
        
        // Tracked devices section
        if (trackedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Tracked Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(trackedDevices) { device ->
                TrackedDeviceCard(device = device, viewModel = viewModel, onNavigateToMap = onNavigateToMap)
            }
        }
        
        // Empty state
        if (suspiciousDevices.isEmpty() && trackedDevices.isEmpty() && knownTrackers.isEmpty()) {
            item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No alerts at this time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousDeviceCard(device: DeviceDisplayModel, viewModel: AlertsViewModel, onNavigateToMap: (deviceUuid: String) -> Unit = {}) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    // State for button feedback
    var markSafeClicked by remember { mutableStateOf(false) }
    var trackClicked by remember { mutableStateOf(false) }
    var ignoreClicked by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.primaryMacAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (device.notes != null) {
                        Text(
                            text = "Label: ${device.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Location history button
                IconButton(
                    onClick = { onNavigateToMap(device.deviceUuid) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Show location history",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Following score
            LinearProgressIndicator(
                progress = device.suspiciousScore,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Following score: ${(device.suspiciousScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Last seen: ${dateFormat.format(Date(device.lastSeen))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "⚠️ This device may be following you. Consider your safety and take appropriate action.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        markSafeClicked = true
                        viewModel.markDeviceAsLegitimate(device.deviceUuid)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (markSafeClicked) "✓ Marked Safe" else "Mark Safe", style = MaterialTheme.typography.labelMedium)
                }
                
                OutlinedButton(
                    onClick = { 
                        trackClicked = true
                        viewModel.trackDevice(device.deviceUuid)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF9800)
                    )
                ) {
                    Text(if (trackClicked) "✓ Tracking" else "Track", style = MaterialTheme.typography.labelMedium)
                }
                
                Button(
                    onClick = { 
                        ignoreClicked = true
                        viewModel.ignoreDevice(device.deviceUuid)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    Text(if (ignoreClicked) "✓ Ignored" else "Ignore", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnownTrackerCard(device: DeviceDisplayModel, viewModel: AlertsViewModel, onNavigateToMap: (deviceUuid: String) -> Unit = {}) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.primaryMacAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "🏷️ Known Tracker Type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Location history button
                IconButton(
                    onClick = { onNavigateToMap(device.deviceUuid) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Show location history",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "${device.macAddressCount} MACs",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Last seen: ${dateFormat.format(Date(device.lastSeen))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (device.suspiciousScore > 0.3f) {
                Text(
                    text = "⚠️ Tracking score: ${(device.suspiciousScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "ℹ️ This appears to be a legitimate tracker (low tracking score)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons for known trackers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.isTracked) {
                    Button(
                        onClick = { viewModel.untrackDevice(device.deviceUuid) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Stop Tracking", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.trackDevice(device.deviceUuid) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Track Device", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                OutlinedButton(
                    onClick = { viewModel.ignoreDevice(device.deviceUuid) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("Ignore", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedDeviceCard(device: DeviceDisplayModel, viewModel: AlertsViewModel, onNavigateToMap: (deviceUuid: String) -> Unit = {}) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.primaryMacAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (device.notes != null) {
                        Text(
                            text = "Label: ${device.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Location history button
                IconButton(
                    onClick = { onNavigateToMap(device.deviceUuid) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Show location history",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "${device.macAddressCount} MACs",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Last seen: ${dateFormat.format(Date(device.lastSeen))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = "✓ This device is being tracked for notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons for tracked devices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.untrackDevice(device.deviceUuid) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    Text("Stop Tracking", style = MaterialTheme.typography.labelMedium)
                }
                
                OutlinedButton(
                    onClick = { viewModel.ignoreDevice(device.deviceUuid) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("Ignore", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
