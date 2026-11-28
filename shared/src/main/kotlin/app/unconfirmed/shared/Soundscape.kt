package app.unconfirmed.shared

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The game's music, made up as it plays rather than read from recordings. It's built to sit
 * quietly under a walk without ever quite settling:
 *  - a low D drone that slowly beats against itself, with a sub-bass beneath
 *  - a pad of slow minor chords whose notes slide into one another
 *  - wind: filtered noise that gusts and drops away
 *  - stray music-box bells from a dark (Phrygian) scale, trailing off through an echo
 *  - with [tension] up, as in an encounter: a heartbeat, a high tremolo rubbing a semitone apart,
 *    and a second drone a semitone above the first
 *
 * [chime] plays the cue for a clue turning up: three bright bells rising to a sour note, with the
 * music ducking under it. [call] plays a creature's own call when it shows itself, ducked the same way.
 *
 * Call [render] from one audio thread. The other controls may be set from any thread.
 */
class Soundscape(private val sampleRate: Int = 22_050, seed: Long = 1) {
    /** How tense the music is, from 0 (wandering) to 1 (it's right there). Changes ease in over a few seconds. */
    @Volatile var tension = 0f

    /** Whether the music plays. With it off, [chime] still sounds. */
    @Volatile var music = true

    /** Overall level, from 0 to 1. It's eased, so dropping it to 0 fades out. */
    @Volatile var volume = 1f

    @Volatile private var chimeRequested = false

    @Volatile private var callRequested: String? = null
    private var calling: CreatureCall? = null
    private var calls = 0

    private val rate = sampleRate.toFloat()
    private val random = Random(seed)
    private var sample = 0L

    // Eased versions of the controls, so nothing ever jumps.
    private var tensionNow = 0f
    private var musicNow = 0f
    private var volumeNow = 0f
    private var duck = 0f
    private val duckRelease = exp(-1f / (rate * 1.2f))

    /** Plays the cue for a clue turning up. */
    fun chime() {
        chimeRequested = true
    }

    /** Plays the call of a creature showing itself. [kind] is its call trait, as its audio clue has it. */
    fun call(kind: String) {
        callRequested = kind
    }

    /** Fills [out] with the next samples, mono, from -1 to 1. */
    fun render(out: FloatArray, from: Int = 0, count: Int = out.size - from) {
        for (i in from until from + count) out[i] = next()
    }

    /** True once the level has faded all the way out. */
    val silent: Boolean get() = volumeNow < 0.0005f && volume == 0f

    // ---- Drone -------------------------------------------------------------------------------------

    private val droneA = Osc(rate)
    private val droneB = Osc(rate)
    private val droneFifth = Osc(rate)
    private val sub = Osc(rate)
    private val rub = Osc(rate)
    private val rubOvertone = Osc(rate)
    private val overtones = Array(3) { Osc(rate) }

    private fun drone(seconds: Float): Float {
        val breathe = 0.65f + 0.35f * sine(seconds * 0.05f)
        val body = droneA.next(D2) * 0.3f + droneB.next(D2 * 1.004f) * 0.3f +
            droneFifth.next(A2 * 0.998f) * 0.2f + sub.next(D2 / 2) * 0.12f
        // A phone speaker can't play much below 200 Hz, so the drone carries overtones the ear
        // hears the low note in. Each drifts in and out on its own, like a bowed string.
        var upper = 0f
        overtones.forEachIndexed { k, osc ->
            val drift = 0.5f + 0.5f * sine(seconds * (0.037f + k * 0.017f) + k * 0.4f)
            upper += osc.next(D2 * (k + 2) * (1f + k * 0.0011f)) * DRONE_OVERTONES[k] * drift
        }
        // A semitone above the root: the sound of something being wrong.
        val wrong = (rub.next(D2 * SEMITONE) * 0.3f + rubOvertone.next(D2 * SEMITONE * 3) * 0.22f) * tensionNow
        return ((body + upper) * breathe + wrong) * 0.22f
    }

    // ---- Pad ---------------------------------------------------------------------------------------

