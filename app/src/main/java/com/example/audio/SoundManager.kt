package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * High-performance sound effects engine for Rai Ludo King.
 * Generates custom synthesized audio waveforms via AudioTrack.
 * Requires zero external audio assets, works instantly offline.
 */
class SoundManager(private val context: Context) {

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun playPcm(samples: ShortArray) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val bufferSize = samples.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Clean up track after duration
                kotlinx.coroutines.delay((samples.size * 1000L / sampleRate) + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun vibrate(durationMs: Long = 40) {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Ignore vibration errors gracefully
        }
    }

    fun playButtonClick() {
        vibrate(25)
        val numSamples = (sampleRate * 0.04).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - (i.toDouble() / numSamples)
            val wave = sin(2.0 * Math.PI * 750.0 * t)
            samples[i] = (wave * 20000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playDiceRoll() {
        vibrate(70)
        // Rattle sound: pulses of noise / wood click
        val numSamples = (sampleRate * 0.45).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random(42)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = (1.0 - (i.toDouble() / numSamples))
            val pulse = sin(2.0 * Math.PI * 28.0 * t) // 28 rattles/sec
            val click = if (pulse > 0.6) (random.nextDouble() * 2 - 1) else 0.0
            val body = sin(2.0 * Math.PI * 220.0 * t) * 0.3
            samples[i] = ((click * 0.7 + body) * 22000 * envelope).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playTokenMove() {
        vibrate(30)
        // Bouncy pop: frequency slide 350Hz -> 650Hz
        val numSamples = (sampleRate * 0.09).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 350.0 + progress * 300.0
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - progress
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 24000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playTokenCapture() {
        vibrate(120)
        // Impact crash: low frequency punch + rapid decay
        val numSamples = (sampleRate * 0.3).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val freq = 160.0 * (1.0 - progress * 0.6)
            val decay = (1.0 - progress) * (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t) + sin(2.0 * Math.PI * (freq * 1.5) * t) * 0.5
            samples[i] = (wave * 26000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playTokenHome() {
        vibrate(80)
        // Ascending chime: 3 bright tones (C5, E5, G5)
        val numSamples = (sampleRate * 0.45).toInt()
        val samples = ShortArray(numSamples)
        val segment = numSamples / 3
        val freqs = doubleArrayOf(523.25, 659.25, 783.99)
        for (i in 0 until numSamples) {
            val noteIdx = (i / segment).coerceIn(0, 2)
            val noteProgress = (i % segment).toDouble() / segment
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - noteProgress * 0.8
            val wave = sin(2.0 * Math.PI * freqs[noteIdx] * t)
            samples[i] = (wave * 22000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playVictory() {
        vibrate(200)
        // Celebration fanfare: G4 -> C5 -> E5 -> G5
        val numSamples = (sampleRate * 0.8).toInt()
        val samples = ShortArray(numSamples)
        val segment = numSamples / 4
        val freqs = doubleArrayOf(392.0, 523.25, 659.25, 783.99)
        for (i in 0 until numSamples) {
            val noteIdx = (i / segment).coerceIn(0, 3)
            val noteProgress = (i % segment).toDouble() / segment
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - noteProgress * 0.7
            val wave = sin(2.0 * Math.PI * freqs[noteIdx] * t) + sin(2.0 * Math.PI * (freqs[noteIdx] * 2) * t) * 0.3
            samples[i] = (wave * 22000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playKbcLock() {
        vibrate(50)
        // Tense suspense lock tone
        val numSamples = (sampleRate * 0.35).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - (i.toDouble() / numSamples)
            val wave = sin(2.0 * Math.PI * 440.0 * t) * 0.7 + sin(2.0 * Math.PI * 466.16 * t) * 0.3
            samples[i] = (wave * 22000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playKbcCorrect() {
        vibrate(150)
        // Jubilant KBC correct ascending chime: C5 -> E5 -> G5 -> C6
        val numSamples = (sampleRate * 0.65).toInt()
        val samples = ShortArray(numSamples)
        val segment = numSamples / 4
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        for (i in 0 until numSamples) {
            val noteIdx = (i / segment).coerceIn(0, 3)
            val noteProgress = (i % segment).toDouble() / segment
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - noteProgress * 0.6
            val wave = sin(2.0 * Math.PI * freqs[noteIdx] * t) + sin(2.0 * Math.PI * (freqs[noteIdx] * 2.0) * t) * 0.25
            samples[i] = (wave * 24000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playKbcWrong() {
        vibrate(250)
        // Deep buzzer
        val numSamples = (sampleRate * 0.5).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - (i.toDouble() / numSamples) * 0.7
            val wave = (sin(2.0 * Math.PI * 130.81 * t) + sin(2.0 * Math.PI * 138.59 * t)) * 0.5
            samples[i] = (wave * 25000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playKbcLifeline() {
        vibrate(60)
        // Mystical twinkle for lifeline activation
        val numSamples = (sampleRate * 0.4).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 600.0 + progress * 800.0
            val t = i.toDouble() / sampleRate
            val decay = 1.0 - progress
            val wave = sin(2.0 * Math.PI * freq * t) + sin(2.0 * Math.PI * (freq * 1.5) * t) * 0.3
            samples[i] = (wave * 20000 * decay).toInt().toShort()
        }
        playPcm(samples)
    }
}
