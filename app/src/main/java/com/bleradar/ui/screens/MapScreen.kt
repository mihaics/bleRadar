package com.bleradar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    viewModel: MapViewModel = hiltViewModel()
) {
    val detections by viewModel.detections.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle(initialValue = null)
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Update map when location or detections change
    LaunchedEffect(currentLocation, detections) {
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
                
                // Center map on current location
                map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
            }
            
            // Add device detection markers
            detections.forEach { detection ->
                val marker = Marker(map)
                marker.position = GeoPoint(detection.latitude, detection.longitude)
                marker.title = "BLE Device"
                marker.snippet = "RSSI: ${detection.rssi} dBm\nAddress: ${detection.deviceAddress}"
                
                // Set marker color based on RSSI
                when {
                    detection.rssi > -50 -> marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    detection.rssi > -70 -> marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    else -> marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                
                map.overlays.add(marker)
            }
            
            map.invalidate()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Devices: ${detections.size}",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { viewModel.refreshDetections() }
                ) {
                    Text("Refresh")
                }
            }
        }
        
        // OpenStreetMap
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    
                    // Set default location if no current location
                    if (currentLocation == null) {
                        controller.setCenter(GeoPoint(0.0, 0.0))
                    }
                    
                    mapView = this
                }
            }
        )
    }
}