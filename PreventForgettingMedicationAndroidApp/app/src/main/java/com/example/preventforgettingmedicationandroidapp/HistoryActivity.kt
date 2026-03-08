package com.example.preventforgettingmedicationandroidapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
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
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.HistoryEvent
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = HistoryListAdapter { viewModel.toggleIncorrect(it) }
        val listView = findViewById<ListView>(R.id.history_list)
        listView.adapter = adapter
        listView.emptyView = findViewById<TextView>(R.id.empty_history)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                adapter.setItems(state.groups)
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is HistoryEvent.Saved -> Toast.makeText(this@HistoryActivity, getString(R.string.history_saved, event.count, 0), Toast.LENGTH_SHORT).show()
                    HistoryEvent.Duplicate -> Toast.makeText(this@HistoryActivity, getString(R.string.duplicate_schedule_skipped), Toast.LENGTH_SHORT).show()
                    is HistoryEvent.Error -> Toast.makeText(this@HistoryActivity, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.add_button).setOnClickListener {
            startAddFlow()
        }

        findViewById<Button>(R.id.footer_add).setOnClickListener {
            startActivity(Intent(this, MedicationRegistrationActivity::class.java))
        }
        findViewById<Button>(R.id.footer_list).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.footer_history).setOnClickListener {
            // already here
        }
        findViewById<Button>(R.id.footer_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun startAddFlow() {
        val options = viewModel.state.value.scheduleOptions
        if (options.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_schedules), Toast.LENGTH_SHORT).show()
            return
        }

        val labels = options.map {
            val slot = when (it.slot) {
                IntakeSlot.MORNING -> getString(R.string.morning)
                IntakeSlot.NOON -> getString(R.string.noon)
                IntakeSlot.EVENING -> getString(R.string.evening)
            }
            "${it.name} ($slot ${TimePreferences.formatMinutes(it.timeMinutes)})"
        }.toTypedArray()

        var selectedIndex = -1
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_schedule))
            .setSingleChoiceItems(labels, -1) { _, which -> selectedIndex = which }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                if (selectedIndex < 0) {
                    Toast.makeText(this@HistoryActivity, getString(R.string.no_selection), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                pickDateTimeAndSave(options[selectedIndex].id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pickDateTimeAndSave(scheduleId: com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId) {
        val now = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)

                        val chosen = cal.timeInMillis
                        if (chosen > System.currentTimeMillis()) {
                            Toast.makeText(this, getString(R.string.future_time_not_allowed), Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }
                        viewModel.addManual(scheduleId, chosen)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).apply {
                    setTitle(getString(R.string.select_time))
                }.show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(getString(R.string.select_date))
        }.show()
    }
}
