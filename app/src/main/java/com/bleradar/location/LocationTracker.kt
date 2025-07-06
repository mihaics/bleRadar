package com.bleradar.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _currentLocation.value = location
            }
        }
    }
    
    private var isTracking = false

    fun startLocationTracking() {
        if (!hasLocationPermission() || isTracking) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(15000L)
            .setMaxUpdateDelayMillis(60000L)
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
    
    fun stopLocationTracking() {
        if (!isTracking) return
        
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        // Try to get last known location first
        try {
            val lastLocation = fusedLocationClient.lastLocation
            lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    _currentLocation.value = location
                }
            }
        } catch (e: SecurityException) {
            return null
        }
        
        // If no recent location, request current location
        return suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                .setWaitForAccurateLocation(false)
                .setMaxUpdateDelayMillis(10000L)
                .build()
            
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.lastLocation?.let { location ->
                                _currentLocation.value = location
                                continuation.resume(location)
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }
                    },
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}