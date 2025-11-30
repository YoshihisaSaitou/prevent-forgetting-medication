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

    // Synchronous accessor for widgets/services
    @Query("SELECT * FROM intake_history ORDER BY takenAt DESC LIMIT :limit")
    fun getRecentSync(limit: Int): List<IntakeHistory>

    @Query("DELETE FROM intake_history")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM intake_history WHERE medicationId = :medId AND takenAt = :takenAt)")
    suspend fun exists(medId: Int, takenAt: Long): Boolean

    @Query("UPDATE intake_history SET incorrectAt = :ts WHERE id = :id")
    suspend fun setIncorrectAt(id: Int, ts: Long?)
}
