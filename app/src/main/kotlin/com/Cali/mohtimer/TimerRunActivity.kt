package com.Cali.mohtimer

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.Locale

class TimerRunActivity : AppCompatActivity(), TimerListener, TextToSpeech.OnInitListener {

    private enum class Phase { IDLE, ARMING, RUNNING }

    private lateinit var contentRoot: View
    private lateinit var tickDial: TickDialView
    private lateinit var tvArmingLabel: TextView
    private lateinit var tvStateBadge: TextView
    private lateinit var tvTimerDisplay: TextView
    private lateinit var tvRoundLabel: TextView
    private lateinit var llRoundRow: View
    private lateinit var tvRoundNumber: TextView
    private lateinit var tvRoundTotal: TextView
    private lateinit var tvModeTitle: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var btnControl: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnRestart: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var llSessionRound: View
    private lateinit var llSessionInterval: View
    private lateinit var tvSessionTotal: TextView
    private lateinit var tvSessionRound: TextView
    private lateinit var tvSessionInterval: TextView
    private lateinit var devPanel: View

    private var phase = Phase.IDLE
    private var isPaused = false
    private var mode: TimerMode = TimerMode.EMOM
    private var totalRounds = 1
    private var intervalMs = 60_000L
    private var totalMs = 600_000L
    private var restMs = 10_000L
    private var accentColorInt = 0xFFFF00E5.toInt()

    private var armTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var vibrator: Vibrator? = null

    private var lastRound = -1
    private var lastCountdownNumber = -1
    private var lastPhaseIsWork = true

