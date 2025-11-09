package com.example.preventforgettingmedicationandroidapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    private const val REQ_MORNING = 1001
    private const val REQ_NOON = 1002
    private const val REQ_EVENING = 1003

    fun scheduleAll(context: Context) {
        val dao = MedicationDatabase.getInstance(context).medicationDao()
        val meds = try { dao.getAllSync() } catch (_: Exception) { emptyList() }
        cancelAll(context)
        if (meds.isEmpty()) return

        val needsMorning = meds.any { it.timing.contains(IntakeSlot.MORNING) }
        val needsNoon = meds.any { it.timing.contains(IntakeSlot.NOON) }
        val needsEvening = meds.any { it.timing.contains(IntakeSlot.EVENING) }

        if (needsMorning) {
            scheduleFor(context, IntakeSlot.MORNING, TimePreferences.getMorningMinutes(context), REQ_MORNING)
        } else {
            cancel(context, REQ_MORNING)
        }
        if (needsNoon) {
            scheduleFor(context, IntakeSlot.NOON, TimePreferences.getNoonMinutes(context), REQ_NOON)
        } else {
            cancel(context, REQ_NOON)
        }
        if (needsEvening) {
            scheduleFor(context, IntakeSlot.EVENING, TimePreferences.getEveningMinutes(context), REQ_EVENING)
        } else {
            cancel(context, REQ_EVENING)
        }
    }

    fun cancelAll(context: Context) {
        cancel(context, REQ_MORNING)
        cancel(context, REQ_NOON)
        cancel(context, REQ_EVENING)
    }

    private fun scheduleFor(context: Context, slot: IntakeSlot, minutes: Int, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, slot, requestCode)

        val cal = nextTriggerTime(minutes)
        val triggerAt = cal.timeInMillis

        // Use setExactAndAllowWhileIdle for timely alarms; reschedule next occurrence on receive
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            // Fallback if exact not permitted
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        am.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(context: Context, slot: IntakeSlot, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.preventforgettingmedicationandroidapp.REMIND_${slot.name}"
            putExtra("slot", slot.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun nextTriggerTime(minutes: Int): Calendar {
        val h = minutes / 60
        val m = minutes % 60
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        if (!cal.after(now)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal
    }
}
