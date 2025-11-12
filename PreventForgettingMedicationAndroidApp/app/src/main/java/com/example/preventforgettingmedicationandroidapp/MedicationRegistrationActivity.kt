package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
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
                memo = memoEdit.text?.toString()?.takeIf { it.isNotBlank() }
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
