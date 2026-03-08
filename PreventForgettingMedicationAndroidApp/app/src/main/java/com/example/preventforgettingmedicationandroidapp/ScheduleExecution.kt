package com.example.preventforgettingmedicationandroidapp

import android.content.Context

data class ScheduleExecutionResult(
    val inserted: Int,
    val skippedDuplicate: Boolean,
    val skippedDisabled: Boolean
)

object ScheduleExecution {
    suspend fun executeNow(
        context: Context,
        scheduleWithMeds: ScheduleWithMedications,
        takenAt: Long = System.currentTimeMillis()
    ): ScheduleExecutionResult {
        val schedule = scheduleWithMeds.schedule
        if (!TakenStateStore.isEnabled(context, schedule.id)) {
            return ScheduleExecutionResult(inserted = 0, skippedDuplicate = false, skippedDisabled = true)
        }

        val meds = scheduleWithMeds.medications
        if (meds.isEmpty()) {
            return ScheduleExecutionResult(inserted = 0, skippedDuplicate = true, skippedDisabled = false)
        }

        val historyDao = MedicationDatabase.getInstance(context).intakeHistoryDao()
        val exists = historyDao.existsScheduleEntry(schedule.id, takenAt)
        if (exists) {
            return ScheduleExecutionResult(inserted = 0, skippedDuplicate = true, skippedDisabled = false)
        }

        val entries = meds.map {
            IntakeHistory(
                scheduleId = schedule.id,
                scheduleName = schedule.name,
                medicationId = it.id,
                medicationName = it.name,
                takenAt = takenAt,
                createdAt = System.currentTimeMillis()
            )
        }
        historyDao.insertAll(entries)
        TakenStateStore.setDisabledForFiveMinutes(context, schedule.id)

        WidgetUtils.refreshMedicationWidgets(context)
        WidgetUtils.refreshHistoryWidgets(context)
        return ScheduleExecutionResult(inserted = entries.size, skippedDuplicate = false, skippedDisabled = false)
    }
}
