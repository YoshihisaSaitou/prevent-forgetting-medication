package com.example.preventforgettingmedicationandroidapp

import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.application.usecase.CreateOrUpdateScheduleUseCase
import com.example.preventforgettingmedicationandroidapp.di.BindingModule
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroup
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroupKey
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.Medication
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.model.Schedule
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.repository.AddManualResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.ExecuteResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.HistoryRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.MedicationRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveMedicationCommand
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveScheduleCommand
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(BindingModule::class)
class HiltRepositoryOverrideTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val fakeScheduleRepository = FakeScheduleRepository()
    private val fakeHistoryRepository = FakeHistoryRepository()
    private val fakeMedicationRepository = FakeMedicationRepository()
    private val fakeReminderPort = FakeReminderPort()
    private val fakeWidgetPort = FakeWidgetPort()
    private val fakeTakenStatePort = FakeTakenStatePort()

    @BindValue
    @JvmField
    val scheduleRepository: ScheduleRepository = fakeScheduleRepository

    @BindValue
    @JvmField
    val historyRepository: HistoryRepository = fakeHistoryRepository

    @BindValue
    @JvmField
    val medicationRepository: MedicationRepository = fakeMedicationRepository

    @BindValue
    @JvmField
    val reminderPort: ReminderPort = fakeReminderPort

    @BindValue
    @JvmField
    val widgetPort: WidgetPort = fakeWidgetPort

    @BindValue
    @JvmField
    val takenStatePort: TakenStatePort = fakeTakenStatePort

    @Inject
    lateinit var createOrUpdateScheduleUseCase: CreateOrUpdateScheduleUseCase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun createSchedule_usesBoundFakeRepositoryAndPorts() = runBlocking {
        createOrUpdateScheduleUseCase(
            SaveScheduleCommand(
                scheduleId = null,
                name = "Injected schedule",
                slot = IntakeSlot.MORNING,
                timeMinutes = 420,
                medicationIds = listOf(1),
                isActive = true
            )
        )

        assertEquals("Injected schedule", fakeScheduleRepository.lastSavedCommand?.name)
        assertTrue(fakeReminderPort.syncCalled)
        assertTrue(fakeWidgetPort.refreshSchedulesCalled)
    }

    private class FakeScheduleRepository : ScheduleRepository {
        var lastSavedCommand: SaveScheduleCommand? = null
        private val schedules = mutableListOf<ScheduleWithMedications>()

        override suspend fun getSchedules(): List<ScheduleWithMedications> = schedules.toList()

        override suspend fun getSchedule(scheduleId: ScheduleId): ScheduleWithMedications? =
            schedules.firstOrNull { it.schedule.id == scheduleId }

        override suspend fun saveSchedule(command: SaveScheduleCommand): ScheduleId {
            lastSavedCommand = command
            val id = command.scheduleId ?: ScheduleId(100)
            schedules.removeAll { it.schedule.id == id }
            schedules.add(
                ScheduleWithMedications(
                    schedule = Schedule(
                        id = id,
                        name = command.name,
                        slot = command.slot,
                        timeMinutes = command.timeMinutes,
                        isActive = command.isActive
                    ),
                    medications = command.medicationIds.map {
                        Medication(
                            id = MedicationId(it),
                            name = "Medication $it",
                            mealTiming = null,
                            memo = null
                        )
                    }
                )
            )
            return id
        }

        override suspend fun deleteSchedule(scheduleId: ScheduleId) {
            schedules.removeAll { it.schedule.id == scheduleId }
        }

        override suspend fun execute(scheduleId: ScheduleId, takenAt: TakenAt): ExecuteResult =
            ExecuteResult(insertedCount = 1, skippedDuplicate = false, skippedDisabled = false, skippedMissing = false)
    }

    private class FakeHistoryRepository : HistoryRepository {
        override suspend fun getHistoryGroups(): List<HistoryGroup> = emptyList()

        override suspend fun addManual(scheduleId: ScheduleId, takenAt: TakenAt): AddManualResult =
            AddManualResult(savedCount = 1, skippedDuplicate = false)

        override suspend fun toggleIncorrect(groupKey: HistoryGroupKey): Long? = null
    }

    private class FakeMedicationRepository : MedicationRepository {
        override suspend fun getMedicationMasters(): List<Medication> = emptyList()

        override suspend fun getMedicationMaster(medicationId: MedicationId): Medication? = null

        override suspend fun saveMedicationMaster(command: SaveMedicationCommand): MedicationId =
            command.medicationId ?: MedicationId(1)

        override suspend fun deleteMedicationMaster(medicationId: MedicationId) = Unit
    }

    private class FakeReminderPort : ReminderPort {
        var syncCalled: Boolean = false
        override fun syncAll() {
            syncCalled = true
        }

        override fun cancelAll() = Unit
    }

    private class FakeWidgetPort : WidgetPort {
        var refreshSchedulesCalled: Boolean = false
        override fun refreshSchedules() {
            refreshSchedulesCalled = true
        }

        override fun refreshHistory() = Unit
    }

    private class FakeTakenStatePort : TakenStatePort {
        override fun isEnabled(scheduleId: ScheduleId): Boolean = true

        override fun disableForFiveMinutes(scheduleId: ScheduleId) = Unit
    }
}