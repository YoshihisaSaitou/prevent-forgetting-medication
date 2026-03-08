package com.example.preventforgettingmedicationandroidapp.di

import com.example.preventforgettingmedicationandroidapp.application.port.ReminderPort
import com.example.preventforgettingmedicationandroidapp.application.port.TakenStatePort
import com.example.preventforgettingmedicationandroidapp.application.port.WidgetPort
import com.example.preventforgettingmedicationandroidapp.domain.repository.HistoryRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.MedicationRepository
import com.example.preventforgettingmedicationandroidapp.domain.repository.ScheduleRepository
import com.example.preventforgettingmedicationandroidapp.infrastructure.adapter.AlarmReminderAdapter
import com.example.preventforgettingmedicationandroidapp.infrastructure.adapter.TakenStateAdapter
import com.example.preventforgettingmedicationandroidapp.infrastructure.adapter.WidgetAdapter
import com.example.preventforgettingmedicationandroidapp.infrastructure.repository.RoomHistoryRepository
import com.example.preventforgettingmedicationandroidapp.infrastructure.repository.RoomMedicationRepository
import com.example.preventforgettingmedicationandroidapp.infrastructure.repository.RoomScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: RoomScheduleRepository): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindMedicationRepository(impl: RoomMedicationRepository): MedicationRepository

    @Binds
    @Singleton
    abstract fun bindReminderPort(impl: AlarmReminderAdapter): ReminderPort

    @Binds
    @Singleton
    abstract fun bindWidgetPort(impl: WidgetAdapter): WidgetPort

    @Binds
    @Singleton
    abstract fun bindTakenStatePort(impl: TakenStateAdapter): TakenStatePort
}
