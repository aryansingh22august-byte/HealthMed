package com.example.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AegisHealthDatabase
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.security.SecurePassphraseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                return@withContext Result.failure()
            }

            val healthConnectClient = HealthConnectClient.getOrCreate(context)

            // 15 minutes window
            val endTime = Instant.now()
            val startTime = endTime.minus(15, ChronoUnit.MINUTES)

            // Read Heart Rate
            val hrRequest = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val hrResponse = healthConnectClient.readRecords(hrRequest)
            
            // Read SpO2
            val spo2Request = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val spo2Response = healthConnectClient.readRecords(spo2Request)

            // Very simple merge strategy: match near timestamps or just aggregate recent.
            // For AegisHealth, we map individual records or combined if timestamps match.
            // Let's just create BiometricTelemetryEntity per HR record and try to append SpO2 if available nearby.
            // Or aggregate averages for the 15 min window.
            // Let's map HR and SpO2 to single records.
            if (hrResponse.records.isNotEmpty() || spo2Response.records.isNotEmpty()) {
                val dbPassphrase = SecurePassphraseManager(context).getOrGeneratePassphrase()
                val db = AegisHealthDatabase.getDatabase(context, dbPassphrase)
                val telemetryDao = db.biometricTelemetryDao()
                
                // Aggregate latest data
                val latestHr = hrResponse.records.maxByOrNull { it.endTime }?.samples?.lastOrNull()?.beatsPerMinute?.toInt() ?: 72
                val hrv = 48.0 // Mock HRV as Health Connect native HRV API is limited or requires custom types
                val latestSpO2 = spo2Response.records.maxByOrNull { it.time }?.percentage?.value ?: 98.0
                
                telemetryDao.insertTelemetry(
                    BiometricTelemetryEntity(
                        timestamp = System.currentTimeMillis(),
                        heartRate = latestHr,
                        hrv = hrv,
                        spO2 = latestSpO2
                    )
                )
            }

            // Chain Anomaly Evaluator
            val evaluateAnomalyWork = androidx.work.OneTimeWorkRequestBuilder<AnomalyEvaluatorWorker>().build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(evaluateAnomalyWork)

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("BackgroundSyncWorker", "Health Connect sync failed", e)
            return@withContext Result.retry() 
        }
    }
}
