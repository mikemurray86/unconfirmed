package app.unconfirmed.shared

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

class SoundscapeTest {
    private val rate = 22_050

    private fun Soundscape.seconds(s: Float): FloatArray = FloatArray((rate * s).toInt()).also { render(it) }

    @Test
    fun `stays within range and never silent while the music plays`() {
        val sound = Soundscape(rate)
        val out = sound.seconds(30f)
        assertTrue(peak(out) <= 1f, "peak ${peak(out)}")
        // Every second has something in it: the drone never drops out.
        out.toList().chunked(rate).forEach { second -> assertTrue(rms(second.toFloatArray()) > 0.005f) }
    }

    @Test
    fun `sits quietly under a walk`() {
        val sound = Soundscape(rate)
        sound.seconds(5f)
        val level = rms(sound.seconds(20f))
        // Background music: well below full scale, but clearly there.
        assertTrue(level in 0.02f..0.1f, "rms $level")
    }

    @Test
    fun `the clue cue stands out from the music`() {
        val sound = Soundscape(rate)
        sound.seconds(5f)
        val before = peak(sound.seconds(1f))
        sound.chime()
        val cue = peak(sound.seconds(1f))
        assertTrue(cue > before * 1.4f, "cue $cue against music $before")
    }

    @Test
    fun `tension makes it heavier`() {
        val calm = Soundscape(rate, seed = 3)
        val tense = Soundscape(rate, seed = 3).apply { tension = 1f }
        calm.seconds(6f)
        tense.seconds(6f)
        val calmLevel = rms(calm.seconds(10f))
        val tenseLevel = rms(tense.seconds(10f))
        assertTrue(tenseLevel > calmLevel * 1.15f, "tense $tenseLevel against calm $calmLevel")
    }

    @Test
    fun `with the music off only the cue sounds, and the volume fades out`() {
        val sound = Soundscape(rate).apply { music = false }
        sound.seconds(4f)
        assertTrue(peak(sound.seconds(1f)) < 0.001f)
        sound.chime()
        assertTrue(peak(sound.seconds(1f)) > 0.1f)

        val fading = Soundscape(rate)
        fading.seconds(3f)
        fading.volume = 0f
        fading.seconds(3f)
        assertTrue(fading.silent)
    }

    /** Not a check: writes a minute to listen to, at shared/build/soundscape-preview.wav. See `just soundscape`. */
    @Test
    fun `writes a preview`() {
        val sound = Soundscape(rate)
        val parts = listOf(
            sound.seconds(20f),
            sound.apply { chime() }.seconds(10f),
            sound.apply { tension = 1f }.seconds(25f),
            sound.apply { chime() }.seconds(5f),
        )
        val all = FloatArray(parts.sumOf { it.size })
        var at = 0
        parts.forEach {
            it.copyInto(all, at)
            at += it.size
        }
        writeWav(File("build/soundscape-preview.wav"), all)
    }

    private fun writeWav(file: File, samples: FloatArray) {
        file.parentFile.mkdirs()
        DataOutputStream(FileOutputStream(file).buffered()).use { out ->
            fun le32(v: Int) = out.writeInt(Integer.reverseBytes(v))
            fun le16(v: Int) = out.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            out.writeBytes("RIFF")
            le32(36 + samples.size * 2)
            out.writeBytes("WAVEfmt ")
            le32(16)
            le16(1)
            le16(1)
            le32(rate)
            le32(rate * 2)
            le16(2)
            le16(16)
            out.writeBytes("data")
            le32(samples.size * 2)
            samples.forEach { le16((it.coerceIn(-1f, 1f) * 32767).toInt()) }
        }
    }
}
