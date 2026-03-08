package com.example.preventforgettingmedicationandroidapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.application.usecase.DeleteScheduleUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.ExecuteScheduleUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetScheduleListUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.presentation.model.ScheduleListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MainEvent {
    data class Recorded(val count: Int) : MainEvent()
    data object Duplicate : MainEvent()
    data object Disabled : MainEvent()
    data object Missing : MainEvent()
    data class Error(val message: String) : MainEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getScheduleListUseCase: GetScheduleListUseCase,
    private val executeScheduleUseCase: ExecuteScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val takenStatePort: TakenStatePort
) : ViewModel() {

    private val _items = MutableStateFlow<List<ScheduleListItem>>(emptyList())
    val items: StateFlow<List<ScheduleListItem>> = _items

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events

    fun load() {
        viewModelScope.launch {
            runCatching {
                getScheduleListUseCase()
            }.onSuccess { data ->
                _items.value = data.mapNotNull {
                    val scheduleId = it.schedule.id ?: return@mapNotNull null
                    ScheduleListItem(
                        id = scheduleId,
                        name = it.schedule.name,
                        slot = it.schedule.slot,
                        timeMinutes = it.schedule.timeMinutes,
                        medicationNames = it.medications.map { med -> med.name },
                        canExecute = takenStatePort.isEnabled(scheduleId)
                    )
                }
            }.onFailure {
                _events.emit(MainEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun execute(scheduleId: ScheduleId) {
        viewModelScope.launch {
            runCatching {
                executeScheduleUseCase(scheduleId, TakenAt.now())
            }.onSuccess { result ->
                when {
                    result.skippedDisabled -> _events.emit(MainEvent.Disabled)
                    result.skippedDuplicate -> _events.emit(MainEvent.Duplicate)
                    result.skippedMissing -> _events.emit(MainEvent.Missing)
                    else -> _events.emit(MainEvent.Recorded(result.insertedCount))
                }
                load()
            }.onFailure {
                _events.emit(MainEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun delete(scheduleId: ScheduleId) {
        viewModelScope.launch {
            runCatching {
                deleteScheduleUseCase(scheduleId)
            }.onSuccess {
                load()
            }.onFailure {
                _events.emit(MainEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }
}
