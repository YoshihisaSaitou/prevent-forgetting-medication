package com.example.preventforgettingmedicationandroidapp.infrastructure.repository

import com.example.preventforgettingmedicationandroidapp.IntakeHistoryDao
import com.example.preventforgettingmedicationandroidapp.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.Medication
import com.example.preventforgettingmedicationandroidapp.MealTiming
import com.example.preventforgettingmedicationandroidapp.Schedule
import com.example.preventforgettingmedicationandroidapp.ScheduleDao
import com.example.preventforgettingmedicationandroidapp.ScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomScheduleRepositoryTest {

    private val scheduleDao = mockk<ScheduleDao>()
    private val historyDao = mockk<IntakeHistoryDao>()
    private val takenStatePort = mockk<TakenStatePort>()

    private val repository = RoomScheduleRepository(
        scheduleDao = scheduleDao,
        historyDao = historyDao,
        takenStatePort = takenStatePort
    )

    @Test
    fun `execute inserts history and disables schedule for five minutes`() = runTest {
        val scheduleId = ScheduleId(1)
        val takenAt = TakenAt(1_700_000_000_000L)

        coEvery { scheduleDao.getWithMedicationsById(1) } returns sampleScheduleWithMedication()
        every { takenStatePort.isEnabled(scheduleId) } returns true
        coEvery { historyDao.existsScheduleEntry(1, takenAt.value) } returns false
        coEvery { historyDao.insertAll(any()) } just runs
        every { takenStatePort.disableForFiveMinutes(scheduleId) } just runs

        val result = repository.execute(scheduleId, takenAt)

        assertEquals(1, result.insertedCount)
        assertTrue(!result.skippedDisabled)
        assertTrue(!result.skippedDuplicate)
        coVerify(exactly = 1) { historyDao.insertAll(any()) }
        verify(exactly = 1) { takenStatePort.disableForFiveMinutes(scheduleId) }
    }

    @Test
    fun `execute skips duplicate when same schedule and timestamp already exists`() = runTest {
        val scheduleId = ScheduleId(1)
        val takenAt = TakenAt(1_700_000_000_000L)

        coEvery { scheduleDao.getWithMedicationsById(1) } returns sampleScheduleWithMedication()
        every { takenStatePort.isEnabled(scheduleId) } returns true
        coEvery { historyDao.existsScheduleEntry(1, takenAt.value) } returns true

        val result = repository.execute(scheduleId, takenAt)

        assertTrue(result.skippedDuplicate)
        coVerify(exactly = 0) { historyDao.insertAll(any()) }
        verify(exactly = 0) { takenStatePort.disableForFiveMinutes(any()) }
    }

    private fun sampleScheduleWithMedication(): ScheduleWithMedications {
        return ScheduleWithMedications(
            schedule = Schedule(
                id = 1,
                name = "Morning meds",
                slot = IntakeSlot.MORNING,
                timeMinutes = 420,
                isActive = true
            ),
            medications = listOf(
                Medication(
                    id = 1,
                    name = "Aspirin",
                    mealTiming = MealTiming.AFTER_MEAL,
                    timing = setOf(IntakeSlot.MORNING),
                    memo = null
                )
            )
        )
    }
}