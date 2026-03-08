package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IntakeHistoryDao {
    @Insert
    suspend fun insert(entry: IntakeHistory)

    @Insert
    suspend fun insertAll(entries: List<IntakeHistory>)

    @Insert
    fun insertSync(entry: IntakeHistory)

    @Query("SELECT * FROM intake_history ORDER BY takenAt DESC")
    suspend fun getAll(): List<IntakeHistory>

    @Query("SELECT * FROM intake_history ORDER BY takenAt DESC LIMIT :limit")
    fun getRecentSync(limit: Int): List<IntakeHistory>

    @Query("DELETE FROM intake_history")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM intake_history WHERE medicationId = :medId AND takenAt = :takenAt)")
    suspend fun exists(medId: Int, takenAt: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM intake_history WHERE scheduleId = :scheduleId AND takenAt = :takenAt)")
    suspend fun existsScheduleEntry(scheduleId: Int, takenAt: Long): Boolean

    @Query("UPDATE intake_history SET incorrectAt = :ts WHERE id = :id")
    suspend fun setIncorrectAt(id: Int, ts: Long?)

    @Query("UPDATE intake_history SET incorrectAt = :ts WHERE scheduleId = :scheduleId AND takenAt = :takenAt")
    suspend fun setIncorrectAtForScheduleGroup(scheduleId: Int, takenAt: Long, ts: Long?)

    @Query("UPDATE intake_history SET incorrectAt = :ts WHERE scheduleId IS NULL AND takenAt = :takenAt")
    suspend fun setIncorrectAtForLegacyGroup(takenAt: Long, ts: Long?)
}
