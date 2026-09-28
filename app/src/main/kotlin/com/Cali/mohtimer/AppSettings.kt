package com.Cali.mohtimer

import android.content.Context

object AppSettings {
    private const val PREFS = "smart_timer_settings"

    // Existing keys
    private const val KEY_GET_READY_ENABLED = "get_ready_enabled"
    private const val KEY_GET_READY_SECONDS = "get_ready_seconds"
    private const val KEY_VOICE_ENABLED = "voice_enabled"
    private const val KEY_BEEP_ENABLED = "beep_enabled"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_SOUND_THEME = "sound_theme"

    // New keys
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_VOICE_TYPE = "voice_type"
    private const val KEY_COUNT_UP = "count_up"
    private const val KEY_EMOM_FROM = "emom_from"
    private const val KEY_TABATA_REST_AFTER_LAST = "tabata_rest_after_last"

    // Sound theme IDs
    const val THEME_CLASSIC = 0
    const val THEME_DIGITAL = 1
    const val THEME_ALARM = 2
    const val THEME_CUSTOM = 3

    // Voice types
    const val VOICE_MALE = 0
    const val VOICE_FEMALE = 1
    const val VOICE_SYSTEM = 2

    // EMOM from
    const val EMOM_FROM_ONE = 0
    const val EMOM_FROM_ZERO = 1

    // Accent color IDs
    const val COLOR_MAGENTA = 0
    const val COLOR_ORANGE = 1
    const val COLOR_BLUE = 2
    const val COLOR_GREEN = 3
    const val COLOR_PURPLE = 4

    // -------- existing getters/setters --------

    fun isGetReadyEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_GET_READY_ENABLED, true)
    fun setGetReadyEnabled(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_GET_READY_ENABLED, v).apply()

    fun getGetReadySeconds(ctx: Context) =
        prefs(ctx).getInt(KEY_GET_READY_SECONDS, 10).coerceIn(3, 30)
    fun setGetReadySeconds(ctx: Context, v: Int) =
        prefs(ctx).edit().putInt(KEY_GET_READY_SECONDS, v.coerceIn(3, 30)).apply()

    fun isVoiceEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_VOICE_ENABLED, true)
    fun setVoiceEnabled(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_VOICE_ENABLED, v).apply()

    fun isBeepEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_BEEP_ENABLED, true)
    fun setBeepEnabled(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BEEP_ENABLED, v).apply()

    fun isKeepScreenOn(ctx: Context) = prefs(ctx).getBoolean(KEY_KEEP_SCREEN_ON, false)
    fun setKeepScreenOn(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_KEEP_SCREEN_ON, v).apply()

    fun isVibrationEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_VIBRATION_ENABLED, false)
    fun setVibrationEnabled(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_VIBRATION_ENABLED, v).apply()

    fun getSoundTheme(ctx: Context) = prefs(ctx).getInt(KEY_SOUND_THEME, THEME_CLASSIC)
    fun setSoundTheme(ctx: Context, theme: Int) =
        prefs(ctx).edit().putInt(KEY_SOUND_THEME, theme).apply()

    // -------- new getters/setters --------

    fun getAccentColor(ctx: Context) = prefs(ctx).getInt(KEY_ACCENT_COLOR, COLOR_MAGENTA)
    fun setAccentColor(ctx: Context, c: Int) =
        prefs(ctx).edit().putInt(KEY_ACCENT_COLOR, c).apply()

    fun isNotificationsEnabled(ctx: Context) =
        prefs(ctx).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    fun setNotificationsEnabled(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, v).apply()

    fun getVoiceType(ctx: Context) = prefs(ctx).getInt(KEY_VOICE_TYPE, VOICE_MALE)
    fun setVoiceType(ctx: Context, t: Int) =
        prefs(ctx).edit().putInt(KEY_VOICE_TYPE, t).apply()

    fun isCountUp(ctx: Context) = prefs(ctx).getBoolean(KEY_COUNT_UP, false)
    fun setCountUp(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_COUNT_UP, v).apply()

    fun getEmomFrom(ctx: Context) = prefs(ctx).getInt(KEY_EMOM_FROM, EMOM_FROM_ONE)
    fun setEmomFrom(ctx: Context, v: Int) =
        prefs(ctx).edit().putInt(KEY_EMOM_FROM, v).apply()

    fun isTabataRestAfterLast(ctx: Context) =
        prefs(ctx).getBoolean(KEY_TABATA_REST_AFTER_LAST, false)
    fun setTabataRestAfterLast(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_TABATA_REST_AFTER_LAST, v).apply()

    // -------- helper: resolve accent color to hex --------

    fun resolveAccentColor(ctx: Context): Int {
        return when (getAccentColor(ctx)) {
            COLOR_MAGENTA -> 0xFFFF00E5.toInt()
            COLOR_ORANGE -> 0xFFFF6A00.toInt()
            COLOR_BLUE -> 0xFF00B0FF.toInt()
            COLOR_GREEN -> 0xFF00E676.toInt()
            COLOR_PURPLE -> 0xFFB026FF.toInt()
            else -> 0xFFFF00E5.toInt()
        }
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}