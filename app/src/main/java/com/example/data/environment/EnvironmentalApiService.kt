package com.example.data.environment

import retrofit2.http.GET
import retrofit2.http.Query

// Note: A placeholder interface for air quality API.
// In a real application, you would pass an API key or use your own backend.
data class AqiResponse(
    val aqi: Int,
    val dominantPollutant: String,
    val city: String
)

interface EnvironmentalApiService {
    @GET("v1/current/air-quality")
    suspend fun getAirQuality(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): AqiResponse
}
