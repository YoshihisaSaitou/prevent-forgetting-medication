package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MedicationMasterEvent
import com.example.preventforgettingmedicationandroidapp.presentation.viewmodel.MedicationMasterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationMasterActivity : AppCompatActivity() {
    private val viewModel: MedicationMasterViewModel by viewModels()
    private lateinit var adapter: MedicationMasterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medication_master)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = MedicationMasterAdapter(
            items = mutableListOf(),
            onEdit = {
                startActivity(Intent(this, MedicationMasterEditActivity::class.java).putExtra("MED_ID", it.value))
            },
            onDelete = {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete_medication_master))
                    .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.delete(it) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )

        val listView = findViewById<ListView>(R.id.medication_master_list)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty_message)

        lifecycleScope.launch {
            viewModel.items.collect { adapter.setItems(it) }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    MedicationMasterEvent.Deleted -> Toast.makeText(this@MedicationMasterActivity, getString(R.string.delete), Toast.LENGTH_SHORT).show()
                    MedicationMasterEvent.InUse -> Toast.makeText(this@MedicationMasterActivity, getString(R.string.medication_delete_blocked_in_use), Toast.LENGTH_SHORT).show()
                    is MedicationMasterEvent.Error -> Toast.makeText(this@MedicationMasterActivity, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.add_medication_button).setOnClickListener {
            startActivity(Intent(this, MedicationMasterEditActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}
