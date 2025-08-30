package com.example.preventforgettingmedicationandroidapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val dao by lazy { MedicationDatabase.getInstance(this).medicationDao() }
    private lateinit var adapter: MedicationAdapter
    private var medications: List<Medication> = emptyList()

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
        // Set empty view for the list
        listView.emptyView = findViewById(R.id.empty_message)
        listView.setOnItemClickListener { _, _, position, _ ->
            val medication = medications[position]
            val intent = Intent(this, MedicationRegistrationActivity::class.java)
            intent.putExtra("MED_ID", medication.id)
            startActivity(intent)
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val medication = medications[position]
            AlertDialog.Builder(this)
                .setMessage("Delete this medication?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        dao.delete(medication)
                        loadMedications()
                    }
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        findViewById<Button>(R.id.add_medication_button).setOnClickListener {
            startActivity(Intent(this, MedicationRegistrationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadMedications()
        }
    }

    private suspend fun loadMedications() {
        medications = dao.getAll()
        adapter.clear()
        adapter.addAll(medications)
        adapter.notifyDataSetChanged()
    }
}
