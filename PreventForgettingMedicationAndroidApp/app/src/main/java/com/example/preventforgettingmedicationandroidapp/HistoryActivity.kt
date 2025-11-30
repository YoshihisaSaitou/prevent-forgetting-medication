package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).intakeHistoryDao() }
    private val medDao by lazy { MedicationDatabase.getInstance(this).medicationDao() }
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

        // Footer menu
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
        lifecycleScope.launch {
            val items = dao.getAll()
            adapter.setItems(items)
        }
    }

    private fun startAddFlow() {
        lifecycleScope.launch {
            val medications = medDao.getAll()
            if (medications.isEmpty()) {
                Toast.makeText(this@HistoryActivity, getString(R.string.no_medications_for_add), Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = medications.map { it.name }.toTypedArray()
            val checked = BooleanArray(medications.size)

            AlertDialog.Builder(this@HistoryActivity)
                .setTitle(getString(R.string.select_medications))
                .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    val selected = medications.filterIndexed { index, _ -> checked[index] }
                    if (selected.isEmpty()) {
                        Toast.makeText(this@HistoryActivity, getString(R.string.no_selection), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    pickDateTimeAndSave(selected.map { it.id to it.name })
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun pickDateTimeAndSave(selectedMeds: List<Pair<Int, String>>) {
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
                        val nowMillis = System.currentTimeMillis()
                        if (chosen > nowMillis) {
                            Toast.makeText(this, getString(R.string.future_time_not_allowed), Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }

                        lifecycleScope.launch {
                            var saved = 0
                            var skipped = 0
                            for ((medId, medName) in selectedMeds) {
                                val exists = dao.exists(medId, chosen)
                                if (exists) {
                                    skipped++
                                } else {
                                    dao.insert(
                                        IntakeHistory(
                                            medicationId = medId,
                                            medicationName = medName,
                                            takenAt = chosen,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    saved++
                                }
                            }
                            Toast.makeText(this@HistoryActivity, getString(R.string.history_saved, saved, skipped), Toast.LENGTH_SHORT).show()

                            // refresh list
                            onResume()

                            // notify widgets
                            WidgetUtils.refreshHistoryWidgets(this@HistoryActivity)
                        }
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).apply { setTitle(getString(R.string.select_time)) }.show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply { setTitle(getString(R.string.select_date)) }.show()
    }
}
