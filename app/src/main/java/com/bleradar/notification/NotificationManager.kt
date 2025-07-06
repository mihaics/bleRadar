package com.bleradar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bleradar.MainActivity
import com.bleradar.R
import com.bleradar.data.database.BleDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_ALERTS = "BLE_RADAR_ALERTS"
        const val CHANNEL_ID_TRACKING = "BLE_RADAR_TRACKING"
        const val NOTIFICATION_ID_FOLLOWING = 100
        const val NOTIFICATION_ID_TRACKED_DEVICE = 200
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for suspicious devices and security threats"
            }
            
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Device Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for tracked devices"
            }
            
            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(trackingChannel)
        }
    }
    
    fun showFollowingAlert(device: BleDevice) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_radar)
            .setContentTitle("⚠️ Security Alert")
            .setContentText("Device ${device.deviceName ?: device.deviceAddress} may be following you")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Device ${device.deviceName ?: device.deviceAddress} has been detected multiple times in your vicinity. Following score: ${(device.followingScore * 100).toInt()}%. Consider your safety and take appropriate action."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_FOLLOWING + device.deviceAddress.hashCode(), notification)
    }
    
    fun showTrackedDeviceNotification(device: BleDevice) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_radar)
            .setContentTitle("Tracked Device Detected")
            .setContentText("${device.label ?: device.deviceName ?: device.deviceAddress} is nearby")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your tracked device ${device.label ?: device.deviceName ?: device.deviceAddress} has been detected nearby with signal strength ${device.rssi} dBm."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_TRACKED_DEVICE + device.deviceAddress.hashCode(), notification)
    }
    
    fun cancelFollowingAlert(deviceAddress: String) {
        notificationManager.cancel(NOTIFICATION_ID_FOLLOWING + deviceAddress.hashCode())
    }
    
    fun cancelTrackedDeviceNotification(deviceAddress: String) {
        notificationManager.cancel(NOTIFICATION_ID_TRACKED_DEVICE + deviceAddress.hashCode())
    }
}