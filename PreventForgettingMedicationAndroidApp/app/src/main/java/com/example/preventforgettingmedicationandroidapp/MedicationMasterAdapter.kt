package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationMasterAdapter(
    private val activity: AppCompatActivity,
    private val items: MutableList<Medication>,
    private val onChanged: () -> Unit
) : BaseAdapter() {

    fun setItems(newItems: List<Medication>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Medication = items[position]

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(activity)
            .inflate(R.layout.item_medication_master, parent, false)

        val med = getItem(position)
        val name = view.findViewById<TextView>(R.id.med_name)
        val detail = view.findViewById<TextView>(R.id.med_details)
        val edit = view.findViewById<Button>(R.id.btn_edit)
        val delete = view.findViewById<Button>(R.id.btn_delete)

        name.text = med.name
        detail.text = when (med.mealTiming) {
            MealTiming.BEFORE_MEAL -> activity.getString(R.string.before_meal)
            MealTiming.AFTER_MEAL -> activity.getString(R.string.after_meal)
            null -> activity.getString(R.string.meal_timing_not_set)
        }

        edit.setOnClickListener {
            val intent = Intent(activity, MedicationMasterEditActivity::class.java)
            intent.putExtra("MED_ID", med.id)
            activity.startActivity(intent)
        }

        delete.setOnClickListener {
            AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.confirm_delete_medication_master))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val db = MedicationDatabase.getInstance(activity)
                        val refs = db.medicationDao().countScheduleReferences(med.id)
                        if (refs > 0) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(activity, activity.getString(R.string.medication_delete_blocked_in_use), Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        db.medicationDao().delete(med)
                        withContext(Dispatchers.Main) {
                            onChanged()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        return view
    }
}
