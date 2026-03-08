package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class MedicationWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var items: List<ScheduleWithMedications> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            val token = Binder.clearCallingIdentity()
            try {
                val dao = MedicationDatabase.getInstance(context).scheduleDao()
                items = try {
                    dao.getAllWithMedicationsSync()
                } catch (_: Exception) {
                    emptyList()
                }
            } finally {
                Binder.restoreCallingIdentity(token)
            }
        }

        override fun onDestroy() {
            items = emptyList()
        }

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= items.size) return null

            val item = items[position]
            val schedule = item.schedule
            val meds = item.medications

            val rv = RemoteViews(context.packageName, R.layout.medication_widget_item)
            rv.setTextViewText(R.id.widget_item_name, schedule.name)

            val slotLabel = when (schedule.slot) {
                IntakeSlot.MORNING -> context.getString(R.string.morning)
                IntakeSlot.NOON -> context.getString(R.string.noon)
                IntakeSlot.EVENING -> context.getString(R.string.evening)
            }
            val medsSummary = meds.joinToString(", ") { it.name }
            rv.setTextViewText(
                R.id.widget_item_details,
                "$slotLabel ${TimePreferences.formatMinutes(schedule.timeMinutes)} | $medsSummary"
            )

            val enabled = TakenStateStore.isEnabled(context, schedule.id)
            rv.setBoolean(R.id.widget_btn_take, "setEnabled", enabled)

            val fillIn = Intent().apply {
                putExtra(MedicationWidgetProvider.EXTRA_SCHEDULE_ID, schedule.id)
            }
            rv.setOnClickFillInIntent(R.id.widget_btn_take, fillIn)
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = items.getOrNull(position)?.schedule?.id?.toLong() ?: position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
