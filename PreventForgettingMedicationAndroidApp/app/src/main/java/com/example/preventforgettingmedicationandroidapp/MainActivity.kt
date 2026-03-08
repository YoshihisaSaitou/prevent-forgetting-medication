package com.example.preventforgettingmedicationandroidapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val scheduleDao by lazy { MedicationDatabase.getInstance(this).scheduleDao() }
    private lateinit var adapter: MedicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = MedicationAdapter(this, mutableListOf())
        val listView = findViewById<ListView>(R.id.medication_list)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty_message)
        listView.setOnItemClickListener { _, _, position, _ ->
            val schedule = adapter.getItem(position).schedule
            val intent = Intent(this, MedicationRegistrationActivity::class.java)
            intent.putExtra("SCHEDULE_ID", schedule.id)
            startActivity(intent)
        }

        findViewById<Button>(R.id.footer_add).setOnClickListener {
            startActivity(Intent(this, MedicationRegistrationActivity::class.java))
        }
        findViewById<Button>(R.id.footer_list).setOnClickListener {
            // already on list
        }
        findViewById<Button>(R.id.footer_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.footer_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestNotificationPermissionIfNeeded()
        AlarmScheduler.scheduleAll(this)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadSchedules()
        }
    }

    private suspend fun loadSchedules() {
        val schedules = withContext(Dispatchers.IO) { scheduleDao.getAllWithMedications() }
        adapter.setItems(schedules)
        WidgetUtils.refreshMedicationWidgets(this)
        if (schedules.isEmpty()) {
            AlarmScheduler.cancelAll(this)
        } else {
            AlarmScheduler.scheduleAll(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
