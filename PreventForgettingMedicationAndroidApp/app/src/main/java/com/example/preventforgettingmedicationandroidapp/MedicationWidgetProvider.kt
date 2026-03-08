package com.example.preventforgettingmedicationandroidapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import com.example.preventforgettingmedicationandroidapp.application.usecase.ExecuteScheduleUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.domain.model.TakenAt
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MedicationWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var executeScheduleUseCase: ExecuteScheduleUseCase

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            WidgetUtils.refreshMedicationWidgets(context)
        } catch (_: Exception) {
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        try {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        } catch (_: Exception) {
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.medication_widget)

            val serviceIntent = Intent(context, MedicationWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val clickIntent = Intent(context, MedicationWidgetProvider::class.java).apply {
                action = ACTION_TAKE_FROM_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingTemplate = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
            views.setPendingIntentTemplate(R.id.widget_list, pendingTemplate)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TAKE_FROM_WIDGET) return

        val scheduleIdRaw = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1)
        if (scheduleIdRaw <= 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = executeScheduleUseCase(ScheduleId(scheduleIdRaw), TakenAt.now())
                withContext(Dispatchers.Main) {
                    when {
                        result.skippedDisabled -> Toast.makeText(context, context.getString(R.string.schedule_disabled_temporarily), Toast.LENGTH_SHORT).show()
                        result.skippedDuplicate -> Toast.makeText(context, context.getString(R.string.duplicate_schedule_skipped), Toast.LENGTH_SHORT).show()
                        result.skippedMissing -> Toast.makeText(context, context.getString(R.string.no_schedules), Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, context.getString(R.string.taken_recorded_count, result.insertedCount), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                // ignore
            } finally {
                val awm = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, MedicationWidgetProvider::class.java)
                val ids = awm.getAppWidgetIds(cn)
                ids.forEach { id -> awm.notifyAppWidgetViewDataChanged(id, R.id.widget_list) }
                try {
                    WidgetUtils.refreshHistoryWidgets(context)
                } catch (_: Exception) {
                }
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKE_FROM_WIDGET = "com.example.preventforgettingmedicationandroidapp.ACTION_TAKE_FROM_WIDGET"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }
}