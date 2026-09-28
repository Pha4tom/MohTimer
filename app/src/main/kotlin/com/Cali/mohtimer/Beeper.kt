package com.Cali.mohtimer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Synthesizes short beeps using AudioTrack. No files, no notification sounds,
 * no MediaPlayer. Just pure generated tones.
 */
object Beeper {

    private const val SAMPLE_RATE = 44100

    /** Returns (frequency Hz, duration ms) for a given sound theme. */
    private fun toneForTheme(theme: Int): Pair<Double, Int> = when (theme) {
        AppSettings.THEME_CLASSIC -> 880.0 to 250    // A5, clean beep
        AppSettings.THEME_DIGITAL -> 1200.0 to 150   // short high pip
        AppSettings.THEME_ALARM -> 660.0 to 400      // lower, urgent
        else -> 880.0 to 250
    }

    /** Plays the standard beep according to the current sound theme. */
    fun play(context: android.content.Context) {
        if (!AppSettings.isBeepEnabled(context)) return
        val (freq, dur) = toneForTheme(AppSettings.getSoundTheme(context))
        playTone(freq, dur)
    }

    /** Plays a soft, shorter, quieter beep — used for round transitions. */
    fun playSoft(context: android.content.Context) {
        if (!AppSettings.isBeepEnabled(context)) return
        playTone(700.0, 120, volume = 0.5f)
    }

    private fun playTone(frequency: Double, durationMs: Int, volume: Float = 0.7f) {
        Thread {
            var track: AudioTrack? = null
            try {
                val numSamples = SAMPLE_RATE * durationMs / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    // Small fade in/out to avoid clicks at the start/end
                    val env = when {
                        i < numSamples * 0.05 -> i / (numSamples * 0.05)
                        i > numSamples * 0.95 -> (numSamples - i) / (numSamples * 0.05)
                        else -> 1.0
                    }
                    val value = sin(2 * PI * frequency * t) * Short.MAX_VALUE * volume * env
                    samples[i] = value.toInt().toShort()
                }

                val bufferSize = samples.size * 2

                @Suppress("DEPRECATION")
                track = AudioTrack(
                    AudioManager.STREAM_ALARM,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )

                track.write(samples, 0, samples.size)
                track.play()

                // Let it finish playing before we release
                Thread.sleep(durationMs.toLong() + 60)

                try { track.stop() } catch (_: Exception) { }
                try { track.release() } catch (_: Exception) { }
                track = null
            } catch (_: Exception) {
                try { track?.release() } catch (_: Exception) { }
            }
        }.start()
    }
}