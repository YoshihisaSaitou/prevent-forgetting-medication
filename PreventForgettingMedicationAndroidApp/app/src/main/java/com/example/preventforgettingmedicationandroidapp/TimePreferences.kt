package com.example.preventforgettingmedicationandroidapp

import android.content.Context

object TimePreferences {
    private const val PREFS = "med_times"
    private const val KEY_MORNING = "time_morning"
    private const val KEY_NOON = "time_noon"
    private const val KEY_EVENING = "time_evening"

    private const val DEFAULT_MORNING = 7 * 60      // 07:00
    private const val DEFAULT_NOON = 12 * 60        // 12:00
    private const val DEFAULT_EVENING = 19 * 60     // 19:00

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMorningMinutes(context: Context): Int =
        prefs(context).getInt(KEY_MORNING, DEFAULT_MORNING)

    fun getNoonMinutes(context: Context): Int =
        prefs(context).getInt(KEY_NOON, DEFAULT_NOON)

    fun getEveningMinutes(context: Context): Int =
        prefs(context).getInt(KEY_EVENING, DEFAULT_EVENING)

    fun setMorningMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_MORNING, minutes).apply()
    }

    fun setNoonMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_NOON, minutes).apply()
    }

    fun setEveningMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_EVENING, minutes).apply()
    }

    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return String.format("%02d:%02d", h, m)
    }
}

