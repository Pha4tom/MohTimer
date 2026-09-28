package com.Cali.mohtimer

import android.content.Context

object DevPrefs {
    private const val PREFS = "smart_timer_dev"
    private const val KEY_UNLOCKED = "dev_unlocked"
    private const val KEY_SPEED = "dev_speed"

    fun isUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)

    fun setUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_UNLOCKED, unlocked)
            .apply()
    }

    fun getSpeed(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SPEED, 1)

    fun setSpeed(context: Context, speed: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SPEED, speed.coerceAtLeast(1))
            .apply()
    }
}