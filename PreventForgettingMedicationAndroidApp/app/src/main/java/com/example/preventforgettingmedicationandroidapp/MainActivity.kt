package com.example.preventforgettingmedicationandroidapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.preventforgettingmedicationandroidapp.application.usecase.SyncAlarmsUseCase
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MainEvent
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var syncAlarmsUseCase: SyncAlarmsUseCase

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

        adapter = MedicationAdapter(
            context = this,
            items = mutableListOf(),
            onTake = { viewModel.execute(it) },
            onEdit = {
                startActivity(Intent(this, MedicationRegistrationActivity::class.java).putExtra("SCHEDULE_ID", it.value))
            },
            onDelete = { scheduleId ->
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete_schedule))
                    .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.delete(scheduleId) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )

        val listView = findViewById<ListView>(R.id.medication_list)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty_message)
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            startActivity(Intent(this, MedicationRegistrationActivity::class.java).putExtra("SCHEDULE_ID", item.id.value))
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

        lifecycleScope.launch {
            viewModel.items.collect { adapter.setItems(it) }
        }
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainEvent.Recorded -> Toast.makeText(this@MainActivity, getString(R.string.taken_recorded_count, event.count), Toast.LENGTH_SHORT).show()
                    MainEvent.Duplicate -> Toast.makeText(this@MainActivity, getString(R.string.duplicate_schedule_skipped), Toast.LENGTH_SHORT).show()
                    MainEvent.Disabled -> Toast.makeText(this@MainActivity, getString(R.string.schedule_disabled_temporarily), Toast.LENGTH_SHORT).show()
                    MainEvent.Missing -> Toast.makeText(this@MainActivity, getString(R.string.no_schedules), Toast.LENGTH_SHORT).show()
                    is MainEvent.Error -> Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        syncAlarmsUseCase()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}
