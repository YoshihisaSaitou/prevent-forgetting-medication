package com.example.preventforgettingmedicationandroidapp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.preventforgettingmedicationandroidapp.domain.model.IntakeSlot
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.ScheduleFormEvent
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.ScheduleFormViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationRegistrationActivity : AppCompatActivity() {
    private val viewModel: ScheduleFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medication_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nameInput = findViewById<EditText>(R.id.medication_name)
        val slotGroup = findViewById<RadioGroup>(R.id.slot_group)
        val morning = findViewById<RadioButton>(R.id.slot_morning)
        val noon = findViewById<RadioButton>(R.id.slot_noon)
        val evening = findViewById<RadioButton>(R.id.slot_evening)
        val selectedTimeText = findViewById<TextView>(R.id.selected_time)
        val selectedMedsText = findViewById<TextView>(R.id.selected_medications)
        val pickTimeButton = findViewById<Button>(R.id.btn_pick_time)
        val selectMedsButton = findViewById<Button>(R.id.btn_select_medications)
        val saveButton = findViewById<Button>(R.id.save_button)

        var selectedMinutes = 7 * 60
        val selectedMedicationIds = linkedSetOf<Int>()

        fun currentSlot(): IntakeSlot = when {
            morning.isChecked -> IntakeSlot.MORNING
            noon.isChecked -> IntakeSlot.NOON
            else -> IntakeSlot.EVENING
        }

        fun refreshSelectedMedsText(options: List<Pair<Int, String>>) {
            val names = options.filter { selectedMedicationIds.contains(it.first) }.map { it.second }
            selectedMedsText.text = if (names.isEmpty()) {
                getString(R.string.no_medications_selected)
            } else {
                names.joinToString(", ")
            }
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (nameInput.text.toString() != state.name) {
                    nameInput.setText(state.name)
                    nameInput.setSelection(nameInput.text.length)
                }

                when (state.slot) {
                    IntakeSlot.MORNING -> morning.isChecked = true
                    IntakeSlot.NOON -> noon.isChecked = true
                    IntakeSlot.EVENING -> evening.isChecked = true
                }

                selectedMinutes = state.timeMinutes
                selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)

                selectedMedicationIds.clear()
                selectedMedicationIds.addAll(state.selectedMedicationIds)
                val options = state.medicationOptions.map { it.id to it.name }
                refreshSelectedMedsText(options)
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    ScheduleFormEvent.Saved -> {
                        Toast.makeText(this@MedicationRegistrationActivity, getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is ScheduleFormEvent.Error -> {
                        Toast.makeText(this@MedicationRegistrationActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        morning.isChecked = true
        selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)

        slotGroup.setOnCheckedChangeListener { _, _ ->
            val slot = currentSlot()
            selectedMinutes = when (slot) {
                IntakeSlot.MORNING -> 7 * 60
                IntakeSlot.NOON -> 12 * 60
                IntakeSlot.EVENING -> 19 * 60
            }
            selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)
            viewModel.setSlot(slot)
            viewModel.setTimeMinutes(selectedMinutes)
        }

        pickTimeButton.setOnClickListener {
            val h = selectedMinutes / 60
            val m = selectedMinutes % 60
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedMinutes = hourOfDay * 60 + minute
                selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)
                viewModel.setTimeMinutes(selectedMinutes)
            }, h, m, true).show()
        }

        selectMedsButton.setOnClickListener {
            val options = viewModel.state.value.medicationOptions
            if (options.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_medications_for_add), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val names = options.map { it.name }.toTypedArray()
            val checked = BooleanArray(options.size) { idx -> selectedMedicationIds.contains(options[idx].id) }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_medications))
                .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    selectedMedicationIds.clear()
                    checked.forEachIndexed { index, isChecked ->
                        if (isChecked) selectedMedicationIds.add(options[index].id)
                    }
                    viewModel.setSelectedMedicationIds(selectedMedicationIds)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        saveButton.setOnClickListener {
            viewModel.setName(nameInput.text.toString().trim())
            viewModel.setSlot(currentSlot())
            viewModel.setTimeMinutes(selectedMinutes)
            viewModel.setSelectedMedicationIds(selectedMedicationIds)
            viewModel.save()
        }

        findViewById<Button>(R.id.footer_list).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.footer_add).setOnClickListener {
            // already here
        }
        findViewById<Button>(R.id.footer_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.footer_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val scheduleId = intent.getIntExtra("SCHEDULE_ID", -1)
        viewModel.load(scheduleId.takeIf { it > 0 })
    }
}
