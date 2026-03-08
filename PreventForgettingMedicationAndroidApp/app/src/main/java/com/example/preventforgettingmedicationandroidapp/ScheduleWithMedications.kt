package com.example.preventforgettingmedicationandroidapp

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ScheduleWithMedications(
    @Embedded val schedule: Schedule,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ScheduleMedicationCrossRef::class,
            parentColumn = "scheduleId",
            entityColumn = "medicationId"
        )
    )
    val medications: List<Medication>
)
