package com.example.preventforgettingmedicationandroidapp.infrastructure.repository

import com.example.preventforgettingmedicationandroidapp.IntakeHistory
import com.example.preventforgettingmedicationandroidapp.IntakeHistoryDao
import com.example.preventforgettingmedicationandroidapp.IntakeSlot as EntityIntakeSlot
import com.example.preventforgettingmedicationandroidapp.Medication as EntityMedication
import com.example.preventforgettingmedicationandroidapp.Schedule as EntitySchedule
import com.example.preventforgettingmedicationandroidapp.ScheduleDao
import com.example.preventforgettingmedicationandroidapp.ScheduleMedicationCrossRef
import com.example.preventforgettingmedicationandroidapp.ScheduleWithMedications as EntityScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.Medication
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.model.Schedule
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleWithMedications
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.domain.repository.ExecuteResult
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveScheduleCommand
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val historyDao: IntakeHistoryDao,
    private val takenStatePort: TakenStatePort
) : ScheduleRepository {

    override suspend fun getSchedules(): List<ScheduleWithMedications> =
        scheduleDao.getAllWithMedications().map { it.toDomain() }

    override suspend fun getSchedule(scheduleId: ScheduleId): ScheduleWithMedications? =
        scheduleDao.getWithMedicationsById(scheduleId.value)?.toDomain()

    override suspend fun saveSchedule(command: SaveScheduleCommand): ScheduleId {
        val entity = EntitySchedule(
            id = command.scheduleId?.value ?: 0,
            name = command.name.trim(),
            slot = command.slot.toEntity(),
            timeMinutes = command.timeMinutes,
            isActive = command.isActive
        )

        val targetId = if (command.scheduleId == null) {
            scheduleDao.insert(entity).toInt()
        } else {
            scheduleDao.update(entity)
            command.scheduleId.value
        }

        scheduleDao.deleteCrossRefsForSchedule(targetId)
        val refs = command.medicationIds.distinct().mapIndexed { index, medId ->
            ScheduleMedicationCrossRef(
                scheduleId = targetId,
                medicationId = medId,
                displayOrder = index
            )
        }
        scheduleDao.insertCrossRefs(refs)

        return ScheduleId(targetId)
    }

    override suspend fun deleteSchedule(scheduleId: ScheduleId) {
        scheduleDao.deleteCrossRefsForSchedule(scheduleId.value)
        scheduleDao.deleteById(scheduleId.value)
    }

    override suspend fun execute(scheduleId: ScheduleId, takenAt: TakenAt): ExecuteResult {
        val scheduleWithMeds = scheduleDao.getWithMedicationsById(scheduleId.value)
            ?: return ExecuteResult(
                insertedCount = 0,
                skippedDuplicate = false,
                skippedDisabled = false,
                skippedMissing = true
            )

        if (scheduleWithMeds.medications.isEmpty()) {
            return ExecuteResult(
                insertedCount = 0,
                skippedDuplicate = false,
                skippedDisabled = false,
                skippedMissing = true
            )
        }

        if (!takenStatePort.isEnabled(scheduleId)) {
            return ExecuteResult(
                insertedCount = 0,
                skippedDuplicate = false,
                skippedDisabled = true,
                skippedMissing = false
            )
        }

        val exists = historyDao.existsScheduleEntry(scheduleId.value, takenAt.value)
        if (exists) {
            return ExecuteResult(
                insertedCount = 0,
                skippedDuplicate = true,
                skippedDisabled = false,
                skippedMissing = false
            )
        }

        val entries = scheduleWithMeds.medications.map {
            IntakeHistory(
                scheduleId = scheduleId.value,
                scheduleName = scheduleWithMeds.schedule.name,
                medicationId = it.id,
                medicationName = it.name,
                takenAt = takenAt.value,
                createdAt = System.currentTimeMillis()
            )
        }

        historyDao.insertAll(entries)
        takenStatePort.disableForFiveMinutes(scheduleId)

        return ExecuteResult(
            insertedCount = entries.size,
            skippedDuplicate = false,
            skippedDisabled = false,
            skippedMissing = false
        )
    }

    private fun EntityScheduleWithMedications.toDomain(): ScheduleWithMedications {
        return ScheduleWithMedications(
            schedule = Schedule(
                id = ScheduleId(schedule.id),
                name = schedule.name,
                slot = schedule.slot.toDomain(),
                timeMinutes = schedule.timeMinutes,
                isActive = schedule.isActive
            ),
            medications = medications.map { med ->
                Medication(
                    id = MedicationId(med.id),
                    name = med.name,
                    mealTiming = med.mealTiming?.toDomain(),
                    memo = med.memo
                )
            }
        )
    }

    private fun IntakeSlot.toEntity(): EntityIntakeSlot = when (this) {
        IntakeSlot.MORNING -> EntityIntakeSlot.MORNING
        IntakeSlot.NOON -> EntityIntakeSlot.NOON
        IntakeSlot.EVENING -> EntityIntakeSlot.EVENING
    }

    private fun EntityIntakeSlot.toDomain(): IntakeSlot = when (this) {
        EntityIntakeSlot.MORNING -> IntakeSlot.MORNING
        EntityIntakeSlot.NOON -> IntakeSlot.NOON
        EntityIntakeSlot.EVENING -> IntakeSlot.EVENING
    }

    private fun com.example.preventforgettingmedicationandroidapp.MealTiming.toDomain(): com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming =
        when (this) {
            com.example.preventforgettingmedicationandroidapp.MealTiming.BEFORE_MEAL -> com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming.BEFORE_MEAL
            com.example.preventforgettingmedicationandroidapp.MealTiming.AFTER_MEAL -> com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming.AFTER_MEAL
        }
}
