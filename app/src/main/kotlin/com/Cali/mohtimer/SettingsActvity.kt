package com.Cali.mohtimer

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchGetReady: MaterialSwitch
    private lateinit var switchVoice: MaterialSwitch
    private lateinit var switchBeep: MaterialSwitch
    private lateinit var switchKeepScreenOn: MaterialSwitch
    private lateinit var switchVibration: MaterialSwitch
    private lateinit var switchNotifications: MaterialSwitch
    private lateinit var switchCountUp: MaterialSwitch
    private lateinit var switchTabataRestAfterLast: MaterialSwitch

    private lateinit var groupDuration: View
    private lateinit var btnSec3: MaterialButton
    private lateinit var btnSec5: MaterialButton
    private lateinit var btnSec10: MaterialButton
    private lateinit var btnSec15: MaterialButton

    private lateinit var themeClassic: TextView
    private lateinit var themeDigital: TextView
    private lateinit var themeAlarm: TextView
    private lateinit var themeCustom: TextView

    private lateinit var voiceMale: TextView
    private lateinit var voiceFemale: TextView
    private lateinit var voiceSystem: TextView

    private lateinit var btnEmomFromOne: MaterialButton
    private lateinit var btnEmomFromZero: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchGetReady = findViewById(R.id.switchGetReady)
        switchVoice = findViewById(R.id.switchVoice)
        switchBeep = findViewById(R.id.switchBeep)
        switchKeepScreenOn = findViewById(R.id.switchKeepScreenOn)
        switchVibration = findViewById(R.id.switchVibration)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchCountUp = findViewById(R.id.switchCountUp)
        switchTabataRestAfterLast = findViewById(R.id.switchTabataRestAfterLast)

        groupDuration = findViewById(R.id.groupDuration)
        btnSec3 = findViewById(R.id.btnSec3)
        btnSec5 = findViewById(R.id.btnSec5)
        btnSec10 = findViewById(R.id.btnSec10)
        btnSec15 = findViewById(R.id.btnSec15)

        themeClassic = findViewById(R.id.themeClassic)
        themeDigital = findViewById(R.id.themeDigital)
        themeAlarm = findViewById(R.id.themeAlarm)
        themeCustom = findViewById(R.id.themeCustom)

        voiceMale = findViewById(R.id.voiceMale)
        voiceFemale = findViewById(R.id.voiceFemale)
        voiceSystem = findViewById(R.id.voiceSystem)

        btnEmomFromOne = findViewById(R.id.btnEmomFromOne)
        btnEmomFromZero = findViewById(R.id.btnEmomFromZero)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Load values
        switchGetReady.isChecked = AppSettings.isGetReadyEnabled(this)
        switchVoice.isChecked = AppSettings.isVoiceEnabled(this)
        switchBeep.isChecked = AppSettings.isBeepEnabled(this)
        switchKeepScreenOn.isChecked = AppSettings.isKeepScreenOn(this)
        switchVibration.isChecked = AppSettings.isVibrationEnabled(this)
        switchNotifications.isChecked = AppSettings.isNotificationsEnabled(this)
        switchCountUp.isChecked = AppSettings.isCountUp(this)
        switchTabataRestAfterLast.isChecked = AppSettings.isTabataRestAfterLast(this)

        applyDurationState(switchGetReady.isChecked)
        highlightSelectedDuration(AppSettings.getGetReadySeconds(this))
        highlightSelectedTheme(AppSettings.getSoundTheme(this))
        highlightSelectedVoice(AppSettings.getVoiceType(this))
        highlightEmomFrom(AppSettings.getEmomFrom(this))
        highlightAccentColor(AppSettings.getAccentColor(this))

        // Listeners
        switchGetReady.setOnCheckedChangeListener { _, v ->
            AppSettings.setGetReadyEnabled(this, v)
            applyDurationState(v)
        }
        switchVoice.setOnCheckedChangeListener { _, v -> AppSettings.setVoiceEnabled(this, v) }
        switchBeep.setOnCheckedChangeListener { _, v -> AppSettings.setBeepEnabled(this, v) }
        switchKeepScreenOn.setOnCheckedChangeListener { _, v -> AppSettings.setKeepScreenOn(this, v) }
        switchVibration.setOnCheckedChangeListener { _, v -> AppSettings.setVibrationEnabled(this, v) }
        switchNotifications.setOnCheckedChangeListener { _, v -> AppSettings.setNotificationsEnabled(this, v) }
        switchCountUp.setOnCheckedChangeListener { _, v -> AppSettings.setCountUp(this, v) }
        switchTabataRestAfterLast.setOnCheckedChangeListener { _, v -> AppSettings.setTabataRestAfterLast(this, v) }

        btnSec3.setOnClickListener { setSeconds(3) }
        btnSec5.setOnClickListener { setSeconds(5) }
        btnSec10.setOnClickListener { setSeconds(10) }
        btnSec15.setOnClickListener { setSeconds(15) }

        themeClassic.setOnClickListener {
            AppSettings.setSoundTheme(this, AppSettings.THEME_CLASSIC)
            highlightSelectedTheme(AppSettings.THEME_CLASSIC)
            previewTheme(ToneGenerator.TONE_PROP_BEEP)
        }
        themeDigital.setOnClickListener {
            AppSettings.setSoundTheme(this, AppSettings.THEME_DIGITAL)
            highlightSelectedTheme(AppSettings.THEME_DIGITAL)
            previewTheme(ToneGenerator.TONE_CDMA_PIP)
        }
        themeAlarm.setOnClickListener {
            AppSettings.setSoundTheme(this, AppSettings.THEME_ALARM)
            highlightSelectedTheme(AppSettings.THEME_ALARM)
            previewTheme(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
        }
        themeCustom.setOnClickListener {
            AppSettings.setSoundTheme(this, AppSettings.THEME_CUSTOM)
            highlightSelectedTheme(AppSettings.THEME_CUSTOM)
        }

        voiceMale.setOnClickListener {
            AppSettings.setVoiceType(this, AppSettings.VOICE_MALE)
            highlightSelectedVoice(AppSettings.VOICE_MALE)
        }
        voiceFemale.setOnClickListener {
            AppSettings.setVoiceType(this, AppSettings.VOICE_FEMALE)
            highlightSelectedVoice(AppSettings.VOICE_FEMALE)
        }
        voiceSystem.setOnClickListener {
            AppSettings.setVoiceType(this, AppSettings.VOICE_SYSTEM)
            highlightSelectedVoice(AppSettings.VOICE_SYSTEM)
        }

        btnEmomFromOne.setOnClickListener {
            AppSettings.setEmomFrom(this, AppSettings.EMOM_FROM_ONE)
            highlightEmomFrom(AppSettings.EMOM_FROM_ONE)
        }
        btnEmomFromZero.setOnClickListener {
            AppSettings.setEmomFrom(this, AppSettings.EMOM_FROM_ZERO)
            highlightEmomFrom(AppSettings.EMOM_FROM_ZERO)
        }

        // Color swatches
        findViewById<View>(R.id.colorMagenta).setOnClickListener {
            AppSettings.setAccentColor(this, AppSettings.COLOR_MAGENTA)
            highlightAccentColor(AppSettings.COLOR_MAGENTA)
        }
        findViewById<View>(R.id.colorOrange).setOnClickListener {
            AppSettings.setAccentColor(this, AppSettings.COLOR_ORANGE)
            highlightAccentColor(AppSettings.COLOR_ORANGE)
        }
        findViewById<View>(R.id.colorBlue).setOnClickListener {
            AppSettings.setAccentColor(this, AppSettings.COLOR_BLUE)
            highlightAccentColor(AppSettings.COLOR_BLUE)
        }
        findViewById<View>(R.id.colorGreen).setOnClickListener {
            AppSettings.setAccentColor(this, AppSettings.COLOR_GREEN)
            highlightAccentColor(AppSettings.COLOR_GREEN)
        }
        findViewById<View>(R.id.colorPurple).setOnClickListener {
            AppSettings.setAccentColor(this, AppSettings.COLOR_PURPLE)
            highlightAccentColor(AppSettings.COLOR_PURPLE)
        }
    }

    private fun previewTheme(tone: Int) {
        try {
            val tg = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            tg.startTone(tone, 200)
            android.os.Handler(mainLooper).postDelayed({ tg.release() }, 400)
        } catch (_: Exception) { }
    }

    private fun setSeconds(sec: Int) {
        AppSettings.setGetReadySeconds(this, sec)
        highlightSelectedDuration(sec)
    }

    private fun applyDurationState(enabled: Boolean) {
        groupDuration.alpha = if (enabled) 1f else 0.4f
        btnSec3.isEnabled = enabled
        btnSec5.isEnabled = enabled
        btnSec10.isEnabled = enabled
        btnSec15.isEnabled = enabled
    }

    private fun highlightSelectedDuration(sec: Int) {
        val accentCsl = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.emom_purple))
        val normalCsl = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_elevated))
        val map = mapOf(btnSec3 to 3, btnSec5 to 5, btnSec10 to 10, btnSec15 to 15)

        for ((btn, value) in map) {
            val selected = value == sec
            btn.backgroundTintList = if (selected) accentCsl else normalCsl
            btn.setTextColor(
                if (selected) ContextCompat.getColor(this, R.color.on_primary)
                else ContextCompat.getColor(this, R.color.text_primary)
            )
        }
    }

    private fun highlightSelectedTheme(theme: Int) {
        val accent = ContextCompat.getColor(this, R.color.emom_purple)
        val normal = ContextCompat.getColor(this, R.color.text_primary)
        themeClassic.setTextColor(if (theme == AppSettings.THEME_CLASSIC) accent else normal)
        themeDigital.setTextColor(if (theme == AppSettings.THEME_DIGITAL) accent else normal)
        themeAlarm.setTextColor(if (theme == AppSettings.THEME_ALARM) accent else normal)
        themeCustom.setTextColor(if (theme == AppSettings.THEME_CUSTOM) accent else normal)
    }

    private fun highlightSelectedVoice(voice: Int) {
        val accent = ContextCompat.getColor(this, R.color.emom_purple)
        val normal = ContextCompat.getColor(this, R.color.text_primary)
        voiceMale.setTextColor(if (voice == AppSettings.VOICE_MALE) accent else normal)
        voiceFemale.setTextColor(if (voice == AppSettings.VOICE_FEMALE) accent else normal)
        voiceSystem.setTextColor(if (voice == AppSettings.VOICE_SYSTEM) accent else normal)
    }

    private fun highlightEmomFrom(v: Int) {
        val accentCsl = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.emom_purple))
        val normalCsl = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_elevated))

        val selectedOne = v == AppSettings.EMOM_FROM_ONE
        btnEmomFromOne.backgroundTintList = if (selectedOne) accentCsl else normalCsl
        btnEmomFromOne.setTextColor(
            if (selectedOne) ContextCompat.getColor(this, R.color.on_primary)
            else ContextCompat.getColor(this, R.color.text_primary)
        )

        val selectedZero = v == AppSettings.EMOM_FROM_ZERO
        btnEmomFromZero.backgroundTintList = if (selectedZero) accentCsl else normalCsl
        btnEmomFromZero.setTextColor(
            if (selectedZero) ContextCompat.getColor(this, R.color.on_primary)
            else ContextCompat.getColor(this, R.color.text_primary)
        )
    }

    private fun highlightAccentColor(colorId: Int) {
        // Draw a white ring around the selected swatch, clear from others
        val ids = listOf(
            AppSettings.COLOR_MAGENTA to R.id.colorMagenta,
            AppSettings.COLOR_ORANGE to R.id.colorOrange,
            AppSettings.COLOR_BLUE to R.id.colorBlue,
            AppSettings.COLOR_GREEN to R.id.colorGreen,
            AppSettings.COLOR_PURPLE to R.id.colorPurple
        )
        for ((id, viewId) in ids) {
            val v = findViewById<View>(viewId)
            v.alpha = if (id == colorId) 1f else 0.55f
            v.scaleX = if (id == colorId) 1.15f else 1f
            v.scaleY = if (id == colorId) 1.15f else 1f
        }
    }
}