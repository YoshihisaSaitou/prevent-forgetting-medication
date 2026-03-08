package com.example.preventforgettingmedicationandroidapp.infrastructure.repository

import com.example.preventforgettingmedicationandroidapp.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.Medication as EntityMedication
import com.example.preventforgettingmedicationandroidapp.MedicationDao
import com.example.preventforgettingmedicationandroidapp.MealTiming as EntityMealTiming
import com.example.preventforgettingmedicationandroidapp.domain.error.InUseException
import com.example.preventforgettingmedicationandroidapp.domain.error.NotFoundException
import com.example.preventforgettingmedicationandroidapp.domain.model.Medication
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming
import com.example.preventforgettingmedicationandroidapp.domain.repository.MedicationRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveMedicationCommand
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao
) : MedicationRepository {

    override suspend fun getMedicationMasters(): List<Medication> =
        medicationDao.getAll().map { it.toDomain() }

    override suspend fun getMedicationMaster(medicationId: MedicationId): Medication? =
        medicationDao.getById(medicationId.value)?.toDomain()

    override suspend fun saveMedicationMaster(command: SaveMedicationCommand): MedicationId {
        val base = command.medicationId?.let { medicationDao.getById(it.value) }
        val entity = EntityMedication(
            id = command.medicationId?.value ?: 0,
            name = command.name.trim(),
            mealTiming = command.mealTiming?.toEntity(),
            timing = base?.timing ?: setOf(IntakeSlot.MORNING),
            memo = command.memo?.takeIf { it.isNotBlank() },
            useAppTimes = base?.useAppTimes ?: true,
            morningMinutes = base?.morningMinutes,
            noonMinutes = base?.noonMinutes,
            eveningMinutes = base?.eveningMinutes
        )

        val id = if (command.medicationId == null) {
            medicationDao.insert(entity).toInt()
        } else {
            medicationDao.update(entity)
            command.medicationId.value
        }

        return MedicationId(id)
    }

    override suspend fun deleteMedicationMaster(medicationId: MedicationId) {
        val refCount = medicationDao.countScheduleReferences(medicationId.value)
        if (refCount > 0) {
            throw InUseException("Medication is used by schedules")
        }

        val target = medicationDao.getById(medicationId.value)
            ?: throw NotFoundException("Medication not found: ${medicationId.value}")

        medicationDao.delete(target)
    }

    private fun EntityMedication.toDomain(): Medication = Medication(
        id = MedicationId(id),
        name = name,
        mealTiming = mealTiming?.toDomain(),
        memo = memo
    )

    private fun MealTiming.toEntity(): EntityMealTiming = when (this) {
        MealTiming.BEFORE_MEAL -> EntityMealTiming.BEFORE_MEAL
        MealTiming.AFTER_MEAL -> EntityMealTiming.AFTER_MEAL
    }

    private fun EntityMealTiming.toDomain(): MealTiming = when (this) {
        EntityMealTiming.BEFORE_MEAL -> MealTiming.BEFORE_MEAL
        EntityMealTiming.AFTER_MEAL -> MealTiming.AFTER_MEAL
    }
}
