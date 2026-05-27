package com.example.data.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClinicalPayloadDto(
    val age: Int?,
    val weightKg: Float?,
    val bloodReportStatus: String?,
    val latestHeartRate: Int?,
    val averageHrv: Double?,
    val latestSpO2: Double?,
    val currentAqi: Int?,
    val currentPollutant: String?,
    val recentCaffeineMg: Int?,
    val recentAlcoholUnits: Double?,
    val subjectiveStressScore: Int?
)

@JsonClass(generateAdapter = true)
data class TherapyInsight(
    val insight_summary: String,
    val physiological_mechanism: String,
    val immediate_action_item: String,
    val urgency_score: Int,
    val severity_bound: String
)

@JsonClass(generateAdapter = true)
data class DietInsight(
    val daily_calories: Int,
    val deficiency_focus: String,
    val breakfast_recommendation: String,
    val lunch_recommendation: String,
    val dinner_recommendation: String,
    val snacks_recommendation: String,
    val actionable_tips: List<String>
)
