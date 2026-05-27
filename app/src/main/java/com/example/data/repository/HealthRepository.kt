package com.example.data.repository

import com.example.data.common.Result
import com.example.data.database.dao.BiometricTelemetryDao
import com.example.data.database.dao.UserLifestyleLogDao
import com.example.data.database.dao.EnvironmentalLogDao
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.database.entity.UserLifestyleLogEntity
import com.example.data.database.entity.EnvironmentalLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.data.healthconnect.BackgroundSyncWorker

import kotlinx.coroutines.flow.firstOrNull
import com.example.data.network.dto.ClinicalPayloadDto
import com.example.data.network.dto.TherapyInsight
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.squareup.moshi.Moshi

class HealthRepository(
    private val telemetryDao: BiometricTelemetryDao,
    private val lifestyleLogDao: UserLifestyleLogDao,
    val environmentalLogDao: EnvironmentalLogDao,
    val bloodReportDao: com.example.data.database.dao.BloodReportDao,
    val medicationDao: com.example.data.database.dao.MedicationDao,
    private val workManager: WorkManager
) {
    suspend fun fetchPersonalizedTherapies(age: Int?, weightKg: Float?, bloodReportStatus: String?, customQuestion: String = ""): Result<TherapyInsight> = withContext(Dispatchers.IO) {
        try {
            val telemetry = telemetryDao.getLatestTelemetryReactive().firstOrNull()
            val aqi = environmentalLogDao.getLatestAqiReactive().firstOrNull()
            val lifestyleLogs = lifestyleLogDao.getAllLifestyleLogsReactive().firstOrNull()
            val latestLifestyle = lifestyleLogs?.firstOrNull()

            val payload = ClinicalPayloadDto(
                age = age,
                weightKg = weightKg,
                bloodReportStatus = bloodReportStatus,
                latestHeartRate = telemetry?.heartRate,
                averageHrv = telemetry?.hrv,
                latestSpO2 = telemetry?.spO2,
                currentAqi = aqi?.aqiScore,
                currentPollutant = aqi?.dominantPollutant,
                recentCaffeineMg = latestLifestyle?.caffeineMg,
                recentAlcoholUnits = latestLifestyle?.alcoholUnits,
                subjectiveStressScore = latestLifestyle?.subjectiveStressScore
            )

            val generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )

            val customInstruction = if (customQuestion.isNotBlank()) "The user has asked a specific question: \"$customQuestion\". Ensure your action item and summary directly address this question." else ""

            val prompt = """
                You are a clinical drafting assistant.
                Based on the following user data, draft preventative wellness optimizations.
                Do NOT diagnose any disease. Only suggest lifestyle and wellness practices.
                $customInstruction
                
                Data:
                ${payload}
                
                You must return a single JSON object strictly matching this format:
                {
                  "insight_summary": "1-2 sentence overview",
                  "physiological_mechanism": "scientific rationale",
                  "immediate_action_item": "short actionable step",
                  "urgency_score": 1, // integer from 1 to 10
                  "severity_bound": "ROUTINE" // or "ELEVATED", "CRITICAL"
                }
            """.trimIndent()

            val responseText = generativeModel.generateContent(prompt).text ?: ""
            
            val cleanJson = responseText
                .replace(Regex("^```json\\s*"), "")
                .replace(Regex("^```\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()

            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(TherapyInsight::class.java)
            val parsedInsight = adapter.fromJson(cleanJson)
                ?: throw IllegalStateException("Could not parse AI JSON")

            Result.Success(parsedInsight)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchDietAdvice(age: Int?, weightKg: Float?, heightCm: Float?, bloodReportStatus: String?): Result<com.example.data.network.dto.DietInsight> = withContext(Dispatchers.IO) {
        try {
            val telemetry = telemetryDao.getLatestTelemetryReactive().firstOrNull()
            val aqi = environmentalLogDao.getLatestAqiReactive().firstOrNull()
            val lifestyleLogs = lifestyleLogDao.getAllLifestyleLogsReactive().firstOrNull()
            val latestLifestyle = lifestyleLogs?.firstOrNull()
            val latestBloodReport = bloodReportDao.getLatestBloodReportReactive().firstOrNull()
            
            // Calculate BMI
            val bmiString = if (weightKg != null && heightCm != null && heightCm > 0) {
                val heightM = heightCm / 100
                val bmi = weightKg / (heightM * heightM)
                "%.1f".format(bmi)
            } else "N/A"

            val payload = com.example.data.network.dto.ClinicalPayloadDto(
                age = age,
                weightKg = weightKg,
                bloodReportStatus = bloodReportStatus,
                latestHeartRate = telemetry?.heartRate,
                averageHrv = telemetry?.hrv,
                latestSpO2 = telemetry?.spO2,
                currentAqi = aqi?.aqiScore,
                currentPollutant = aqi?.dominantPollutant,
                recentCaffeineMg = latestLifestyle?.caffeineMg,
                recentAlcoholUnits = latestLifestyle?.alcoholUnits,
                subjectiveStressScore = latestLifestyle?.subjectiveStressScore
            )

            val generativeModel = com.google.ai.client.generativeai.GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = com.example.BuildConfig.GEMINI_API_KEY,
                generationConfig = com.google.ai.client.generativeai.type.generationConfig {
                    responseMimeType = "application/json"
                }
            )

            val prompt = """
                You are a clinical dietitian assistant.
                Based on the following user data (including BMI: $bmiString, Glucose: ${latestBloodReport?.glucose ?: "N/A"}, Cholesterol: ${latestBloodReport?.cholesterol ?: "N/A"}), draft a healthy daily diet plan to resolve any deficiencies and improve the user's health. For example, if BMI is low, recommend a calorie surplus; if glucose is high, recommend low sugar options.

                Data:
                $payload
                
                You must return a single JSON object strictly matching this format:
                {
                  "daily_calories": 2000,
                  "deficiency_focus": "Short description of what the diet helps with",
                  "breakfast_recommendation": "meal description",
                  "lunch_recommendation": "meal description",
                  "dinner_recommendation": "meal description",
                  "snacks_recommendation": "meal description",
                  "actionable_tips": ["tip1", "tip2"]
                }
            """.trimIndent()

            val responseText = generativeModel.generateContent(prompt).text ?: ""
            
            val cleanJson = responseText
                .replace(Regex("^```json\\s*"), "")
                .replace(Regex("^```\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()

            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter(com.example.data.network.dto.DietInsight::class.java)
            val parsedInsight = adapter.fromJson(cleanJson)
                ?: throw IllegalStateException("Could not parse AI JSON")

            Result.Success(parsedInsight)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchBloodReportTrends(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allReports = bloodReportDao.getAllBloodReportsReactive().firstOrNull()
            if (allReports.isNullOrEmpty() || allReports.size < 2) {
                return@withContext Result.Success("Not enough historical data to analyze trends. Please upload more blood reports.")
            }

            val formattedData = allReports.sortedBy { it.timestampMs }.mapIndexed { index, report ->
                "Report ${index + 1} (${java.util.Date(report.timestampMs)}): Cholesterol=${report.cholesterol}, Glucose=${report.glucose}, Hemoglobin=${report.hemoglobin}"
            }.joinToString("\n")

            val generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            val prompt = """
                You are a clinical data analyst. Analyze the following historical blood reports and identify key trends, improvements, or worsening conditions.
                Provide a short, easy-to-read summary (max 3 paragraphs) emphasizing changes over time.
                
                Data:
                $formattedData
            """.trimIndent()

            val responseText = generativeModel.generateContent(prompt).text ?: "No analysis available"
            Result.Success(responseText)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun analyzeBloodReportImage(bitmap: android.graphics.Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )
            val inputContent = com.google.ai.client.generativeai.type.content {
                image(bitmap)
                text("Extract cholesterol, glucose and hemoglobin levels from this blood report in JSON format. Return a single JSON object strictly matching this format: { \"cholesterol\": \"value with unit\", \"glucose\": \"value with unit\", \"hemoglobin\": \"value with unit\" }")
            }
            val responseText = generativeModel.generateContent(inputContent).text ?: ""
            
            val cleanJson = responseText
                .replace(Regex("^```json\\s*"), "")
                .replace(Regex("^```\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()

            val jsonObject = org.json.JSONObject(cleanJson)
            val cholesterol = jsonObject.optString("cholesterol", "N/A")
            val glucose = jsonObject.optString("glucose", "N/A")
            val hemoglobin = jsonObject.optString("hemoglobin", "N/A")
            
            bloodReportDao.insertBloodReport(
                com.example.data.database.entity.BloodReportEntity(
                    cholesterol = cholesterol,
                    glucose = glucose,
                    hemoglobin = hemoglobin
                )
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun startBackgroundSync() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "HealthConnectSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
    // 1. Reactive Streams wrapped in Result flow mapping
    val biometricTelemetryStream: Flow<Result<List<BiometricTelemetryEntity>>> = telemetryDao.getAllTelemetryReactive()
        .map { Result.Success(it) as Result<List<BiometricTelemetryEntity>> }
        .catch { emit(Result.Error(it)) }

    val latestTelemetryStream: Flow<Result<BiometricTelemetryEntity?>> = telemetryDao.getLatestTelemetryReactive()
        .map { Result.Success(it) as Result<BiometricTelemetryEntity?> }
        .catch { emit(Result.Error(it)) }

    val lifestyleLogStream: Flow<Result<List<UserLifestyleLogEntity>>> = lifestyleLogDao.getAllLifestyleLogsReactive()
        .map { Result.Success(it) as Result<List<UserLifestyleLogEntity>> }
        .catch { emit(Result.Error(it)) }

    val latestAqiStream: Flow<Result<EnvironmentalLogEntity?>> = environmentalLogDao.getLatestAqiReactive()
        .map { Result.Success(it) as Result<EnvironmentalLogEntity?> }
        .catch { emit(Result.Error(it)) }

    // 2. Safe Writes switching execution context to IO dispatcher
    suspend fun insertBiometricTelemetry(telemetry: BiometricTelemetryEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = telemetryDao.insertTelemetry(telemetry)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun insertLifestyleLog(log: UserLifestyleLogEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = lifestyleLogDao.insertLifestyleLog(log)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // 3. Safe Deletions
    suspend fun deleteTelemetryById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            telemetryDao.deleteTelemetryById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteLifestyleLogById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            lifestyleLogDao.deleteLifestyleLogById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            telemetryDao.clearAllTelemetry()
            lifestyleLogDao.clearAllLifestyleLogs()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
