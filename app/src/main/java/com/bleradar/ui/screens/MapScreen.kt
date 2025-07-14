package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bleradar.ui.viewmodel.MapViewModel
import com.bleradar.data.database.BleDetection
import com.bleradar.data.database.BleDevice
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    focusedDeviceAddress: String? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    val detections by viewModel.detections.collectAsStateWithLifecycle(initialValue = emptyList())
    val deviceLocations by viewModel.deviceLocations.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle(initialValue = null)
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Track if we've centered on user location yet  
    var hasUserLocationCentered by remember { mutableStateOf(false) }
    
    // Reset location centered flag when refresh is called
    LaunchedEffect(detections) {
        // Allow recentering when detections are refreshed
    }
    
    // Track if we've centered on focused device yet
    var hasFocusedDeviceCentered by remember { mutableStateOf(false) }
    
    // Filter state
    var showOnlyThreatDevices by remember { mutableStateOf(false) }
    var showOnlyRecentDevices by remember { mutableStateOf(false) }
    
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
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        
        val threatScore = device?.followingScore ?: 0f
        when {
            focusedDeviceAddress == deviceLocation.deviceAddress -> {
                // Focused device - bright and prominent
                marker.setAlpha(1.0f)
                marker.title = "🎯 $deviceName"
            }
            device?.isKnownTracker == true -> {
                // Known tracker - high visibility
                marker.setAlpha(0.95f)
                marker.title = "🚨 $deviceName"
            }
            threatScore > 0.7f -> {
                // High threat - red marker
                marker.setAlpha(0.9f)
                marker.title = "🔴 $deviceName"
            }
            threatScore > 0.5f -> {
                // Medium threat - orange marker
                marker.setAlpha(0.8f)
                marker.title = "🟠 $deviceName"
            }
            threatScore > 0.3f -> {
                // Low threat - yellow marker
                marker.setAlpha(0.7f)
                marker.title = "🟡 $deviceName"
            }
            else -> {
                // Normal device - blue marker
                marker.setAlpha(0.6f)
                marker.title = "🔵 $deviceName"
            }
        }
        
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
    
    // Function to recenter map on current location
    fun recenterOnLocation() {
        currentLocation?.let { location ->
            mapView?.let { map ->
                if (filteredDeviceLocations.isEmpty()) {
                    // Only current location, use standard zoom
                    map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                    map.controller.setZoom(15.0)
                } else {
                    // Has devices, fit all markers including current location
                    fitMarkersInView()
                }
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
            
            // Add device markers with clustering
            clusteredDevices.entries.forEach { (_, deviceGroup) ->
                if (deviceGroup.size == 1) {
                    // Single device - create normal marker
                    val deviceLocation = deviceGroup.first()
                    val marker = createDeviceMarker(map, deviceLocation, devices, focusedDeviceAddress, false)
                    map.overlays.add(marker)
                } else {
                    // Multiple devices in same area - create cluster marker
                    val centerLat = deviceGroup.map { it.latitude }.average()
                    val centerLon = deviceGroup.map { it.longitude }.average()
                    
                    val clusterMarker = Marker(map)
                    clusterMarker.position = GeoPoint(centerLat, centerLon)
                    clusterMarker.title = "Device Cluster (${deviceGroup.size})"
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
                    }
                    
                    // Set cluster appearance based on highest threat level in group
                    val maxThreatScore = deviceGroup.maxOfOrNull { deviceLocation ->
                        devices.find { it.deviceAddress == deviceLocation.deviceAddress }?.followingScore ?: 0f
                    } ?: 0f
                    
                    clusterMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    when {
                        maxThreatScore > 0.7f -> clusterMarker.setAlpha(0.9f) // High threat cluster
                        maxThreatScore > 0.5f -> clusterMarker.setAlpha(0.8f) // Medium threat cluster
                        else -> clusterMarker.setAlpha(0.7f) // Normal cluster
                    }
                    
                    map.overlays.add(clusterMarker)
                }
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
                                "Unique Devices: ${filteredDeviceLocations.size}"
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
            // Fit all markers button
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
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Fit all devices",
                        tint = if (filteredDeviceLocations.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            
            // Recenter on current location button
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
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Recenter on current location",
                        tint = if (currentLocation != null) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            
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
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Zoom out",
                        tint = MaterialTheme.colorScheme.primary
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