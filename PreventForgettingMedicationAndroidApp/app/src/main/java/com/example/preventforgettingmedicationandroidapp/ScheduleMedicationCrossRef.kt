package com.example.preventforgettingmedicationandroidapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "schedule_medications",
    primaryKeys = ["scheduleId", "medicationId"],
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId"), Index("medicationId")]
)
data class ScheduleMedicationCrossRef(
    val scheduleId: Int,
    val medicationId: Int,
    val displayOrder: Int = 0
)
