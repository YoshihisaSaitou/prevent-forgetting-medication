package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.app.TimePickerDialog
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MedicationRegistrationActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).medicationDao() }

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
        val beforeMeal = findViewById<RadioButton>(R.id.before_meal)
        val afterMeal = findViewById<RadioButton>(R.id.after_meal)
        val morning = findViewById<CheckBox>(R.id.slot_morning)
        val noon = findViewById<CheckBox>(R.id.slot_noon)
        val evening = findViewById<CheckBox>(R.id.slot_evening)
        val saveButton = findViewById<Button>(R.id.save_button)
        val memoEdit = findViewById<EditText>(R.id.memo_edit)
        val timeModeGroup = findViewById<RadioGroup>(R.id.time_mode_group)
        val useAppTimes = findViewById<RadioButton>(R.id.use_app_times)
        val useMedTimes = findViewById<RadioButton>(R.id.use_med_times)
        val defaultMedContainer = findViewById<View>(R.id.default_med_time_container)
        val perMedContainer = findViewById<View>(R.id.per_med_time_container)
        val tvMorning = findViewById<TextView>(R.id.med_time_morning)
        val tvNoon = findViewById<TextView>(R.id.med_time_noon)
        val tvEvening = findViewById<TextView>(R.id.med_time_evening)
        val btnMorning = findViewById<Button>(R.id.btn_time_morning)
        val btnNoon = findViewById<Button>(R.id.btn_time_noon)
        val btnEvening = findViewById<Button>(R.id.btn_time_evening)

        // Default per-med minutes seeded from app settings
        var perMorning = TimePreferences.getMorningMinutes(this)
        var perNoon = TimePreferences.getNoonMinutes(this)
        var perEvening = TimePreferences.getEveningMinutes(this)

        fun updateTimeTexts() {
            tvMorning.text = TimePreferences.formatMinutes(perMorning)
            tvNoon.text = TimePreferences.formatMinutes(perNoon)
            tvEvening.text = TimePreferences.formatMinutes(perEvening)
        }
        updateTimeTexts()

        fun pickTime(initMinutes: Int, onPicked: (Int) -> Unit) {
            val h = initMinutes / 60
            val m = initMinutes % 60
            TimePickerDialog(this, { _, hourOfDay, minute ->
                onPicked(hourOfDay * 60 + minute)
                updateTimeTexts()
            }, h, m, true).show()
        }
        tvMorning.setOnClickListener { pickTime(perMorning) { perMorning = it } }
        tvNoon.setOnClickListener { pickTime(perNoon) { perNoon = it } }
        tvEvening.setOnClickListener { pickTime(perEvening) { perEvening = it } }
        btnMorning.setOnClickListener { pickTime(perMorning) { perMorning = it } }
        btnNoon.setOnClickListener { pickTime(perNoon) { perNoon = it } }
        btnEvening.setOnClickListener { pickTime(perEvening) { perEvening = it } }

        fun applyModeUI(useApp: Boolean) {
            // Explicitly control visibility for both modes
            // - Use app times: show default container, hide per-medication inputs
            // - Per-medication times: hide default container, show per-medication inputs
            defaultMedContainer.visibility = if (useApp) View.VISIBLE else View.GONE
            perMedContainer.visibility = if (useApp) View.GONE else View.VISIBLE

            // Always show the slot checkboxes (morning/noon/evening) regardless of mode
            morning.visibility = View.VISIBLE
            noon.visibility = View.VISIBLE
            evening.visibility = View.VISIBLE
        }
        useAppTimes.isChecked = true
        applyModeUI(true)
        timeModeGroup.setOnCheckedChangeListener { _, _ ->
            applyModeUI(useAppTimes.isChecked)
        }

        val medId = intent.getIntExtra("MED_ID", -1)
        if (medId != -1) {
            lifecycleScope.launch {
                val existingMedication = dao.getById(medId)
                existingMedication?.let { medication ->
                    nameInput.setText(medication.name)
                    when (medication.mealTiming) {
                        MealTiming.BEFORE_MEAL -> beforeMeal.isChecked = true
                        MealTiming.AFTER_MEAL -> afterMeal.isChecked = true
                        else -> {}
                    }
                    morning.isChecked = medication.timing.contains(IntakeSlot.MORNING)
                    noon.isChecked = medication.timing.contains(IntakeSlot.NOON)
                    evening.isChecked = medication.timing.contains(IntakeSlot.EVENING)
                    memoEdit.setText(medication.memo ?: "")

                    // Time mode and overrides
                    useAppTimes.isChecked = medication.useAppTimes
                    useMedTimes.isChecked = !medication.useAppTimes
                    applyModeUI(medication.useAppTimes)
                    medication.morningMinutes?.let { perMorning = it }
                    medication.noonMinutes?.let { perNoon = it }
                    medication.eveningMinutes?.let { perEvening = it }
                    updateTimeTexts()
                }
            }
        }


        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val mealTiming = when {
                beforeMeal.isChecked -> MealTiming.BEFORE_MEAL
                afterMeal.isChecked -> MealTiming.AFTER_MEAL
                else -> null
            }
            val slots = mutableSetOf<IntakeSlot>()
            if (morning.isChecked) slots.add(IntakeSlot.MORNING)
            if (noon.isChecked) slots.add(IntakeSlot.NOON)
            if (evening.isChecked) slots.add(IntakeSlot.EVENING)

            if (name.isEmpty() || slots.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.error_enter_name_select_times),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val medication = Medication(
                id = if (medId != -1) medId else 0,
                name = name,
                mealTiming = mealTiming,
                timing = slots,
                memo = memoEdit.text?.toString()?.takeIf { it.isNotBlank() },
                useAppTimes = useAppTimes.isChecked,
                morningMinutes = if (useMedTimes.isChecked) perMorning else null,
                noonMinutes = if (useMedTimes.isChecked) perNoon else null,
                eveningMinutes = if (useMedTimes.isChecked) perEvening else null
            )

            lifecycleScope.launch {
                if (medId != -1) {
                    dao.update(medication)
                } else {
                    dao.insert(medication)
                }
                runOnUiThread {
                    WidgetUtils.refreshMedicationWidgets(this@MedicationRegistrationActivity)
                    Toast.makeText(
                        this@MedicationRegistrationActivity,
                        getString(R.string.medication_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        // Footer menu
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
    }
}
