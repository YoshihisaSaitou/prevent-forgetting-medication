package com.example.preventforgettingmedicationandroidapp

import android.content.Context

object TakenStateStore {
    private const val PREFS = "taken_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(scheduleId: Int) = "schedule_disabled_until_$scheduleId"

    fun getDisabledUntil(context: Context, scheduleId: Int): Long =
        prefs(context).getLong(key(scheduleId), 0L)

    fun setDisabledForFiveMinutes(context: Context, scheduleId: Int) {
        val until = System.currentTimeMillis() + 5 * 60 * 1000L
        prefs(context).edit().putLong(key(scheduleId), until).apply()
    }

    fun clearDisabled(context: Context, scheduleId: Int) {
        prefs(context).edit().remove(key(scheduleId)).apply()
    }

    fun isEnabled(context: Context, scheduleId: Int): Boolean =
        System.currentTimeMillis() >= getDisabledUntil(context, scheduleId)
}
