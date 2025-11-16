package com.example.preventforgettingmedicationandroidapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

class HistoryWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Force refresh right after the first widget instance is placed
        try { WidgetUtils.refreshHistoryWidgets(context) } catch (_: Exception) {}
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Force refresh when options change (often called right after placement)
        try { appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list) } catch (_: Exception) {}
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.history_widget)

            // Set up the collection adapter
            val serviceIntent = Intent(context, HistoryWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Set a single PendingIntent for the entire widget area
            val openHistory = Intent(context, HistoryActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val globalPi = PendingIntent.getActivity(context, appWidgetId, openHistory, flags)
            views.setOnClickPendingIntent(R.id.history_widget_root, globalPi)
            // Ensure the title area also responds to taps
            views.setOnClickPendingIntent(R.id.widget_title, globalPi)

            // Also ensure tapping on list rows opens the same activity (covers most of the area)
            views.setPendingIntentTemplate(R.id.widget_list, globalPi)
            // And tapping the empty view area
            views.setOnClickPendingIntent(R.id.widget_empty, globalPi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }
}
