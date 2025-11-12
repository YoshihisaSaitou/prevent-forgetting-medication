package com.example.preventforgettingmedicationandroidapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationAdapter(
    context: Context,
    private val medications: List<Medication>
) : ArrayAdapter<Medication>(context, 0, medications) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_medication, parent, false)

        val medication = getItem(position)
        medication?.let { med ->
            val nameTextView = view.findViewById<TextView>(R.id.med_name)
            val detailsTextView = view.findViewById<TextView>(R.id.med_details)
            val takeButton = view.findViewById<Button>(R.id.btn_take)
            val editButton = view.findViewById<Button>(R.id.btn_edit)
            val deleteButton = view.findViewById<Button>(R.id.btn_delete)

            nameTextView.text = med.name
            
            val details = buildString {
                // 食事タイミング
                when (med.mealTiming) {
                    MealTiming.BEFORE_MEAL -> append("食前")
                    MealTiming.AFTER_MEAL -> append("食後")
                    null -> append("食事タイミング未設定")
                }
                
                append(" | ")
                
                // 服用時間帯
                val timeSlots = med.timing.map { slot ->
                    when (slot) {
                        IntakeSlot.MORNING -> {
                            val t = TimePreferences.formatMinutes(TimePreferences.getMorningMinutes(context))
                            "朝 $t"
                        }
                        IntakeSlot.NOON -> {
                            val t = TimePreferences.formatMinutes(TimePreferences.getNoonMinutes(context))
                            "昼 $t"
                        }
                        IntakeSlot.EVENING -> {
                            val t = TimePreferences.formatMinutes(TimePreferences.getEveningMinutes(context))
                            "夕 $t"
                        }
                    }
                }
                
                if (timeSlots.isNotEmpty()) {
                    append(timeSlots.joinToString(", "))
                } else {
                    append("服用時間未設定")
                }
            }
            
            detailsTextView.text = details

            // Apply disabled state based on last taken time
            val enabled = TakenStateStore.isEnabled(context, med.id)
            takeButton.isEnabled = enabled
            takeButton.alpha = if (enabled) 1f else 0.5f

            takeButton.setOnClickListener {
                val dao = MedicationDatabase.getInstance(context).intakeHistoryDao()
                val entry = IntakeHistory(
                    medicationId = med.id,
                    medicationName = med.name,
                    takenAt = System.currentTimeMillis()
                )
                // Immediately disable for 5 minutes in UI and persist state
                TakenStateStore.setDisabledForFiveMinutes(context, med.id)
                takeButton.isEnabled = false
                takeButton.alpha = 0.5f
                CoroutineScope(Dispatchers.IO).launch {
                    dao.insert(entry)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.taken_recorded),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        WidgetUtils.refreshMedicationWidgets(context)
                        // Schedule a refresh after 5 minutes to re-enable if this row is visible
                        Handler(Looper.getMainLooper()).postDelayed({
                            notifyDataSetChanged()
                            WidgetUtils.refreshMedicationWidgets(context)
                        }, 5 * 60 * 1000L)
                    }
                }
            }

            editButton.setOnClickListener {
                val intent = Intent(context, MedicationRegistrationActivity::class.java)
                intent.putExtra("MED_ID", med.id)
                context.startActivity(intent)
            }

            deleteButton.setOnClickListener {
                AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.confirm_delete))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val medDao = MedicationDatabase.getInstance(context).medicationDao()
                        CoroutineScope(Dispatchers.IO).launch {
                            medDao.delete(med)
                            withContext(Dispatchers.Main) {
                                remove(med)
                                notifyDataSetChanged()
                                WidgetUtils.refreshMedicationWidgets(context)
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        return view
    }
} 
