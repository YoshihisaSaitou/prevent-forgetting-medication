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

            val medication = Medication(name = name, mealTiming = mealTiming, timing = slots)
            dao.insert(medication)
            Toast.makeText(
                this,
                getString(R.string.medication_saved),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}
