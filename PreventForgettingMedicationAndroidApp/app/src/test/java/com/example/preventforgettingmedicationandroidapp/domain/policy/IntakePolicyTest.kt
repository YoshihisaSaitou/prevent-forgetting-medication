package com.example.preventforgettingmedicationandroidapp.domain.policy

import com.example.preventforgettingmedicationandroidapp.domain.error.ValidationException
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import org.junit.Assert.assertThrows
import org.junit.Test

class IntakePolicyTest {

    @Test
    fun `validateTimeMinutes throws when out of range`() {
        assertThrows(ValidationException::class.java) {
            IntakePolicy.validateTimeMinutes(1440)
        }
    }

    @Test
    fun `validateManualTakenAt throws for future datetime`() {
        val now = 1_700_000_000_000L
        assertThrows(ValidationException::class.java) {
            IntakePolicy.validateManualTakenAt(TakenAt(now + 1), now)
        }
    }

    @Test
    fun `validateMedicationSelection throws when empty`() {
        assertThrows(ValidationException::class.java) {
            IntakePolicy.validateMedicationSelection(emptyList())
        }
    }
}