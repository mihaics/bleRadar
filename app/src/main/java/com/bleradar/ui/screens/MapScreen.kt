package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bleradar.ui.viewmodel.MapViewModel
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleDevice
import com.bleradar.preferences.SettingsManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun MapLegend(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Map Legend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close legend",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend items
            LegendSection(
                title = "Device History",
                items = listOf(
                    LegendItem("🎯", "Focused device (latest)", Color(0xFF4CAF50)), // Green
                    LegendItem("📍", "Recent location (< 6h)", Color(0xFF2196F3)), // Blue
                    LegendItem("📌", "Older location (< 24h)", Color(0xFF9C27B0)), // Purple
                    LegendItem("🔘", "Historical location (> 24h)", Color(0xFF757575)) // Gray
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LegendSection(
                title = "Threat Levels",
                items = listOf(
                    LegendItem("🚨", "Known tracker", Color(0xFFF44336)), // Bright red
                    LegendItem("🔴", "High threat (>70%)", Color(0xFFE53935)), // Red
                    LegendItem("🟠", "Medium threat (50-70%)", Color(0xFFFF9800)), // Orange
                    LegendItem("🟡", "Low threat (30-50%)", Color(0xFFFFEB3B)), // Yellow
                    LegendItem("🔵", "Normal device", Color(0xFF2196F3)) // Blue
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LegendSection(
                title = "Cluster Markers",
                items = listOf(
                    LegendItem("⚪", "Normal cluster", Color(0xFF2196F3)), // Blue
                    LegendItem("🟠", "Medium threat cluster", Color(0xFFFF9800)), // Orange
                    LegendItem("🔴", "High threat cluster", Color(0xFFE53935)) // Red
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer note
            Text(
                text = "Tap markers for details • Long press clusters to expand",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LegendSection(
    title: String,
    items: List<LegendItem>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            LegendItemRow(item)
        }
    }
}

@Composable
fun LegendItemRow(item: LegendItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.symbol,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = item.color
        )
    }
}

data class LegendItem(
    val symbol: String,
    val description: String,
    val color: Color
)

data class InfoWindowData(
    val device: BleDevice?,
    val deviceLocation: MapViewModel.DeviceLocation,
    val position: GeoPoint,
    val isCluster: Boolean = false,
    val clusterDevices: List<MapViewModel.DeviceLocation> = emptyList()
)

@Composable
fun DeviceInfoWindow(
    infoWindowData: InfoWindowData,
    onDismiss: () -> Unit,
    onTrackDevice: (String) -> Unit,
    onIgnoreDevice: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    onExpandCluster: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .width(300.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .zIndex(1000f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (infoWindowData.isCluster) {
                    Text(
                        text = "Device Cluster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = infoWindowData.device?.deviceName ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (infoWindowData.isCluster) {
                // Cluster info
                ClusterInfoContent(
                    clusterDevices = infoWindowData.clusterDevices,
                    onExpandCluster = onExpandCluster
                )
            } else {
                // Single device info
                DeviceInfoContent(
                    device = infoWindowData.device,
                    deviceLocation = infoWindowData.deviceLocation,
                    onTrackDevice = onTrackDevice,
                    onIgnoreDevice = onIgnoreDevice,
                    onShowDetails = onShowDetails
                )
            }
        }
    }
}

@Composable
fun ClusterInfoContent(
    clusterDevices: List<MapViewModel.DeviceLocation>,
    onExpandCluster: (() -> Unit)?
) {
    Column {
        Text(
            text = "${clusterDevices.size} devices in this area",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show first few devices
        clusterDevices.take(3).forEach { deviceLocation ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• ${deviceLocation.deviceAddress.takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${deviceLocation.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (clusterDevices.size > 3) {
            Text(
                text = "... and ${clusterDevices.size - 3} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Expand cluster button
        onExpandCluster?.let { expandAction ->
            Button(
                onClick = expandAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "👁",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Expand Cluster")
            }
        }
    }
}

@Composable
fun DeviceInfoContent(
    device: BleDevice?,
    deviceLocation: MapViewModel.DeviceLocation,
    onTrackDevice: (String) -> Unit,
    onIgnoreDevice: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Column {
        // Device address
        Text(
            text = deviceLocation.deviceAddress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Device details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DetailItem("Signal", "${deviceLocation.rssi} dBm")
                DetailItem("Last Seen", SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(deviceLocation.timestamp)))
                
                if (deviceLocation.detectionCount > 1) {
                    DetailItem("Detections", "${deviceLocation.detectionCount}")
                }
                
                device?.let { dev ->
                    if (dev.label != null) {
                        DetailItem("Label", dev.label)
                    }
                    if (dev.followingScore > 0.3f) {
                        DetailItem("Threat Level", "${(dev.followingScore * 100).toInt()}%")
                    }
                }
            }
            
            // Threat level indicator
            device?.let { dev ->
                ThreatLevelIndicator(
                    threatScore = dev.followingScore,
                    isKnownTracker = dev.isKnownTracker
                )
            }
        }
        
        device?.let { dev ->
            if (dev.isKnownTracker) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Known Tracker${dev.trackerType?.let { " ($it)" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Track button
            OutlinedButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTrackDevice(deviceLocation.deviceAddress)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (device?.isTracked == true) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        Color.Transparent
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (device?.isTracked == true) "👁" else "👁",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (device?.isTracked == true) "Untrack" else "Track",
                        fontSize = 14.sp
                    )
                }
            }
            
            // Ignore button
            OutlinedButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIgnoreDevice(deviceLocation.deviceAddress)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (device?.isIgnored == true) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        Color.Transparent
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚫",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (device?.isIgnored == true) "Unignore" else "Ignore",
                        fontSize = 14.sp
                    )
                }
            }
            
            // Details button
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowDetails(deviceLocation.deviceAddress)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Details",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ThreatLevelIndicator(
    threatScore: Float,
    isKnownTracker: Boolean
) {
    val (color, icon) = when {
        isKnownTracker -> Pair(MaterialTheme.colorScheme.error, "🚨")
        threatScore > 0.7f -> Pair(MaterialTheme.colorScheme.error, "🔴")
        threatScore > 0.5f -> Pair(Color(0xFFFF9800), "🟠")
        threatScore > 0.3f -> Pair(Color(0xFFFFEB3B), "🟡")
        else -> Pair(MaterialTheme.colorScheme.primary, "🔵")
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(24.dp)
            )
            .border(
                2.dp,
                color.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}


@Composable
fun MapScreen(
    focusedDeviceAddress: String? = null,
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateToDeviceDetail: ((String) -> Unit)? = null
) {
    val detections by viewModel.detections.collectAsStateWithLifecycle(initialValue = emptyList())
    val deviceLocations by viewModel.deviceLocations.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle(initialValue = null)
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Get SettingsManager instance
    val settingsManager = remember { SettingsManager(context) }
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Track if we've centered on user location yet  
    var hasUserLocationCentered by remember { mutableStateOf(false) }
    
    // Reset location centered flag when refresh is called
    LaunchedEffect(detections) {
        // Allow recentering when detections are refreshed
    }
    
    // Track if we've centered on focused device yet
    var hasFocusedDeviceCentered by remember { mutableStateOf(false) }
    
    // Cluster expansion state
    var expandedClusterKey by remember { mutableStateOf<String?>(null) }
    var expandedClusterDevices by remember { mutableStateOf<List<MapViewModel.DeviceLocation>>(emptyList()) }
    var clusterExpansionAnimating by remember { mutableStateOf(false) }
    
    // Filter state
    var showOnlyThreatDevices by remember { mutableStateOf(false) }
    var showOnlyRecentDevices by remember { mutableStateOf(false) }
    
    // Legend state - persistent across app sessions
    var showLegend by remember { mutableStateOf(settingsManager.showMapLegend) }
    
    // Tutorial state for first-time users
    var showTutorialTooltip by remember { mutableStateOf(false) }
    
    // Info window state
    var infoWindowData by remember { mutableStateOf<InfoWindowData?>(null) }
    
    // Show legend automatically on first launch or when focused device is specified
    LaunchedEffect(focusedDeviceAddress) {
        if (focusedDeviceAddress != null && !showLegend) {
            showLegend = true
        }
    }
    
    // Show tutorial tooltip for first-time users after a delay
    LaunchedEffect(Unit) {
        if (settingsManager.isFirstLaunch && deviceLocations.isNotEmpty()) {
            delay(2000) // Wait 2 seconds after map loads
            showTutorialTooltip = true
            delay(5000) // Show for 5 seconds
            showTutorialTooltip = false
            if (!showLegend) {
                showLegend = true // Automatically show legend for first-time users
            }
        }
    }
    
    // Update settings when legend state changes
    LaunchedEffect(showLegend) {
        settingsManager.showMapLegend = showLegend
    }
    
    // Helper function to zoom to cluster devices
    fun zoomToCluster(deviceGroup: List<MapViewModel.DeviceLocation>) {
        mapView?.let { map ->
            if (deviceGroup.isNotEmpty()) {
                val latitudes = deviceGroup.map { it.latitude }
                val longitudes = deviceGroup.map { it.longitude }
                
                val minLat = latitudes.minOrNull() ?: return
                val maxLat = latitudes.maxOrNull() ?: return
                val minLon = longitudes.minOrNull() ?: return
                val maxLon = longitudes.maxOrNull() ?: return
                
                // Calculate center point
                val centerLat = (minLat + maxLat) / 2
                val centerLon = (minLon + maxLon) / 2
                
                // Calculate appropriate zoom level based on the spread of devices
                val latSpan = maxLat - minLat
                val lonSpan = maxLon - minLon
                val maxSpan = maxOf(latSpan, lonSpan)
                
                // Set zoom level based on span (with some padding)
                val zoomLevel = when {
                    maxSpan > 0.1 -> 12.0  // Wide spread
                    maxSpan > 0.05 -> 14.0 // Medium spread
                    maxSpan > 0.01 -> 16.0 // Close spread
                    else -> 18.0           // Very close devices
                }
                
                // Animate to the cluster location
                map.controller.animateTo(GeoPoint(centerLat, centerLon))
                map.controller.setZoom(zoomLevel)
            }
        }
    }
    
    // Helper function to expand cluster
    fun expandCluster(clusterKey: String, deviceGroup: List<MapViewModel.DeviceLocation>) {
        if (expandedClusterKey == clusterKey) {
            // Collapse if already expanded
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            expandedClusterKey = null
            expandedClusterDevices = emptyList()
        } else {
            // Expand cluster
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            clusterExpansionAnimating = true
            expandedClusterKey = clusterKey
            expandedClusterDevices = deviceGroup
            
            // Zoom to fit all devices in the expanded cluster
            zoomToCluster(deviceGroup)
            
            // Reset animation flag after animation completes
            CoroutineScope(Dispatchers.Main).launch {
                delay(300)
                clusterExpansionAnimating = false
            }
        }
    }
    
    // Device action handlers
    fun handleTrackDevice(deviceAddress: String) {
        viewModel.toggleDeviceTracking(deviceAddress)
        // Refresh info window data
        infoWindowData?.let { currentData ->
            if (currentData.deviceLocation.deviceAddress == deviceAddress) {
                val updatedDevice = devices.find { it.deviceAddress == deviceAddress }
                infoWindowData = currentData.copy(device = updatedDevice)
            }
        }
    }
    
    fun handleIgnoreDevice(deviceAddress: String) {
        viewModel.toggleDeviceIgnore(deviceAddress)
        // Refresh info window data
        infoWindowData?.let { currentData ->
            if (currentData.deviceLocation.deviceAddress == deviceAddress) {
                val updatedDevice = devices.find { it.deviceAddress == deviceAddress }
                infoWindowData = currentData.copy(device = updatedDevice)
            }
        }
    }
    
    fun handleShowDetails(deviceAddress: String) {
        onNavigateToDeviceDetail?.invoke(deviceAddress)
        infoWindowData = null // Close info window
    }
    
    fun showDeviceInfoWindow(deviceLocation: MapViewModel.DeviceLocation, position: GeoPoint) {
        val device = devices.find { it.deviceAddress == deviceLocation.deviceAddress }
        infoWindowData = InfoWindowData(
            device = device,
            deviceLocation = deviceLocation,
            position = position,
            isCluster = false
        )
    }
    
    fun showClusterInfoWindow(clusterKey: String, deviceGroup: List<MapViewModel.DeviceLocation>, position: GeoPoint) {
        infoWindowData = InfoWindowData(
            device = null,
            deviceLocation = deviceGroup.first(), // Use first device as representative
            position = position,
            isCluster = true,
            clusterDevices = deviceGroup
        )
    }
    
    // Helper function to create cluster marker with tap handling
    fun createClusterMarker(
        map: MapView,
        clusterKey: String,
        deviceGroup: List<MapViewModel.DeviceLocation>,
        devices: List<BleDevice>,
        isExpanded: Boolean
    ): Marker {
        val centerLat = deviceGroup.map { it.latitude }.average()
        val centerLon = deviceGroup.map { it.longitude }.average()
        
        val clusterMarker = Marker(map)
        clusterMarker.position = GeoPoint(centerLat, centerLon)
        
        // Title will be set below based on threat level
        
        // Set cluster snippet with device information and interaction hint
        clusterMarker.snippet = buildString {
            append("${deviceGroup.size} devices in this area:\n")
            deviceGroup.take(3).forEach { deviceLocation ->
                val deviceName = devices.find { it.deviceAddress == deviceLocation.deviceAddress }?.deviceName
                    ?: "Unknown Device"
                append("• $deviceName (${deviceLocation.rssi} dBm)\n")
            }
            if (deviceGroup.size > 3) {
                append("• ... and ${deviceGroup.size - 3} more")
            }
            if (isExpanded && deviceGroup.size > 20) {
                append("\nShowing first 20 devices for performance")
            }
            append("\nTap to ${if (isExpanded) "collapse" else "expand and zoom"}")
        }
        
        // Set cluster appearance based on highest threat level and expansion state
        val maxThreatScore = deviceGroup.maxOfOrNull { deviceLocation ->
            devices.find { it.deviceAddress == deviceLocation.deviceAddress }?.followingScore ?: 0f
        } ?: 0f
        
        // Set cluster appearance based on threat level
        clusterMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        // Set cluster title with emoji based on threat level
        val threatEmoji = when {
            maxThreatScore > 0.7f -> "🔴" // Red for high threat
            maxThreatScore > 0.5f -> "🟠" // Orange for medium threat
            else -> "⚪" // White for normal
        }
        
        clusterMarker.title = if (isExpanded) {
            "$threatEmoji Cluster (${deviceGroup.size}) - Tap to collapse"
        } else {
            "$threatEmoji Cluster (${deviceGroup.size}) - Tap to expand"
        }
        
        // Add tap listener for cluster expansion and info window
        clusterMarker.setOnMarkerClickListener { marker, mapView ->
            showClusterInfoWindow(clusterKey, deviceGroup, marker.position)
            true // Consume the event
        }
        
        return clusterMarker
    }
    
    // Helper function to create individual device markers
    fun createDeviceMarker(
        map: MapView,
        deviceLocation: MapViewModel.DeviceLocation,
        devices: List<BleDevice>,
        focusedDeviceAddress: String?,
        @Suppress("UNUSED_PARAMETER") isInCluster: Boolean
    ): Marker {
        val marker = Marker(map)
        marker.position = GeoPoint(deviceLocation.latitude, deviceLocation.longitude)
        
        // Find device info
        val device = devices.find { it.deviceAddress == deviceLocation.deviceAddress }
        val deviceName = device?.deviceName ?: "Unknown Device"
        
        marker.title = deviceName
        marker.snippet = buildString {
            append("Address: ${deviceLocation.deviceAddress}\n")
            append("RSSI: ${deviceLocation.rssi} dBm\n")
            append("Last seen: ${java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(deviceLocation.timestamp))}")
            if (deviceLocation.detectionCount > 1) {
                append("\nDetections: ${deviceLocation.detectionCount}")
            }
            
            // Add device info if available
            device?.let { dev ->
                if (dev.label != null) {
                    append("\nLabel: ${dev.label}")
                }
                if (dev.followingScore > 0.3f) {
                    append("\nThreat: ${(dev.followingScore * 100).toInt()}%")
                }
                if (dev.isKnownTracker) {
                    append("\n⚠️ Known Tracker")
                }
                if (dev.trackerType != null) {
                    append("\nType: ${dev.trackerType}")
                }
            }
        }
        
        // Enhanced marker appearance based on device characteristics
        val threatScore = device?.followingScore ?: 0f
        
        // Set marker title with emoji based on threat level for visual distinction
        val markerTitle = when {
            focusedDeviceAddress == deviceLocation.deviceAddress -> {
                // Focused device - show historical trail with different symbols
                val currentTime = System.currentTimeMillis()
                val ageInHours = (currentTime - deviceLocation.timestamp) / (1000 * 60 * 60)
                
                when {
                    ageInHours < 1 -> "🎯 $deviceName"
                    ageInHours < 6 -> "📍 $deviceName"
                    ageInHours < 24 -> "📌 $deviceName"
                    else -> "🔘 $deviceName"
                }
            }
            device?.isKnownTracker == true -> {
                // Known tracker - bright red with warning
                "🚨 $deviceName"
            }
            threatScore > 0.7f -> {
                // High threat - red marker
                "🔴 $deviceName"
            }
            threatScore > 0.5f -> {
                // Medium threat - orange marker
                "🟠 $deviceName"
            }
            threatScore > 0.3f -> {
                // Low threat - yellow marker
                "🟡 $deviceName"
            }
            else -> {
                // Normal device - blue marker
                "🔵 $deviceName"
            }
        }
        
        // Set marker appearance and click behavior
        marker.title = markerTitle
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        
        // Add click listener for device markers
        marker.setOnMarkerClickListener { clickedMarker, mapView ->
            showDeviceInfoWindow(deviceLocation, clickedMarker.position)
            true // Consume the event
        }
        
        return marker
    }
    
    // Helper function to create expanded device markers with animation offset
    fun createExpandedDeviceMarker(
        map: MapView,
        deviceLocation: MapViewModel.DeviceLocation,
        devices: List<BleDevice>,
        focusedDeviceAddress: String?,
        index: Int,
        totalDevices: Int
    ): Marker {
        val marker = createDeviceMarker(map, deviceLocation, devices, focusedDeviceAddress, true)
        
        // Calculate animation offset for expansion effect
        val angle = (2 * Math.PI * index) / totalDevices
        val radius = 0.0005 // Small radius for expansion effect
        val offsetLat = radius * Math.cos(angle)
        val offsetLon = radius * Math.sin(angle)
        
        val originalPos = marker.position
        marker.position = GeoPoint(
            originalPos.latitude + offsetLat,
            originalPos.longitude + offsetLon
        )
        
        // Enhanced marker appearance for expanded state
        marker.setAlpha(0.95f)
        marker.title = "🔍 ${marker.title}" // Add search icon to indicate expanded state
        
        return marker
    }
    
    // Filter device locations for focused device if specified
    val filteredDeviceLocations = remember(deviceLocations, focusedDeviceAddress, devices, showOnlyThreatDevices, showOnlyRecentDevices) {
        var filtered = if (focusedDeviceAddress != null) {
            deviceLocations.filter { it.deviceAddress == focusedDeviceAddress }
        } else {
            deviceLocations
        }
        
        // Apply threat filter
        if (showOnlyThreatDevices) {
            val threatDeviceAddresses = devices.filter { it.followingScore > 0.5f }.map { it.deviceAddress }
            filtered = filtered.filter { it.deviceAddress in threatDeviceAddresses }
        }
        
        // Apply recent filter (last 2 hours)
        if (showOnlyRecentDevices) {
            val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
            filtered = filtered.filter { it.timestamp > twoHoursAgo }
        }
        
        filtered
    }
    
    // Function to calculate optimal bounds for all visible markers
    fun calculateOptimalBounds(): Pair<GeoPoint, Double>? {
        val allPoints = mutableListOf<GeoPoint>()
        
        // Add current location if available
        currentLocation?.let { location ->
            allPoints.add(GeoPoint(location.latitude, location.longitude))
        }
        
        // Add all filtered device locations
        filteredDeviceLocations.forEach { deviceLocation ->
            allPoints.add(GeoPoint(deviceLocation.latitude, deviceLocation.longitude))
        }
        
        if (allPoints.isEmpty()) return null
        if (allPoints.size == 1) return Pair(allPoints[0], 15.0)
        
        // Calculate bounds
        val minLat = allPoints.minOf { it.latitude }
        val maxLat = allPoints.maxOf { it.latitude }
        val minLon = allPoints.minOf { it.longitude }
        val maxLon = allPoints.maxOf { it.longitude }
        
        // Calculate center point
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2
        val center = GeoPoint(centerLat, centerLon)
        
        // Calculate appropriate zoom level based on bounds with padding
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)
        
        // Add padding factor (20%) to ensure markers aren't at edge
        val paddedDiff = maxDiff * 1.2
        
        val zoom = when {
            paddedDiff > 10.0 -> 4.0   // Country/continent level
            paddedDiff > 5.0 -> 6.0    // Large region
            paddedDiff > 1.0 -> 8.0    // Region
            paddedDiff > 0.5 -> 10.0   // City area
            paddedDiff > 0.1 -> 12.0   // District
            paddedDiff > 0.01 -> 14.0  // Neighborhood
            paddedDiff > 0.001 -> 16.0 // Street level
            paddedDiff > 0.0001 -> 17.0 // Very close
            else -> 18.0            // Building level (max zoom)
        }.coerceIn(4.0, 19.0) // Ensure zoom stays within OSM limits
        
        return Pair(center, zoom)
    }
    
    // Function to fit all visible markers in view
    fun fitMarkersInView() {
        mapView?.let { map ->
            calculateOptimalBounds()?.let { (center, zoom) ->
                map.controller.animateTo(center)
                map.controller.setZoom(zoom)
            }
        }
    }
    
    // Function to recenter map on current location only
    fun recenterOnLocation() {
        currentLocation?.let { location ->
            mapView?.let { map ->
                // Always center on user location with fixed zoom, regardless of devices
                map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                map.controller.setZoom(16.0) // Fixed zoom level for user location
                // Reset flag to allow automatic centering on next location update
                hasUserLocationCentered = false
            }
        }
    }
    
    // Function to zoom in/out
    fun zoomIn() {
        mapView?.let { map ->
            map.controller.setZoom(map.zoomLevelDouble + 1.0)
        }
    }
    
    fun zoomOut() {
        mapView?.let { map ->
            map.controller.setZoom(map.zoomLevelDouble - 1.0)
        }
    }
    
    // Auto-fit view when filters change
    LaunchedEffect(filteredDeviceLocations, showOnlyThreatDevices, showOnlyRecentDevices) {
        // Only auto-fit when not focusing on a specific device
        if (focusedDeviceAddress == null && filteredDeviceLocations.isNotEmpty()) {
            kotlinx.coroutines.delay(300) // Small delay to allow UI to update
            fitMarkersInView()
        }
    }
    
    // Load device location history when focused device is specified
    LaunchedEffect(focusedDeviceAddress) {
        if (focusedDeviceAddress != null) {
            viewModel.loadDeviceLocationHistory(focusedDeviceAddress, days = 7)
        }
    }
    
    // Focus on device if specified
    LaunchedEffect(focusedDeviceAddress, filteredDeviceLocations) {
        if (focusedDeviceAddress != null && filteredDeviceLocations.isNotEmpty() && !hasFocusedDeviceCentered) {
            kotlinx.coroutines.delay(300) // Small delay to allow markers to update
            fitMarkersInView() // Fit all locations for focused device
            hasFocusedDeviceCentered = true
        }
    }
    
    // Update map when location or device locations change
    LaunchedEffect(currentLocation, filteredDeviceLocations) {
        mapView?.let { map ->
            // Clear existing markers
            map.overlays.clear()
            
            // Add current location marker
            currentLocation?.let { location ->
                val marker = Marker(map)
                marker.position = GeoPoint(location.latitude, location.longitude)
                marker.title = "Your Location"
                marker.snippet = "Current position"
                map.overlays.add(marker)
                
                // Center map on current location (only once when first obtained)
                if (!hasUserLocationCentered) {
                    map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                    map.controller.setZoom(15.0) // Zoom in for user location
                    hasUserLocationCentered = true
                }
            }
            
            // Group device locations by coordinates for clustering (devices within 50m)
            val clusteredDevices = mutableMapOf<String, MutableList<MapViewModel.DeviceLocation>>()
            filteredDeviceLocations.forEach { deviceLocation ->
                val locationKey = "${(deviceLocation.latitude * 1000).toInt()}_${(deviceLocation.longitude * 1000).toInt()}"
                clusteredDevices.getOrPut(locationKey) { mutableListOf() }.add(deviceLocation)
            }
            
            // Add device markers with expandable clustering
            clusteredDevices.entries.forEach { (locationKey, deviceGroup) ->
                if (deviceGroup.size == 1) {
                    // Single device - create normal marker
                    val deviceLocation = deviceGroup.first()
                    val marker = createDeviceMarker(map, deviceLocation, devices, focusedDeviceAddress, false)
                    map.overlays.add(marker)
                } else {
                    // Multiple devices in same area - create expandable cluster
                    val isExpanded = expandedClusterKey == locationKey
                    
                    if (isExpanded) {
                        // Show expanded individual markers (limit to 20 for performance)
                        val maxMarkersToShow = 20
                        val markersToShow = deviceGroup.take(maxMarkersToShow)
                        
                        markersToShow.forEachIndexed { index, deviceLocation ->
                            val marker = createExpandedDeviceMarker(
                                map, deviceLocation, devices, focusedDeviceAddress, 
                                index, markersToShow.size
                            )
                            map.overlays.add(marker)
                        }
                        
                        // Also add the cluster marker in expanded state
                        val clusterMarker = createClusterMarker(
                            map, locationKey, deviceGroup, devices, isExpanded
                        )
                        map.overlays.add(clusterMarker)
                    } else {
                        // Show collapsed cluster marker
                        val clusterMarker = createClusterMarker(
                            map, locationKey, deviceGroup, devices, isExpanded
                        )
                        map.overlays.add(clusterMarker)
                    }
                }
            }
            
            // Add map tap listener to collapse clusters and close info windows when tapping outside
            map.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    // Check if tap was not on a marker by adding a small delay
                    // If no marker was clicked, collapse any expanded clusters and close info windows
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(100) // Small delay to allow marker click to process
                        if (expandedClusterKey != null) {
                            expandedClusterKey = null
                            expandedClusterDevices = emptyList()
                        }
                        infoWindowData = null // Close info window
                    }
                }
                false // Don't consume the event
            }
            
            
            map.invalidate()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // OpenStreetMap (background)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    // Set initial zoom level suitable for Europe view
                    controller.setZoom(4.0)
                    
                    // Set default location over Europe if no current location
                    controller.setCenter(GeoPoint(50.0, 10.0))
                    
                    mapView = this
                }
            }
        )
        
        // Map controls overlay (top)
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (focusedDeviceAddress != null) {
                                "Focused Device: ${filteredDeviceLocations.size} locations"
                            } else {
                                "Devices: ${filteredDeviceLocations.size} (Total: ${deviceLocations.size})"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        if (focusedDeviceAddress != null) {
                            val deviceName = devices.find { it.deviceAddress == focusedDeviceAddress }?.deviceName
                            Text(
                                text = deviceName ?: focusedDeviceAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        } else {
                            // Debug information
                            Text(
                                text = "All devices: ${devices.size} | Filters: ${if (showOnlyThreatDevices) "Threats" else "All"} ${if (showOnlyRecentDevices) "Recent" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refreshDetections() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Filter controls
                if (focusedDeviceAddress == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showOnlyThreatDevices = !showOnlyThreatDevices },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showOnlyThreatDevices) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Threats Only", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showOnlyRecentDevices = !showOnlyRecentDevices },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showOnlyRecentDevices) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Recent (2h)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        // Map zoom and location controls (right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Location controls group
            
            // Fit all devices in view button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                IconButton(
                    onClick = { fitMarkersInView() },
                    enabled = filteredDeviceLocations.isNotEmpty(),
                    modifier = Modifier.size(48.dp)
                ) {
                    // Use a text-based icon for "fit all devices"
                    Text(
                        text = "⛶",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (filteredDeviceLocations.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            
            // Center on my location button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                IconButton(
                    onClick = { recenterOnLocation() },
                    enabled = currentLocation != null,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Use a text-based icon for "center on location"
                    Text(
                        text = "🎯",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (currentLocation != null) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            
            // Spacer to separate location controls from zoom controls
            Spacer(modifier = Modifier.height(8.dp))
            
            // Legend toggle button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (showLegend) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        Color.White.copy(alpha = 0.9f)
                )
            ) {
                IconButton(
                    onClick = { showLegend = !showLegend },
                    modifier = Modifier.size(48.dp)
                ) {
                    // Use a text-based icon for legend toggle
                    Text(
                        text = "❓",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (showLegend) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Spacer to separate legend from zoom controls
            Spacer(modifier = Modifier.height(8.dp))
            
            // Zoom controls group
            
            // Zoom in button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                IconButton(
                    onClick = { zoomIn() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Zoom out button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                IconButton(
                    onClick = { zoomOut() },
                    modifier = Modifier.size(48.dp)
                ) {
                    // Use a Text-based minus sign for zoom out
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
        }
        
        // Empty state message when no devices match filters
        if (filteredDeviceLocations.isEmpty() && (showOnlyThreatDevices || showOnlyRecentDevices)) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Black
                    )
                    Text(
                        text = if (showOnlyThreatDevices && showOnlyRecentDevices) {
                            "No recent threat devices detected"
                        } else if (showOnlyThreatDevices) {
                            "No threat devices detected"
                        } else {
                            "No recent devices detected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Try adjusting your filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Tutorial tooltip overlay
        AnimatedVisibility(
            visible = showTutorialTooltip,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = EaseOutCubic)
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300, easing = EaseInCubic)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Tap the ❓ button to see what each colored marker means!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Legend overlay (bottom-right)
        AnimatedVisibility(
            visible = showLegend,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (currentLocation != null) 100.dp else 16.dp)
        ) {
            MapLegend(
                onDismiss = { showLegend = false }
            )
        }
        
        // Info window overlay
        infoWindowData?.let { data ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1000f)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(200, easing = EaseOutCubic)
                    ) + fadeIn(
                        animationSpec = tween(200)
                    ),
                    exit = scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(200, easing = EaseInCubic)
                    ) + fadeOut(
                        animationSpec = tween(200)
                    )
                ) {
                    DeviceInfoWindow(
                        infoWindowData = data,
                        onDismiss = { infoWindowData = null },
                        onTrackDevice = { deviceAddress ->
                            handleTrackDevice(deviceAddress)
                        },
                        onIgnoreDevice = { deviceAddress ->
                            handleIgnoreDevice(deviceAddress)
                        },
                        onShowDetails = { deviceAddress ->
                            handleShowDetails(deviceAddress)
                        },
                        onExpandCluster = if (data.isCluster) {
                            {
                                expandCluster(
                                    "${(data.position.latitude * 1000).toInt()}_${(data.position.longitude * 1000).toInt()}",
                                    data.clusterDevices
                                )
                                infoWindowData = null
                            }
                        } else null
                    )
                }
            }
        }
        
        // Location info overlay (bottom)
        currentLocation?.let { location ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Current Location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Text(
                            text = "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "±${String.format("%.0f", location.accuracy)}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}