package com.example.preventforgettingmedicationandroidapp.domain.repository

import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroup
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroupKey
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt

data class AddManualResult(
    val savedCount: Int,
    val skippedDuplicate: Boolean
)

interface HistoryRepository {
    suspend fun getHistoryGroups(): List<HistoryGroup>
    suspend fun addManual(scheduleId: ScheduleId, takenAt: TakenAt): AddManualResult
    suspend fun toggleIncorrect(groupKey: HistoryGroupKey): Long?
}
