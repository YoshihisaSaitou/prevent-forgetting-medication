package com.example.preventforgettingmedicationandroidapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preventforgettingmedicationandroidapp.application.usecase.AddManualHistoryUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetHistoryGroupsUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetScheduleListUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.ToggleIncorrectUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.HistoryGroupKey
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import com.example.preventforgettingmedicationandroidapp.presentation.model.HistoryGroupItem
import com.example.preventforgettingmedicationandroidapp.presentation.model.ScheduleOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val groups: List<HistoryGroupItem> = emptyList(),
    val scheduleOptions: List<ScheduleOption> = emptyList()
)

sealed class HistoryEvent {
    data class Saved(val count: Int) : HistoryEvent()
    data object Duplicate : HistoryEvent()
    data class Error(val message: String) : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryGroupsUseCase: GetHistoryGroupsUseCase,
    private val getScheduleListUseCase: GetScheduleListUseCase,
    private val addManualHistoryUseCase: AddManualHistoryUseCase,
    private val toggleIncorrectUseCase: ToggleIncorrectUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events

    fun load() {
        viewModelScope.launch {
            runCatching {
                val groups = getHistoryGroupsUseCase().map {
                    HistoryGroupItem(
                        scheduleId = it.key.scheduleId,
                        scheduleName = it.scheduleName,
                        takenAt = it.key.takenAt.value,
                        createdAt = it.createdAt,
                        medicationNames = it.medicationNames,
                        incorrectAt = it.incorrectAt
                    )
                }

                val options = getScheduleListUseCase().mapNotNull { swm ->
                    val id = swm.schedule.id ?: return@mapNotNull null
                    ScheduleOption(
                        id = id,
                        name = swm.schedule.name,
                        slot = swm.schedule.slot,
                        timeMinutes = swm.schedule.timeMinutes
                    )
                }
                HistoryState(groups = groups, scheduleOptions = options)
            }.onSuccess {
                _state.value = it
            }.onFailure {
                _events.emit(HistoryEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun addManual(scheduleId: ScheduleId, takenAtMillis: Long) {
        viewModelScope.launch {
            runCatching {
                addManualHistoryUseCase(scheduleId, TakenAt(takenAtMillis))
            }.onSuccess { result ->
                if (result.skippedDuplicate) {
                    _events.emit(HistoryEvent.Duplicate)
                } else {
                    _events.emit(HistoryEvent.Saved(result.savedCount))
                }
                load()
            }.onFailure {
                _events.emit(HistoryEvent.Error(it.message ?: "Unknown error"))
            }
        }
    }

    fun toggleIncorrect(item: HistoryGroupItem) {
        viewModelScope.launch {
            runCatching {
                toggleIncorrectUseCase(
                    HistoryGroupKey(
                        scheduleId = item.scheduleId,
                        takenAt = TakenAt(item.takenAt)
                    )
                )
            }.onFailure {
                _events.emit(HistoryEvent.Error(it.message ?: "Unknown error"))
            }.also {
                load()
            }
        }
    }
}