    /** True when this activity was opened while a timer is already running. */
    private var rebindingToRunningService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer_run)

        tts = TextToSpeech(this, this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        applyScreenOnSetting()

        contentRoot = findViewById(R.id.contentRoot)
        tickDial = findViewById(R.id.tickDial)
        tvArmingLabel = findViewById(R.id.tvArmingLabel)
        tvStateBadge = findViewById(R.id.tvStateBadge)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        tvRoundLabel = findViewById(R.id.tvRoundLabel)
        llRoundRow = findViewById(R.id.llRoundRow)
        tvRoundNumber = findViewById(R.id.tvRoundNumber)
        tvRoundTotal = findViewById(R.id.tvRoundTotal)
        tvModeTitle = findViewById(R.id.tvModeTitle)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        btnControl = findViewById(R.id.btnControl)
        btnSkip = findViewById(R.id.btnSkip)
        btnRestart = findViewById(R.id.btnRestart)
        btnSettings = findViewById(R.id.btnSettings)
        llSessionRound = findViewById(R.id.llSessionRound)
        llSessionInterval = findViewById(R.id.llSessionInterval)
        tvSessionTotal = findViewById(R.id.tvSessionTotal)
        tvSessionRound = findViewById(R.id.tvSessionRound)
        tvSessionInterval = findViewById(R.id.tvSessionInterval)
        devPanel = findViewById(R.id.devPanel)

        // Detect: are we rebinding to a running service (from notification)?
        rebindingToRunningService = TimerService.isRunning &&
                (intent.getBooleanExtra("from_notification", false) || TimerService.isRunning)

        if (rebindingToRunningService) {
            // Use the service's active state, ignore intent extras if not provided
            mode = TimerService.activeMode
            intervalMs = TimerService.activeIntervalMs
            restMs = TimerService.activeRestMs
            totalMs = TimerService.activeTotalMs
            totalRounds = TimerService.activeRounds
            isPaused = TimerService.activeIsPaused
        } else {
            // Normal launch from a config screen
            mode = try {
                TimerMode.valueOf(intent.getStringExtra("mode") ?: "EMOM")
            } catch (_: Exception) { TimerMode.EMOM }
            intervalMs = intent.getLongExtra("interval_ms", 60_000L)
            totalMs = intent.getLongExtra("total_ms", 600_000L)
            totalRounds = intent.getIntExtra("rounds", 10)
            restMs = intent.getLongExtra("rest_ms", 10_000L)
        }

        tvModeTitle.text = mode.name

        // Use user-selected accent color if set, otherwise fall back to mode accent
        val userAccent = AppSettings.getAccentColor(this)
        val useUserAccent = userAccent != AppSettings.COLOR_MAGENTA ||
                AppSettings.getAccentColor(this) == AppSettings.COLOR_MAGENTA
        // Default behavior: only use mode accent if user hasn't touched the setting
        accentColorInt = if (rebindingToRunningService || AppSettings.getAccentColor(this) != AppSettings.COLOR_MAGENTA) {
            AppSettings.resolveAccentColor(this)
        } else {
            ContextCompat.getColor(this, when (mode) {
                TimerMode.EMOM -> R.color.emom_purple
                TimerMode.AMRAP -> R.color.amrap_orange
                TimerMode.FOR_TIME -> R.color.fortime_blue
                TimerMode.TABATA -> R.color.tabata_green
                TimerMode.TIMER -> R.color.fortime_blue
            })
        }

        val accentCsl = android.content.res.ColorStateList.valueOf(accentColorInt)
        tickDial.setAccentColor(accentColorInt)
        tvTimerDisplay.setTextColor(accentColorInt)
        tvRoundNumber.setTextColor(accentColorInt)
        btnControl.backgroundTintList = accentCsl

        when (mode) {
            TimerMode.FOR_TIME, TimerMode.AMRAP, TimerMode.TIMER -> {
                llRoundRow.visibility = View.GONE
                tvRoundLabel.visibility = View.GONE
                llSessionRound.visibility = View.GONE
                llSessionInterval.visibility = View.GONE
            }
            else -> {
                tvRoundTotal.text = "/ $totalRounds"
                tvSessionInterval.text = formatTime(intervalMs)
            }
        }

        tvSessionTotal.text = if (mode == TimerMode.FOR_TIME) formatTime(0L) else formatTime(totalMs)
        tvSessionRound.text = "0 / $totalRounds"

        // X button: TAP = minimize (keep service running), LONG-PRESS = stop timer
        val btnExit = findViewById<ImageButton>(R.id.btnExit)
        btnExit.setOnClickListener {
            animatePress(it)
            // Minimize: finish the activity, timer keeps running
            finish()
        }
        btnExit.setOnLongClickListener {
            // Long-press to stop the timer completely
            armTimer?.cancel()
            stopService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_STOP })
            Toast.makeText(this, "Timer stopped", Toast.LENGTH_SHORT).show()
            finish()
            true
        }

        if (DevPrefs.isUnlocked(this)) {
            btnSettings.visibility = View.VISIBLE
            btnSettings.setOnClickListener {
                animatePress(it)
                if (devPanel.visibility == View.GONE) {
                    devPanel.visibility = View.VISIBLE
                    devPanel.alpha = 0f
                    devPanel.animate().alpha(1f).setDuration(200L).start()
                } else {
                    devPanel.animate().alpha(0f).setDuration(180L)
                        .withEndAction { devPanel.visibility = View.GONE }.start()
                }
            }
        } else {
            btnSettings.visibility = View.GONE
        }

        btnControl.setOnClickListener {
            animatePress(it)
            when (phase) {
                Phase.IDLE -> {
                    val skipArm = mode == TimerMode.TIMER || !AppSettings.isGetReadyEnabled(this)
                    if (skipArm) {
                        tvTimerDisplay.text = "GO"
                        launchTimerService()
                    } else {
                        startArming()
                    }
                }
                Phase.RUNNING -> {
                    if (mode == TimerMode.FOR_TIME && !isPaused) {
                        stopService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_STOP })
                        finish()
                    } else {
                        val serviceAction =
                            if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                        startService(Intent(this, TimerService::class.java).apply { action = serviceAction })
                    }
                }
                Phase.ARMING -> { }
            }
        }

        btnSkip.setOnClickListener {
            animatePress(it)
            if (phase == Phase.RUNNING) {
                startService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_DEV_SKIP })
            }
        }

        btnRestart.setOnClickListener {
            animatePress(it)
            stopService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_STOP })
            phase = Phase.IDLE
            isPaused = false
            enterIdleState()
        }

        setupDevPanel()
        playEntryAnimation()

        if (rebindingToRunningService) {
            // Jump straight into the running UI
            enterRunningStateImmediately()
        } else {
            enterIdleState()
        }
    }

    /**
     * When the activity is opened from the notification while a timer is running,
     * skip IDLE/ARMING and go straight to the running visual state.
     */
    private fun enterRunningStateImmediately() {
        phase = Phase.RUNNING
        tvArmingLabel.visibility = View.GONE
        tvTimerDisplay.visibility = View.VISIBLE
        tvTimerDisplay.alpha = 1f
        tvTimerDisplay.scaleX = 1f
        tvTimerDisplay.scaleY = 1f
        btnControl.isEnabled = true
        btnControl.alpha = 1f
        btnControl.text = when {
            mode == TimerMode.FOR_TIME -> "FINISH"
            isPaused -> "RESUME"
            else -> "PAUSE"
        }
        if (mode == TimerMode.TABATA) {
            tvStateBadge.visibility = View.VISIBLE
            tvStateBadge.text = "WORK"
        }
        tvProgressLabel.visibility = View.VISIBLE
    }

    // ---------- SCREEN ON ----------
    private fun applyScreenOnSetting() {
        if (AppSettings.isKeepScreenOn(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ---------- VIBRATION ----------
    private fun vibrateShort() {
        if (!AppSettings.isVibrationEnabled(this)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) { }
    }

    private fun vibrateLong() {
        if (!AppSettings.isVibrationEnabled(this)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(220)
            }
        } catch (_: Exception) { }
    }

    // ---------- ANIMATIONS ----------
    private fun playEntryAnimation() {
        contentRoot.alpha = 0f
        contentRoot.animate().alpha(1f).setDuration(400L).start()
    }

    private fun animatePress(v: View) {
        v.animate().cancel()
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80L).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()
        }.start()
    }

    private fun crossfadeText(tv: TextView, newText: String) {
        if (tv.text.toString() == newText) return
        tv.animate().cancel()
        tv.animate().alpha(0f).setDuration(110L).withEndAction {
            tv.text = newText
            tv.animate().alpha(1f).setDuration(180L).start()
        }.start()
    }

    // ---------- STATE: IDLE ----------
    private fun enterIdleState() {
        phase = Phase.IDLE
        isPaused = false

        tvRoundNumber.text =
            if (mode == TimerMode.FOR_TIME || mode == TimerMode.AMRAP || mode == TimerMode.TIMER) "--" else "01"
        tvRoundTotal.text = "/ $totalRounds"
        tvArmingLabel.visibility = View.GONE
        tvStateBadge.visibility = View.GONE

        tvTimerDisplay.visibility = View.VISIBLE
        tvTimerDisplay.alpha = 1f
        tvTimerDisplay.scaleX = 1f
        tvTimerDisplay.scaleY = 1f
        tvTimerDisplay.text = when (mode) {
            TimerMode.FOR_TIME -> "00:00"
            TimerMode.TIMER, TimerMode.AMRAP -> formatTime(totalMs)
            else -> formatTime(intervalMs)
        }

        btnControl.visibility = View.VISIBLE
        btnControl.alpha = 1f
        btnControl.isEnabled = true
        btnControl.text = "START"

        tvSessionTotal.text = if (mode == TimerMode.FOR_TIME) formatTime(0L) else formatTime(totalMs)
        tvSessionRound.text = "0 / $totalRounds"

        tvProgressLabel.visibility = View.GONE

        tickDial.setProgressImmediate(0)
        lastRound = 1
        lastCountdownNumber = -1
        lastPhaseIsWork = true
    }

    // ---------- STATE: ARMING ----------
    private fun startArming() {
        phase = Phase.ARMING
        isPaused = false

        val armSeconds = AppSettings.getGetReadySeconds(this).coerceAtLeast(3)
        val armMs = armSeconds * 1000L

        tvArmingLabel.alpha = 0f
        tvArmingLabel.visibility = View.VISIBLE
        tvArmingLabel.animate().alpha(1f).setDuration(300L).start()

        btnControl.text = "GET READY"
        btnControl.isEnabled = false
        btnControl.alpha = 0.4f

        tvTimerDisplay.text = armSeconds.toString()
        tvTimerDisplay.alpha = 1f
        tvTimerDisplay.scaleX = 1f
        tvTimerDisplay.scaleY = 1f

        tvStateBadge.visibility = View.GONE
        tvProgressLabel.visibility = View.GONE
        lastCountdownNumber = armSeconds

        var lastBeepSecondLocal = -1
        armTimer = object : CountDownTimer(armMs, 100L) {
            override fun onTick(msUntilFinished: Long) {
                val secondsLeft = ((msUntilFinished + 999) / 1000).toInt()
                if (secondsLeft != lastCountdownNumber) {
                    lastCountdownNumber = secondsLeft
                    tvTimerDisplay.text = secondsLeft.toString()
                    tvTimerDisplay.animate().cancel()
                    tvTimerDisplay.scaleX = 1.08f
                    tvTimerDisplay.scaleY = 1.08f
                    tvTimerDisplay.animate().scaleX(1f).scaleY(1f).setDuration(220L)
                        .setInterpolator(OvershootInterpolator()).start()
                }
                if (secondsLeft in 1..3 && secondsLeft != lastBeepSecondLocal) {
                    lastBeepSecondLocal = secondsLeft
                    playBeep()
                    vibrateShort()
                }
            }

            override fun onFinish() {
                playGoFlash()
            }
        }.start()
    }

    private fun playGoFlash() {
        tvArmingLabel.animate().alpha(0f).setDuration(200L).withEndAction {
            tvArmingLabel.visibility = View.GONE
        }.start()

        tvTimerDisplay.text = "GO"
        tvTimerDisplay.scaleX = 0.6f
        tvTimerDisplay.scaleY = 0.6f
        tvTimerDisplay.animate()
            .scaleX(1.4f).scaleY(1.4f).alpha(0f)
            .setDuration(600L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                tvTimerDisplay.alpha = 1f
                tvTimerDisplay.scaleX = 1f
                tvTimerDisplay.scaleY = 1f
                launchTimerService()
            }
            .start()
        playBeep()
        vibrateLong()
    }

    // ---------- STATE: RUNNING ----------
    private fun launchTimerService() {
        phase = Phase.RUNNING
        tvArmingLabel.visibility = View.GONE
        tvTimerDisplay.visibility = View.VISIBLE
        tvTimerDisplay.alpha = 1f
        tvTimerDisplay.scaleX = 1f
        tvTimerDisplay.scaleY = 1f

        btnControl.isEnabled = true
        btnControl.alpha = 1f
        btnControl.text = if (mode == TimerMode.FOR_TIME) "FINISH" else "PAUSE"

        if (mode == TimerMode.TABATA) {
            tvStateBadge.visibility = View.VISIBLE
            crossfadeText(tvStateBadge, "WORK")
        }

        tvProgressLabel.visibility = View.VISIBLE

        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_MODE, mode.name)
            putExtra(TimerService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(TimerService.EXTRA_REST_MS, restMs)
            putExtra(TimerService.EXTRA_TOTAL_MS, totalMs)
            putExtra(TimerService.EXTRA_ROUNDS, totalRounds)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun playBeep() {
        if (!AppSettings.isBeepEnabled(this)) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri).play()
        } catch (_: Exception) { }
    }

    // ---------- DEV PANEL ----------
    private fun setupDevPanel() {
        if (!DevPrefs.isUnlocked(this)) return

        findViewById<MaterialButton>(R.id.btnSpeed1x).setOnClickListener {
            DevPrefs.setSpeed(this, 1)
            Toast.makeText(this, "Speed: 1x", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnSpeed10x).setOnClickListener {
            DevPrefs.setSpeed(this, 10)
            Toast.makeText(this, "Speed: 10x", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnSpeed60x).setOnClickListener {
            DevPrefs.setSpeed(this, 60)
            Toast.makeText(this, "Speed: 60x", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnDevSkip).setOnClickListener {
            startService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_DEV_SKIP })
        }
        findViewById<MaterialButton>(R.id.btnDevJumpLast).setOnClickListener {
            startService(Intent(this, TimerService::class.java).apply { action = TimerService.ACTION_DEV_JUMP_LAST })
        }
        findViewById<MaterialButton>(R.id.btnVoiceNext).setOnClickListener { speak("Next round") }
        findViewById<MaterialButton>(R.id.btnVoiceHalf).setOnClickListener { speak("Halfway there") }
        findViewById<MaterialButton>(R.id.btnVoiceLast).setOnClickListener { speak("Last round") }
        findViewById<MaterialButton>(R.id.btnVoiceDone).setOnClickListener { speak("Workout done") }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            try {
                val voices = tts?.voices
                if (voices != null) {
                    val male = voices.firstOrNull { v ->
                        v.locale.language == "en" &&
                        v.name.contains("male", ignoreCase = true) &&
                        !v.name.contains("female", ignoreCase = true)
                    }
                    if (male != null) tts?.voice = male
                }
            } catch (_: Exception) { }
            tts?.setPitch(0.80f)
            tts?.setSpeechRate(1.0f)
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (!AppSettings.isVoiceEnabled(this)) return
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "test")
    }

    override fun onStart() { super.onStart(); TimerBus.listener = this }
    override fun onStop() { super.onStop(); TimerBus.listener = null }

    override fun onTick(
        remainingMs: Long,
        currentRound: Int,
        totalRounds: Int,
        progressPercent: Int,
        isWorkPhase: Boolean
    ) {
        if (phase != Phase.RUNNING) return

        tickDial.setProgress(progressPercent)
        tvProgressLabel.text = "${formatTime(remainingMs)} remaining"

        if (mode == TimerMode.FOR_TIME) {
            tvTimerDisplay.text = formatTime(remainingMs)
            tvSessionTotal.text = formatTime(remainingMs)
            return
        }

        if (mode == TimerMode.TIMER || mode == TimerMode.AMRAP) {
            tvTimerDisplay.text = formatTime(remainingMs)
            tvSessionTotal.text = formatTime(remainingMs)
            return
        }

        val roundStr = currentRound.toString().padStart(2, '0')
        if (tvRoundNumber.text.toString() != roundStr) {
            tvRoundNumber.text = roundStr
            tvRoundNumber.animate().cancel()
            tvRoundNumber.scaleX = 1.08f
            tvRoundNumber.scaleY = 1.08f
            tvRoundNumber.animate().scaleX(1f).scaleY(1f).setDuration(250L)
                .setInterpolator(OvershootInterpolator()).start()
        }

        tvTimerDisplay.text = formatTime(remainingMs)

        if (mode == TimerMode.TABATA && isWorkPhase != lastPhaseIsWork) {
            lastPhaseIsWork = isWorkPhase
            crossfadeText(tvStateBadge, if (isWorkPhase) "WORK" else "REST")
        }

        tvSessionTotal.text = formatTime(remainingMs + (totalRounds - currentRound) * intervalMs)
        tvSessionRound.text = "${currentRound - 1} / $totalRounds"
        tvSessionInterval.text = formatTime(intervalMs)
    }

    override fun onPaused(isPaused: Boolean) {
        if (phase != Phase.RUNNING) return
        this.isPaused = isPaused
        if (mode != TimerMode.FOR_TIME) {
            btnControl.text = if (isPaused) "RESUME" else "PAUSE"
        }
        vibrateShort()
    }

    override fun onFinished() {
        tickDial.setAccentColor(0xFFFFC107.toInt())
        tickDial.setProgress(100)

        tvTimerDisplay.text = "DONE"
        tvTimerDisplay.scaleX = 0.6f
        tvTimerDisplay.scaleY = 0.6f
        tvTimerDisplay.animate().scaleX(1f).scaleY(1f)
            .setDuration(400L)
            .setInterpolator(OvershootInterpolator())
            .start()

        btnControl.isEnabled = false
        btnControl.alpha = 0.4f
        btnControl.text = "DONE"
        tvArmingLabel.visibility = View.GONE
        tvStateBadge.visibility = View.GONE
        tvProgressLabel.text = "Complete"

        if (AppSettings.isVibrationEnabled(this)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
                }
            } catch (_: Exception) { }
        }

        Toast.makeText(this, "Workout complete 💪", Toast.LENGTH_LONG).show()

        tvTimerDisplay.postDelayed({
            contentRoot.animate().alpha(0f).setDuration(400L).withEndAction { finish() }.start()
        }, 1600)
    }

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000).toInt()
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    override fun onDestroy() {
        armTimer?.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}