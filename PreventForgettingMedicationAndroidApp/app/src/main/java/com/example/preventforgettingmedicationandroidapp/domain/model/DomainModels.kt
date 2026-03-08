package com.example.preventforgettingmedicationandroidapp.domain.model

enum class IntakeSlot {
    MORNING,
    NOON,
    EVENING
}

enum class MealTiming {
    BEFORE_MEAL,
    AFTER_MEAL
}

data class Medication(
    val id: MedicationId?,
    val name: String,
    val mealTiming: MealTiming?,
    val memo: String?
)

data class Schedule(
    val id: ScheduleId?,
    val name: String,
    val slot: IntakeSlot,
    val timeMinutes: Int,
    val isActive: Boolean
)

data class ScheduleWithMedications(
    val schedule: Schedule,
    val medications: List<Medication>
)

data class HistoryGroupKey(
    val scheduleId: ScheduleId?,
    val takenAt: TakenAt
)

data class HistoryGroup(
    val key: HistoryGroupKey,
    val scheduleName: String,
    val createdAt: Long,
    val medicationNames: List<String>,
    val incorrectAt: Long?
)
