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
        // If there are no registered medications or no meds for this slot, do not notify
        val dao = MedicationDatabase.getInstance(context).medicationDao()
        val meds = try { dao.getAllSync() } catch (_: Exception) { emptyList() }
        if (meds.isEmpty()) {
            AlarmScheduler.cancelAll(context)
            return
        }
        val slotName = intent.getStringExtra("slot") ?: IntakeSlot.MORNING.name
        val slot = IntakeSlot.valueOf(slotName)
        if (meds.none { it.timing.contains(slot) }) {
            // No longer need this slot
            AlarmScheduler.scheduleAll(context)
            return
        }

        val channelId = CHANNEL_ID
        ensureChannel(context, channelId)

        val title = when (slot) {
            IntakeSlot.MORNING -> context.getString(R.string.morning)
            IntakeSlot.NOON -> context.getString(R.string.noon)
            IntakeSlot.EVENING -> context.getString(R.string.evening)
        }
        val message = context.getString(R.string.notification_message, title)

        val tapIntent = Intent(context, MainActivity::class.java)
        val tapPi = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(slot.ordinal + 1, notification)

        // Schedule next day for this slot again
        val minutes = when (slot) {
            IntakeSlot.MORNING -> TimePreferences.getMorningMinutes(context)
            IntakeSlot.NOON -> TimePreferences.getNoonMinutes(context)
            IntakeSlot.EVENING -> TimePreferences.getEveningMinutes(context)
        }
        // Reschedule using the same requestCode mapping
        when (slot) {
            IntakeSlot.MORNING -> AlarmScheduler.scheduleAll(context) // simple: reschedule all
            IntakeSlot.NOON -> AlarmScheduler.scheduleAll(context)
            IntakeSlot.EVENING -> AlarmScheduler.scheduleAll(context)
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
