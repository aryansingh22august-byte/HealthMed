package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class VitalNotificationManager(private val context: Context) {

    companion object {
        const val ADHERENCE_CHANNEL_ID = "adherence_channel"
        const val CRITICAL_CHANNEL_ID = "critical_alerts_channel"
        const val ADHERENCE_NOTIF_ID = 1001
        const val CRITICAL_NOTIF_ID = 2001
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val adherenceChannel = NotificationChannel(
                ADHERENCE_CHANNEL_ID,
                "Adherence Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for medication and daily lifestyle tasks."
            }

            val criticalChannel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                "Critical Biometric Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for detected biometric anomalies."
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(adherenceChannel)
            notificationManager.createNotificationChannel(criticalChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showAdherenceReminder(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val builder = NotificationCompat.Builder(context, ADHERENCE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(ADHERENCE_NOTIF_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission was revoked between check and notify
        }
    }

    fun showCriticalAlert(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val builder = NotificationCompat.Builder(context, CRITICAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(CRITICAL_NOTIF_ID + System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission was revoked between check and notify
        }
    }
}
