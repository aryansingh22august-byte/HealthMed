package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("SELECT * FROM medication ORDER BY targetTimeMs ASC")
    fun getAllMedicationsReactive(): Flow<List<MedicationEntity>>

    @Query("DELETE FROM medication WHERE id = :id")
    suspend fun deleteMedicationById(id: Long)

    @Query("UPDATE medication SET isTaken = :isTaken WHERE id = :id")
    suspend fun updateMedicationTakenStatus(id: Long, isTaken: Boolean)
}
