package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private data class WidgetHistoryGroup(
        val scheduleName: String,
        val takenAt: Long,
        val createdAt: Long,
        val medicationNames: List<String>,
        val incorrectAt: Long?
    )

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var items: List<WidgetHistoryGroup> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            val token = Binder.clearCallingIdentity()
            try {
                val dao = MedicationDatabase.getInstance(context).intakeHistoryDao()
                val raw = try {
                    dao.getRecentSync(200)
                } catch (_: Exception) {
                    emptyList()
                }
                items = raw
                    .groupBy { Pair(it.scheduleId, it.takenAt) }
                    .values
                    .map { chunk ->
                        val first = chunk.first()
                        WidgetHistoryGroup(
                            scheduleName = first.scheduleName
                                ?: if (first.scheduleId == null) context.getString(R.string.legacy_history_group) else context.getString(R.string.unknown_schedule),
                            takenAt = first.takenAt,
                            createdAt = chunk.minOfOrNull { it.createdAt } ?: first.createdAt,
                            medicationNames = chunk.map { it.medicationName }.distinct(),
                            incorrectAt = chunk.firstOrNull { it.incorrectAt != null }?.incorrectAt
                        )
                    }
                    .sortedByDescending { it.takenAt }
                    .take(50)
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
            rv.setTextViewText(R.id.widget_item_name, entry.scheduleName)

            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val time = sdf.format(Date(entry.takenAt))
            val manual = if (entry.createdAt != entry.takenAt) " (${context.getString(R.string.manual_label)})" else ""
            val meds = entry.medicationNames.joinToString(", ")
            rv.setTextViewText(R.id.widget_item_time, "$time$manual | $meds")

            val strikeFlags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
            val normalFlags = Paint.ANTI_ALIAS_FLAG
            if (entry.incorrectAt != null) {
                rv.setInt(R.id.widget_item_name, "setPaintFlags", strikeFlags)
                rv.setInt(R.id.widget_item_time, "setPaintFlags", strikeFlags)
            } else {
                rv.setInt(R.id.widget_item_name, "setPaintFlags", normalFlags)
                rv.setInt(R.id.widget_item_time, "setPaintFlags", normalFlags)
            }

            val fillIn = Intent()
            rv.setOnClickFillInIntent(R.id.history_item_root, fillIn)
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = false
    }
}
