package com.example.preventforgettingmedicationandroidapp.infrastructure.repository

import com.example.preventforgettingmedicationandroidapp.IntakeHistory
import com.example.preventforgettingmedicationandroidapp.IntakeHistoryDao
import com.example.preventforgettingmedicationandroidapp.ScheduleDao
import com.example.preventforgettingmedicationandroidapp.domain.error.NotFoundException
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroup
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroupKey
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.repository.AddManualResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val historyDao: IntakeHistoryDao,
    private val scheduleDao: ScheduleDao
) : HistoryRepository {

    override suspend fun getHistoryGroups(): List<HistoryGroup> {
        return historyDao.getAll()
            .groupBy { it.scheduleId to it.takenAt }
            .values
            .map { chunk ->
                val first = chunk.first()
                HistoryGroup(
                    key = HistoryGroupKey(
                        scheduleId = first.scheduleId?.let { ScheduleId(it) },
                        takenAt = TakenAt(first.takenAt)
                    ),
                    scheduleName = first.scheduleName ?: "Legacy record",
                    createdAt = chunk.minOfOrNull { it.createdAt } ?: first.createdAt,
                    medicationNames = chunk.map { it.medicationName }.distinct(),
                    incorrectAt = chunk.firstOrNull { it.incorrectAt != null }?.incorrectAt
                )
            }
            .sortedByDescending { it.key.takenAt.value }
    }

    override suspend fun addManual(scheduleId: ScheduleId, takenAt: TakenAt): AddManualResult {
        val scheduleWithMeds = scheduleDao.getWithMedicationsById(scheduleId.value)
            ?: throw NotFoundException("Schedule not found: ${scheduleId.value}")

        if (historyDao.existsScheduleEntry(scheduleId.value, takenAt.value)) {
            return AddManualResult(savedCount = 0, skippedDuplicate = true)
        }

        val entries = scheduleWithMeds.medications.map {
            IntakeHistory(
                scheduleId = scheduleId.value,
                scheduleName = scheduleWithMeds.schedule.name,
                medicationId = it.id,
                medicationName = it.name,
                takenAt = takenAt.value,
                createdAt = System.currentTimeMillis()
            )
        }

        if (entries.isNotEmpty()) {
            historyDao.insertAll(entries)
        }

        return AddManualResult(savedCount = entries.size, skippedDuplicate = false)
    }

    override suspend fun toggleIncorrect(groupKey: HistoryGroupKey): Long? {
        val all = historyDao.getAll().filter {
            it.scheduleId == groupKey.scheduleId?.value && it.takenAt == groupKey.takenAt.value
        }
        val currentHasIncorrect = all.any { it.incorrectAt != null }
        val newValue = if (currentHasIncorrect) null else System.currentTimeMillis()

        if (groupKey.scheduleId != null) {
            historyDao.setIncorrectAtForScheduleGroup(
                scheduleId = groupKey.scheduleId.value,
                takenAt = groupKey.takenAt.value,
                ts = newValue
            )
        } else {
            historyDao.setIncorrectAtForLegacyGroup(
                takenAt = groupKey.takenAt.value,
                ts = newValue
            )
        }

        return newValue
    }
}
