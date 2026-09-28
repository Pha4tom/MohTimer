package com.Cali.mohtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

enum class TimerMode { EMOM, AMRAP, FOR_TIME, TABATA, TIMER }

object TimerBus {
    var listener: TimerListener? = null
}

interface TimerListener {
    fun onTick(remainingMs: Long, currentRound: Int, totalRounds: Int, progressPercent: Int, isWorkPhase: Boolean)
    fun onPaused(isPaused: Boolean)
    fun onFinished()
}

class TimerService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "smart_timer_channel"
        const val NOTIFICATION_ID = 42

        const val ACTION_START = "com.Cali.mohtimer.ACTION_START"
        const val ACTION_PAUSE = "com.Cali.mohtimer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.Cali.mohtimer.ACTION_RESUME"
        const val ACTION_STOP = "com.Cali.mohtimer.ACTION_STOP"
        const val ACTION_DEV_SKIP = "com.Cali.mohtimer.ACTION_DEV_SKIP"
        const val ACTION_DEV_JUMP_LAST = "com.Cali.mohtimer.ACTION_DEV_JUMP_LAST"

        const val EXTRA_MODE = "mode"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val EXTRA_REST_MS = "rest_ms"
        const val EXTRA_TOTAL_MS = "total_ms"
        const val EXTRA_ROUNDS = "rounds"

        @Volatile var isRunning = false
        @Volatile var activeMode: TimerMode = TimerMode.EMOM
        @Volatile var activeIntervalMs: Long = 60_000L
        @Volatile var activeRestMs: Long = 10_000L
        @Volatile var activeTotalMs: Long = 600_000L
        @Volatile var activeRounds: Int = 1
        @Volatile var activeIsPaused: Boolean = false
    }

    private var mode = TimerMode.EMOM
    private var intervalMs = 60_000L
    private var restMs = 10_000L
    private var totalMs = 600_000L
    private var rounds = 10

    private var currentRound = 1
    private var remainingMs = 60_000L
    private var elapsedMs = 0L
    private var isWorkPhase = true
    private var isPaused = false

    // Per-phase announcement flags (reset on every new phase)
    private var halfwayAnnounced = false
    private var tenSecondsAnnounced = false
    private var lastBeepSecond = -1

    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            when (AppSettings.getVoiceType(this)) {
                AppSettings.VOICE_MALE -> {
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
                }
                AppSettings.VOICE_FEMALE -> {
                    try {
                        val voices = tts?.voices
                        if (voices != null) {
                            val female = voices.firstOrNull { v ->
                                v.locale.language == "en" &&
                                v.name.contains("female", ignoreCase = true)
                            }
                            if (female != null) tts?.voice = female
                        }
                    } catch (_: Exception) { }
                    tts?.setPitch(1.10f)
                }
                else -> tts?.setPitch(1.0f)
            }
            tts?.setSpeechRate(1.0f)
            ttsReady = true
        }
    }

    // ---------- SPEECH ----------

    private fun speak(text: String) {
        if (!AppSettings.isVoiceEnabled(this)) return
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue_${System.currentTimeMillis()}")
    }

    private fun speakQueued(text: String) {
        if (!AppSettings.isVoiceEnabled(this)) return
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "cue_${System.currentTimeMillis()}")
    }

    private fun numberToWord(n: Int): String = when (n) {
        1 -> "one"
        2 -> "two"
        3 -> "three"
        4 -> "four"
        5 -> "five"
        6 -> "six"
        7 -> "seven"
        8 -> "eight"
        9 -> "nine"
        10 -> "ten"
        11 -> "eleven"
        12 -> "twelve"
        13 -> "thirteen"
        14 -> "fourteen"
        15 -> "fifteen"
        16 -> "sixteen"
        17 -> "seventeen"
        18 -> "eighteen"
        19 -> "nineteen"
        20 -> "twenty"
        else -> n.toString()
    }

    // ---------- SOUND (synthesized tones) ----------

    private fun playLoudBeep() {
        Beeper.play(this)
    }

    private fun playSoftBeep() {
        Beeper.playSoft(this)
    }

    /** Two quick soft beeps followed by the "Halfway there" voice cue. */
    private fun playHalfwayCue() {
        Beeper.playSoft(this)
        handler.postDelayed({ Beeper.playSoft(this) }, 180)
        handler.postDelayed({ speak("Halfway there") }, 460)
    }

    // ---------- WAKE LOCK ----------

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartTimer::TimerWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) { }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) { }
        wakeLock = null
    }

    // ---------- LIFECYCLE ----------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                mode = try {
                    TimerMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: "EMOM")
                } catch (_: Exception) { TimerMode.EMOM }

                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 60_000L)
                restMs = intent.getLongExtra(EXTRA_REST_MS, 10_000L)
                totalMs = intent.getLongExtra(EXTRA_TOTAL_MS, 600_000L)
                rounds = intent.getIntExtra(EXTRA_ROUNDS, 10)

                activeMode = mode
                activeIntervalMs = intervalMs
                activeRestMs = restMs
                activeTotalMs = totalMs
                activeRounds = rounds

                currentRound = 1
                isWorkPhase = true
                isPaused = false
                activeIsPaused = false
                elapsedMs = 0L
                resetPhaseFlags()

                remainingMs = when (mode) {
                    TimerMode.AMRAP, TimerMode.TIMER -> totalMs
                    else -> intervalMs
                }

                isRunning = true
                acquireWakeLock()
                startForegroundSafely(buildNotification())
                startTicker()

                // Announce the first round
                announceRoundStart(currentRound, isFirst = true)
            }

            ACTION_PAUSE -> if (!isPaused) {
                isPaused = true
                activeIsPaused = true
                TimerBus.listener?.onPaused(true)
                updateNotification()
            }

            ACTION_RESUME -> if (isPaused) {
                isPaused = false
                activeIsPaused = false
                TimerBus.listener?.onPaused(false)
                updateNotification()
            }

            ACTION_STOP -> finishAndStop()

            ACTION_DEV_SKIP -> {
                if (mode == TimerMode.FOR_TIME) finishAndStop()
                else remainingMs = 0
            }

            ACTION_DEV_JUMP_LAST -> {
                currentRound = (rounds - 1).coerceAtLeast(1)
                remainingMs = 1000L
                resetPhaseFlags()
            }
        }
        return START_NOT_STICKY
    }

    // ---------- ANNOUNCEMENTS ----------

    private fun resetPhaseFlags() {
        halfwayAnnounced = false
        tenSecondsAnnounced = false
        lastBeepSecond = -1
    }

    /**
     * Called at the start of every round (or first phase).
     * Round 1 → "Round one" + "Let's go"
     * Round 2+ → "Round two", "Round three", ...
     */
    private fun announceRoundStart(round: Int, isFirst: Boolean) {
        when (mode) {
            TimerMode.FOR_TIME -> {
                if (isFirst) speak("Let's go")
                return
            }
            TimerMode.TABATA -> {
                if (!isWorkPhase) {
                    // rest phase — just a soft beep, no voice
                    return
                }
                if (isFirst) {
                    speak("Round ${numberToWord(round)}")
                    speakQueued("Let's go")
                } else {
                    speak("Round ${numberToWord(round)}")
                }
                return
            }
            else -> {
                if (isFirst) {
                    speak("Round ${numberToWord(round)}")
                    speakQueued("Let's go")
                } else {
                    speak("Round ${numberToWord(round)}")
                }
            }
        }
    }

    /** Length of the phase that is currently counting down (for halfway calc). */
    private fun currentPhaseTotalMs(): Long = when (mode) {
        TimerMode.EMOM -> intervalMs
        TimerMode.AMRAP, TimerMode.TIMER -> totalMs
        TimerMode.TABATA -> if (isWorkPhase) intervalMs else restMs
        TimerMode.FOR_TIME -> 0L
    }

    // ---------- TICKER ----------

    private fun startTicker() {
        stopTicker()
        ticker = object : Runnable {
            override fun run() {
                if (!isRunning) return
                if (isPaused) {
                    handler.postDelayed(this, 100)
                    return
                }

                val speed = DevPrefs.getSpeed(applicationContext).coerceAtLeast(1)
                val virtualDelta = 100L * speed

                if (mode == TimerMode.FOR_TIME) {
                    elapsedMs += virtualDelta
                    TimerBus.listener?.onTick(elapsedMs, 1, 1, 0, true)
                    updateNotification()
                    handler.postDelayed(this, 100)
                    return
                }

                remainingMs -= virtualDelta
                val phaseTotal = currentPhaseTotalMs()

                // 1) Halfway cue — fires once per phase, only if phase >= 30s
                if (!halfwayAnnounced && phaseTotal >= 30_000L && remainingMs <= phaseTotal / 2) {
                    halfwayAnnounced = true
                    playHalfwayCue()
                }

                // 2) Ten-second voice cue — fires once per phase, only if phase > 15s
                if (!tenSecondsAnnounced && phaseTotal > 15_000L && remainingMs <= 10_000L) {
                    tenSecondsAnnounced = true
                    speak("10 seconds")
                }

                // 3) Three loud beeps at 3, 2, 1 seconds
                val currentSecond = ((remainingMs + 999) / 1000).toInt()
                if (currentSecond in 1..3 && currentSecond != lastBeepSecond) {
                    lastBeepSecond = currentSecond
                    playLoudBeep()
                }

                if (remainingMs <= 0) {
                    remainingMs = 0
                    TimerBus.listener?.onTick(0, currentRound, rounds, 100, isWorkPhase)
                    handlePhaseComplete()
                    return
                }

                val progress = if (phaseTotal > 0) {
                    ((phaseTotal - remainingMs).toFloat() / phaseTotal * 100).toInt().coerceIn(0, 100)
                } else 0

                TimerBus.listener?.onTick(remainingMs, currentRound, rounds, progress, isWorkPhase)
                updateNotification()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(ticker!!)
    }

    private fun stopTicker() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    // ---------- PHASE / ROUND LOGIC ----------

    private fun handlePhaseComplete() {
        when (mode) {
            TimerMode.EMOM -> {
                if (currentRound < rounds) {
                    currentRound++
                    remainingMs = intervalMs
                    resetPhaseFlags()
                    handler.postDelayed({
                        if (!isRunning) return@postDelayed
                        announceRoundStart(currentRound, isFirst = false)
                        startTicker()
                    }, 700)
                } else {
                    finishAndStop()
                }
            }

            TimerMode.AMRAP, TimerMode.TIMER -> finishAndStop()

            TimerMode.TABATA -> {
                val restAfterLast = AppSettings.isTabataRestAfterLast(this)
                if (isWorkPhase) {
                    if (currentRound == rounds && !restAfterLast) {
                        finishAndStop()
                    } else {
                        playSoftBeep()
                        isWorkPhase = false
                        remainingMs = restMs
                        resetPhaseFlags()
                        handler.postDelayed({
                            if (!isRunning) return@postDelayed
                            startTicker()
                        }, 700)
                    }
                } else {
                    if (currentRound >= rounds) {
                        finishAndStop()
                    } else {
                        currentRound++
                        isWorkPhase = true
                        remainingMs = intervalMs
                        resetPhaseFlags()
                        handler.postDelayed({
                            if (!isRunning) return@postDelayed
                            announceRoundStart(currentRound, isFirst = false)
                            startTicker()
                        }, 700)
                    }
                }
            }

            TimerMode.FOR_TIME -> finishAndStop()
        }
    }

    private fun finishAndStop() {
        stopTicker()
        isRunning = false
        releaseWakeLock()
        TimerBus.listener?.onFinished()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------- FOREGROUND / NOTIFICATION ----------

    private fun startForegroundSafely(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, TimerRunActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("mode", mode.name)
            putExtra("interval_ms", intervalMs)
            putExtra("rest_ms", restMs)
            putExtra("total_ms", totalMs)
            putExtra("rounds", rounds)
            putExtra("from_notification", true)
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (mode) {
            TimerMode.EMOM -> "EMOM — Round $currentRound/$rounds"
            TimerMode.AMRAP -> "AMRAP"
            TimerMode.TABATA -> "Tabata — Round $currentRound/$rounds ${if (isWorkPhase) "WORK" else "REST"}"
            TimerMode.FOR_TIME -> "For Time"
            TimerMode.TIMER -> "Timer"
        }
        val body = if (mode == TimerMode.FOR_TIME) formatTime(elapsedMs) else formatTime(remainingMs)
        val speed = DevPrefs.getSpeed(this)
        val speedTag = if (speed > 1) " (${speed}x)" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title + speedTag)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Workout Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the countdown while a timer is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000).toInt()
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    override fun onDestroy() {
        isRunning = false
        stopTicker()
        releaseWakeLock()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}