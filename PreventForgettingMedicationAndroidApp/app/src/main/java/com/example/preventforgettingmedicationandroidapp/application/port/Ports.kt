package com.example.preventforgettingmedicationandroidapp.application.port

import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId

interface ReminderPort {
    fun syncAll()
    fun cancelAll()
}

interface WidgetPort {
    fun refreshSchedules()
    fun refreshHistory()
}

interface TakenStatePort {
    fun isEnabled(scheduleId: ScheduleId): Boolean
    fun disableForFiveMinutes(scheduleId: ScheduleId)
}
