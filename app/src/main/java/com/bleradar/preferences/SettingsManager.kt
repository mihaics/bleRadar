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
        private const val KEY_DATA_RETENTION_DAYS = "data_retention_days"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_DETECTION_RETENTION_DAYS = "detection_retention_days"
        private const val KEY_LOCATION_RETENTION_DAYS = "location_retention_days"
        
        const val DEFAULT_SCAN_INTERVAL = 5 // 5 minutes
        const val DEFAULT_SERVICE_ENABLED = false
        const val DEFAULT_DATA_RETENTION_DAYS = 30 // 30 days
        const val DEFAULT_AUTO_CLEANUP_ENABLED = true
        const val DEFAULT_DETECTION_RETENTION_DAYS = 90 // 90 days
        const val DEFAULT_LOCATION_RETENTION_DAYS = 30 // 30 days
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
    
    var dataRetentionDays: Int
        get() = preferences.getInt(KEY_DATA_RETENTION_DAYS, DEFAULT_DATA_RETENTION_DAYS)
        set(value) = preferences.edit().putInt(KEY_DATA_RETENTION_DAYS, value).apply()
    
    var autoCleanupEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_CLEANUP_ENABLED, DEFAULT_AUTO_CLEANUP_ENABLED)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_CLEANUP_ENABLED, value).apply()
    
    var detectionRetentionDays: Int
        get() = preferences.getInt(KEY_DETECTION_RETENTION_DAYS, DEFAULT_DETECTION_RETENTION_DAYS)
        set(value) = preferences.edit().putInt(KEY_DETECTION_RETENTION_DAYS, value).apply()
    
    var locationRetentionDays: Int
        get() = preferences.getInt(KEY_LOCATION_RETENTION_DAYS, DEFAULT_LOCATION_RETENTION_DAYS)
        set(value) = preferences.edit().putInt(KEY_LOCATION_RETENTION_DAYS, value).apply()
    
    fun getScanIntervalMillis(): Long {
        return scanIntervalMinutes * 60 * 1000L
    }
    
    fun getDataRetentionMillis(): Long {
        return dataRetentionDays * 24 * 60 * 60 * 1000L
    }
    
    fun getDetectionRetentionMillis(): Long {
        return detectionRetentionDays * 24 * 60 * 60 * 1000L
    }
    
    fun getLocationRetentionMillis(): Long {
        return locationRetentionDays * 24 * 60 * 60 * 1000L
    }
    
    fun getRetentionOptions(): List<Int> {
        return listOf(7, 30, 90, 180, 365) // 1 week, 1 month, 3 months, 6 months, 1 year
    }
    
    fun getRetentionDisplayName(days: Int): String {
        return when (days) {
            7 -> "1 Week"
            30 -> "1 Month"
            90 -> "3 Months"
            180 -> "6 Months"
            365 -> "1 Year"
            else -> "$days Days"
        }
    }
}