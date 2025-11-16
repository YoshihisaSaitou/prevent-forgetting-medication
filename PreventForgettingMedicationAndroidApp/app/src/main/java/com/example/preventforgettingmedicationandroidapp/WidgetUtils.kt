package com.example.preventforgettingmedicationandroidapp

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUtils {
    fun refreshMedicationWidgets(context: Context) {
        try {
            val awm = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, MedicationWidgetProvider::class.java)
            val ids = awm.getAppWidgetIds(cn)
            if (ids != null && ids.isNotEmpty()) {
                awm.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        } catch (_: Exception) {
        }
    }

    fun refreshHistoryWidgets(context: Context) {
        try {
            val awm = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, HistoryWidgetProvider::class.java)
            val ids = awm.getAppWidgetIds(cn)
            if (ids != null && ids.isNotEmpty()) {
                awm.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        } catch (_: Exception) {
        }
    }
}
