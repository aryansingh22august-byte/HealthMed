package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.BloodReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodReport(report: BloodReportEntity): Long

    @Query("SELECT * FROM blood_report ORDER BY timestampMs DESC LIMIT 1")
    fun getLatestBloodReportReactive(): Flow<BloodReportEntity?>

    @Query("SELECT * FROM blood_report ORDER BY timestampMs DESC")
    fun getAllBloodReportsReactive(): Flow<List<BloodReportEntity>>

    @Query("DELETE FROM blood_report")
    suspend fun clearAllBloodReports()
}
