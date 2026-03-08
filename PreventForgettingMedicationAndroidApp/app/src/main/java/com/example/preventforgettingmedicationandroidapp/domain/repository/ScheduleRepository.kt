package com.example.preventforgettingmedicationandroidapp.domain.repository

import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt

data class SaveScheduleCommand(
    val scheduleId: ScheduleId?,
    val name: String,
    val slot: com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot,
    val timeMinutes: Int,
    val medicationIds: List<Int>,
    val isActive: Boolean = true
)

data class ExecuteResult(
    val insertedCount: Int,
    val skippedDuplicate: Boolean,
    val skippedDisabled: Boolean,
    val skippedMissing: Boolean
)

interface ScheduleRepository {
    suspend fun getSchedules(): List<ScheduleWithMedications>
    suspend fun getSchedule(scheduleId: ScheduleId): ScheduleWithMedications?
    suspend fun saveSchedule(command: SaveScheduleCommand): ScheduleId
    suspend fun deleteSchedule(scheduleId: ScheduleId)
    suspend fun execute(scheduleId: ScheduleId, takenAt: TakenAt): ExecuteResult
}
