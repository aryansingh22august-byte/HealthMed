package com.example.data.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.notifications.VitalNotificationManager

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("MESSAGE") ?: "It is time for your scheduled medication."

        val notificationManager = VitalNotificationManager(context)
        notificationManager.showAdherenceReminder(
            title = "Medication Reminder",
            message = message
        )
    }
}
