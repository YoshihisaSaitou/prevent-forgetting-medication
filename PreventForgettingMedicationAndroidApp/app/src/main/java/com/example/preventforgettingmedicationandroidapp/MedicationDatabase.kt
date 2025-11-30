package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Medication::class, IntakeHistory::class], version = 6)
@TypeConverters(Converters::class)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
