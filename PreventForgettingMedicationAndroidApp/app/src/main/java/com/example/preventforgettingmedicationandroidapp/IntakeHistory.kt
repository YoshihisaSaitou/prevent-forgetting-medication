package com.example.preventforgettingmedicationandroidapp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intake_history",
    indices = [
        Index(value = ["scheduleId", "takenAt"]),
        Index(value = ["medicationId", "takenAt"])
    ]
)
data class IntakeHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleId: Int?,
    val scheduleName: String?,
    val medicationId: Int,
    val medicationName: String,
    val takenAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val incorrectAt: Long? = null
)
