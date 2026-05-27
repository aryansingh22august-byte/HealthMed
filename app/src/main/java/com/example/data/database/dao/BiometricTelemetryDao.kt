package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.BiometricTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BiometricTelemetryDao {
    @Query("SELECT * FROM biometric_telemetry ORDER BY timestamp DESC")
    fun getAllTelemetryReactive(): Flow<List<BiometricTelemetryEntity>>

    @Query("SELECT * FROM biometric_telemetry ORDER BY timestamp DESC LIMIT 1")
    fun getLatestTelemetryReactive(): Flow<BiometricTelemetryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: BiometricTelemetryEntity): Long

    @Query("DELETE FROM biometric_telemetry WHERE id = :id")
    suspend fun deleteTelemetryById(id: Long)

    @Query("DELETE FROM biometric_telemetry")
    suspend fun clearAllTelemetry()
}
