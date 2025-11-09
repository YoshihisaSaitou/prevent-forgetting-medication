package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val morningPicker = findViewById<TimePicker>(R.id.time_morning)
        val noonPicker = findViewById<TimePicker>(R.id.time_noon)
        val eveningPicker = findViewById<TimePicker>(R.id.time_evening)
        val saveButton = findViewById<Button>(R.id.save_times_button)

        morningPicker.setIs24HourView(true)
        noonPicker.setIs24HourView(true)
        eveningPicker.setIs24HourView(true)

        // Initialize from preferences
        setPickerTime(morningPicker, TimePreferences.getMorningMinutes(this))
        setPickerTime(noonPicker, TimePreferences.getNoonMinutes(this))
        setPickerTime(eveningPicker, TimePreferences.getEveningMinutes(this))

        saveButton.setOnClickListener {
            TimePreferences.setMorningMinutes(this, getPickerMinutes(morningPicker))
            TimePreferences.setNoonMinutes(this, getPickerMinutes(noonPicker))
            TimePreferences.setEveningMinutes(this, getPickerMinutes(eveningPicker))

            // Reschedule alarms with new times
            AlarmScheduler.scheduleAll(this)

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        // Footer menu
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
            // already here
        }
    }

    private fun setPickerTime(picker: TimePicker, minutes: Int) {
        val h = minutes / 60
        val m = minutes % 60
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            picker.hour = h
            picker.minute = m
        } else {
            @Suppress("DEPRECATION")
            run {
                picker.currentHour = h
                picker.currentMinute = m
            }
        }
    }

    private fun getPickerMinutes(picker: TimePicker): Int {
        val h: Int
        val m: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            h = picker.hour
            m = picker.minute
        } else {
            @Suppress("DEPRECATION")
            run {
                h = picker.currentHour
                m = picker.currentMinute
            }
        }
        return h * 60 + m
    }
}
