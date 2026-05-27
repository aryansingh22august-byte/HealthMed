package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_report")
data class BloodReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val cholesterol: String?,
    val glucose: String?,
    val hemoglobin: String?
)
