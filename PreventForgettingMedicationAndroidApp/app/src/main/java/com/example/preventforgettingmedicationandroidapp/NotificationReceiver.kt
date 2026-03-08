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

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getIntExtra("schedule_id", -1)
        if (scheduleId == -1) {
            AlarmScheduler.scheduleAll(context)
            return
        }

        val scheduleDao = MedicationDatabase.getInstance(context).scheduleDao()
        val scheduleWithMeds = try {
            scheduleDao.getWithMedicationsByIdSync(scheduleId)
        } catch (_: Exception) {
            null
        }

        if (scheduleWithMeds == null || scheduleWithMeds.medications.isEmpty() || !scheduleWithMeds.schedule.isActive) {
            AlarmScheduler.scheduleAll(context)
            return
        }

        ensureChannel(context, CHANNEL_ID)

        val medsPreview = scheduleWithMeds.medications.take(3).joinToString(", ") { it.name }
        val title = context.getString(R.string.notification_title)
        val message = context.getString(
            R.string.notification_message_schedule,
            scheduleWithMeds.schedule.name,
            medsPreview
        )

        val tapIntent = Intent(context, MainActivity::class.java)
        val tapPi = PendingIntent.getActivity(
            context,
            scheduleId,
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

        nm.notify(scheduleId, notification)

        AlarmScheduler.scheduleAll(context)
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

