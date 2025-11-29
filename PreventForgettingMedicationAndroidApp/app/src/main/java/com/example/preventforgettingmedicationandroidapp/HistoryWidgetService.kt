package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.os.Binder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var items: List<IntakeHistory> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            val token = Binder.clearCallingIdentity()
            try {
                val dao = MedicationDatabase.getInstance(context).intakeHistoryDao()
                items = try { dao.getRecentSync(50) } catch (_: Exception) { emptyList() }
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
            val entry = items[position]
            val rv = RemoteViews(context.packageName, R.layout.history_widget_item)
            rv.setTextViewText(R.id.widget_item_name, entry.medicationName)
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val time = sdf.format(Date(entry.takenAt))
            val manual = if (entry.createdAt != entry.takenAt) " (${context.getString(R.string.manual_label)})" else ""
            rv.setTextViewText(R.id.widget_item_time, time + manual)
            // Make the whole row clickable to open HistoryActivity via template
            val fillIn = Intent()
            rv.setOnClickFillInIntent(R.id.history_item_root, fillIn)
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: position.toLong()
        override fun hasStableIds(): Boolean = true
    }
}
