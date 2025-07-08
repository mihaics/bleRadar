package com.bleradar

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.bleradar.preferences.SettingsManager
import com.bleradar.service.AnalyticsCollectionService
import com.bleradar.service.BleRadarService
import com.bleradar.ui.theme.BleRadarTheme
import com.bleradar.ui.navigation.BleRadarNavigation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsManager: SettingsManager
    
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BleRadarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissions = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            add(Manifest.permission.BLUETOOTH_SCAN)
                            add(Manifest.permission.BLUETOOTH_ADVERTISE)
                            add(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            add(Manifest.permission.BLUETOOTH)
                            add(Manifest.permission.BLUETOOTH_ADMIN)
                        }
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    
                    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
                    
                    LaunchedEffect(permissionState.allPermissionsGranted) {
                        if (!permissionState.allPermissionsGranted) {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                    
                    // Log permission status for debugging
                    LaunchedEffect(permissionState.allPermissionsGranted) {
                        if (permissionState.allPermissionsGranted) {
                            android.util.Log.d("MainActivity", "All permissions granted")
                            
                            // Start services when permissions are granted
                            startServicesIfNeeded()
                        } else {
                            android.util.Log.w("MainActivity", "Some permissions not granted")
                        }
                    }
                    
                    BleRadarNavigation()
                }
            }
        }
    }
    
    private fun startServicesIfNeeded() {
        try {
            // Auto-start BLE radar service if it was enabled previously
            if (settingsManager.isServiceEnabled) {
                val intent = Intent(this, BleRadarService::class.java)
                startForegroundService(intent)
                android.util.Log.d("MainActivity", "Started BleRadarService")
            }
            
            // Start analytics collection service (foreground service)
            val analyticsIntent = Intent(this, AnalyticsCollectionService::class.java)
            startForegroundService(analyticsIntent)
            android.util.Log.d("MainActivity", "Started AnalyticsCollectionService")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start services", e)
        }
    }
}