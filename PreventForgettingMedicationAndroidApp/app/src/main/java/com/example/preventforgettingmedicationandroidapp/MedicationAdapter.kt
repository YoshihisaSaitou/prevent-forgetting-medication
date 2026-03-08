package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import com.example.preventforgettingmedicationandroidapp.presentation.model.ScheduleListItem

class MedicationAdapter(
    private val context: Context,
    private val items: MutableList<ScheduleListItem>,
    private val onTake: (ScheduleId) -> Unit,
    private val onEdit: (ScheduleId) -> Unit,
    private val onDelete: (ScheduleId) -> Unit
) : BaseAdapter() {

    fun setItems(newItems: List<ScheduleListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): ScheduleListItem = items[position]

    override fun getItemId(position: Int): Long = items[position].id.value.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_medication, parent, false)

        val item = getItem(position)

        val nameTextView = view.findViewById<TextView>(R.id.med_name)
        val detailsTextView = view.findViewById<TextView>(R.id.med_details)
        val takeButton = view.findViewById<Button>(R.id.btn_take)
        val editButton = view.findViewById<Button>(R.id.btn_edit)
        val deleteButton = view.findViewById<Button>(R.id.btn_delete)

        nameTextView.text = item.name

        val slotLabel = when (item.slot) {
            IntakeSlot.MORNING -> context.getString(R.string.morning)
            IntakeSlot.NOON -> context.getString(R.string.noon)
            IntakeSlot.EVENING -> context.getString(R.string.evening)
        }
        val medsSummary = if (item.medicationNames.isEmpty()) {
            context.getString(R.string.no_medications)
        } else {
            item.medicationNames.joinToString(", ")
        }
        detailsTextView.text = "$slotLabel ${TimePreferences.formatMinutes(item.timeMinutes)} | $medsSummary"

        takeButton.isEnabled = item.canExecute
        takeButton.alpha = if (item.canExecute) 1f else 0.5f

        takeButton.setOnClickListener { onTake(item.id) }
        editButton.setOnClickListener { onEdit(item.id) }
        deleteButton.setOnClickListener { onDelete(item.id) }

        return view
    }
}
