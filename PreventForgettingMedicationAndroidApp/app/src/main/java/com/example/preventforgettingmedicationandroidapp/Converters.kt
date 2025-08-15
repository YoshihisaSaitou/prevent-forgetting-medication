package com.example.preventforgettingmedicationandroidapp

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMealTiming(value: MealTiming?): String? = value?.name

    @TypeConverter
    fun toMealTiming(value: String?): MealTiming? = value?.let { MealTiming.valueOf(it) }

    @TypeConverter
    fun fromIntakeSlots(slots: Set<IntakeSlot>): String =
        slots.joinToString(",") { it.name }

    @TypeConverter
    fun toIntakeSlots(data: String): Set<IntakeSlot> =
        if (data.isEmpty()) emptySet() else data.split(",").map { IntakeSlot.valueOf(it) }.toSet()
}
