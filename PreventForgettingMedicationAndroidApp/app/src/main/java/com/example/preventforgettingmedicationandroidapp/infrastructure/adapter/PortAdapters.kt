package com.example.preventforgettingmedicationandroidapp.infrastructure.adapter

import android.content.Context
import com.example.preventforgettingmedicationandroidapp.AlarmScheduler
import com.example.preventforgettingmedicationandroidapp.TakenStateStore
import com.example.preventforgettingmedicationandroidapp.WidgetUtils
import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class AlarmReminderAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderPort {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun syncAll() {
        ioScope.launch {
            AlarmScheduler.scheduleAll(context)
        }
    }

    override fun cancelAll() {
        ioScope.launch {
            AlarmScheduler.cancelAll(context)
        }
    }
}

@Singleton
class WidgetAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetPort {
    override fun refreshSchedules() {
        WidgetUtils.refreshMedicationWidgets(context)
    }

    override fun refreshHistory() {
        WidgetUtils.refreshHistoryWidgets(context)
    }
}

@Singleton
class TakenStateAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : TakenStatePort {
    override fun isEnabled(scheduleId: ScheduleId): Boolean =
        TakenStateStore.isEnabled(context, scheduleId.value)

    override fun disableForFiveMinutes(scheduleId: ScheduleId) {
        TakenStateStore.setDisabledForFiveMinutes(context, scheduleId.value)
    }
}