package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Medication::class], version = 1)
@TypeConverters(Converters::class)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: MedicationDatabase? = null

        fun getInstance(context: Context): MedicationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicationDatabase::class.java,
                    "medications.db"
                ).allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
