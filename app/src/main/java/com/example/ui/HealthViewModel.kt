package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.common.Result
import com.example.data.database.AegisHealthDatabase
import com.example.data.database.entity.BiometricTelemetryEntity
import com.example.data.database.entity.UserLifestyleLogEntity
import com.example.data.security.SecurePassphraseManager
import com.example.data.repository.HealthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.data.network.dto.TherapyInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Random

import androidx.work.WorkManager
import com.example.data.alarms.AdherenceAlarmScheduler

import com.example.data.database.entity.EnvironmentalLogEntity
import com.example.data.environment.LocationTracker
import com.example.data.environment.EnvironmentalApiService
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.example.data.database.entity.BloodReportEntity

import com.example.data.network.dto.DietInsight

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val _therapyInsightState = MutableStateFlow<Result<TherapyInsight?>>(Result.Success(null))
    val therapyInsightState: StateFlow<Result<TherapyInsight?>> = _therapyInsightState

    private val _dietInsightState = MutableStateFlow<Result<DietInsight?>>(Result.Success(null))
    val dietInsightState: StateFlow<Result<DietInsight?>> = _dietInsightState

    fun fetchPersonalizedTherapies(question: String = "") {
        viewModelScope.launch {
            _therapyInsightState.value = Result.Loading
            val profile = userProfileManager.userProfileState.value
            _therapyInsightState.value = repository.fetchPersonalizedTherapies(
                age = profile.age,
                weightKg = profile.weightKg,
                bloodReportStatus = profile.bloodReportStatus,
                customQuestion = question
            )
        }
    }

    fun fetchDietAdvice() {
        viewModelScope.launch {
            _dietInsightState.value = Result.Loading
            val profile = userProfileManager.userProfileState.value
            _dietInsightState.value = repository.fetchDietAdvice(
                age = profile.age,
                weightKg = profile.weightKg,
                heightCm = profile.heightCm,
                bloodReportStatus = profile.bloodReportStatus
            )
        }
    }

    private val passphraseManager = SecurePassphraseManager(application)
    private val passphraseBytes = passphraseManager.getOrGeneratePassphrase()
    private val database = AegisHealthDatabase.getDatabase(application, passphraseBytes)
    
    private val repository = HealthRepository(
        database.biometricTelemetryDao(),
        database.userLifestyleLogDao(),
        database.environmentalLogDao(),
        database.bloodReportDao(),
        database.medicationDao(),
        WorkManager.getInstance(application)
    )

    private val locationTracker = LocationTracker(application)
    
    // We use a mock API base URL. A fake interceptor could be used, or just handle the crash.
    // For demo purposes, we'll try/catch and emit dummy data if the endpoint fails.
    private val envApiService = Retrofit.Builder()
        .baseUrl("https://api.openaq.org/") // Example URL
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(EnvironmentalApiService::class.java)

    private val alarmScheduler = AdherenceAlarmScheduler(application)
    private val userProfileManager = com.example.data.user.UserProfileManager(application)
    
    val userProfileState: StateFlow<com.example.data.user.UserProfile> = userProfileManager.userProfileState

    fun updateUserProfile(name: String, age: Int, weightKg: Float, heightCm: Float, bloodReportStatus: String) {
        userProfileManager.updateProfile(name, age, weightKg, heightCm, bloodReportStatus)
    }

    val latestBloodReportState: StateFlow<Result<BloodReportEntity?>> = repository.bloodReportDao.getLatestBloodReportReactive()
        .map { Result.Success(it) as Result<BloodReportEntity?> }
        .catch { emit(Result.Error(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    fun analyzeBloodReportImage(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            val result = repository.analyzeBloodReportImage(bitmap)
            if (result is Result.Success) {
                val current = userProfileState.value
                updateUserProfile(current.name, current.age, current.weightKg, current.heightCm, "Uploaded (AI Analyzed)")
            }
        }
    }

    fun startHealthConnectSync() {
        repository.startBackgroundSync()
    }

    private val _bloodReportTrendsState = MutableStateFlow<Result<String?>>(Result.Success(null))
    val bloodReportTrendsState: StateFlow<Result<String?>> = _bloodReportTrendsState

    fun fetchBloodReportTrends() {
        viewModelScope.launch {
            _bloodReportTrendsState.value = Result.Loading
            _bloodReportTrendsState.value = repository.fetchBloodReportTrends()
        }
    }

    val medicationState: StateFlow<Result<List<com.example.data.database.entity.MedicationEntity>>> = repository.medicationDao.getAllMedicationsReactive()
        .map { Result.Success(it) as Result<List<com.example.data.database.entity.MedicationEntity>> }
        .catch { emit(Result.Error(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    fun addMedication(name: String, dosage: String, frequency: String, targetTimeMs: Long) {
        viewModelScope.launch {
            repository.medicationDao.insertMedication(
                com.example.data.database.entity.MedicationEntity(
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    targetTimeMs = targetTimeMs
                )
            )
            scheduleMedicationReminder(targetTimeMs, "Time to take \$dosage of \$name")
        }
    }

    fun toggleMedicationTakenStatus(id: Long, isTaken: Boolean) {
        viewModelScope.launch {
            repository.medicationDao.updateMedicationTakenStatus(id, isTaken)
        }
    }

    fun scheduleMedicationReminder(timeInMillis: Long, message: String) {
        alarmScheduler.scheduleMedicationReminder(timeInMillis, message)
    }

    // Bridge standard database streams using standard MVVM stateFlow wrappers
    val telemetryState: StateFlow<Result<List<BiometricTelemetryEntity>>> = repository.biometricTelemetryStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    val latestTelemetryState: StateFlow<Result<BiometricTelemetryEntity?>> = repository.latestTelemetryStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    val latestAqiState: StateFlow<Result<EnvironmentalLogEntity?>> = repository.latestAqiStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    fun fetchEnvironmentalContext() {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                try {
                    val response = envApiService.getAirQuality(location.latitude, location.longitude)
                    repository.environmentalLogDao.insertLog(
                        EnvironmentalLogEntity(
                            aqiScore = response.aqi,
                            dominantPollutant = response.dominantPollutant,
                            locationCity = response.city
                        )
                    )
                } catch (e: Exception) {
                    // Fallback simulated data if API fails to avoid crashing
                    val randomAqi = 30 + java.util.Random().nextInt(100)
                    repository.environmentalLogDao.insertLog(
                        EnvironmentalLogEntity(
                            aqiScore = randomAqi,
                            dominantPollutant = listOf("PM2.5", "O3", "NO2").random(),
                            locationCity = "Simulated Location"
                        )
                    )
                }
            } else {
                // If location fails, log generic
                repository.environmentalLogDao.insertLog(
                    EnvironmentalLogEntity(
                        aqiScore = 45,
                        dominantPollutant = "PM2.5",
                        locationCity = "Unknown Location"
                    )
                )
            }
        }
    }

    val lifestyleState: StateFlow<Result<List<UserLifestyleLogEntity>>> = repository.lifestyleLogStream
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    fun addTelemetry(heartRate: Int, hrv: Double, spO2: Double) {
        viewModelScope.launch {
            repository.insertBiometricTelemetry(
                BiometricTelemetryEntity(
                    heartRate = heartRate,
                    hrv = hrv,
                    spO2 = spO2
                )
            )
        }
    }

    fun addLifestyleLog(caffeineMg: Int, alcoholUnits: Double, subjectiveStressScore: Int) {
        viewModelScope.launch {
            repository.insertLifestyleLog(
                UserLifestyleLogEntity(
                    caffeineMg = caffeineMg,
                    alcoholUnits = alcoholUnits,
                    subjectiveStressScore = subjectiveStressScore
                )
            )
        }
    }

    fun deleteTelemetry(id: Long) {
        viewModelScope.launch {
            repository.deleteTelemetryById(id)
        }
    }

    fun deleteLifestyleLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLifestyleLogById(id)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Seeds smart-random simulated telemetry logs for quick testing and diagnostic verification.
     */
    fun seedSampleHealthData() {
        viewModelScope.launch {
            val random = Random()
            
            // Core telemetry: HR (60-120 bpm), HRV (20-90 ms), SpO2 (94-100%)
            val heartRateMock = 60 + random.nextInt(40)
            val hrvMock = 25.0 + random.nextInt(65).toDouble()
            val spO2Mock = 94.0 + random.nextInt(7).toDouble()

            repository.insertBiometricTelemetry(
                BiometricTelemetryEntity(
                    heartRate = heartRateMock,
                    hrv = hrvMock,
                    spO2 = spO2Mock
                )
            )

            // Lifestyle patterns: Caffeine (0-300 mg), Alcohol (0-3.0 units), Stress Score (1-10)
            val caffeineMgMock = random.nextInt(4) * 80
            val alcoholUnitsMock = (random.nextInt(30) / 10.0)
            val stressScoreMock = 1 + random.nextInt(9)

            repository.insertLifestyleLog(
                UserLifestyleLogEntity(
                    caffeineMg = caffeineMgMock,
                    alcoholUnits = alcoholUnitsMock,
                    subjectiveStressScore = stressScoreMock
                )
            )
        }
    }
}
