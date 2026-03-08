package com.example.preventforgettingmedicationandroidapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntakeHistoryGroup(
    val scheduleId: Int?,
    val scheduleName: String,
    val takenAt: Long,
    val createdAt: Long,
    val medicationNames: List<String>,
    val incorrectAt: Long?
)

class HistoryListAdapter(
    private val activity: AppCompatActivity
) : BaseAdapter() {

    private val groups: MutableList<IntakeHistoryGroup> = mutableListOf()
    private val dao by lazy { MedicationDatabase.getInstance(activity).intakeHistoryDao() }

    fun setItems(entries: List<IntakeHistory>) {
        groups.clear()
        val grouped = entries.groupBy { Pair(it.scheduleId, it.takenAt) }
        grouped.values.forEach { chunk ->
            val first = chunk.first()
            val scheduleName = first.scheduleName
                ?: if (first.scheduleId == null) activity.getString(R.string.legacy_history_group) else activity.getString(R.string.unknown_schedule)
            val incorrectAt = chunk.firstOrNull { it.incorrectAt != null }?.incorrectAt
            val createdAt = chunk.minOfOrNull { it.createdAt } ?: first.createdAt
            groups.add(
                IntakeHistoryGroup(
                    scheduleId = first.scheduleId,
                    scheduleName = scheduleName,
                    takenAt = first.takenAt,
                    createdAt = createdAt,
                    medicationNames = chunk.map { it.medicationName }.distinct(),
                    incorrectAt = incorrectAt
                )
            )
        }
        groups.sortByDescending { it.takenAt }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = groups.size

    override fun getItem(position: Int): IntakeHistoryGroup = groups[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(activity)
            .inflate(R.layout.item_history, parent, false)

        val group = getItem(position)
        val text = view.findViewById<TextView>(R.id.history_text)
        val button = view.findViewById<Button>(R.id.btn_incorrect)

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val time = sdf.format(Date(group.takenAt))
        val manual = if (group.createdAt != group.takenAt) " (${activity.getString(R.string.manual_label)})" else ""
        val meds = group.medicationNames.joinToString(", ")
        text.text = "$time - ${group.scheduleName}$manual\n$meds"

        if (group.incorrectAt != null) {
            text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            button.text = activity.getString(R.string.incorrect_marked)
        } else {
            text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            button.text = activity.getString(R.string.mark_incorrect)
        }

        button.setOnClickListener {
            val newTs = if (group.incorrectAt == null) System.currentTimeMillis() else null
            activity.lifecycleScope.launch(Dispatchers.IO) {
                if (group.scheduleId != null) {
                    dao.setIncorrectAtForScheduleGroup(group.scheduleId, group.takenAt, newTs)
                } else {
                    dao.setIncorrectAtForLegacyGroup(group.takenAt, newTs)
                }
                withContext(Dispatchers.Main) {
                    groups[position] = group.copy(incorrectAt = newTs)
                    notifyDataSetChanged()
                    WidgetUtils.refreshHistoryWidgets(activity)
                }
            }
        }

        return view
    }
}
