package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import javax.inject.Inject

class GetScheduleListUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(): List<ScheduleWithMedications> = scheduleRepository.getSchedules()
}

class GetScheduleByIdUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(scheduleId: ScheduleId): ScheduleWithMedications? =
        scheduleRepository.getSchedule(scheduleId)
}
