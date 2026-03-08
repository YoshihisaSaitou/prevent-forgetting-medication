package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationMasterEditActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).medicationDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medication_master_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nameInput = findViewById<EditText>(R.id.medication_name)
        val memoInput = findViewById<EditText>(R.id.memo_edit)
        val beforeMeal = findViewById<RadioButton>(R.id.before_meal)
        val afterMeal = findViewById<RadioButton>(R.id.after_meal)
        val saveButton = findViewById<Button>(R.id.save_button)

        val medId = intent.getIntExtra("MED_ID", -1)
        var existing: Medication? = null

        if (medId != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
                existing = dao.getById(medId)
                withContext(Dispatchers.Main) {
                    existing?.let { med ->
                        nameInput.setText(med.name)
                        memoInput.setText(med.memo ?: "")
                        when (med.mealTiming) {
                            MealTiming.BEFORE_MEAL -> beforeMeal.isChecked = true
                            MealTiming.AFTER_MEAL -> afterMeal.isChecked = true
                            null -> {}
                        }
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_medication_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mealTiming = when {
                beforeMeal.isChecked -> MealTiming.BEFORE_MEAL
                afterMeal.isChecked -> MealTiming.AFTER_MEAL
                else -> null
            }

            val memo = memoInput.text.toString().trim().ifEmpty { null }
            val base = existing

            val medication = Medication(
                id = if (medId == -1) 0 else medId,
                name = name,
                mealTiming = mealTiming,
                timing = base?.timing ?: setOf(IntakeSlot.MORNING),
                memo = memo,
                useAppTimes = base?.useAppTimes ?: true,
                morningMinutes = base?.morningMinutes,
                noonMinutes = base?.noonMinutes,
                eveningMinutes = base?.eveningMinutes
            )

            lifecycleScope.launch(Dispatchers.IO) {
                if (medId == -1) {
                    dao.insert(medication)
                } else {
                    dao.update(medication)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MedicationMasterEditActivity, getString(R.string.medication_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        findViewById<Button>(R.id.footer_list).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.footer_add).setOnClickListener {
            startActivity(Intent(this, MedicationRegistrationActivity::class.java))
        }
        findViewById<Button>(R.id.footer_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.footer_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
