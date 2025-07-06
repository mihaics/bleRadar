package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bleradar.ui.viewmodel.DeviceListViewModel
import com.bleradar.data.database.BleDevice
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle(initialValue = false)
    
    var showLabelDialog by remember { mutableStateOf<BleDevice?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with scan status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isScanning) Color.Green.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isScanning) "Scanning..." else "Idle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Device list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                DeviceCard(
                    device = device,
                    onIgnore = { viewModel.ignoreDevice(device.deviceAddress) },
                    onLabel = { showLabelDialog = device },
                    onTrack = { viewModel.toggleTracking(device.deviceAddress) }
                )
            }
        }
    }
    
    // Label dialog
    showLabelDialog?.let { device ->
        LabelDialog(
            device = device,
            onDismiss = { showLabelDialog = null },
            onConfirm = { label ->
                viewModel.labelDevice(device.deviceAddress, label)
                showLabelDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: BleDevice,
    onIgnore: () -> Unit,
    onLabel: () -> Unit,
    onTrack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                device.followingScore > 0.7f -> Color.Red.copy(alpha = 0.1f)
                device.followingScore > 0.5f -> Color(0xFFFFA500).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
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
                        text = device.deviceName ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.deviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (device.label != null) {
                        Text(
                            text = "Label: ${device.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(device.lastSeen)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Following score indicator
            if (device.followingScore > 0.3f) {
                LinearProgressIndicator(
                    progress = device.followingScore,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        device.followingScore > 0.7f -> Color.Red
                        device.followingScore > 0.5f -> Color(0xFFFFA500)
                        else -> Color.Yellow
                    }
                )
                Text(
                    text = "Following score: ${(device.followingScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Ignore")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ignore")
                }
                
                OutlinedButton(
                    onClick = onLabel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Label")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Label")
                }
                
                Button(
                    onClick = onTrack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Track")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (device.isTracked) "Untrack" else "Track")
                }
            }
        }
    }
}

@Composable
fun LabelDialog(
    device: BleDevice,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var label by remember { mutableStateOf(device.label ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Label Device") },
        text = {
            Column {
                Text("Enter a label for ${device.deviceName ?: device.deviceAddress}:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g., My Phone, Colleague's Watch") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}