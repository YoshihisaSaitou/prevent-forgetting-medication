package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.error.ValidationException
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveScheduleCommand
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CreateOrUpdateScheduleUseCaseTest {

    private val scheduleRepository = mockk<ScheduleRepository>()
    private val reminderPort = mockk<ReminderPort>()
    private val widgetPort = mockk<WidgetPort>()

    private val useCase = CreateOrUpdateScheduleUseCase(
        scheduleRepository = scheduleRepository,
        reminderPort = reminderPort,
        widgetPort = widgetPort
    )

    @Test
    fun `invoke saves schedule and syncs reminder widgets`() = runTest {
        val command = SaveScheduleCommand(
            scheduleId = null,
            name = "Morning meds",
            slot = IntakeSlot.MORNING,
            timeMinutes = 420,
            medicationIds = listOf(1, 2),
            isActive = true
        )

        coEvery { scheduleRepository.saveSchedule(command) } returns ScheduleId(10)
        every { reminderPort.syncAll() } just runs
        every { widgetPort.refreshSchedules() } just runs

        val actual = useCase(command)

        assertEquals(10, actual.value)
        coVerify(exactly = 1) { scheduleRepository.saveSchedule(command) }
        verify(exactly = 1) { reminderPort.syncAll() }
        verify(exactly = 1) { widgetPort.refreshSchedules() }
    }

    @Test
    fun `invoke throws when medication list is empty`() = runTest {
        val command = SaveScheduleCommand(
            scheduleId = null,
            name = "Morning meds",
            slot = IntakeSlot.MORNING,
            timeMinutes = 420,
            medicationIds = emptyList(),
            isActive = true
        )

        var thrown: ValidationException? = null
        try {
            useCase(command)
        } catch (e: ValidationException) {
            thrown = e
        }

        assertNotNull(thrown)
        coVerify(exactly = 0) { scheduleRepository.saveSchedule(any()) }
    }
}