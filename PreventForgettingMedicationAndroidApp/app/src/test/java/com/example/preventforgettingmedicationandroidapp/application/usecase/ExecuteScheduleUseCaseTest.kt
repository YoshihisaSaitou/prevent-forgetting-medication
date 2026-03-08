package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.repository.ExecuteResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExecuteScheduleUseCaseTest {

    private val scheduleRepository = mockk<ScheduleRepository>()
    private val widgetPort = mockk<WidgetPort>()

    private val useCase = ExecuteScheduleUseCase(
        scheduleRepository = scheduleRepository,
        widgetPort = widgetPort
    )

    @Test
    fun `invoke refreshes widgets when records inserted`() = runTest {
        val scheduleId = ScheduleId(1)
        val takenAt = TakenAt(1_700_000_000_000)
        val result = ExecuteResult(
            insertedCount = 2,
            skippedDuplicate = false,
            skippedDisabled = false,
            skippedMissing = false
        )
        coEvery { scheduleRepository.execute(scheduleId, takenAt) } returns result
        every { widgetPort.refreshSchedules() } just runs
        every { widgetPort.refreshHistory() } just runs

        useCase(scheduleId, takenAt)

        coVerify(exactly = 1) { scheduleRepository.execute(scheduleId, takenAt) }
        verify(exactly = 1) { widgetPort.refreshSchedules() }
        verify(exactly = 1) { widgetPort.refreshHistory() }
    }

    @Test
    fun `invoke does not refresh widgets when skipped`() = runTest {
        val scheduleId = ScheduleId(1)
        val takenAt = TakenAt(1_700_000_000_000)
        val result = ExecuteResult(
            insertedCount = 0,
            skippedDuplicate = true,
            skippedDisabled = false,
            skippedMissing = false
        )
        coEvery { scheduleRepository.execute(scheduleId, takenAt) } returns result

        useCase(scheduleId, takenAt)

        verify(exactly = 0) { widgetPort.refreshSchedules() }
        verify(exactly = 0) { widgetPort.refreshHistory() }
    }
}