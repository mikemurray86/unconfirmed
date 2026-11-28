package app.unconfirmed.game

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import app.unconfirmed.shared.Soundscape

/**
 * Plays the game's [Soundscape] while the game is on screen. It's made on the fly, on an audio
 * thread of its own.
 *
 * It keeps out of the way:
 *  - nothing plays, or is even worked out, while the phone is on silent or vibrate
 *  - if something else is already playing, like a podcast for the walk, the music stays off and
 *    only the clue cue sounds
 *  - the player can turn it all off, and that's remembered
 */
class SoundPlayer(private val context: Context) {
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val soundscape = Soundscape(SAMPLE_RATE)
    private val lock = Any()
    private var thread: Thread? = null

    @Volatile private var wanted = false

    @Volatile private var otherAudio = false

    // Brings the sound back when the phone comes off silent while the game is on screen.
    private val ringerChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (audible()) startThread() else updateLevels()
        }
    }
    private var listening = false

    /** Whether the game makes any sound at all. The player sets it, and it's remembered. */
    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, true))
        private set

    fun turnOn(on: Boolean) {
        enabled = on
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        if (audible()) startThread() else updateLevels()
    }

    /** How tense the music is, from 0 to 1. See [Soundscape.tension]. */
    var tension: Float
        get() = soundscape.tension
        set(value) {
            soundscape.tension = value
        }

    /** Starts the sound, when the game comes on screen. */
    fun start() {
        // Checked before our own sound starts, which would count too.
        otherAudio = audio.isMusicActive
        wanted = true
        if (!listening) {
            ContextCompat.registerReceiver(
                context,
                ringerChanges,
                IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            listening = true
        }
        if (audible()) startThread()
    }

    /** Fades the sound out, when the game goes off screen. */
    fun stop() {
        wanted = false
        if (listening) {
            context.unregisterReceiver(ringerChanges)
            listening = false
        }
        updateLevels()
    }

    /** The cue for a clue turning up. */
    fun chime() {
        if (enabled && wanted) soundscape.chime()
    }

    /** A creature's own call, as it shows itself. [kind] is its call trait. */
    fun call(kind: String) {
        if (enabled && wanted) soundscape.call(kind)
    }

    /** On screen, switched on, and the phone isn't on silent or vibrate. */
    private fun audible() = enabled && wanted && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL

    private fun updateLevels() {
        soundscape.volume = if (audible()) 1f else 0f
        soundscape.music = !otherAudio
    }

    private fun startThread() = synchronized(lock) {
        updateLevels()
        if (thread?.isAlive == true) return@synchronized
        thread = Thread(::play, "soundscape").apply { start() }
    }

    private fun play() {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT) *
                    2,
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val buffer = FloatArray(CHUNK)
        try {
            track.play()
            var chunks = 0
            while (true) {
                // The ringer can be switched to silent at any moment.
                if (chunks++ % CHUNKS_PER_CHECK == 0) updateLevels()
                synchronized(lock) {
                    // Done once it has faded out and shouldn't be heard, so a phone on silent
                    // isn't left working out sound nobody hears.
                    if (soundscape.silent && !audible()) {
                        thread = null
                        return
                    }
                }
                soundscape.render(buffer)
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val CHUNK = 1_024
        const val CHUNKS_PER_CHECK = 10
        const val KEY_ENABLED = "sound_enabled"
    }
}

/** The game's sound, for any screen that needs to play a cue or set the mood. Null in previews. */
val LocalSound = staticCompositionLocalOf<SoundPlayer?> { null }