    private val padVoices = Array(3) { PadVoice(rate) }
    private var chordIndex = -1

    private fun pad(seconds: Float): Float {
        val chord = ((seconds / CHORD_SECONDS).toInt()) % CHORDS.size
        if (chord != chordIndex) {
            chordIndex = chord
            CHORDS[chord].forEachIndexed { v, f -> padVoices[v].target = f }
        }
        var sum = 0f
        padVoices.forEachIndexed { v, voice ->
            // Each note swells on its own slow cycle, so the chord shimmers.
            val swell = 0.55f + 0.45f * sine(seconds * (0.06f + v * 0.023f) + v * 0.31f)
            sum += voice.next() * swell
        }
        return sum * 0.075f
    }

    private class PadVoice(private val rate: Float) {
        var target = 0f
        var freq = 0f
        private val a = Osc(rate)
        private val b = Osc(rate)
        private val octave = Osc(rate)
        private var low = 0f

        fun next(): Float {
            if (freq == 0f) freq = target
            // Glides to each new note over a couple of seconds, so the chords bend into one another.
            freq += (target - freq) * (1f / (rate * 0.9f))
            val raw = a.next(freq) + b.next(freq * 1.0035f) + octave.next(freq * 2.001f) * 0.3f
            low += (raw - low) * 0.25f
            return low
        }
    }

    // ---- Wind --------------------------------------------------------------------------------------

    private var windLow = 0f
    private var windBand = 0f
    private var gust = 0f
    private var gustTarget = 0.4f
    private var nextGust = 0f

    private fun wind(seconds: Float): Float {
        if (seconds >= nextGust) {
            gustTarget = 0.15f + random.nextFloat() * random.nextFloat()
            nextGust = seconds + 2f + random.nextFloat() * 6f
        }
        gust += (gustTarget - gust) * (1f / (rate * 1.6f))
        // A band-pass that wanders, so the wind seems to whistle round things.
        val centre = 280f + 260f * sine(seconds * 0.031f) + 180f * gust
        val f = 2f * sin(PI.toFloat() * centre / rate)
        val noise = random.nextFloat() * 2f - 1f
        windLow += f * windBand
        val high = noise - windLow - 0.35f * windBand
        windBand += f * high
        return windBand * gust * 0.12f
    }

    // ---- Bells -------------------------------------------------------------------------------------

    private val bells = ArrayList<Bell>()
    private var nextBell = 2.5f
    private val pendingBells = ArrayList<Pair<Float, Bell>>()

    private fun scheduleBells(seconds: Float) {
        if (seconds < nextBell) return
        val calm = tensionNow < 0.5f
        val scale = if (calm) PHRYGIAN else TRITONES
        val start = scale[random.nextInt(scale.size)]
        // Now and then a few notes falling away, like a music box winding down.
        val notes = if (random.nextFloat() < 0.35f) 3 else 1
        var step = start
        repeat(notes) { n ->
            val freq = D5 * SEMITONE.pow(step)
            pendingBells += seconds + n * 0.42f to Bell(freq, 0.11f, rate)
            step -= 1 + random.nextInt(3)
        }
        nextBell = seconds + if (calm) 3.5f + random.nextFloat() * 6f else 6f + random.nextFloat() * 8f
    }

    private fun bells(seconds: Float, loud: Boolean): Float {
        val it = pendingBells.iterator()
        while (it.hasNext()) {
            val (at, bell) = it.next()
            if (seconds >= at && bell.loud == loud) {
                bells += bell
                it.remove()
            }
        }
        var sum = 0f
        val each = bells.iterator()
        while (each.hasNext()) {
            val bell = each.next()
            if (bell.loud != loud) continue
            sum += bell.next()
            if (bell.done) each.remove()
        }
        return sum
    }

