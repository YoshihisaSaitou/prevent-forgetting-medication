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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationRegistrationActivity : AppCompatActivity() {
    private val db by lazy { MedicationDatabase.getInstance(this) }
    private val scheduleDao by lazy { db.scheduleDao() }
    private val medicationDao by lazy { db.medicationDao() }

    private var allMedications: List<Medication> = emptyList()
    private val selectedMedicationIds = linkedSetOf<Int>()
    private var selectedMinutes = 7 * 60

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

        morning.isChecked = true
        selectedMinutes = TimePreferences.getMorningMinutes(this)
        selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)

        fun currentSlot(): IntakeSlot = when {
            morning.isChecked -> IntakeSlot.MORNING
            noon.isChecked -> IntakeSlot.NOON
            else -> IntakeSlot.EVENING
        }

        fun refreshSelectedMedsText() {
            val selected = allMedications.filter { selectedMedicationIds.contains(it.id) }
            selectedMedsText.text = if (selected.isEmpty()) {
                getString(R.string.no_medications_selected)
            } else {
                selected.joinToString(", ") { it.name }
            }
        }

        pickTimeButton.setOnClickListener {
            val h = selectedMinutes / 60
            val m = selectedMinutes % 60
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedMinutes = hourOfDay * 60 + minute
                selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)
            }, h, m, true).show()
        }

        slotGroup.setOnCheckedChangeListener { _, _ ->
            selectedMinutes = when (currentSlot()) {
                IntakeSlot.MORNING -> TimePreferences.getMorningMinutes(this)
                IntakeSlot.NOON -> TimePreferences.getNoonMinutes(this)
                IntakeSlot.EVENING -> TimePreferences.getEveningMinutes(this)
            }
            selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)
        }

        selectMedsButton.setOnClickListener {
            if (allMedications.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_medications_for_add), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val names = allMedications.map { it.name }.toTypedArray()
            val checked = BooleanArray(allMedications.size) { idx ->
                selectedMedicationIds.contains(allMedications[idx].id)
            }
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_medications))
                .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    selectedMedicationIds.clear()
                    checked.forEachIndexed { index, isChecked ->
                        if (isChecked) selectedMedicationIds.add(allMedications[index].id)
                    }
                    refreshSelectedMedsText()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        val scheduleId = intent.getIntExtra("SCHEDULE_ID", -1)

        lifecycleScope.launch(Dispatchers.IO) {
            allMedications = medicationDao.getAll()
            val editing = if (scheduleId != -1) scheduleDao.getWithMedicationsById(scheduleId) else null
            withContext(Dispatchers.Main) {
                if (editing != null) {
                    nameInput.setText(editing.schedule.name)
                    when (editing.schedule.slot) {
                        IntakeSlot.MORNING -> morning.isChecked = true
                        IntakeSlot.NOON -> noon.isChecked = true
                        IntakeSlot.EVENING -> evening.isChecked = true
                    }
                    selectedMinutes = editing.schedule.timeMinutes
                    selectedTimeText.text = TimePreferences.formatMinutes(selectedMinutes)
                    selectedMedicationIds.clear()
                    editing.medications.forEach { selectedMedicationIds.add(it.id) }
                }
                refreshSelectedMedsText()
            }
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_schedule_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedMedicationIds.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_select_at_least_one_medication), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val slot = currentSlot()
            val schedule = Schedule(
                id = if (scheduleId == -1) 0 else scheduleId,
                name = name,
                slot = slot,
                timeMinutes = selectedMinutes,
                isActive = true
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val targetId = if (scheduleId == -1) {
                    scheduleDao.insert(schedule).toInt()
                } else {
                    scheduleDao.update(schedule)
                    scheduleId
                }

                scheduleDao.deleteCrossRefsForSchedule(targetId)
                val refs = selectedMedicationIds.mapIndexed { index, medId ->
                    ScheduleMedicationCrossRef(targetId, medId, index)
                }
                scheduleDao.insertCrossRefs(refs)

                AlarmScheduler.scheduleAll(this@MedicationRegistrationActivity)
                WidgetUtils.refreshMedicationWidgets(this@MedicationRegistrationActivity)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MedicationRegistrationActivity, getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
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
    }
}

