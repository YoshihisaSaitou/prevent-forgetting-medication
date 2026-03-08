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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {
    private val db by lazy { MedicationDatabase.getInstance(this) }
    private val dao by lazy { db.intakeHistoryDao() }
    private val scheduleDao by lazy { db.scheduleDao() }
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

        adapter = HistoryListAdapter(this)
        val listView = findViewById<ListView>(R.id.history_list)
        listView.adapter = adapter
        listView.emptyView = findViewById<TextView>(R.id.empty_history)

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
        lifecycleScope.launch(Dispatchers.IO) {
            val items = dao.getAll()
            withContext(Dispatchers.Main) {
                adapter.setItems(items)
            }
        }
    }

    private fun startAddFlow() {
        lifecycleScope.launch(Dispatchers.IO) {
            val schedules = scheduleDao.getAllWithMedications()
            if (schedules.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, getString(R.string.no_schedules), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val labels = schedules.map {
                val slot = when (it.schedule.slot) {
                    IntakeSlot.MORNING -> getString(R.string.morning)
                    IntakeSlot.NOON -> getString(R.string.noon)
                    IntakeSlot.EVENING -> getString(R.string.evening)
                }
                "${it.schedule.name} ($slot ${TimePreferences.formatMinutes(it.schedule.timeMinutes)})"
            }.toTypedArray()

            withContext(Dispatchers.Main) {
                var selectedIndex = -1
                AlertDialog.Builder(this@HistoryActivity)
                    .setTitle(getString(R.string.select_schedule))
                    .setSingleChoiceItems(labels, -1) { _, which -> selectedIndex = which }
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        if (selectedIndex < 0) {
                            Toast.makeText(this@HistoryActivity, getString(R.string.no_selection), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        pickDateTimeAndSave(schedules[selectedIndex])
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun pickDateTimeAndSave(scheduleWithMeds: ScheduleWithMedications) {
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

                        lifecycleScope.launch(Dispatchers.IO) {
                            val scheduleId = scheduleWithMeds.schedule.id
                            val exists = dao.existsScheduleEntry(scheduleId, chosen)
                            if (exists) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@HistoryActivity, getString(R.string.duplicate_schedule_skipped), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            val entries = scheduleWithMeds.medications.map {
                                IntakeHistory(
                                    scheduleId = scheduleId,
                                    scheduleName = scheduleWithMeds.schedule.name,
                                    medicationId = it.id,
                                    medicationName = it.name,
                                    takenAt = chosen,
                                    createdAt = System.currentTimeMillis()
                                )
                            }
                            dao.insertAll(entries)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@HistoryActivity,
                                    getString(R.string.history_saved, entries.size, 0),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onResume()
                                WidgetUtils.refreshHistoryWidgets(this@HistoryActivity)
                            }
                        }
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
