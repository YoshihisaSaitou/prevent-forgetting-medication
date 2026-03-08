package com.example.preventforgettingmedicationandroidapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.preventforgettingmedicationandroidapp.application.usecase.GetScheduleByIdUseCase
import com.example.preventforgettingmedicationandroidapp.application.usecase.SyncAlarmsUseCase
import com.example.preventforgettingmedicationandroidapp.domain.model.ScheduleId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getScheduleByIdUseCase: GetScheduleByIdUseCase

    @Inject
    lateinit var syncAlarmsUseCase: SyncAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleIdRaw = intent.getIntExtra("schedule_id", -1)
        if (scheduleIdRaw <= 0) {
            syncAlarmsUseCase()
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduleWithMeds = getScheduleByIdUseCase(ScheduleId(scheduleIdRaw))
                if (scheduleWithMeds == null || scheduleWithMeds.medications.isEmpty() || !scheduleWithMeds.schedule.isActive) {
                    return@launch
                }

                val medsPreview = scheduleWithMeds.medications.take(3).joinToString(", ") { it.name }
                val title = context.getString(R.string.notification_title)
                val message = context.getString(
                    R.string.notification_message_schedule,
                    scheduleWithMeds.schedule.name,
                    medsPreview
                )

                withContext(Dispatchers.Main) {
                    ensureChannel(context, CHANNEL_ID)

                    val tapIntent = Intent(context, MainActivity::class.java)
                    val tapPi = PendingIntent.getActivity(
                        context,
                        scheduleIdRaw,
                        tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setContentIntent(tapPi)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    nm.notify(scheduleIdRaw, notification)
                }
            } finally {
                syncAlarmsUseCase()
                pending.finish()
            }
        }
    }

    private fun ensureChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
    }
}