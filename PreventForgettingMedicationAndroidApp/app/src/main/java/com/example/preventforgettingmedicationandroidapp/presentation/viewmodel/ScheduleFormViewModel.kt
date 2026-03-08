package com.example.preventforgettingmedicationandroidapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preventforgettingmedicationandroidapp.application.usecase.CreateOrUpdateScheduleUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetMedicationMastersUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetScheduleByIdUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveScheduleCommand
import com.example.preventforgettingmedicationandroidapp.presentation.model.MedicationOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScheduleFormState(
    val scheduleId: ScheduleId? = null,
    val name: String = "",
    val slot: IntakeSlot = IntakeSlot.MORNING,
    val timeMinutes: Int = 7 * 60,
    val selectedMedicationIds: Set<Int> = emptySet(),
    val medicationOptions: List<MedicationOption> = emptyList()
)

sealed class ScheduleFormEvent {
    data object Saved : ScheduleFormEvent()
    data class Error(val message: String) : ScheduleFormEvent()
}

@HiltViewModel
class ScheduleFormViewModel @Inject constructor(
    private val getMedicationMastersUseCase: GetMedicationMastersUseCase,
    private val getScheduleByIdUseCase: GetScheduleByIdUseCase,
    private val createOrUpdateScheduleUseCase: CreateOrUpdateScheduleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleFormState())
    val state: StateFlow<ScheduleFormState> = _state

    private val _events = MutableSharedFlow<ScheduleFormEvent>()
    val events: SharedFlow<ScheduleFormEvent> = _events

    fun load(scheduleIdRaw: Int?) {
        viewModelScope.launch {
            runCatching {
                val meds = getMedicationMastersUseCase().map {
                    MedicationOption(id = it.id?.value ?: 0, name = it.name)
                }.filter { it.id > 0 }

                if (scheduleIdRaw == null || scheduleIdRaw <= 0) {
                    val defaultMinutes = when (_state.value.slot) {
                        IntakeSlot.MORNING -> 7 * 60
                        IntakeSlot.NOON -> 12 * 60
                        IntakeSlot.EVENING -> 19 * 60
                    }
                    _state.value.copy(
                        scheduleId = null,
                        timeMinutes = defaultMinutes,
                        medicationOptions = meds
                    )
                } else {
                    val schedule = getScheduleByIdUseCase(ScheduleId(scheduleIdRaw))
                    if (schedule == null) {
                        _state.value.copy(medicationOptions = meds)
                    } else {
                        _state.value.copy(
                            scheduleId = schedule.schedule.id,
                            name = schedule.schedule.name,
                            slot = schedule.schedule.slot,
                            timeMinutes = schedule.schedule.timeMinutes,
                            selectedMedicationIds = schedule.medications.mapNotNull { it.id?.value }.toSet(),
                            medicationOptions = meds
                        )
                    }
                }
            }.onSuccess { _state.value = it }
                .onFailure {
                    _events.emit(ScheduleFormEvent.Error(it.message ?: "Unknown error"))
                }
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun setSlot(slot: IntakeSlot) {
        _state.value = _state.value.copy(slot = slot)
    }

    fun setTimeMinutes(minutes: Int) {
        _state.value = _state.value.copy(timeMinutes = minutes)
    }

    fun setSelectedMedicationIds(ids: Set<Int>) {
        _state.value = _state.value.copy(selectedMedicationIds = ids)
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            runCatching {
                createOrUpdateScheduleUseCase(
                    SaveScheduleCommand(
                        scheduleId = s.scheduleId,
                        name = s.name,
                        slot = s.slot,
                        timeMinutes = s.timeMinutes,
                        medicationIds = s.selectedMedicationIds.toList(),
                        isActive = true
                    )
                )
            }.onSuccess {
                _events.emit(ScheduleFormEvent.Saved)
            }.onFailure {
                _events.emit(ScheduleFormEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }
}
