package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle(initialValue = null)
    val devices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Track if we've centered on user location yet
    var hasUserLocationCentered by remember { mutableStateOf(false) }
    
    // Track if we've centered on focused device yet
    var hasFocusedDeviceCentered by remember { mutableStateOf(false) }
    
    // Filter detections for focused device if specified
    val filteredDetections = remember(detections, focusedDeviceAddress) {
        if (focusedDeviceAddress != null) {
            detections.filter { it.deviceAddress == focusedDeviceAddress }
        } else {
            detections
        }
    }
    
    // Function to recenter map on current location
    fun recenterOnLocation() {
        currentLocation?.let { location ->
            mapView?.let { map ->
                map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                map.controller.setZoom(15.0)
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
    
    // Focus on device if specified
    LaunchedEffect(focusedDeviceAddress, filteredDetections) {
        if (focusedDeviceAddress != null && filteredDetections.isNotEmpty() && !hasFocusedDeviceCentered) {
            val latestDetection = filteredDetections.maxByOrNull { it.timestamp }
            latestDetection?.let { detection ->
                mapView?.let { map ->
                    map.controller.animateTo(GeoPoint(detection.latitude, detection.longitude))
                    map.controller.setZoom(16.0)
                    hasFocusedDeviceCentered = true
                }
            }
        }
    }
    
    // Update map when location or detections change
    LaunchedEffect(currentLocation, filteredDetections) {
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
            
            // Add device detection markers
            filteredDetections.forEach { detection ->
                val marker = Marker(map)
                marker.position = GeoPoint(detection.latitude, detection.longitude)
                
                // Find device name from devices list
                val deviceName = devices.find { it.deviceAddress == detection.deviceAddress }?.deviceName
                    ?: "Unknown Device"
                
                marker.title = deviceName
                marker.snippet = buildString {
                    append("Address: ${detection.deviceAddress}\n")
                    append("RSSI: ${detection.rssi} dBm\n")
                    append("Detected: ${java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(detection.timestamp))}")
                    
                    // Add device info if available
                    devices.find { it.deviceAddress == detection.deviceAddress }?.let { device ->
                        if (device.label != null) {
                            append("\nLabel: ${device.label}")
                        }
                        if (device.followingScore > 0.3f) {
                            append("\nThreat: ${(device.followingScore * 100).toInt()}%")
                        }
                    }
                }
                
                // Set marker appearance based on threat level and focused device
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                // Color marker based on threat level or if it's focused
                val device = devices.find { it.deviceAddress == detection.deviceAddress }
                if (focusedDeviceAddress == detection.deviceAddress) {
                    // Focused device - make it stand out
                    marker.setAlpha(1.0f)
                } else if (device != null && device.followingScore > 0.7f) {
                    // High threat - red marker
                    marker.setAlpha(0.8f)
                } else if (device != null && device.followingScore > 0.5f) {
                    // Medium threat - orange marker
                    marker.setAlpha(0.7f)
                } else {
                    // Normal device
                    marker.setAlpha(0.6f)
                }
                
                map.overlays.add(marker)
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
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (focusedDeviceAddress != null) {
                            "Focused Device Locations: ${filteredDetections.size}"
                        } else {
                            "All Devices: ${filteredDetections.size}"
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
        }
        
        // Map zoom and location controls (right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recenter button
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
                        Icons.Default.LocationOn,
                        contentDescription = "Recenter on location",
                        tint = if (currentLocation != null) 
                            MaterialTheme.colorScheme.primary 
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