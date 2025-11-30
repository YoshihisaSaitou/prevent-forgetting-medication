package com.example.preventforgettingmedicationandroidapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intake_history")
data class IntakeHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val takenAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val incorrectAt: Long? = null
)
