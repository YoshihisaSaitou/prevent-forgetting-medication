package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroup
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroupKey
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.policy.IntakePolicy
import com.example.preventforgettingmedicationandroidapp.domain.repository.AddManualResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.HistoryRepository
import javax.inject.Inject

class GetHistoryGroupsUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(): List<HistoryGroup> = historyRepository.getHistoryGroups()
}

class AddManualHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val widgetPort: WidgetPort
) {
    suspend operator fun invoke(scheduleId: ScheduleId, takenAt: TakenAt): AddManualResult {
        IntakePolicy.validateManualTakenAt(takenAt)
        val result = historyRepository.addManual(scheduleId, takenAt)
        if (result.savedCount > 0) {
            widgetPort.refreshHistory()
        }
        return result
    }
}

class ToggleIncorrectUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val widgetPort: WidgetPort
) {
    suspend operator fun invoke(groupKey: HistoryGroupKey): Long? {
        val value = historyRepository.toggleIncorrect(groupKey)
        widgetPort.refreshHistory()
        return value
    }
}
