package com.example.preventforgettingmedicationandroidapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.preventforgettingmedicationandroidapp.presentation.model.HistoryGroupItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryListAdapter(
    private val onToggleIncorrect: (HistoryGroupItem) -> Unit
) : BaseAdapter() {

    private val items: MutableList<HistoryGroupItem> = mutableListOf()

    fun setItems(newItems: List<HistoryGroupItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): HistoryGroupItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context)
            .inflate(R.layout.item_history, parent, false)

        val entry = getItem(position)
        val context = view.context
        val text = view.findViewById<TextView>(R.id.history_text)
        val button = view.findViewById<Button>(R.id.btn_incorrect)

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val time = sdf.format(Date(entry.takenAt))
        val manual = if (entry.createdAt != entry.takenAt) " (${context.getString(R.string.manual_label)})" else ""
        val meds = entry.medicationNames.joinToString(", ")
        text.text = "$time - ${entry.scheduleName}$manual\n$meds"

        if (entry.incorrectAt != null) {
            text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            button.text = context.getString(R.string.incorrect_marked)
        } else {
            text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            button.text = context.getString(R.string.mark_incorrect)
        }

        button.setOnClickListener { onToggleIncorrect(entry) }
        return view
    }
}
