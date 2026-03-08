package com.example.preventforgettingmedicationandroidapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preventforgettingmedicationandroidapp.application.usecase.DeleteMedicationMasterUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetMedicationMastersUseCase
import com.example.preventforgettingmedicationandroidapp.domain.error.InUseException
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming
import com.example.preventforgettingmedicationandroidapp.presentation.model.MedicationOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MedicationMasterItem(
    val id: MedicationId,
    val name: String,
    val mealTiming: MealTiming?
)

sealed class MedicationMasterEvent {
    data object Deleted : MedicationMasterEvent()
    data object InUse : MedicationMasterEvent()
    data class Error(val message: String) : MedicationMasterEvent()
}

@HiltViewModel
class MedicationMasterViewModel @Inject constructor(
    private val getMedicationMastersUseCase: GetMedicationMastersUseCase,
    private val deleteMedicationMasterUseCase: DeleteMedicationMasterUseCase
) : ViewModel() {

    private val _items = MutableStateFlow<List<MedicationMasterItem>>(emptyList())
    val items: StateFlow<List<MedicationMasterItem>> = _items

    private val _events = MutableSharedFlow<MedicationMasterEvent>()
    val events: SharedFlow<MedicationMasterEvent> = _events

    fun load() {
        viewModelScope.launch {
            runCatching {
                getMedicationMastersUseCase().mapNotNull { med ->
                    val id = med.id ?: return@mapNotNull null
                    MedicationMasterItem(
                        id = id,
                        name = med.name,
                        mealTiming = med.mealTiming
                    )
                }
            }.onSuccess {
                _items.value = it
            }.onFailure {
                _events.emit(MedicationMasterEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun delete(id: MedicationId) {
        viewModelScope.launch {
            runCatching {
                deleteMedicationMasterUseCase(id)
            }.onSuccess {
                _events.emit(MedicationMasterEvent.Deleted)
                load()
            }.onFailure {
                if (it is InUseException) {
                    _events.emit(MedicationMasterEvent.InUse)
                } else {
                    _events.emit(MedicationMasterEvent.Error(it.message ?: "Unknown error"))
                }
            }
        }
    }
}
