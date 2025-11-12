package com.example.preventforgettingmedicationandroidapp

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class MedicationWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext, intent)

    private class Factory(private val context: Context, intent: Intent) : RemoteViewsFactory {
        private var items: List<Medication> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            val dao = MedicationDatabase.getInstance(context).medicationDao()
            items = try { dao.getAllSync() } catch (_: Exception) { emptyList() }
        }

        override fun onDestroy() {
            items = emptyList()
        }

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= items.size) return null
            val med = items[position]
            val rv = RemoteViews(context.packageName, R.layout.medication_widget_item)
            rv.setTextViewText(R.id.widget_item_name, med.name)

            // Build time slot details like "朝 07:00, 昼 12:00, 夜 19:00"
            val details = med.timing.map { slot ->
                when (slot) {
                    IntakeSlot.MORNING ->
                        context.getString(R.string.morning) + " " + TimePreferences.formatMinutes(TimePreferences.getMorningMinutes(context))
                    IntakeSlot.NOON ->
                        context.getString(R.string.noon) + " " + TimePreferences.formatMinutes(TimePreferences.getNoonMinutes(context))
                    IntakeSlot.EVENING ->
                        context.getString(R.string.evening) + " " + TimePreferences.formatMinutes(TimePreferences.getEveningMinutes(context))
                }
            }.joinToString(", ")
            rv.setTextViewText(R.id.widget_item_details, if (details.isNotEmpty()) details else "")

            val enabled = TakenStateStore.isEnabled(context, med.id)
            rv.setBoolean(R.id.widget_btn_take, "setEnabled", enabled)

            // Fill-in intent for "Taken" button
            val fillIn = Intent().apply {
                putExtra(MedicationWidgetProvider.EXTRA_MED_ID, med.id)
                putExtra(MedicationWidgetProvider.EXTRA_MED_NAME, med.name)
            }
            rv.setOnClickFillInIntent(R.id.widget_btn_take, fillIn)
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: position.toLong()
        override fun hasStableIds(): Boolean = true
    }
}
