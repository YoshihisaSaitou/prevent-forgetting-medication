package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import javax.inject.Inject

class SyncAlarmsUseCase @Inject constructor(
    private val reminderPort: ReminderPort
) {
    operator fun invoke() {
        reminderPort.syncAll()
    }
}

class CancelAlarmsUseCase @Inject constructor(
    private val reminderPort: ReminderPort
) {
    operator fun invoke() {
        reminderPort.cancelAll()
    }
}
