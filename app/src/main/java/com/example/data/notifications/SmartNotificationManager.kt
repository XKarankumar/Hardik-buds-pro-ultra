package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.NotificationPreferences

class SmartNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_CONNECTION = "hardik_buds_connection"
        const val CHANNEL_BATTERY = "hardik_buds_battery"
        const val CHANNEL_SECURITY = "hardik_buds_security"
        const val CHANNEL_RADIO = "hardik_buds_radio"

        private const val NOTIF_ID_CONNECTION = 1001
        private const val NOTIF_ID_BATTERY = 1002
        private const val NOTIF_ID_SECURITY = 1003
        private const val NOTIF_ID_RADIO = 1004
    }

    fun ensureChannelsCreated(customSoundUri: String? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = customSoundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(
                CHANNEL_CONNECTION,
                "Earbuds Connection & Disconnection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Bluetooth earbuds connect, disconnect, or drop connection"
                setSound(soundUri, audioAttrs)
            },
            NotificationChannel(
                CHANNEL_BATTERY,
                "Battery & Charging Telemetry",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Low battery warnings and charging status updates"
            },
            NotificationChannel(
                CHANNEL_SECURITY,
                "Security & Unauthorized Access Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts on failed PIN attempts or unauthorized companion access"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_RADIO,
                "Bluetooth Radio Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications when phone Bluetooth is turned off"
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyConnected(deviceName: String, batteryPercent: Int?, prefs: NotificationPreferences) {
        if (!prefs.masterEnabled || !prefs.onConnected) return
        val batteryText = batteryPercent?.let { " • Battery $it%" } ?: " • Battery unavailable"
        postNotification(
            channelId = CHANNEL_CONNECTION,
            notifId = NOTIF_ID_CONNECTION,
            title = "Earbuds Connected",
            message = "$deviceName connected$batteryText",
            customSoundUri = prefs.customSoundUri
        )
    }

    fun notifyDisconnected(deviceName: String, unexpected: Boolean, prefs: NotificationPreferences) {
        if (!prefs.masterEnabled) return
        if (unexpected && !prefs.onConnectionLost) return
        if (!unexpected && !prefs.onDisconnected) return
        val title = if (unexpected) "Earbuds Connection Lost" else "Earbuds Disconnected"
        val msg = if (unexpected) {
            "$deviceName dropped connection. Open Find My Earbuds for last known phone location."
        } else {
            "$deviceName disconnected."
        }
        postNotification(
            channelId = CHANNEL_CONNECTION,
            notifId = NOTIF_ID_CONNECTION,
            title = title,
            message = msg,
            customSoundUri = prefs.customSoundUri
        )
    }

    fun notifyLowBattery(deviceName: String, percent: Int, prefs: NotificationPreferences) {
        if (!prefs.masterEnabled || !prefs.onLowBattery) return
        if (percent > prefs.lowBatteryThreshold) return
        postNotification(
            channelId = CHANNEL_BATTERY,
            notifId = NOTIF_ID_BATTERY,
            title = "Low Earbuds Battery ($percent%)",
            message = "$deviceName battery is at $percent%. Place earbuds in the charging case soon.",
            customSoundUri = prefs.customSoundUri
        )
    }

    fun notifyBluetoothDisabled(prefs: NotificationPreferences) {
        if (!prefs.masterEnabled || !prefs.onBluetoothDisabled) return
        postNotification(
            channelId = CHANNEL_RADIO,
            notifId = NOTIF_ID_RADIO,
            title = "Bluetooth Disabled",
            message = "Phone Bluetooth was turned off. Earbuds monitoring is paused.",
            customSoundUri = null
        )
    }

    fun notifyUnauthorizedAccess(attempts: Int, prefs: NotificationPreferences) {
        if (!prefs.masterEnabled || !prefs.onUnauthorizedAccess) return
        postNotification(
            channelId = CHANNEL_SECURITY,
            notifId = NOTIF_ID_SECURITY,
            title = "Unauthorized Access Alert",
            message = "Failed companion PIN attempt detected ($attempts failed). Security log updated.",
            customSoundUri = prefs.customSoundUri
        )
    }

    private fun postNotification(
        channelId: String,
        notifId: Int,
        title: String,
        message: String,
        customSoundUri: String?
    ) {
        if (!hasNotificationPermission()) return
        ensureChannelsCreated(customSoundUri)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_art)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (customSoundUri != null) {
            builder.setSound(Uri.parse(customSoundUri))
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
