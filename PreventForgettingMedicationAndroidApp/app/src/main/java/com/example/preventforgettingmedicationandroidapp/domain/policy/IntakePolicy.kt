package com.example.preventforgettingmedicationandroidapp.domain.policy

import com.example.preventforgettingmedicationandroidapp.domain.error.ValidationException
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt

object IntakePolicy {
    fun validateTimeMinutes(timeMinutes: Int) {
        if (timeMinutes !in 0..1439) {
            throw ValidationException("timeMinutes must be in 0..1439")
        }
    }

    fun validateScheduleName(name: String) {
        if (name.isBlank()) {
            throw ValidationException("Schedule name is required")
        }
    }

    fun validateMedicationName(name: String) {
        if (name.isBlank()) {
            throw ValidationException("Medication name is required")
        }
    }

    fun validateManualTakenAt(takenAt: TakenAt, nowMillis: Long = System.currentTimeMillis()) {
        if (takenAt.value > nowMillis) {
            throw ValidationException("Future time is not allowed")
        }
    }

    fun validateMedicationSelection(ids: Collection<Int>) {
        if (ids.isEmpty()) {
            throw ValidationException("At least one medication must be selected")
        }
    }
}
