package com.example.preventforgettingmedicationandroidapp.domain.repository

import com.example.preventforgettingmedicationandroidapp.domain.model.Medication
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId

data class SaveMedicationCommand(
    val medicationId: MedicationId?,
    val name: String,
    val mealTiming: com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming?,
    val memo: String?
)

interface MedicationRepository {
    suspend fun getMedicationMasters(): List<Medication>
    suspend fun getMedicationMaster(medicationId: MedicationId): Medication?
    suspend fun saveMedicationMaster(command: SaveMedicationCommand): MedicationId
    suspend fun deleteMedicationMaster(medicationId: MedicationId)
}
