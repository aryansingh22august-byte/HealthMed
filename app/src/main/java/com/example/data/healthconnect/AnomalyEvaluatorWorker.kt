package com.example.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AegisHealthDatabase
import com.example.data.notifications.VitalNotificationManager
import com.example.data.security.SecurePassphraseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AnomalyEvaluatorWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dbPassphrase = SecurePassphraseManager(context).getOrGeneratePassphrase()
            val db = AegisHealthDatabase.getDatabase(context, dbPassphrase)
            val telemetryDao = db.biometricTelemetryDao()

            val latest = telemetryDao.getLatestTelemetryReactive().firstOrNull()

            if (latest != null) {
                var triggered = false
                var alertMsg = ""

                if (latest.spO2 < 92.0) {
                    triggered = true
                    alertMsg += "Low Blood Oxygen detected: ${latest.spO2}%. "
                }
                if (latest.heartRate > 120) {
                    triggered = true
                    alertMsg += "High Resting Heart Rate detected: ${latest.heartRate} bpm."
                }

                if (triggered) {
                    val notifManager = VitalNotificationManager(context)
                    notifManager.showCriticalAlert("Critical Biometric Anomaly", alertMsg)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AnomalyEvaluatorWorker", "Anomaly evaluation failed", e)
            Result.retry()
        }
    }
}