    /** A bell or music-box note: a few out-of-tune partials, each dying away at its own rate. */
    private class Bell(freq: Float, private val level: Float, private val rate: Float, val loud: Boolean = false) {
        private val oscs = BELL_PARTIALS.map { Osc(rate) }
        private val freqs = BELL_PARTIALS.map { freq * it.first }
        private val decay = BELL_PARTIALS.map { exp(-1f / (rate * it.third)) }
        private val amp = BELL_PARTIALS.map { it.second }.toFloatArray()
        private var age = 0
        var done = false
            private set

        fun next(): Float {
            age++
            val attack = (age / (rate * 0.004f)).coerceAtMost(1f)
            var sum = 0f
            for (k in oscs.indices) {
                sum += oscs[k].next(freqs[k]) * amp[k]
                amp[k] *= decay[k]
            }
            if (amp[0] < 0.0005f) done = true
            return sum * attack * level
        }
    }

    // ---- Echo --------------------------------------------------------------------------------------

    private val echo = FloatArray((sampleRate * 1.2f).toInt())
    private var echoAt = 0
    private var echoLow = 0f

    private fun echo(input: Float): Float {
        val tapA = echo[(echoAt - (rate * 0.42f).toInt() + echo.size) % echo.size]
        val tapB = echo[(echoAt - (rate * 0.71f).toInt() + echo.size) % echo.size]
        val wet = tapA * 0.6f + tapB * 0.4f
        // Each repeat comes back darker, as if from further off.
        echoLow += (wet - echoLow) * 0.35f
        echo[echoAt] = input + echoLow * 0.5f
        echoAt = (echoAt + 1) % echo.size
        return wet
    }

    // ---- Tension ---------------------------------------------------------------------------------

    private val tremoloA = Osc(rate)
    private val tremoloB = Osc(rate)
    private var beatPhase = 0f
    private var thumpAge = -1f
    private var thumpFreq = 0f
    private val thump = Osc(rate)
    private val thumpUpper = Osc(rate)

    private fun tense(seconds: Float): Float {
        if (tensionNow < 0.01f) return 0f
        // Lub-dub, quickening as the tension climbs.
        val bpm = 58f + 34f * tensionNow
        val before = beatPhase
        beatPhase = (beatPhase + bpm / 60f / rate) % 1f
        if (beatPhase < before || (before < 0.28f && beatPhase >= 0.28f)) {
            thumpAge = 0f
            thumpFreq = if (beatPhase < 0.28f) 62f else 52f
        }
        var heart = 0f
        if (thumpAge >= 0f) {
            val t = thumpAge / rate
            val bend = 0.7f + 0.3f * exp(-t * 18f)
            val attack = 1f - exp(-t * 400f)
            // The low thud, plus a shorter knock an octave and a fifth up that a phone speaker can play.
            heart =
                (
                    thump.next(thumpFreq * bend) * exp(-t * 9f) +
                        thumpUpper.next(thumpFreq * 3f * bend) * 0.6f * exp(-t * 22f)
                    ) *
                attack
            thumpAge++
            if (t > 0.6f) thumpAge = -1f
        }
        val shiver = 0.5f + 0.5f * sine(seconds * 6.5f)
        val swell = 0.6f + 0.4f * sine(seconds * 0.09f)
        val strings = (tremoloA.next(D6) + tremoloB.next(D6 * SEMITONE)) * shiver * swell
        return (heart * 0.55f + strings * 0.022f) * tensionNow
    }

    // ---- Mixing ------------------------------------------------------------------------------------

