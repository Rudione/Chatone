package io.rudione.chatone.domain.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object SoundSynth {
    const val RATE = 44_100

    fun render(sound: BuiltInSound): PcmAudio? = when (sound) {
        BuiltInSound.Tag -> null
        BuiltInSound.Chime -> chime()
        BuiltInSound.Drop -> drop()
        BuiltInSound.Bell -> bell()
        BuiltInSound.Beep -> beep()
    }

    fun beep(): PcmAudio = synth(0.15) { t, i, n ->
        val fade = if (i < n / 10) i.toDouble() / (n / 10) else ((n - i).toDouble() / (n * 0.8)).coerceIn(0.0, 1.0)
        sin(2 * PI * 880.0 * t) * 0.5 * fade
    }

    fun chime(): PcmAudio = normalized(synth(0.75) { t, _, _ ->
        note(t, 987.77, 0.0, 0.13) + note(t, 1318.51, 0.085, 0.24)
    }, 0.62f)

    fun drop(): PcmAudio = normalized(synth(0.26) { t, _, _ ->
        val phase = 2 * PI * (520.0 * t + 900.0 * 0.035 * (1 - exp(-t / 0.035)))
        sin(phase) * attack(t, 0.002) * exp(-t / 0.075)
    }, 0.66f)

    fun bell(): PcmAudio = normalized(synth(1.2) { t, _, _ ->
        val ratios = doubleArrayOf(1.0, 2.0, 2.76, 5.4, 8.93)
        val amps = doubleArrayOf(1.0, 0.55, 0.38, 0.2, 0.08)
        val decays = doubleArrayOf(0.85, 0.5, 0.36, 0.18, 0.09)
        var sum = 0.0
        for (k in ratios.indices) sum += sin(2 * PI * 1046.5 * ratios[k] * t) * amps[k] * exp(-t / decays[k])
        sum * attack(t, 0.003)
    }, 0.58f)

    private fun note(t: Double, freq: Double, start: Double, decay: Double): Double {
        val local = t - start
        if (local < 0) return 0.0
        val tone = sin(2 * PI * freq * local) + 0.22 * sin(4 * PI * freq * local) + 0.08 * sin(6 * PI * freq * local)
        return tone * attack(local, 0.004) * exp(-local / decay)
    }

    private fun attack(t: Double, length: Double): Double = min(1.0, t / length)

    private inline fun synth(seconds: Double, sample: (t: Double, i: Int, n: Int) -> Double): PcmAudio {
        val n = (RATE * seconds).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = sample(i.toDouble() / RATE, i, n).toFloat()
        val tail = min(n, RATE / 100)
        for (i in 0 until tail) out[n - 1 - i] *= i.toFloat() / tail
        return PcmAudio(RATE, 1, out)
    }

    private fun normalized(audio: PcmAudio, peak: Float): PcmAudio {
        var top = 0f
        for (s in audio.samples) top = max(top, abs(s))
        if (top <= 0f) return audio
        val scale = peak / top
        return PcmAudio(audio.sampleRate, audio.channels, FloatArray(audio.samples.size) { audio.samples[it] * scale })
    }
}
