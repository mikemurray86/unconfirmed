package app.unconfirmed.shared

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * A creature's own call, for when it shows itself while the player searches. It's the same sound
 * its audio clue describes, so a player who heard the recording might know it. [kind] is the
 * creature's call trait. One with no call of its own rustles through the undergrowth instead.
 */
internal class CreatureCall(kind: String, private val rate: Float, seed: Int) {
    private val random = Random(seed)
    private val voice: (Float) -> Float = when (kind) {
        CALL_SHRILL -> ::shrill
        CALL_HOWL -> ::howl
        CALL_SPLASH -> ::splash
        CALL_VOICE -> ::singing
        CALL_KNOCK -> ::knock
        else -> ::rustle
    }
    private val length = when (kind) {
        CALL_SHRILL -> 1.4f
        CALL_HOWL -> 2.6f
        CALL_SPLASH -> 1.6f
        CALL_VOICE -> 2.4f
        CALL_KNOCK -> 1.6f
        else -> 1.8f
    }
    private var sample = 0
    private var phase = 0f
    private var phase2 = 0f
    private var low = 0f
    private var high = 0f

    val done: Boolean get() = sample / rate >= length

    fun next(): Float {
        val t = sample / rate
        sample++
        if (t >= length) return 0f
        return voice(t) * CALL_LEVEL
    }

    /** Advances the main oscillator at [freq] and returns a sine with a little of its second harmonic. */
    private fun tone(freq: Float, second: Float = 0.25f): Float {
        phase = (phase + freq / rate) % 1f
        phase2 = (phase2 + 2 * freq / rate) % 1f
        return sin(2 * PI.toFloat() * phase) + second * sin(2 * PI.toFloat() * phase2)
    }

    private fun noise() = random.nextFloat() * 2 - 1

    /** Four quick shrieks, each dropping in pitch. */
    private fun shrill(t: Float): Float {
        val burst = 0.32f
        val k = (t / burst).toInt()
        val u = t - k * burst
        if (k >= 4 || u > 0.18f) {
            tone(0f)
            return 0f
        }
        val env = sin(PI.toFloat() * u / 0.18f)
        return tone(2_400f - 4_000f * u, second = 0.35f) * env * 1.6f
    }

    /** One long howl, rising and falling away, with a slow waver. */
    private fun howl(t: Float): Float {
        val rise = (t / 0.9f).coerceAtMost(1f)
        val fall = ((length - t) / 1.1f).coerceIn(0f, 1f)
        val pitch = 300f + 220f * rise - 120f * (1 - fall) + 9f * sin(2 * PI.toFloat() * 5f * t)
        return tone(pitch, second = 0.3f) * rise * fall
    }

    /** Something heavy going into the water, then drops pattering down. */
    private fun splash(t: Float): Float {
        // Noise that starts bright and darkens as it dies away.
        val cutoff = (0.6f * exp(-t * 2.5f)).coerceAtLeast(0.04f)
        low += (noise() - low) * cutoff
        val wash = low * exp(-t * 1.6f) * 4.5f
        val thump = tone(70f, second = 0f) * exp(-t * 9f) * 0.8f
        val drops = if (t > 0.35f && random.nextFloat() < 0.0009f) 0.5f else 0f
        high = high * 0.995f + drops
        val drip = sin(2 * PI.toFloat() * 1_800f * t) * high * 0.4f
        return wash + thump + drip
    }

    /** A few sung phrases, like a voice answering from across the field. */
    private fun singing(t: Float): Float {
        val phrase = 0.7f
        val k = (t / phrase).toInt()
        val u = t - k * phrase
        if (u > 0.5f) {
            tone(0f)
            return 0f
        }
        val note = floatArrayOf(330f, 294f, 349f, 262f)[k % 4]
        val env = sin(PI.toFloat() * u / 0.5f)
        val vibrato = 1f + 0.012f * sin(2 * PI.toFloat() * 5.5f * t)
        return tone(note * vibrato, second = 0.5f) * env * 1.1f
    }

    /** Three hard, even knocks, like stone on stone. */
    private fun knock(t: Float): Float {
        val gap = 0.45f
        val k = (t / gap).toInt()
        val u = t - k * gap
        if (k >= 3) return 0f
        val body = sin(2 * PI.toFloat() * 190f * u) * exp(-u * 16f) * 2.4f
        val click = noise() * exp(-u * 120f) * 1.2f
        return body + click
    }

    /** Something moving through the undergrowth: soft noise that swells and settles. */
    private fun rustle(t: Float): Float {
        high += (noise() - high) * 0.35f
        low += (high - low) * 0.08f
        val swell = sin(PI.toFloat() * t / length).coerceAtLeast(0f)
        val crackle = 0.6f + 0.4f * sin(2 * PI.toFloat() * 7f * t + sin(2 * PI.toFloat() * 3f * t))
        return (high - low) * swell * crackle * 3.6f
    }

    private companion object {
        const val CALL_LEVEL = 0.32f
    }
}
