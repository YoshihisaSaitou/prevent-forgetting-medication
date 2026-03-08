package com.example.preventforgettingmedicationandroidapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetMedicationMasterUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.SaveMedicationMasterUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.repository.SaveMedicationCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MedicationMasterEditState(
    val medicationId: MedicationId? = null,
    val name: String = "",
    val memo: String = "",
    val mealTiming: MealTiming? = null
)

sealed class MedicationMasterEditEvent {
    data object Saved : MedicationMasterEditEvent()
    data class Error(val message: String) : MedicationMasterEditEvent()
}

@HiltViewModel
class MedicationMasterEditViewModel @Inject constructor(
    private val getMedicationMasterUseCase: GetMedicationMasterUseCase,
    private val saveMedicationMasterUseCase: SaveMedicationMasterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationMasterEditState())
    val state: StateFlow<MedicationMasterEditState> = _state

    private val _events = MutableSharedFlow<MedicationMasterEditEvent>()
    val events: SharedFlow<MedicationMasterEditEvent> = _events

    fun load(medicationIdRaw: Int?) {
        viewModelScope.launch {
            if (medicationIdRaw == null || medicationIdRaw <= 0) return@launch
            runCatching {
                getMedicationMasterUseCase(MedicationId(medicationIdRaw))
            }.onSuccess { med ->
                if (med != null) {
                    _state.value = MedicationMasterEditState(
                        medicationId = med.id,
                        name = med.name,
                        memo = med.memo ?: "",
                        mealTiming = med.mealTiming
                    )
                }
            }.onFailure {
                _events.emit(MedicationMasterEditEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun setMemo(value: String) {
        _state.value = _state.value.copy(memo = value)
    }

    fun setMealTiming(value: MealTiming?) {
        _state.value = _state.value.copy(mealTiming = value)
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            runCatching {
                saveMedicationMasterUseCase(
                    SaveMedicationCommand(
                        medicationId = s.medicationId,
                        name = s.name,
                        mealTiming = s.mealTiming,
                        memo = s.memo
                    )
                )
            }.onSuccess {
                _events.emit(MedicationMasterEditEvent.Saved)
            }.onFailure {
                _events.emit(MedicationMasterEditEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }
}
