package com.example.preventforgettingmedicationandroidapp

import android.content.Context

object TakenStateStore {
    private const val PREFS = "taken_state"
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(medId: Int) = "med_disabled_until_$medId"

    fun getDisabledUntil(context: Context, medId: Int): Long =
        prefs(context).getLong(key(medId), 0L)

    fun setDisabledForFiveMinutes(context: Context, medId: Int) {
        val until = System.currentTimeMillis() + 5 * 60 * 1000L
        prefs(context).edit().putLong(key(medId), until).apply()
    }

    fun clearDisabled(context: Context, medId: Int) {
        prefs(context).edit().remove(key(medId)).apply()
    }

    fun isEnabled(context: Context, medId: Int): Boolean =
        System.currentTimeMillis() >= getDisabledUntil(context, medId)
}

