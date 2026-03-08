package com.example.preventforgettingmedicationandroidapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationAdapter(
    private val context: Context,
    private val items: MutableList<ScheduleWithMedications>
) : BaseAdapter() {

    fun setItems(newItems: List<ScheduleWithMedications>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): ScheduleWithMedications = items[position]

    override fun getItemId(position: Int): Long = items[position].schedule.id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_medication, parent, false)

        val scheduleWithMeds = getItem(position)
        val schedule = scheduleWithMeds.schedule
        val meds = scheduleWithMeds.medications

        val nameTextView = view.findViewById<TextView>(R.id.med_name)
        val detailsTextView = view.findViewById<TextView>(R.id.med_details)
        val takeButton = view.findViewById<Button>(R.id.btn_take)
        val editButton = view.findViewById<Button>(R.id.btn_edit)
        val deleteButton = view.findViewById<Button>(R.id.btn_delete)

        nameTextView.text = schedule.name

        val slotLabel = when (schedule.slot) {
            IntakeSlot.MORNING -> context.getString(R.string.morning)
            IntakeSlot.NOON -> context.getString(R.string.noon)
            IntakeSlot.EVENING -> context.getString(R.string.evening)
        }
        val medsSummary = if (meds.isEmpty()) {
            context.getString(R.string.no_medications)
        } else {
            meds.joinToString(", ") { it.name }
        }
        detailsTextView.text = "$slotLabel ${TimePreferences.formatMinutes(schedule.timeMinutes)} | $medsSummary"

        val enabled = TakenStateStore.isEnabled(context, schedule.id)
        takeButton.isEnabled = enabled
        takeButton.alpha = if (enabled) 1f else 0.5f

        takeButton.setOnClickListener {
            val activity = context as? AppCompatActivity ?: return@setOnClickListener
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val result = ScheduleExecution.executeNow(context, scheduleWithMeds)
                withContext(Dispatchers.Main) {
                    when {
                        result.skippedDisabled -> {
                            Toast.makeText(context, context.getString(R.string.schedule_disabled_temporarily), Toast.LENGTH_SHORT).show()
                        }
                        result.skippedDuplicate -> {
                            Toast.makeText(context, context.getString(R.string.duplicate_schedule_skipped), Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.taken_recorded_count, result.inserted),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    notifyDataSetChanged()
                }
            }
        }

        editButton.setOnClickListener {
            val intent = Intent(context, MedicationRegistrationActivity::class.java)
            intent.putExtra("SCHEDULE_ID", schedule.id)
            context.startActivity(intent)
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.confirm_delete_schedule))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val activity = context as? AppCompatActivity ?: return@setPositiveButton
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val scheduleDao = MedicationDatabase.getInstance(context).scheduleDao()
                        scheduleDao.deleteCrossRefsForSchedule(schedule.id)
                        scheduleDao.deleteById(schedule.id)
                        AlarmScheduler.scheduleAll(context)
                        WidgetUtils.refreshMedicationWidgets(context)
                        withContext(Dispatchers.Main) {
                            items.removeAt(position)
                            notifyDataSetChanged()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        return view
    }
}
