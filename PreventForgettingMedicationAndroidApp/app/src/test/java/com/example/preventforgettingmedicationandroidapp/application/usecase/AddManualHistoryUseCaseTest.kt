package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.error.ValidationException
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.repository.AddManualResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class AddManualHistoryUseCaseTest {

    private val historyRepository = mockk<HistoryRepository>()
    private val widgetPort = mockk<WidgetPort>()

    private val useCase = AddManualHistoryUseCase(
        historyRepository = historyRepository,
        widgetPort = widgetPort
    )

    @Test
    fun `invoke rejects future datetime`() {
        val future = TakenAt(System.currentTimeMillis() + 60_000)
        assertThrows(ValidationException::class.java) {
            runTest { useCase(ScheduleId(1), future) }
        }
        coVerify(exactly = 0) { historyRepository.addManual(any(), any()) }
    }

    @Test
    fun `invoke refreshes history widget when saved`() = runTest {
        coEvery { historyRepository.addManual(any(), any()) } returns AddManualResult(savedCount = 1, skippedDuplicate = false)
        every { widgetPort.refreshHistory() } just runs

        useCase(ScheduleId(1), TakenAt(1_700_000_000_000L))

        coVerify(exactly = 1) { historyRepository.addManual(any(), any()) }
        verify(exactly = 1) { widgetPort.refreshHistory() }
    }
}