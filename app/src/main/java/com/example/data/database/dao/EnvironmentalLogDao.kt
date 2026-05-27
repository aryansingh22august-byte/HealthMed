package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.EnvironmentalLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentalLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EnvironmentalLogEntity)

    @Query("SELECT * FROM environmental_log ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAqiReactive(): Flow<EnvironmentalLogEntity?>
    
    @Query("SELECT * FROM environmental_log ORDER BY timestamp DESC LIMIT 10")
    fun getRecentAqiReactive(): Flow<List<EnvironmentalLogEntity>>
}
