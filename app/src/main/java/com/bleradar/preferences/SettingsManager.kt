package com.bleradar.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ble_radar_settings"
        private const val KEY_SCAN_INTERVAL = "scan_interval_minutes"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        
        const val DEFAULT_SCAN_INTERVAL = 5 // 5 minutes
        const val DEFAULT_SERVICE_ENABLED = false
    }
    
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var scanIntervalMinutes: Int
        get() = preferences.getInt(KEY_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)
        set(value) = preferences.edit().putInt(KEY_SCAN_INTERVAL, value).apply()
    
    var isServiceEnabled: Boolean
        get() = preferences.getBoolean(KEY_SERVICE_ENABLED, DEFAULT_SERVICE_ENABLED)
        set(value) = preferences.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()
    
    var isFirstLaunch: Boolean
        get() = preferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = preferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    fun getScanIntervalMillis(): Long {
        return scanIntervalMinutes * 60 * 1000L
    }
}