package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MedicationAdapter(
    context: Context,
    private val medications: List<Medication>
) : ArrayAdapter<Medication>(context, 0, medications) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val medication = getItem(position)
        medication?.let {
            val nameTextView = view.findViewById<TextView>(android.R.id.text1)
            val detailsTextView = view.findViewById<TextView>(android.R.id.text2)

            nameTextView.text = it.name
            
            val details = buildString {
                // 食事タイミング
                when (it.mealTiming) {
                    MealTiming.BEFORE_MEAL -> append("食前")
                    MealTiming.AFTER_MEAL -> append("食後")
                    null -> append("食事タイミング未設定")
                }
                
                append(" | ")
                
                // 服用時間帯
                val timeSlots = it.timing.map { slot ->
                    when (slot) {
                        IntakeSlot.MORNING -> "朝"
                        IntakeSlot.NOON -> "昼"
                        IntakeSlot.EVENING -> "夕"
                    }
                }
                
                if (timeSlots.isNotEmpty()) {
                    append(timeSlots.joinToString(", "))
                } else {
                    append("服用時間未設定")
                }
            }
            
            detailsTextView.text = details
        }

        return view
    }
} 