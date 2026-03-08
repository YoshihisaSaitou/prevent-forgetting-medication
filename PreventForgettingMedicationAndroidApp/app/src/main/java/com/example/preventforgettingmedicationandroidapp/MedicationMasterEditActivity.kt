package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.preventforgettingmedicationandroidapp.domain.model.MealTiming
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MedicationMasterEditEvent
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MedicationMasterEditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationMasterEditActivity : AppCompatActivity() {
    private val viewModel: MedicationMasterEditViewModel by viewModels()

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

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (nameInput.text.toString() != state.name) {
                    nameInput.setText(state.name)
                    nameInput.setSelection(nameInput.text.length)
                }
                if (memoInput.text.toString() != state.memo) {
                    memoInput.setText(state.memo)
                    memoInput.setSelection(memoInput.text.length)
                }
                when (state.mealTiming) {
                    MealTiming.BEFORE_MEAL -> beforeMeal.isChecked = true
                    MealTiming.AFTER_MEAL -> afterMeal.isChecked = true
                    null -> {
                        beforeMeal.isChecked = false
                        afterMeal.isChecked = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    MedicationMasterEditEvent.Saved -> {
                        Toast.makeText(this@MedicationMasterEditActivity, getString(R.string.medication_saved), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is MedicationMasterEditEvent.Error -> {
                        Toast.makeText(this@MedicationMasterEditActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            viewModel.setName(nameInput.text.toString().trim())
            viewModel.setMemo(memoInput.text.toString())
            val timing = when {
                beforeMeal.isChecked -> MealTiming.BEFORE_MEAL
                afterMeal.isChecked -> MealTiming.AFTER_MEAL
                else -> null
            }
            viewModel.setMealTiming(timing)
            viewModel.save()
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

        val medId = intent.getIntExtra("MED_ID", -1)
        viewModel.load(medId.takeIf { it > 0 })
    }
}
