package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Int): Schedule?

    @Query("SELECT * FROM schedules WHERE isActive = 1 ORDER BY timeMinutes ASC, id ASC")
    fun getActiveSchedulesSync(): List<Schedule>

    @Query("SELECT * FROM schedules ORDER BY timeMinutes ASC, id ASC")
    suspend fun getAll(): List<Schedule>

    @Query("SELECT * FROM schedules ORDER BY timeMinutes ASC, id ASC")
    fun getAllSync(): List<Schedule>

    @Transaction
    @Query("SELECT * FROM schedules ORDER BY timeMinutes ASC, id ASC")
    suspend fun getAllWithMedications(): List<ScheduleWithMedications>

    @Transaction
    @Query("SELECT * FROM schedules ORDER BY timeMinutes ASC, id ASC")
    fun getAllWithMedicationsSync(): List<ScheduleWithMedications>

    @Transaction
    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getWithMedicationsById(id: Int): ScheduleWithMedications?

    @Transaction
    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getWithMedicationsByIdSync(id: Int): ScheduleWithMedications?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<ScheduleMedicationCrossRef>)

    @Query("DELETE FROM schedule_medications WHERE scheduleId = :scheduleId")
    suspend fun deleteCrossRefsForSchedule(scheduleId: Int)

    @Query("SELECT COUNT(*) FROM schedules")
    fun countAllSync(): Int
}
