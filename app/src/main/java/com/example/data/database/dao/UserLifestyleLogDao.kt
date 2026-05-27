package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.UserLifestyleLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLifestyleLogDao {
    @Query("SELECT * FROM user_lifestyle_log ORDER BY timestamp DESC")
    fun getAllLifestyleLogsReactive(): Flow<List<UserLifestyleLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifestyleLog(log: UserLifestyleLogEntity): Long

    @Query("DELETE FROM user_lifestyle_log WHERE id = :id")
    suspend fun deleteLifestyleLogById(id: Long)

    @Query("DELETE FROM user_lifestyle_log")
    suspend fun clearAllLifestyleLogs()
}
