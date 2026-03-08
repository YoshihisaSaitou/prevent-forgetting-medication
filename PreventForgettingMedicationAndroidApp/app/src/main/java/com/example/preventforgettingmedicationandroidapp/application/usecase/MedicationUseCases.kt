package com.example.preventforgettingmedicationandroidapp.application.usecase

import com.example.preventforgettingmedicationandroidapp.domain.model.Medication
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.policy.IntakePolicy
import com.example.preventforgettingmedicationandroidapp.domain.repository.MedicationRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveMedicationCommand
import javax.inject.Inject

class GetMedicationMastersUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(): List<Medication> = medicationRepository.getMedicationMasters()
}

class GetMedicationMasterUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(medicationId: MedicationId): Medication? =
        medicationRepository.getMedicationMaster(medicationId)
}

class SaveMedicationMasterUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(command: SaveMedicationCommand): MedicationId {
        IntakePolicy.validateMedicationName(command.name)
        return medicationRepository.saveMedicationMaster(command)
    }
}

class DeleteMedicationMasterUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(medicationId: MedicationId) {
        medicationRepository.deleteMedicationMaster(medicationId)
    }
}
