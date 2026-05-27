package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "environmental_log")
data class EnvironmentalLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val aqiScore: Int,
    val dominantPollutant: String,
    val locationCity: String
)
