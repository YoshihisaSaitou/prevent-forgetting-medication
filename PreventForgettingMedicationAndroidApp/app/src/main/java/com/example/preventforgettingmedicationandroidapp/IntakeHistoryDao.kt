package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IntakeHistoryDao {
    @Insert
    suspend fun insert(entry: IntakeHistory)

    @Insert
    fun insertSync(entry: IntakeHistory)

    @Query("SELECT * FROM intake_history ORDER BY takenAt DESC")
    suspend fun getAll(): List<IntakeHistory>

    @Query("DELETE FROM intake_history")
    suspend fun clearAll()
}
