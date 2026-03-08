package com.example.preventforgettingmedicationandroidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationMasterActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).medicationDao() }
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

        adapter = MedicationMasterAdapter(this, mutableListOf()) {
            loadMedications()
        }

        val listView = findViewById<ListView>(R.id.medication_master_list)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty_message)

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
        loadMedications()
    }

    private fun loadMedications() {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = dao.getAll()
            withContext(Dispatchers.Main) {
                adapter.setItems(items)
            }
        }
    }
}

