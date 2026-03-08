package com.example.preventforgettingmedicationandroidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.preventforgettingmedicationandroidapp.application.usecase.SyncAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncAlarmsUseCase: SyncAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            syncAlarmsUseCase()
        }
    }
}