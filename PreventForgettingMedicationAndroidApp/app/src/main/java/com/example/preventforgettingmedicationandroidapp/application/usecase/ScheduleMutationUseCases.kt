package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.policy.IntakePolicy
import com.example.preventforgettingmedicationandroidapp.domain.repository.ExecuteResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveScheduleCommand
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import javax.inject.Inject

class CreateOrUpdateScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val reminderPort: ReminderPort,
    private val widgetPort: WidgetPort
) {
    suspend operator fun invoke(command: SaveScheduleCommand): ScheduleId {
        IntakePolicy.validateScheduleName(command.name)
        IntakePolicy.validateTimeMinutes(command.timeMinutes)
        IntakePolicy.validateMedicationSelection(command.medicationIds)

        val id = scheduleRepository.saveSchedule(command)
        reminderPort.syncAll()
        widgetPort.refreshSchedules()
        return id
    }
}

class DeleteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val reminderPort: ReminderPort,
    private val widgetPort: WidgetPort
) {
    suspend operator fun invoke(scheduleId: ScheduleId) {
        scheduleRepository.deleteSchedule(scheduleId)
        reminderPort.syncAll()
        widgetPort.refreshSchedules()
    }
}

class ExecuteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val widgetPort: WidgetPort
) {
    suspend operator fun invoke(
        scheduleId: ScheduleId,
        takenAt: TakenAt = TakenAt.now()
    ): ExecuteResult {
        val result = scheduleRepository.execute(scheduleId, takenAt)
        if (result.insertedCount > 0) {
            widgetPort.refreshSchedules()
            widgetPort.refreshHistory()
        }
        return result
    }
}
