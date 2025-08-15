package com.example.preventforgettingmedicationandroidapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mealTiming: MealTiming?,
    val timing: Set<IntakeSlot>
)
