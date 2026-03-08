package com.example.preventforgettingmedicationandroidapp.presentation.model

import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId

data class ScheduleListItem(
    val id: ScheduleId,
    val name: String,
    val slot: IntakeSlot,
    val timeMinutes: Int,
    val medicationNames: List<String>,
    val canExecute: Boolean
)

data class MedicationOption(
    val id: Int,
    val name: String
)

data class ScheduleOption(
    val id: ScheduleId,
    val name: String,
    val slot: IntakeSlot,
    val timeMinutes: Int
)

data class HistoryGroupItem(
    val scheduleId: ScheduleId?,
    val scheduleName: String,
    val takenAt: Long,
    val createdAt: Long,
    val medicationNames: List<String>,
    val incorrectAt: Long?
)
