package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val targetTimeMs: Long,
    val isTaken: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
