package com.example.preventforgettingmedicationandroidapp

import android.content.Context
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

class HistoryListAdapter(
    private val context: Context
) : BaseAdapter() {

    private val items: MutableList<IntakeHistory> = mutableListOf()
    private val dao by lazy { MedicationDatabase.getInstance(context).intakeHistoryDao() }

    fun setItems(newItems: List<IntakeHistory>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): IntakeHistory = items[position]

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false)

        val entry = getItem(position)
        val text = view.findViewById<TextView>(R.id.history_text)
        val button = view.findViewById<Button>(R.id.btn_incorrect)

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val time = sdf.format(Date(entry.takenAt))
        val manual = if (entry.createdAt != entry.takenAt) " (${context.getString(R.string.manual_label)})" else ""
        text.text = "$time - ${entry.medicationName}$manual"

        // strike-through if incorrect
        if (entry.incorrectAt != null) {
            text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            button.text = context.getString(R.string.incorrect_marked)
        } else {
            text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            button.text = context.getString(R.string.mark_incorrect)
        }

        button.setOnClickListener {
            val newTs = if (entry.incorrectAt == null) System.currentTimeMillis() else null
            // update DB then UI
            (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                dao.setIncorrectAt(entry.id, newTs)
                val updated = entry.copy(incorrectAt = newTs)
                withContext(Dispatchers.Main) {
                    items[position] = updated
                    notifyDataSetChanged()
                    WidgetUtils.refreshHistoryWidgets(context)
                }
            }
        }

        return view
    }
}

