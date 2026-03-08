package com.example.preventforgettingmedicationandroidapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    private const val PREFS = "schedule_alarm_state"
    private const val KEY_SCHEDULE_IDS = "scheduled_ids"
    private const val ACTION_REMIND_SCHEDULE = "com.example.preventforgettingmedicationandroidapp.REMIND_SCHEDULE"

    fun scheduleAll(context: Context) {
        val dao = MedicationDatabase.getInstance(context).scheduleDao()
        val schedules = try {
            dao.getActiveSchedulesSync()
        } catch (_: Exception) {
            emptyList()
        }

        cancelStored(context)
        if (schedules.isEmpty()) {
            saveScheduledIds(context, emptySet())
            return
        }

        val ids = mutableSetOf<Int>()
        schedules.forEach { schedule ->
            scheduleFor(context, schedule.id, schedule.timeMinutes)
            ids.add(schedule.id)
        }
        saveScheduledIds(context, ids)
    }

    fun cancelAll(context: Context) {
        cancelStored(context)
        saveScheduledIds(context, emptySet())
    }

    private fun scheduleFor(context: Context, scheduleId: Int, minutes: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, scheduleId)
        val triggerAt = nextTriggerTime(minutes).timeInMillis

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelStored(context: Context) {
        loadScheduledIds(context).forEach { cancel(context, it) }
    }

    private fun cancel(context: Context, scheduleId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, scheduleId)
        am.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(context: Context, scheduleId: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_REMIND_SCHEDULE
            putExtra("schedule_id", scheduleId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, scheduleId, intent, flags)
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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun saveScheduledIds(context: Context, ids: Set<Int>) {
        val data = ids.joinToString(",")
        prefs(context).edit().putString(KEY_SCHEDULE_IDS, data).apply()
    }

    private fun loadScheduledIds(context: Context): Set<Int> {
        val data = prefs(context).getString(KEY_SCHEDULE_IDS, "") ?: ""
        if (data.isBlank()) return emptySet()
        return data.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
