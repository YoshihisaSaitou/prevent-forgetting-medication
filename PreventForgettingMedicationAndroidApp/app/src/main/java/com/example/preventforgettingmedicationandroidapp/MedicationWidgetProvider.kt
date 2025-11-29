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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Force refresh when the first instance is added
        try { WidgetUtils.refreshMedicationWidgets(context) } catch (_: Exception) {}
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Force refresh on placement/resizing
        try { appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list) } catch (_: Exception) {}
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.medication_widget)

            // Set up the collection adapter
            val serviceIntent = Intent(context, MedicationWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Set a pending intent template for item buttons
            val clickIntent = Intent(context, MedicationWidgetProvider::class.java).apply {
                action = ACTION_TAKE_FROM_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            // Use MUTABLE so fill-in extras from collection items are merged
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingTemplate = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
            views.setPendingIntentTemplate(R.id.widget_list, pendingTemplate)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TAKE_FROM_WIDGET) {
            val medId = intent.getIntExtra(EXTRA_MED_ID, -1)
            val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: ""
            if (medId != -1) {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MedicationDatabase.getInstance(context)
                        val now = System.currentTimeMillis()
                        db.intakeHistoryDao().insert(
                            IntakeHistory(
                                medicationId = medId,
                                medicationName = medName,
                                takenAt = now,
                                createdAt = now
                            )
                        )
                        TakenStateStore.setDisabledForFiveMinutes(context, medId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.taken_recorded), Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        // ignore
                    } finally {
                        // Refresh all widgets
                        val awm = AppWidgetManager.getInstance(context)
                        val cn = ComponentName(context, MedicationWidgetProvider::class.java)
                        val ids = awm.getAppWidgetIds(cn)
                        ids.forEach { id ->
                            awm.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
                        }
                        // Also refresh history widgets to reflect the new entry
                        try {
                            WidgetUtils.refreshHistoryWidgets(context)
                        } catch (_: Exception) {}
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TAKE_FROM_WIDGET = "com.example.preventforgettingmedicationandroidapp.ACTION_TAKE_FROM_WIDGET"
        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
    }
}
