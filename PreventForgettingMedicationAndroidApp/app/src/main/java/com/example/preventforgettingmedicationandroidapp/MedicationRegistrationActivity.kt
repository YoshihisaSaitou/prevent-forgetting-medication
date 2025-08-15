package com.example.preventforgettingmedicationandroidapp

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MedicationRegistrationActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).medicationDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_registration)

        val nameInput = findViewById<EditText>(R.id.medication_name)
        val beforeMeal = findViewById<RadioButton>(R.id.before_meal)
        val afterMeal = findViewById<RadioButton>(R.id.after_meal)
        val morning = findViewById<CheckBox>(R.id.slot_morning)
        val noon = findViewById<CheckBox>(R.id.slot_noon)
        val evening = findViewById<CheckBox>(R.id.slot_evening)
        val saveButton = findViewById<Button>(R.id.save_button)

        val medId = intent.getIntExtra("MED_ID", -1)
        val existingMedication = if (medId != -1) dao.getById(medId) else null

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
                timing = slots
            )

            if (medId != -1) {
                dao.update(medication)
            } else {
                dao.insert(medication)
            }
            Toast.makeText(
                this,
                getString(R.string.medication_saved),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}
