package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MedicationDao {
    @Insert
    fun insert(medication: Medication)

    @Update
    fun update(medication: Medication)

    @Query("SELECT * FROM medications")
    fun getAll(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getById(id: Int): Medication?
}
