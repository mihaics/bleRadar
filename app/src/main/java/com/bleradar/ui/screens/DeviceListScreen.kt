package com.bleradar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
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
import com.bleradar.ui.viewmodel.DeviceListViewModel
import com.bleradar.ui.model.DeviceDisplayModel
import com.bleradar.ui.model.DeviceDisplaySortOption
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onDeviceClick: (String) -> Unit = {},
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle(initialValue = false)
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showNewDevicesOnly by viewModel.showNewDevicesOnly.collectAsStateWithLifecycle()
    
    var showLabelDialog by remember { mutableStateOf<DeviceDisplayModel?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with scan status - clickable for emergency scan
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .let { 
                    if (!isScanning) {
                        it.clickable { viewModel.performEmergencyScan() }
                    } else {
                        it
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isScanning) Color.Green.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isScanning) "Scanning..." else "Idle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isScanning) {
                        Text(
                            text = "Tap for emergency scan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search and filter controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search toggle
            IconButton(
                onClick = { showSearchBar = !showSearchBar }
            ) {
                Icon(
                    if (showSearchBar) Icons.Default.Clear else Icons.Default.Search,
                    contentDescription = if (showSearchBar) "Hide Search" else "Show Search"
                )
            }
            
            // Sort button
            OutlinedButton(
                onClick = { showSortDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Sort",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Sort: ${sortOption.name.replace('_', ' ')}",
                    fontSize = 12.sp
                )
            }
            
            // New devices filter
            FilterChip(
                selected = showNewDevicesOnly,
                onClick = { viewModel.toggleNewDevicesOnly() },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "New devices",
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "New",
                            fontSize = 11.sp
                        )
                    }
                }
            )
        }
        
        // Search bar
        if (showSearchBar) {
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search devices...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Device list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                DeviceCard(
                    device = device,
                    isNew = viewModel.isDeviceNew(device),
                    onClick = { onDeviceClick(device.deviceUuid) },
                    onIgnore = { viewModel.ignoreDevice(device.deviceUuid) },
                    onLabel = { showLabelDialog = device },
                    onTrack = { viewModel.toggleTracking(device.deviceUuid) }
                )
            }
        }
    }
    
    // Sort dialog
    if (showSortDialog) {
        SortDialog(
            currentSort = sortOption,
            onSortSelected = { option ->
                viewModel.setSortOption(option)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
    
    // Label dialog
    showLabelDialog?.let { device ->
        LabelDialog(
            device = device,
            onDismiss = { showLabelDialog = null },
            onConfirm = { label ->
                viewModel.labelDevice(device.deviceUuid, label)
                showLabelDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: DeviceDisplayModel,
    isNew: Boolean = false,
    onClick: () -> Unit = {},
    onIgnore: () -> Unit,
    onLabel: () -> Unit,
    onTrack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isNew) {
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = RoundedCornerShape(50),
                                color = Color.Green
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "New device",
                                    modifier = Modifier.size(10.dp).padding(3.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
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
                    if (device.macAddressCount > 1) {
                        Text(
                            text = "${device.macAddressCount} MAC addresses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = device.getDisplayIcon(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = dateFormat.format(Date(device.lastSeen)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (isNew) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Threat level indicator
            if (device.suspiciousScore > 0.3f) {
                LinearProgressIndicator(
                    progress = device.suspiciousScore,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        device.suspiciousScore > 0.7f -> Color.Red
                        device.suspiciousScore > 0.5f -> Color(0xFFFFA500)
                        else -> Color.Yellow
                    }
                )
                Text(
                    text = "Threat level: ${device.getThreatLevel().name} (${(device.suspiciousScore * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Ignore",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Ignore",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                
                OutlinedButton(
                    onClick = onLabel,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Label",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Label",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                
                Button(
                    onClick = onTrack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Track",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (device.isTracked) "Untrack" else "Track",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun LabelDialog(
    device: DeviceDisplayModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var label by remember { mutableStateOf(device.notes ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Label Device") },
        text = {
            Column {
                Text("Enter a label for ${device.displayName}:")
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
                onClick = { onConfirm(label) },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SortDialog(
    currentSort: DeviceDisplaySortOption,
    onSortSelected: (DeviceDisplaySortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Devices") },
        text = {
            Column {
                DeviceDisplaySortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { onSortSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                DeviceDisplaySortOption.LAST_SEEN -> "Last Seen"
                                DeviceDisplaySortOption.FIRST_SEEN -> "First Seen"
                                DeviceDisplaySortOption.NAME -> "Device Name"
                                DeviceDisplaySortOption.THREAT_LEVEL -> "Threat Level"
                                DeviceDisplaySortOption.DEVICE_TYPE -> "Device Type"
                                DeviceDisplaySortOption.MAC_COUNT -> "MAC Count"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}