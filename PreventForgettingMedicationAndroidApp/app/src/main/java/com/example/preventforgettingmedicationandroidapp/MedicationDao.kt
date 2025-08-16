package com.example.preventforgettingmedicationandroidapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MedicationDao {
    @Insert
    suspend fun insert(medication: Medication)

    @Update
    suspend fun update(medication: Medication)

    @Query("SELECT * FROM medications")
    suspend fun getAll(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Int): Medication?

    @Delete
    suspend fun delete(medication: Medication)
}
