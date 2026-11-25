package app.unconfirmed.shared

/*
 * What each clue type can show. Two suspects with the same trait for a type can't be told apart by
 * that clue. The app draws every value: see ClueArt.kt, which keeps its own copy of these names.
 */
internal const val PRINTS_DEEP = "deep"
internal const val PRINTS_NONE = "none"
internal const val PRINTS_SHALLOW = "shallow"
internal const val PRINTS_CLAWED = "clawed"
internal const val PRINTS_HOOVES = "hooves"

/** Round pads with no claw marks, like a big cat's. */
internal const val PRINTS_PADDED = "padded"

internal const val CALL_SHRILL = "shrill"
internal const val CALL_HOWL = "howl"
internal const val CALL_NONE = "silent"
internal const val CALL_SPLASH = "splash"

/** A voice singing back: only the Jackalope. */
internal const val CALL_VOICE = "voice"

/** Steady tapping, like a hammer on rock: the Tommyknocker. */
internal const val CALL_KNOCK = "knock"

internal const val CAM_LOW = "low"
internal const val CAM_UPRIGHT = "upright"
internal const val CAM_SHADOW = "shadow"
internal const val CAM_GLOW = "glow"

internal const val SKETCH_HUMPS = "humps"
internal const val SKETCH_SMUDGE = "smudge"
internal const val SKETCH_HUNCHED = "hunched"
internal const val SKETCH_MAN = "man"
internal const val SKETCH_WINGED = "winged"
internal const val SKETCH_LIZARD = "lizard"
internal const val SKETCH_LEGS = "legs"
internal const val SKETCH_HORNED = "horned"

/** A man-shaped figure with a dog's head and pointed ears: Dogman. */
internal const val SKETCH_DOG = "dog"

/** A long, low cat with a long tail: the black panther. */
internal const val SKETCH_CAT = "cat"

/** A snake bent into a ring with its tail in its mouth: the hoop snake. */
internal const val SKETCH_HOOP = "hoop"

/** Long arms curling up out of the water: the kraken. */
internal const val SKETCH_TENTACLES = "tentacles"

internal const val SAMPLE_HAIR = "hair"
internal const val SAMPLE_SLIME = "slime"
internal const val SAMPLE_FEATHER = "feather"
internal const val SAMPLE_NOTHING = "nothing"
internal const val SAMPLE_SCALE = "scale"

internal fun traits(prints: String, call: String, cam: String, sketch: String, sample: String) = mapOf(
    ClueType.FOOTPRINTS to prints,
    ClueType.AUDIO to call,
    ClueType.TRAIL_CAM to cam,
    ClueType.WITNESS_SKETCH to sketch,
    ClueType.SAMPLE to sample,
)
