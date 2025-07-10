package com.bleradar.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class for managing power optimization settings to ensure
 * the BLE radar service continues running in the background.
 */
class PowerManagementHelper(private val context: Context) {
    
    /**
     * Check if the app is whitelisted from battery optimization
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true // Pre-M devices don't have battery optimization
    }
    
    /**
     * Request to disable battery optimization for the app
     */
    fun requestBatteryOptimizationExemption(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            
            // Use activity result launcher for better handling
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { _ ->
                // Handle result if needed
            }
            
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                // Fallback to general battery optimization settings
                openBatteryOptimizationSettings(activity)
            }
        }
    }
    
    /**
     * Open battery optimization settings page
     */
    fun openBatteryOptimizationSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            activity.startActivity(intent)
        }
    }
    
    /**
     * Get device-specific power management instructions
     */
    fun getPowerManagementInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("samsung") -> {
                """
                Samsung Device Instructions:
                1. Go to Settings > Apps > BLE Guardian
                2. Tap "Battery" 
                3. Turn off "Optimize battery usage"
                4. Go to Settings > Device care > Battery > More battery settings
                5. Turn off "Adaptive battery" or add BLE Guardian to "Apps that won't be optimized"
                """.trimIndent()
            }
            manufacturer.contains("huawei") -> {
                """
                Huawei Device Instructions:
                1. Go to Settings > Apps > BLE Guardian
                2. Tap "Battery"
                3. Set "Launch" to "Manage manually"
                4. Enable "Auto-launch", "Secondary launch", and "Run in background"
                5. Go to Settings > Battery > More battery settings
                6. Turn off "Optimize battery usage" for BLE Guardian
                """.trimIndent()
            }
            manufacturer.contains("xiaomi") -> {
                """
                Xiaomi Device Instructions:
                1. Go to Settings > Apps > Manage apps > BLE Guardian
                2. Tap "Battery saver" and select "No restrictions"
                3. Enable "Autostart"
                4. Go to Settings > Battery & performance > Battery
                5. Turn off "Optimize battery usage" for BLE Guardian
                """.trimIndent()
            }
            manufacturer.contains("oppo") || manufacturer.contains("oneplus") -> {
                """
                OnePlus/OPPO Device Instructions:
                1. Go to Settings > Apps > BLE Guardian
                2. Tap "Battery" and select "Don't optimize"
                3. Go to Settings > Battery > Battery optimization
                4. Find BLE Guardian and select "Don't optimize"
                5. Enable "Allow background activity" in app settings
                """.trimIndent()
            }
            manufacturer.contains("vivo") -> {
                """
                Vivo Device Instructions:
                1. Go to Settings > Apps > BLE Guardian
                2. Tap "Battery" and turn off "Background app refresh"
                3. Go to Settings > Battery > High background power consumption
                4. Add BLE Guardian to the whitelist
                """.trimIndent()
            }
            else -> {
                """
                General Instructions:
                1. Go to Settings > Apps > BLE Guardian
                2. Look for "Battery" or "Power" settings
                3. Disable battery optimization/power saving for this app
                4. Enable "Allow background activity" if available
                5. Check your device's power management settings for app-specific controls
                """.trimIndent()
            }
        }
    }
    
    /**
     * Check if we're likely affected by aggressive power management
     */
    fun isAggressivePowerManagement(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("samsung") || 
               manufacturer.contains("huawei") || 
               manufacturer.contains("xiaomi") || 
               manufacturer.contains("oppo") || 
               manufacturer.contains("oneplus") || 
               manufacturer.contains("vivo")
    }
    
    /**
     * Get a user-friendly message about power management
     */
    fun getPowerManagementMessage(): String {
        return if (isBatteryOptimizationDisabled()) {
            "✅ Battery optimization is disabled. BLE Guardian should work properly in the background."
        } else {
            "⚠️ Battery optimization is enabled. This may prevent BLE Guardian from detecting devices when your phone is inactive. Please disable battery optimization for better tracking."
        }
    }
}