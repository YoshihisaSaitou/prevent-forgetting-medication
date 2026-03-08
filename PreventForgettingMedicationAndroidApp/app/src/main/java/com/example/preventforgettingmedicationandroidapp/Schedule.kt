package com.example.preventforgettingmedicationandroidapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val slot: IntakeSlot,
    val timeMinutes: Int,
    val isActive: Boolean = true
)