    private fun next(): Float {
        val seconds = sample / rate
        sample++
        if (chimeRequested) {
            chimeRequested = false
            CHIME.forEachIndexed { n, (step, level) ->
                pendingBells += seconds + n * 0.11f to Bell(D5 * SEMITONE.pow(step), level, rate, loud = true)
            }
            duck = 1f
        }
        callRequested?.let { kind ->
            callRequested = null
            calling = CreatureCall(kind, rate, seed = calls++)
            duck = 1f
        }
        val call = calling?.let { voice ->
            voice.next().also { if (voice.done) calling = null }
        } ?: 0f
        val ease = 1f / (rate * 2.5f)
        tensionNow += (tension.coerceIn(0f, 1f) - tensionNow) * ease
        musicNow += ((if (music) 1f else 0f) - musicNow) * (1f / (rate * 1.5f))
        volumeNow += (volume.coerceIn(0f, 1f) - volumeNow) * (1f / (rate * 0.3f))
        duck *= duckRelease

        var musicMix = 0f
        var bell = 0f
        if (musicNow > 0.001f) {
            scheduleBells(seconds)
            bell = bells(seconds, loud = false) * musicNow
            musicMix = (drone(seconds) + pad(seconds) + wind(seconds) + tense(seconds)) * musicNow
        }
        val cue = bells(seconds, loud = true) + call
        val wet = echo(bell + cue * 0.6f)
        // The music dips under the clue cue, then comes back up.
        val mixed = (musicMix + bell * 0.7f) * MUSIC_LEVEL * (1f - 0.55f * duck) + cue + wet * 0.55f
        return softClip(mixed * volumeNow)
    }

    /** A slow sine for swells and drifts, [cycles] turns into its cycle. */
    private fun sine(cycles: Float): Float = sineAt(cycles - floor(cycles))

    private companion object {
        const val CHORD_SECONDS = 13f

        /** The drone's second, third and fourth harmonics. */
        val DRONE_OVERTONES = floatArrayOf(0.32f, 0.2f, 0.1f)

        /** How loud the music sits under the clue cue: well back, as a bed for the walk. */
        const val MUSIC_LEVEL = 0.6f
        val SEMITONE = 2f.pow(1f / 12f)
        const val D2 = 73.42f
        const val A2 = 110f
        const val D5 = 587.33f
        const val D6 = 1174.66f

        /** Slow, sad and a little off: D minor, B-flat with a seventh, G minor with a ninth, then the flat second. */
        val CHORDS = arrayOf(
            floatArrayOf(146.83f, 174.61f, 220.00f),
            floatArrayOf(116.54f, 174.61f, 220.00f),
            floatArrayOf(98.00f, 116.54f, 220.00f),
            floatArrayOf(155.56f, 196.00f, 233.08f),
        )

        /** Semitones above D for the stray bells: D Phrygian, dark but still tuneful. */
        val PHRYGIAN = intArrayOf(0, 1, 3, 5, 7, 8, 10, 12, 13, 15)

        /** For an encounter: only the most unsettling intervals. */
        val TRITONES = intArrayOf(0, 1, 6, 7, 12, 13)

        /** Bell partials: how far above the note, how loud, and how many seconds to die away. */
        val BELL_PARTIALS = listOf(
            Triple(1f, 1f, 0.75f),
            Triple(2.756f, 0.4f, 0.35f),
            Triple(5.404f, 0.2f, 0.18f),
            Triple(8.933f, 0.08f, 0.09f),
        )

        /** The clue cue, in semitones above D5 and level: up a fifth, up the octave, then a semitone too far. */
        val CHIME = listOf(0 to 0.28f, 7 to 0.28f, 13 to 0.32f)

        /** Rounds off anything too loud instead of clipping it harshly. */
        fun softClip(x: Float): Float {
            val c = x.coerceIn(-3f, 3f)
            return c * (27f + c * c) / (27f + 9f * c * c)
        }
    }
}

/** A sine oscillator that keeps its own phase. */
private class Osc(private val rate: Float) {
    private var phase = 0f

    fun next(freq: Float): Float {
        phase += freq / rate
        if (phase >= 1f) phase -= phase.toInt()
        return sineAt(phase)
    }
}

private val SINE_TABLE = FloatArray(4096) { sin(2 * PI * it / 4096).toFloat() }

private fun sineAt(phase: Float): Float = SINE_TABLE[(phase * 4096).toInt() and 4095]

/** How loud a stretch of sound is: its root mean square. */
fun rms(samples: FloatArray): Float {
    var sum = 0.0
    for (s in samples) sum += s * s
    return kotlin.math.sqrt(sum / samples.size).toFloat()
}

/** The loudest sample in a stretch of sound. */
fun peak(samples: FloatArray): Float = samples.maxOf { abs(it) }
