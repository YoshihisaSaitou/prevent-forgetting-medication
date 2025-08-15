package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MedicationDao {
    @Insert
    fun insert(medication: Medication)

    @Query("SELECT * FROM medications")
    fun getAll(): List<Medication>
}
