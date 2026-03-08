package com.example.preventforgettingmedicationandroidapp.di

import android.content.Context
import com.example.preventforgettingmedicationandroidapp.IntakeHistoryDao
import com.example.preventforgettingmedicationandroidapp.MedicationDao
import com.example.preventforgettingmedicationandroidapp.MedicationDatabase
import com.example.preventforgettingmedicationandroidapp.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MedicationDatabase =
        MedicationDatabase.getInstance(context)

    @Provides
    fun provideMedicationDao(db: MedicationDatabase): MedicationDao = db.medicationDao()

    @Provides
    fun provideScheduleDao(db: MedicationDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideIntakeHistoryDao(db: MedicationDatabase): IntakeHistoryDao = db.intakeHistoryDao()
}
