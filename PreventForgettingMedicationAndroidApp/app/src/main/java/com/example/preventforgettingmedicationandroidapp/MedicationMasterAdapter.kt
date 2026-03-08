package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.preventforgettingmedicationandroidapp.domain.model.MedicationId
import com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MedicationMasterItem

class MedicationMasterAdapter(
    private val items: MutableList<MedicationMasterItem>,
    private val onEdit: (MedicationId) -> Unit,
    private val onDelete: (MedicationId) -> Unit
) : BaseAdapter() {

    fun setItems(newItems: List<MedicationMasterItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): MedicationMasterItem = items[position]

    override fun getItemId(position: Int): Long = items[position].id.value.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context)
            .inflate(R.layout.item_medication_master, parent, false)

        val item = getItem(position)
        val context = view.context
        val name = view.findViewById<TextView>(R.id.med_name)
        val detail = view.findViewById<TextView>(R.id.med_details)
        val edit = view.findViewById<Button>(R.id.btn_edit)
        val delete = view.findViewById<Button>(R.id.btn_delete)

        name.text = item.name
        detail.text = when (item.mealTiming) {
            MealTiming.BEFORE_MEAL -> context.getString(R.string.before_meal)
            MealTiming.AFTER_MEAL -> context.getString(R.string.after_meal)
            null -> context.getString(R.string.meal_timing_not_set)
        }

        edit.setOnClickListener { onEdit(item.id) }
        delete.setOnClickListener { onDelete(item.id) }

        return view
    }
}
