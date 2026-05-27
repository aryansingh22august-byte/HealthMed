package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_lifestyle_log")
data class UserLifestyleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val caffeineMg: Int,
    val alcoholUnits: Double,
    val subjectiveStressScore: Int
)
