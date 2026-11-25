package app.unconfirmed.shared

/** Full names for the states cases are set in, for showing to players. */
val STATE_NAMES = mapOf(
    "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas", "CA" to "California",
    "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware", "FL" to "Florida", "GA" to "Georgia",
    "HI" to "Hawaii", "ID" to "Idaho", "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa",
    "KS" to "Kansas", "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine", "MD" to "Maryland",
    "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota", "MS" to "Mississippi",
    "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada", "NH" to "New Hampshire",
    "NJ" to "New Jersey", "NM" to "New Mexico", "NY" to "New York", "NC" to "North Carolina",
    "ND" to "North Dakota", "OH" to "Ohio", "OK" to "Oklahoma", "OR" to "Oregon", "PA" to "Pennsylvania",
    "RI" to "Rhode Island", "SC" to "South Carolina", "SD" to "South Dakota", "TN" to "Tennessee",
    "TX" to "Texas", "UT" to "Utah", "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington",
    "WV" to "West Virginia", "WI" to "Wisconsin", "WY" to "Wyoming",
)

fun stateName(code: String?): String = code?.let { STATE_NAMES[it] ?: it } ?: "this area"

/*
 * Creatures that range over a whole region. Each joins the field guide of every state in its range:
 * see Regional.kt. They're declared before the states' guides, which take them in. Research notes
 * and sources for the Southwest's are in build/southwest.md.
 */
private val SOUTHWEST = setOf("TX", "NM", "AZ")

private val CHUPACABRA = RegionalCreature(
    Suspect(
        "chupacabra",
        "Chupacabra",
        "The goat-sucker. First reported in Puerto Rico in 1995, then across Mexico and into Texas, New " +
            "Mexico and Arizona. Here it's a hairless, grey, hunched thing on four legs, with a ridge of " +
            "spines down its back and a long snout. Its prints are clawed, like a dog's. It screeches, and " +
            "the animals it kills are left with two neat punctures and hardly any blood.",
        traits(PRINTS_CLAWED, CALL_SHRILL, CAM_LOW, SKETCH_HUNCHED, SAMPLE_NOTHING),
    ),
    // Reports spread east along the Gulf too, and Florida had its own wave in 1996.
    SOUTHWEST + setOf("OK", "LA", "MS", "AL", "GA", "FL", "AR", "SC"),
)

private val WINGED_MONSTER = RegionalCreature(
    Suspect(
        "winged-monster",
        "Winged Monster",
        "In April 1890 the Tombstone Epitaph told of a \"winged monster\" killed in the desert: no " +
            "feathers, smooth skin and wings like a bat's. Southern New Mexico has reported scaly winged " +
            "things since the 1800s, the size of small planes. It doesn't come down, so it leaves no " +
            "tracks, only a shriek, a vast shadow and the odd shed scale.",
        traits(PRINTS_NONE, CALL_SHRILL, CAM_SHADOW, SKETCH_WINGED, SAMPLE_SCALE),
    ),
    SOUTHWEST,
)

private val MOGOLLON_MONSTER = RegionalCreature(
    Suspect(
        "mogollon-monster",
        "Mogollon Monster",
        "The Bigfoot of the Mogollon Rim, reported since 1903 and the terror of every scout camp up " +
            "there. Seven feet or more, upright and covered in long dark hair, with a strong smell. Deep, " +
            "five-toed prints, whistles and screams at night, and rocks thrown at campers.",
        traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
    ),
    setOf("AZ", "NM"),
)

/** The Southwest's, then those that range more widely, from RegionalCases.kt. */
private val REGIONAL_CREATURES: List<RegionalCreature> =
    listOf(CHUPACABRA, WINGED_MONSTER, MOGOLLON_MONSTER) + WIDE_CREATURES

/*
 * Montana's field guide. Each clue type sorts the six entries into groups that the clue can't tell
 * apart:
 *   - Footprints: deep (Ringdocus, Bigfoot), none (lake monster, Blacktail), shallow and far apart
 *     (Prairie Giant, Thunderbird)
 *   - Field recording: a shrill cry (Ringdocus, Thunderbird), howls and knocks (Bigfoot), no voice
 *     (lake monster, Prairie Giant, Blacktail)
 *   - Trail cam: low and long (lake monster, Ringdocus), upright (Bigfoot, Prairie Giant), only a
 *     shadow (Blacktail, Thunderbird)
 *   - Witness sketch: humps and a neck (lake monster), a featureless smudge (Blacktail), hunched
 *     with the head low (Ringdocus, Thunderbird), man-shaped (Bigfoot, Prairie Giant)
 *   - Sample: hair (Ringdocus, Bigfoot), slime (lake monster), a feather (Thunderbird), nothing
 *     (Prairie Giant, Blacktail)
 * Any two entries differ on at least three types, so whichever three clues a playthrough draws
 * settle the case. Two clues often leave a pair of suspects, and the rarer traits (the lake
 * monster's sketch and slime, the Blacktail's smudge, the Thunderbird's feather) settle it alone.
 * The Jackalope joins from RegionalCases.kt (shallow, a voice, low, horned, hair), and it too
 * differs from each of the six on at least three types.
 */
private val FLATHEAD_MONSTER = Suspect(
    "flathead-lake-monster",
    "Flathead Lake Monster",
    "Northwest Montana's lake serpent, reported since a steamboat sighting in 1889. A long, dark, " +
        "humped back that rides low in the water, with a long neck at one end. Leaves no footprints, " +
        "only a slick of fishy slime where it has come ashore. Silent apart from splashing and a " +
        "slow, heavy breath.",
    traits(PRINTS_NONE, CALL_NONE, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
)

private val RINGDOCUS = Suspect(
    "ringdocus",
    "Ringdocus",
    "The wolf-like beast shot near Ennis in 1886 after it got into the livestock. Its mount is " +
        "in the Madison Valley History Museum. Low-slung, with high, hunched shoulders, a sloping " +
        "back and its head carried low. Deep, clawed prints and coarse, bristly hair. Its yelping " +
        "cry sounds like laughter.",
    traits(PRINTS_DEEP, CALL_SHRILL, CAM_LOW, SKETCH_HUNCHED, SAMPLE_HAIR),
)

private val MONTANA_BIGFOOT = Suspect(
    "bigfoot",
    "Bigfoot",
    "Western Montana's forest giant, reported most in the mountains around Missoula and the " +
        "Bitterroot. Upright and man-shaped, heavy and covered in long dark hair. Deep, wide, " +
        "five-toed prints. Known for long howls and wood knocks.",
    traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
)

private val PRAIRIE_GIANT = Suspect(
    "prairie-giant",
    "Prairie Giant",
    "Reported in the ranch country around Great Falls during the 1975–76 cattle-mutilation " +
        "scare. Upright and man-shaped, but very tall and light on its feet: its prints are shallow " +
        "and far apart. Never heard to call, and leaves nothing behind, not even a hair. Witnesses " +
        "mention a low electrical hum.",
    traits(PRINTS_SHALLOW, CALL_NONE, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_NOTHING),
)

private val BLACKTAIL_MONSTER = Suspect(
    "blacktail-monster",
    "Blacktail Monster",
    "Blacktail Pond near Dillon is better known for kids' fishing derbies, but locals talk of a " +
        "long, inky shape just below the surface that moves against the current. It never leaves " +
        "the water, so there are no tracks, and it makes no sound beyond sudden ripples. Nobody has " +
        "seen more than its shadow, and nets come back empty.",
    traits(PRINTS_NONE, CALL_NONE, CAM_SHADOW, SKETCH_SMUDGE, SAMPLE_NOTHING),
)

private val THUNDERBIRD = Suspect(
    "thunderbird",
    "Thunderbird",
    "Giant birds reported over Montana's mountains, with wingspans said to run ten to twenty " +
        "feet. Most often seen as a vast shadow sweeping the ground. It lands in long hops, so its " +
        "talon marks are shallow and far apart. Perched, it hunches with its head low between high " +
        "shoulders. A piercing, cackling screech, and feathers longer than any eagle's.",
    traits(PRINTS_SHALLOW, CALL_SHRILL, CAM_SHADOW, SKETCH_HUNCHED, SAMPLE_FEATHER),
)

private val MONTANA_SUSPECTS = fieldGuide(
    "MT",
    listOf(FLATHEAD_MONSTER, RINGDOCUS, MONTANA_BIGFOOT, PRAIRIE_GIANT, BLACKTAIL_MONSTER, THUNDERBIRD),
    REGIONAL_CREATURES,
)

val FLAT_WATER = CaseDefinition(
    id = "flat-water",
    state = "MT",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "The Flat Water",
        "Calm Morning, Big Wake",
        "Guide Swears It Was Bigger Than His Boat",
        "No Wind, But Something Moved",
        "What Rolled Past the Dock?",
    ),
    teasers = listOf(
        "Something bigger than a boat rolled through calm water.",
        "A retired fishing guide stopped dead on his morning walk.",
        "No wind, no boats out, and the water moved anyway.",
        "A man who's fished here forty years says he's never seen the like.",
        "Whatever it was, it didn't come up for long.",
    ),
    lead = "A retired fishing guide was out walking the shore when he stopped dead. Something rolled " +
        "through the water, he says, bigger than his old boat, on a morning with no wind.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Soft ground right down to the water's edge, and not one footprint in it. Whatever " +
                "was here never walked the bank.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Twelve minutes from a phone left recording overnight. Water moving, and something " +
                "large breathing out slowly. Nothing ever calls, howls or cries.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "One frame, taken low to the ground. A long, dark back, lower than a fence rail, " +
                "curving out of the frame. No head, and no legs you can make out.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The guide drew it on a napkin: a row of humps along the waterline, and at one end " +
                "something like a long neck rising out of the water.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "No hair and no feathers. Just a clear, fishy slime coating the reeds where " +
                "something heavy slid back into the lake.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = FLATHEAD_MONSTER.id,
    solvedNote = "A long low back, no voice and no footprints. Polson would be proud.",
)

val CARRIES_OFF_DOGS = CaseDefinition(
    id = "carries-off-dogs",
    state = "MT",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Carries Off Dogs",
        "Third Dog Gone This Month",
        "Porch Light Didn't Scare It",
        "Pet Owners on Edge",
        "Watcher at the End of the Road",
    ),
    teasers = listOf(
        "Three dogs missing in a month, and something that doesn't fear the porch light.",
        "Neighbors are keeping their dogs in after dark.",
        "One owner saw something at the end of the road, and it saw her.",
        "It watched from the dark, then left in its own time.",
        "Three families, three empty doghouses, no answers.",
    ),
    lead = "Three dogs have gone missing around here this month. One owner saw something at the end " +
        "of the gravel road after dark. It didn't run from the porch light. It just watched, then left.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep prints in the mud, broad and heavy-set. Something with real weight behind it " +
                "came through here more than once.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Dogs barking across the valley, then stopping all at once. A moment later, a high, " +
                "broken cry answers them. The witness's first thought was someone laughing.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A long, dark shape, low to the ground, slipping under the bottom rail of a fence. " +
                "It's gone by the next frame.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn by the owner from the porch: hunched, high at the shoulders, with its head " +
                "held low and pushed forward.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, dark hairs snagged on the bottom wire of a fence, right where the dog " +
                "run meets the hayfield.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = RINGDOCUS.id,
    solvedNote = "Low to the ground, heavy and loud. The Madison Valley museum might want a word.",
)

val KNOCKS_IN_THE_TIMBER = CaseDefinition(
    id = "knocks-in-the-timber",
    state = "MT",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Knocks in the Timber",
        "Followed Back to the Truck",
        "Hikers Say Woods Weren't Empty",
        "Something Kept Pace",
        "Couple Cuts Walk Short",
    ),
    teasers = listOf(
        "Two hikers were followed all the way back to their truck.",
        "They never saw it. They're sure it saw them.",
        "A quiet hike turned into a long one back to the trailhead.",
        "Something matched them step for step, just out of sight.",
        "A couple who know these woods won't be back soon.",
    ),
    lead = "A couple walking these woods say something kept pace with them the whole way back to " +
        "the truck, always just out of sight. They didn't see it. They're sure it saw them.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "A line of deep impressions in the soft ground, pressed in hard and spaced like " +
                "something big that was in no hurry.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A long howl rolling down the slope, then two sharp knocks like wood on wood. On " +
                "the recording, the witness's dog can be heard whining to go inside.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A grainy figure standing upright between two trees, its head above the lowest " +
                "branches. It's facing away from the camera.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn from memory: tall and upright, shaped like a man but bigger, with its arms " +
                "hanging at its sides.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs caught in the bark of a pine, higher up the trunk than " +
                "either hiker could reach.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = MONTANA_BIGFOOT.id,
    solvedNote = "Heavy, upright and not shy about calling. Western Montana's most reported visitor.",
)

val LIGHTS_OVER_THE_PASTURE = CaseDefinition(
    id = "lights-over-the-pasture",
    state = "MT",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Lights Over the Pasture",
        "Cattle Break Through Fence",
        "Sheriff Fields Calls About Night Sky",
        "Rancher's Herd Spooked",
        "Nobody Owns a Helicopter",
    ),
    teasers = listOf(
        "Spooked cattle, a broken fence and lights in the sky.",
        "A herd broke loose overnight, and two neighbors called the sheriff.",
        "Something got the cattle running, and it wasn't a coyote.",
        "Lights in the sky, a fence down, and nobody can explain either.",
        "A rancher spent the morning rounding up a frightened herd.",
    ),
    lead = "A rancher's cattle broke through a fence out this way last night. By morning, two " +
        "neighbors had called the sheriff about lights in the sky. Nobody owns a helicopter.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The ground should hold prints and barely does. A few shallow marks, much too far " +
                "apart to be a person walking.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Cattle bawling, then quiet. No call, no cry, nothing with a voice. Underneath it, " +
                "something steady that the witness can't place.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "An upright shape at the far edge of the frame, arms at its sides. There's nothing " +
                "nearby to judge its height against.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A neighbor drew it against the fence line: upright and shaped like a man, but its " +
                "head comes well above the posts.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The deputy bagged everything near the broken fence. No hair, no blood, no scat. " +
                "Nothing that came from an animal at all.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = PRAIRIE_GIANT.id,
    solvedNote = "Tall, silent and barely touching the ground. Cascade County has seen this before.",
)

val THE_WATCHED_SHORE = CaseDefinition(
    id = "the-watched-shore",
    state = "MT",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "The Watched Shore",
        "Dog Won't Go Near the Water",
        "Late-Night Walker Spooked",
        "Calm Water, Moving Shadow",
        "Something Swam Against the Current",
    ),
    teasers = listOf(
        "A dog walker swears the water was watching back.",
        "A late-night walk ended with a dog that won't go near the edge.",
        "The water went flat calm, then something moved under it.",
        "A local says what she saw was longer than a canoe.",
        "Her dog saw it first, and hasn't forgotten.",
    ),
    lead = "A late-night dog walker on this stretch of shore says the water went flat calm, then a shadow longer " +
        "than a canoe slid under the surface, against the current. Her dog won't go near the " +
        "edge now.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Mud along the bank, soft enough to hold anything, and not a single print in it. " +
                "Nothing came out of the water here.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Forty minutes of shoreline at dusk. Frogs, then nothing. A run of sudden ripples " +
                "lapping the bank, and no call of any kind.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A long, dark shadow across the bottom of the frame, blurred at the edges. " +
                "Whatever cast it isn't in the picture.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The witness tried three times and gave up on a shape. The page is a long, dark " +
                "smudge with no head, no fins and no legs.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A dip net swept along the bank comes back clean every time. Not a scale, not a " +
                "hair, not a scrap of anything.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = BLACKTAIL_MONSTER.id,
    solvedNote = "All shadow, no sound and nothing left behind. Try leaving candy on the shore.",
)

val SHADOW_OVER_THE_RIDGE = CaseDefinition(
    id = "shadow-over-the-ridge",
    state = "MT",
    scene = Scene.RIDGE,
    headlines = listOf(
        "Shadow Over the Ridge",
        "Calf Vanishes in Broad Daylight",
        "Rancher Saw Only a Shadow",
        "Ranch Loses Calf, Finds No Trace",
        "Rancher Short One Calf",
    ),
    teasers = listOf(
        "A calf went missing, and the only witness was looking up.",
        "A rancher lost a calf yesterday and didn't see what took it.",
        "One moment the calf was there. The next, the pasture was empty.",
        "No tracks to follow and no carcass to find.",
        "A rancher says he saw something, just not the thing itself.",
    ),
    lead = "A rancher below the ridge lost a calf yesterday afternoon. He didn't see what took it. " +
        "He saw a shadow wider than his truck sweep across the pasture, and then the sky was empty.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "A few shallow marks pressed into the dirt, so far apart that whatever made them was " +
                "barely touching down.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A recording from a camp on the ridge: a piercing cry, broken up and rising, close " +
                "to a cackle. Then only wind.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The camera fired in full daylight and caught nothing solid, only a huge shadow " +
                "crossing the grass.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn from across the valley: something hunched on a rock, high at the shoulders, " +
                "its head held low and forward.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A single feather caught on the barbed wire, longer than the rancher's forearm and " +
                "darker than any eagle's.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = THUNDERBIRD.id,
    solvedNote = "A shadow from the sky, a screech and a feather no eagle grew. Look up next time.",
)

/*
 * New Jersey's field guide: the Jersey Devil and the regional creatures that reach the state.
 *   - Footprints: hooves (Jersey Devil), deep (Bigfoot), padded (black panther), none (Hidebehind,
 *     sea serpent)
 *   - Field recording: a scream (Jersey Devil, panther), a howl (Bigfoot), a splash (sea serpent),
 *     nothing (Hidebehind)
 *   - Trail cam: upright (Jersey Devil, Bigfoot), low (panther, sea serpent), a shadow (Hidebehind)
 *   - Witness sketch: winged, man-shaped, a cat, a smudge, humps: one each
 *   - Sample: hair (Jersey Devil, Bigfoot, panther), nothing (Hidebehind), slime (sea serpent)
 * Any two differ on at least three types, so any three clues settle a case.
 */
private val JERSEY_DEVIL = Suspect(
    "jersey-devil",
    "Jersey Devil",
    "The Pine Barrens' own, said to be Mother Leeds's thirteenth child, born in 1735, and reported " +
        "well outside the Pines since. Hooves, leathery wings, a horse-like head and a forked tail. It " +
        "stands upright, screams, and leaves coarse hair where it has roosted.",
    traits(PRINTS_HOOVES, CALL_SHRILL, CAM_UPRIGHT, SKETCH_WINGED, SAMPLE_HAIR),
)

private val NEW_JERSEY_SUSPECTS = fieldGuide("NJ", listOf(JERSEY_DEVIL), REGIONAL_CREATURES)

/** The first case written. Its first three clues came before clue types were drawn at random. */
val DUSK_CROSSING = CaseDefinition(
    id = "dusk-crossing",
    state = "NJ",
    scene = Scene.PINE_BARRENS,
    headlines = listOf(
        "The Dusk Crossing",
        "Witnesses Can't Agree",
        "Something Crossed the Sand Road",
        "Sighting Runs on Page Nine",
        "Big, Fast and Gone",
    ),
    teasers = listOf(
        "Two witnesses agree on the size and nothing else.",
        "Something large crossed a sand road in the Pines at dusk.",
        "Two people saw it. They tell two different stories.",
        "It crossed the road and was gone before anyone could look twice.",
        "The local paper ran it on page nine. We're running it here.",
    ),
    lead = "Something large crossed the sand road out here at dusk. Two witnesses. They agree on " +
        "the size and nothing else. The local paper ran it on page nine.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Two-legged stride, far too long for a person. The prints are small and split down " +
                "the middle, like a hoof. They stop dead in open ground with nothing around them.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Eleven seconds of wind. Then a scream that the witness's dog would not stay in the " +
                "room for, followed by slow, heavy, leathery wingbeats moving away.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "One frame, badly blurred. A long neck and a narrow, horse-like head. A thin tail " +
                "that splits at the tip. The infrared flash caught no shine from its eyes.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The second witness's drawing: standing upright, with great leathery wings half open " +
                "behind it and a long, horse-like head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, dark hairs caught in the bark of a pitch pine beside the road, higher than a " +
                "man could reach.",
        ),
    ),
    suspects = NEW_JERSEY_SUSPECTS,
    answerId = JERSEY_DEVIL.id,
    solvedNote = "Hooves, wings, a horse's head and a forked tail. Mother Leeds's thirteenth child is still out there.",
)

/*
 * Washington's field guide. Research notes and sources for these and the other candidates are
 * in build/washington.md.
 *   - Footprints: deep (Bigfoot, Batsquatch, Gumberoo), none (Caddy), clawed (Lake Chelan)
 *   - Field recording: howls and knocks (Bigfoot), a screech (Batsquatch), no voice (the rest)
 *   - Trail cam: upright (Bigfoot), a shadow overhead (Batsquatch), low (Caddy, Chelan, Gumberoo)
 *   - Witness sketch: man-shaped, winged, humps, a lizard, hunched: one each
 *   - Sample: hair (Bigfoot, Batsquatch), slime (Caddy), a scale (Chelan), nothing (Gumberoo)
 * Any two differ on at least three types, so any three clues settle a case.
 */
private val WASHINGTON_BIGFOOT = Suspect(
    "bigfoot",
    "Bigfoot",
    "The Cascades' forest giant. Skamania County made killing one illegal in 1969, and miners at " +
        "Ape Canyon said a band of them stoned their cabin in 1924. Upright and man-shaped, covered " +
        "in long dark hair. Deep, wide, five-toed prints. Whoops, howls and wood knocks.",
    traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
)

private val BATSQUATCH = Suspect(
    "batsquatch",
    "Batsquatch",
    "Reported in the foothills of Mount Rainier in 1994: a winged ape, nine feet tall, with " +
        "bluish fur and great leathery wings as well as arms. Heavy enough to leave deep prints " +
        "where it lands. A high screech, and from below only a vast shape passing over.",
    traits(PRINTS_DEEP, CALL_SHRILL, CAM_SHADOW, SKETCH_WINGED, SAMPLE_HAIR),
)

private val CADBOROSAURUS = Suspect(
    "cadborosaurus",
    "Cadborosaurus",
    "Caddy, the Salish Sea's serpent, named for Cadboro Bay near Victoria and reported in " +
        "Washington waters since the 1890s. A long body in humps, and a horse-like head on a long " +
        "neck. It never comes ashore, so there are no tracks. Silent apart from a blow at the " +
        "surface, and it leaves a fishy slime.",
    traits(PRINTS_NONE, CALL_NONE, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
)

private val LAKE_CHELAN_MONSTER = Suspect(
    "lake-chelan-monster",
    "Lake Chelan Monster",
    "A dragon first written up in 1892, said to live in Lake Chelan's 1,500-foot depths. Built " +
        "like an alligator, with a serpent's head and a long scaly tail. It hauls out on the shore, " +
        "leaving clawed prints and shed scales. Never heard to make a sound.",
    traits(PRINTS_CLAWED, CALL_NONE, CAM_LOW, SKETCH_LIZARD, SAMPLE_SCALE),
)

private val GUMBEROO = Suspect(
    "gumberoo",
    "Gumberoo",
    "A terror of the old logging camps: bigger than a bear, coal-black and almost hairless, with " +
        "a hide that bullets bounce off. It goes on all fours and leaves deep, round prints. Silent, " +
        "and it sheds no hair at all, though it smells of smoke. Only fire is said to kill it.",
    traits(PRINTS_DEEP, CALL_NONE, CAM_LOW, SKETCH_HUNCHED, SAMPLE_NOTHING),
)

private val WASHINGTON_SUSPECTS = fieldGuide(
    "WA",
    listOf(WASHINGTON_BIGFOOT, BATSQUATCH, CADBOROSAURUS, LAKE_CHELAN_MONSTER, GUMBEROO),
    REGIONAL_CREATURES,
)

val ROCKS_ON_THE_ROOF = CaseDefinition(
    id = "rocks-on-the-roof",
    state = "WA",
    scene = Scene.RIDGE,
    headlines = listOf(
        "Rocks on the Roof",
        "Renters Pelted Through the Night",
        "Cabin Under Siege",
        "Porch Buried in Stones",
        "Who Threw the Rocks?",
    ),
    teasers = listOf(
        "Something pelted a cabin with stones all night.",
        "A couple's weekend away ended with a porch full of rocks.",
        "Thrown, not falling, say the renters, and it went on for hours.",
        "Nobody slept in the cabin below the ridge last night.",
        "Someone, or something, didn't want visitors.",
    ),
    lead = "A couple renting a cabin below the ridge say stones came down on the roof for hours " +
        "after dark. Thrown, not falling. By morning the porch was covered in them.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, wide prints in the cinders behind the cabin, five toes splayed, each one sunk " +
                "farther in than a person could press.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone left on the windowsill. Rocks thudding onto the roof, then a long, rising " +
                "whoop from up the slope, and another answering farther off.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A figure standing upright at the treeline, broad as a door, one long arm hanging " +
                "lower than its knee.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn by the renter: man-shaped but massive, no neck to speak of, its arms down past " +
                "its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs snagged on the cabin's woodpile, well above head height.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = WASHINGTON_BIGFOOT.id,
    solvedNote = "Rocks at night and a whoop from the ridge. The miners at Ape Canyon had the same trouble in 1924.",
)

val SOMETHING_IN_THE_STRAIT = CaseDefinition(
    id = "something-in-the-strait",
    state = "WA",
    scene = Scene.LAKE_SHORE,
    place = "Ferry landing",
    headlines = listOf(
        "Something in the Strait",
        "Deckhand Sees Something Off the Bow",
        "Early Ferry, Strange Passenger",
        "It Was Watching the Boat",
        "Ferry Crew Member Shaken",
    ),
    teasers = listOf(
        "A ferry deckhand saw a head like a horse's, on a neck.",
        "The early ferry had company this morning, for a moment.",
        "A deckhand says something surfaced off the bow and watched the boat.",
        "It went under before anyone else turned around.",
        "One crew member saw it. The rest are still asking him about it.",
    ),
    lead = "A deckhand on the early ferry swears a head came up off the bow: long-necked, like a " +
        "horse's, watching the boat. It went under before anyone else turned around.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Smooth wet sand all the way to the tide line. Gull tracks and one dog. Nothing came " +
                "out of the water on foot.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A recording from the beach at dawn: waves, and twice a long breath blown out at the " +
                "surface, like a whale but closer. No voice at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a dock piling caught a long, low back between the swells, darker than " +
                "the water and longer than a kayak.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The deckhand's drawing: three humps in a row, and in front of them a long neck with " +
                "a head like a horse's.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "No hair and no feathers. The kelp on the rocks is coated in a clear, fishy slime, " +
                "above where the tide reaches.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = CADBOROSAURUS.id,
    solvedNote = "Humps, a horse's head and a fishy slick. They'll want to hear about this in Cadboro Bay.",
)

val SOMETHING_LANDED_ON_THE_ROAD = CaseDefinition(
    id = "something-landed-on-the-road",
    state = "WA",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Something Landed on the Road",
        "Driver Brakes for Winged Thing",
        "It Came Down Out of the Trees",
        "Teen Driver Won't Take Back Road Again",
        "Wings Wider Than the Pickup",
    ),
    teasers = listOf(
        "A young driver says something with wings set down in his headlights.",
        "It stood in the road like a man, then it opened its wings.",
        "The back road home took a lot longer last night.",
        "He says its wings reached from ditch to ditch.",
        "Whatever it was, it went up, not into the trees.",
    ),
    lead = "A young driver coming home late on the back road through the timber says something came " +
        "down out of the trees and landed in his headlights. It stood up like a man, then spread wings " +
        "wider than his pickup and went straight up.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Two deep prints in the soft shoulder of the road, sunk in hard as if something heavy had " +
                "dropped onto them. No trail leads in or out.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The truck's dashcam kept recording. Engine idling, then a high, ragged screech overhead " +
                "that drowns out the radio.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a gatepost caught no animal at all, only a huge shadow sweeping across the " +
                "gravel from above.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The driver's drawing: man-shaped and heavy, with arms, and behind them a great pair of " +
                "wings like a bat's.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse hairs caught in a fir branch above the road, higher than the cab of the " +
                "truck.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = BATSQUATCH.id,
    solvedNote = "An ape with wings, landing on a back road. The foothills of Mount Rainier have heard " +
        "this one since 1994.",
)

val DRAG_MARKS_AT_THE_DOCK = CaseDefinition(
    id = "drag-marks-at-the-dock",
    state = "WA",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Drag Marks at the Dock",
        "Kayaks Knocked Off Their Rack",
        "Something Came Up the Boat Ramp",
        "Campers Find a Trail Into the Lake",
        "Deep Water, Visitor on the Beach",
    ),
    teasers = listOf(
        "Something hauled itself out of the lake overnight and went back in.",
        "Campers woke to a trail across the beach, and claw marks along it.",
        "Whatever came up the ramp left the kayaks scattered.",
        "The lake is deep here. Something came up out of it anyway.",
        "One camper heard the kayaks go over and didn't look out.",
    ),
    lead = "Campers by the deep end of the lake woke to find their kayaks knocked off the rack and a " +
        "wide trail dragged across the beach, from the water up to the fire pit and back again.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Along both sides of the drag mark, prints with long claws dug into the sand, splayed " +
                "out to the side like a lizard's.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone left recording in a tent. The kayaks clatter over, something heavy drags across " +
                "the sand, then water. No growl, no call, nothing with a voice.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera by the boat ramp caught a long, low shape along the bottom of the frame, no " +
                "higher than the ramp's railing.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn by a camper who did look out: built like an alligator, with short legs, a narrow " +
                "snake's head and a long tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A scale as big as a playing card in the sand by the fire pit, hard and ridged, and pale " +
                "as bone.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = LAKE_CHELAN_MONSTER.id,
    solvedNote = "Claws, scales and fifteen hundred feet of water to go home to. The Spokane papers " +
        "wrote it up in 1892.",
)

val BULLETS_BOUNCED_OFF = CaseDefinition(
    id = "bullets-bounced-off",
    state = "WA",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Bullets Bounced Off",
        "Logging Camp Raided Overnight",
        "Night Watchman Swears He Hit It",
        "Cook Tent Torn Open, Coolers Emptied",
        "It Didn't Even Flinch",
    ),
    teasers = listOf(
        "The watchman fired three times and swears every shot hit.",
        "A logging crew's camp was cleaned out of food overnight.",
        "Something bigger than a bear tore through the cook tent.",
        "The crew boss says nothing he's heard of shrugs off a rifle.",
        "Every cooler empty, and the watchman's still shaking.",
    ),
    lead = "A logging crew's camp was raided last night. The cook tent was torn open and every cooler " +
        "emptied. The night watchman fired at it three times, swears he hit it every time, and says it " +
        "didn't even flinch.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep prints all round the cook tent, pressed in hard by something very heavy. The ground " +
                "is too churned to make out their shape.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The watchman's radio was keyed the whole time. Canvas ripping, three rifle shots, coolers " +
                "being tipped over. The animal never makes a sound.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the skid road caught something long and low and black crossing in front of " +
                "the trucks, no higher than their bumpers.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The watchman's drawing: hunched and heavy, high at the shoulders, its head carried low " +
                "and forward.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "No hair, no blood, no scat anywhere in camp. Only a smell of smoke on the torn canvas, " +
                "though the fire was out.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = GUMBEROO.id,
    solvedNote = "A hide bullets bounce off and a whiff of smoke. The old lumberjacks would tell you to " +
        "keep it away from the fire.",
)

/*
 * California's field guide. Research notes and sources for these and the other candidates are
 * in build/california.md.
 *   - Footprints: deep (Bigfoot), none (Dark Watchers, Tessie), tiny (Nightcrawler), hooves (Billiwhack)
 *   - Field recording: howls and knocks (Bigfoot), a splash (Tessie), a scream (Billiwhack),
 *     nothing (Dark Watchers, Nightcrawler)
 *   - Trail cam: upright (Bigfoot, Dark Watchers, Billiwhack), a shadow (Tessie), a white glow (Nightcrawler)
 *   - Witness sketch: man-shaped (Bigfoot, Dark Watchers), a smudge (Tessie), all legs
 *     (Nightcrawler), horned (Billiwhack)
 *   - Sample: hair (Bigfoot, Billiwhack), nothing (the rest)
 * Any two differ on at least three types, so any three clues settle a case.
 */
private val CALIFORNIA_BIGFOOT = Suspect(
    "bigfoot",
    "Bigfoot",
    "Northern California's forest giant. The name itself was born at Bluff Creek in 1958, when " +
        "a road crew found sixteen-inch footprints around their bulldozer. Upright, man-shaped and " +
        "covered in dark hair. Deep, five-toed prints, whoops and wood knocks.",
    traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
)

private val DARK_WATCHERS = Suspect(
    "dark-watchers",
    "Dark Watchers",
    "Tall dark figures seen standing still on the ridgelines above Big Sur, watching. They're gone " +
        "when you get close. Steinbeck wrote of them in 1938. Upright and man-shaped, some say in a " +
        "hat. They leave no tracks and no trace, and never make a sound.",
    traits(PRINTS_NONE, CALL_NONE, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_NOTHING),
)

private val TAHOE_TESSIE = Suspect(
    "tahoe-tessie",
    "Tahoe Tessie",
    "Lake Tahoe's monster, reported since the 1860s. Witnesses see only a long, dark back under " +
        "the clear water, never a head. It never comes ashore. The only sound is a heavy slap on " +
        "the water as it rolls.",
    traits(PRINTS_NONE, CALL_SPLASH, CAM_SHADOW, SKETCH_SMUDGE, SAMPLE_NOTHING),
)

private val FRESNO_NIGHTCRAWLER = Suspect(
    "fresno-nightcrawler",
    "Fresno Nightcrawler",
    "Caught on a Fresno security camera in 2007: small, white and nearly all legs, gliding across " +
        "a lawn at night. Usually seen in pairs. Tiny, light prints, if any. Silent, it glows white " +
        "on infrared and leaves nothing behind.",
    traits(PRINTS_SHALLOW, CALL_NONE, CAM_GLOW, SKETCH_LEGS, SAMPLE_NOTHING),
)

private val BILLIWHACK_MONSTER = Suspect(
    "billiwhack-monster",
    "Billiwhack Monster",
    "Said to haunt Aliso Canyon near Santa Paula since the 1940s, around an old dairy. Upright " +
        "like a man, with twisted horns and coarse gray-white hair. It walks on hooves and screams " +
        "like a goat.",
    traits(PRINTS_HOOVES, CALL_SHRILL, CAM_UPRIGHT, SKETCH_HORNED, SAMPLE_HAIR),
)

private val CALIFORNIA_SUSPECTS = fieldGuide(
    "CA",
    listOf(CALIFORNIA_BIGFOOT, DARK_WATCHERS, TAHOE_TESSIE, FRESNO_NIGHTCRAWLER, BILLIWHACK_MONSTER),
    REGIONAL_CREATURES,
)

val TRACKS_ROUND_THE_DOZER = CaseDefinition(
    id = "tracks-round-the-dozer",
    state = "CA",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Tracks Around the Dozer",
        "Road Crew Finds Visitor's Calling Card",
        "Fuel Drum Tossed Down Bank",
        "Logging Road Vandal Leaves Big Prints",
        "Who Circled the Bulldozer?",
    ),
    teasers = listOf(
        "A road crew found giant footprints circling their bulldozer.",
        "A logging crew came back to find their machine had a visitor.",
        "Somebody tossed a full fuel drum down the bank overnight.",
        "The crew left the dozer at the end of the road. Something came by.",
        "The footprints go around twice, then into the trees.",
    ),
    lead = "A road crew left their bulldozer at the end of a new logging road last night. This " +
        "morning, giant footprints circle it, and a fuel drum has been tossed down the bank.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Sixteen inches long and five-toed, pressed deep into the freshly graded dirt. They go " +
                "around the dozer twice before heading into the trees.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone left in the cab overnight: a whooping call from down the creek, then knocks, " +
                "wood on wood, answering from across it.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A figure crossing the road upright, mid-stride, turning to look back over its shoulder " +
                "at the camera.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The foreman's drawing: man-shaped, huge in the chest, with its arms swinging down past " +
                "its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark hairs caught in the dozer's tracks, higher than a person's head.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = CALIFORNIA_BIGFOOT.id,
    solvedNote = "Big feet, a long stride and a look back over the shoulder. Bluff Creek has seen this before.",
)

val CLEAR_WATER = CaseDefinition(
    id = "clear-water",
    state = "CA",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Clear Water",
        "Paddleboarder Shaken",
        "Longer Than a Bus",
        "Something Passed Underneath",
        "Thirty Feet Down, Something Moved",
    ),
    teasers = listOf(
        "A paddleboarder saw a shadow pass under her, longer than a bus.",
        "The water's clear thirty feet down, and something was down there.",
        "A morning paddle turned into a quick trip back to shore.",
        "She never saw a head, she says, and she was looking.",
        "Something big passed underneath and kept going.",
    ),
    lead = "The water off this shore is clear thirty feet down. A paddleboarder says something " +
        "passed underneath her this morning, longer than a bus, and she never saw a head.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Nothing on the shore but people. Whatever it is, it hasn't come out of the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Wind and gulls, then a single huge slap on the water, like something heavy rolling " +
                "over. No call, no cry, just the waves it left.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a dock caught a dark shape under the surface, longer than the dock, " +
                "moving against the wind.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing is a long dark smear under the surface. No head, no tail, no fins she " +
                "could swear to.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Nothing. The water is too cold and clear to hold anything, and the nets come back " +
                "empty.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = TAHOE_TESSIE.id,
    solvedNote = "A long shadow in the clearest lake in the West. Tessie has kept people guessing since the 1860s.",
)

val FIGURE_ON_THE_NEXT_RIDGE = CaseDefinition(
    id = "figure-on-the-next-ridge",
    state = "CA",
    scene = Scene.RIDGE,
    headlines = listOf(
        "Figure on the Next Ridge",
        "Hikers Say They Were Being Watched",
        "Gone When They Got Close",
        "It Stood There Till Sundown",
        "Who Was on the Skyline?",
    ),
    teasers = listOf(
        "Two hikers watched a figure on the skyline watch them back.",
        "It didn't move for an hour, they say. Then it wasn't there.",
        "A sunset hike ended with a long look over the shoulder.",
        "They walked toward it. By the time they got there, nothing.",
        "Tall, dark and perfectly still, against the evening sky.",
    ),
    lead = "Two hikers on the ridge trail at dusk say a tall, dark figure stood on the next ridge over " +
        "for most of an hour, perfectly still, facing them. When they hiked over to it, there was " +
        "nobody there.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Dust and loose grit on the ridgetop where it stood, and not a single print in it. Not " +
                "even a scuff.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The hikers' video has sound. Wind, birdsong, then the birds stop all at once. Nothing " +
                "else. No call, no footsteps.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the fire road caught an upright figure on the skyline in frame after frame, " +
                "never moving. Then a frame with only the ridge.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One hiker drew it: tall and man-shaped, a dark outline against the sky, with no face they " +
                "could make out.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The ridgetop combed on hands and knees. No hair, no thread, no scat. Nothing was ever " +
                "standing there, by the look of it.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = DARK_WATCHERS.id,
    solvedNote = "Still, silent and gone when you get close. Steinbeck warned you not to look at them too long.",
)

val ACROSS_THE_LAWN = CaseDefinition(
    id = "across-the-lawn",
    state = "CA",
    scene = Scene.RIVER_TOWN,
    place = "Edge of town",
    headlines = listOf(
        "Across the Lawn",
        "Doorbell Camera Catches Late Visitors",
        "Homeowner Can't Explain 3 a.m. Footage",
        "Two of Them, Walking Together",
        "Neighbors Swap Security Video",
    ),
    teasers = listOf(
        "A doorbell camera caught two white shapes crossing the yard.",
        "A homeowner has watched the clip a hundred times and still can't say what it is.",
        "Small, white and walking together, at three in the morning.",
        "The neighbors' cameras are being checked tonight.",
        "They didn't run. They just walked out of frame.",
    ),
    lead = "A homeowner on the edge of town checked the doorbell camera this morning and found two " +
        "small white shapes walking across the front lawn at three a.m., side by side, smooth and " +
        "unhurried, then out of frame.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Faint marks in the wet grass, barely pressed in and much too far apart for something so " +
                "small.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The camera's microphone picked up sprinklers ticking and a dog two yards over. From the " +
                "lawn itself, nothing at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A neighbor's infrared camera: two shapes glowing white against the dark, much brighter " +
                "than the warm sidewalk around them.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn from the paused video: a small head and two long legs, and nothing else. No arms, " +
                "and hardly a body.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The lawn and the flower beds gone over with a flashlight. No hair, no droppings, not a " +
                "broken stem.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = FRESNO_NIGHTCRAWLER.id,
    solvedNote = "Two of them, all legs and glowing on camera. Fresno has had this footage since 2007.",
)

val SCREAMING_IN_THE_CANYON = CaseDefinition(
    id = "screaming-in-the-canyon",
    state = "CA",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Screaming in the Canyon",
        "Teens Say Something Beat on Their Car",
        "Dented Hood, No Explanation",
        "Parked Couple Flees Canyon Road",
        "What Was Standing in the Headlights?",
    ),
    teasers = listOf(
        "Two teenagers say something pounded on their car and screamed at them.",
        "A late drive up the canyon ended with a dented hood.",
        "It stood up in the headlights, they say, and it had horns.",
        "The scream sounded like a goat, only much bigger.",
        "Their parents want to know about the hood. So do we.",
    ),
    lead = "Two teenagers parked on the canyon road past the old dairy last night say something came " +
        "out of the dark, pounded on the hood of their car and screamed at them. It was standing up, " +
        "they say, and it had horns.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Cloven hoofprints in the dirt beside the car, but only two of them to a stride. Whatever " +
                "made them was walking upright.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "One of them was filming. Fists on the hood, then a long, bleating scream right outside " +
                "the window, like a goat but far too big.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a ranch gate caught a figure walking upright along the fence line, broad " +
                "through the shoulders.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Both teenagers drew it. The drawings agree on one thing: a pair of twisted horns curling " +
                "back from its head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse hairs caught in the dent in the hood, and more on the barbed wire across the " +
                "road.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = BILLIWHACK_MONSTER.id,
    solvedNote = "Hooves, horns and a goat's scream. Aliso Canyon has had this trouble since the 1940s.",
)

/*
 * Wyoming's field guide. Research notes and sources are in build/wyoming.md.
 *   - Footprints: shallow and far apart (Jackalope), deep (Bigfoot), none (Smetty, Wimpus)
 *   - Field recording: a voice singing back (Jackalope), a splash (Smetty), howls and knocks
 *     (Bigfoot), nothing (Wimpus)
 *   - Trail cam: low (Jackalope, Smetty), upright (Bigfoot), a shadow (Wimpus)
 *   - Witness sketch: horned (Jackalope, Wimpus), humps (Smetty), man-shaped (Bigfoot)
 *   - Sample: hair (Jackalope, Bigfoot), nothing (Smetty), slime (Wimpus)
 * Any two differ on at least three types, so any three clues settle a case. The closest pairs are
 * the Jackalope and Bigfoot (both leave hair) and the Jackalope and Wimpus (both horned), and each
 * still differs on four.
 */
private val JACKALOPE = Suspect(
    "jackalope",
    "Jackalope",
    "A jackrabbit with antlers, at home around Douglas since the Herrick brothers' taxidermy shop " +
        "in the 1930s. Said to be blinding fast and almost impossible to see unless it's standing " +
        "still. It bounds, so its prints are small and far apart, and it sheds soft fur. It mimics " +
        "voices, and sings back to cowboys around the fire at night.",
    traits(PRINTS_SHALLOW, CALL_VOICE, CAM_LOW, SKETCH_HORNED, SAMPLE_HAIR),
)

private val SMETTY = Suspect(
    "smetty",
    "Smetty",
    "The serpent of Lake De Smet, near Buffalo, reported since 1892. Thirty or forty feet long, as " +
        "thick as a tree trunk, with a bony ridge down its back like a horse's mane. It never comes " +
        "ashore and leaves nothing behind, but it rolls with a heavy splash that sends waves across " +
        "a windless lake. In 1938 a Sheridan man swore he caught it, and that it exploded.",
    traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_NOTHING),
)

private val WYOMING_BIGFOOT = Suspect(
    "bigfoot",
    "Bigfoot",
    "Reported from the Wind River Range to Park County. In 1972 a state biologist camped high in " +
        "the Wind Rivers spent two hours with something pressing down on his tent and pelting it " +
        "with pinecones. Upright, man-shaped and covered in dark hair. Deep, five-toed prints, " +
        "long calls and wood knocks.",
    traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
)

private val WIMPUS = Suspect(
    "wimpus",
    "Wimpus",
    "Pulled out of Lake De Smet by a carp fisherman in July 1915, and shown in a Sheridan saloon: " +
        "\"a bull head and three horns on each side\", as long as a short piece of rope and the color " +
        "of lead. Said to be deadly poisonous. It stays in the water, makes no sound, and leaves " +
        "slime on whatever it touches.",
    traits(PRINTS_NONE, CALL_NONE, CAM_SHADOW, SKETCH_HORNED, SAMPLE_SLIME),
)

private val WYOMING_SUSPECTS = fieldGuide("WY", listOf(JACKALOPE, SMETTY, WYOMING_BIGFOOT, WIMPUS), REGIONAL_CREATURES)

val SINGING_IN_THE_SAGEBRUSH = CaseDefinition(
    id = "singing-in-the-sagebrush",
    state = "WY",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Singing in the Sagebrush",
        "Ranch Hand Hears His Song Sung Back",
        "Somebody Out There Knows the Words",
        "Nobody for Miles, and Still a Voice",
        "Fence Rider Spooked at Sundown",
    ),
    teasers = listOf(
        "A ranch hand whistled a tune at dusk, and something sang it back.",
        "There's nobody for miles out there. Somebody was singing anyway.",
        "It got the tune right and most of the words.",
        "He says the voice came from down low in the sage.",
        "The horse heard it first, and wanted to go home.",
    ),
    lead = "A ranch hand riding fence at sundown says he was whistling an old tune when a voice in " +
        "the sagebrush sang it back to him. There's nobody living for miles. When he rode over, " +
        "something shot off through the brush, too fast to see.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Small, light prints in the dust between the sage, barely pressed in and much too far " +
                "apart for something that size. It wasn't walking.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His phone was recording in his shirt pocket. His whistling stops, and a thin, " +
                "reedy voice picks up the tune, word after word, then breaks off.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a stock tank caught something low to the ground, a streak across three " +
                "frames and then gone.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: something small and low, with horns on its head. He says he only saw " +
                "the horns clearly.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A tuft of soft fur snagged on the bottom strand of the barbed wire.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = JACKALOPE.id,
    solvedNote = "Horns, a hop and a fine singing voice. Douglas will want you to buy a tag, good on June 31.",
)

val NO_WIND_ON_THE_LAKE = CaseDefinition(
    id = "no-wind-on-the-lake",
    state = "WY",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "No Wind on the Lake",
        "Anglers Row for Shore",
        "Boat Lifted Under Them",
        "Waves on a Glassy Morning",
        "Something Rose Beside the Boat",
    ),
    teasers = listOf(
        "Two anglers felt the water lift their boat on a dead-calm morning.",
        "Something rolled past them, taller than the gunwale.",
        "They didn't wait to see it twice.",
        "Waves hit the shore with no wind and no other boats out.",
        "One of them has fished this lake fifty years and says he's never rowed so fast.",
    ),
    lead = "Two men out fishing at dawn say the lake was glass until the water rose under their " +
        "boat. A long back rolled past, taller than the gunwale, with a ridge along the top. Then " +
        "waves hit the shore on a morning with no wind at all.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The bank is soft mud for a hundred yards, and there's not a print in it that a " +
                "person or a deer didn't make.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A camp on the far shore was recording birdsong. Then one enormous slap on the water, " +
                "and waves breaking for a long time after.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the boat ramp caught a long, low back out on the water, darker than the " +
                "lake and longer than the dock.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn on the back of a bait receipt: a row of humps along the waterline, with a ridge " +
                "running down them like a horse's mane.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Nothing on the bank and nothing on the boat. The nets come back holding only carp.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = SMETTY.id,
    solvedNote = "A long back, a heavy splash and nothing left behind. Smetty hasn't exploded yet, " +
        "whatever they said in 1938.",
)

val PINECONES_ON_THE_TENT = CaseDefinition(
    id = "pinecones-on-the-tent",
    state = "WY",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Pinecones on the Tent",
        "Field Worker's Long Night",
        "Something Leaned on the Tent",
        "Two Hours Till Daylight",
        "Survey Camp Gets a Visitor",
    ),
    teasers = listOf(
        "Something pressed down on a field worker's tent, and stayed.",
        "Pinecones rained on her camp for most of two hours.",
        "She says it was bigger than the tent, and it wasn't a bear.",
        "A wildlife survey camp had company last night.",
        "She stayed in her sleeping bag until the sky went gray.",
    ),
    lead = "A wildlife survey worker camped alone up high says she woke to something pressing down " +
        "on the roof of her tent, as if a huge hand were leaning on it. For nearly two hours after, " +
        "pinecones kept landing all around the camp.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep prints in the needles beside the tent, five toes splayed, sunk in farther than " +
                "her boots go.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her recorder caught pinecones hitting the fly, then a long, rising call from upslope " +
                "and two knocks, wood on wood, from across the draw.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the game trail caught a figure standing upright between the pines, its " +
                "head level with the lowest branches.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn the next morning: man-shaped and enormous, its arms hanging down past its " +
                "knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse hairs stuck in the pitch on a lodgepole pine, higher than she can reach.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = WYOMING_BIGFOOT.id,
    solvedNote = "A hand on the tent and pinecones till dawn. The Wind Rivers have heard this one since 1972.",
)

val NOT_A_CARP = CaseDefinition(
    id = "not-a-carp",
    state = "WY",
    scene = Scene.LAKE_SHORE,
    place = "Fishing dock",
    headlines = listOf(
        "Not a Carp",
        "Boy Hooks Something With Horns",
        "Catch Gets Away, Story Doesn't",
        "What Came Up on the Line?",
        "Dock Fishing Turns Strange",
    ),
    teasers = listOf(
        "A boy fishing for carp hauled up something with horns.",
        "It was the color of lead, he says, and it slid off the hook.",
        "Nobody on the dock believed him until they saw the slime.",
        "It got back in the water before anyone took a picture.",
        "His grandfather says he's heard of one before, a long time ago.",
    ),
    lead = "A boy fishing off the dock for carp hauled up something he couldn't name: grey as lead, " +
        "about as long as his arm, with horns down the sides of its head. It slid off the hook and " +
        "back into the lake before anyone else saw it.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Wet boards and soft mud under the dock. Nothing climbed out. Whatever it was went " +
                "straight back into the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His uncle was filming. The reel clicking, the boy shouting, and from the thing on " +
                "the line, nothing at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the dock post caught only a shadow in the shallows, moving in under the " +
                "boards.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The boy's drawing: a blunt head with horns coming off it, and not much else he can " +
                "swear to.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The line and hook are coated in a thick, clear slime that won't wipe off.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = WIMPUS.id,
    solvedNote = "Horns, slime and not a sound. Sheridan put one on show in 1915. Some say it's only a salamander.",
)

/*
 * Texas's and Arizona's own creatures. The regional ones above join them: see build/southwest.md
 * for how the clues sort. Texas has its own entry for the winged monster, Big Bird, which wins over
 * the regional one there.
 */
private val LAKE_WORTH_MONSTER = Suspect(
    "lake-worth-monster",
    "Lake Worth Monster",
    "Seen around Lake Worth and Greer Island, on the edge of Fort Worth, in the summer of 1969: " +
        "upright and hairy, half man and half goat, with horns. It walked on hooves, screamed, and " +
        "once threw a tire at a crowd from a bluff. The Nature Center throws it a party every October.",
    traits(PRINTS_HOOVES, CALL_SHRILL, CAM_UPRIGHT, SKETCH_HORNED, SAMPLE_HAIR),
)

private val TEXAS_BIGFOOT = Suspect(
    "bigfoot",
    "Bigfoot",
    "Reported in the East Texas river bottoms since the \"Wild Woman of the Navidad\" of 1837, who took " +
        "food from settlers' cabins. Upright, man-shaped and covered in dark hair. Deep, five-toed prints, " +
        "long calls and wood knocks.",
    traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
)

private val BIG_BIRD = WINGED_MONSTER.suspect.copy(
    name = "Big Bird",
    entry = "The Rio Grande Valley's Big Bird, reported all over the Valley in January 1976, with rewards " +
        "offered for it: leathery wings like a bat's, a bald head, a strange, near-human face and red " +
        "eyes. It doesn't come down, so it leaves no tracks, only a shriek, a vast shadow and the odd " +
        "shed scale.",
)

private val RED_GHOST = Suspect(
    "red-ghost",
    "Red Ghost",
    "Eastern Arizona's terror of the 1880s: a huge red beast seen across the Salt River country, with " +
        "something strapped upright on its back like a rider long dead. It was a camel, one of the Army's, " +
        "turned loose after the Camel Corps. It left cloven tracks twice the size of a horse's and coarse " +
        "red hair on the brush, and never made a sound.",
    traits(PRINTS_HOOVES, CALL_NONE, CAM_UPRIGHT, SKETCH_HUMPS, SAMPLE_HAIR),
)

private val TEXAS_SUSPECTS = fieldGuide("TX", listOf(LAKE_WORTH_MONSTER, TEXAS_BIGFOOT, BIG_BIRD), REGIONAL_CREATURES)

private val ARIZONA_SUSPECTS = fieldGuide("AZ", listOf(RED_GHOST), REGIONAL_CREATURES)

val THE_TIRE_CAME_FLYING = CaseDefinition(
    id = "the-tire-came-flying",
    state = "TX",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "The Tire Came Flying",
        "Something Threw a Tire",
        "Parked by the Lake, Then Pelted",
        "Couples Flee the Lake Road",
        "What Was Up on the Bluff?",
    ),
    teasers = listOf(
        "Somebody, or something, threw a tire at a crowd from the bluff.",
        "Three couples parked by the lake left in a hurry.",
        "They say it stood on two legs and had horns.",
        "The tire landed in the road. Nobody saw who could have thrown it.",
        "The sheriff found the tire. He didn't find anything else.",
    ),
    lead = "Three couples parked down by the lake last night say something stood up on the bluff " +
        "above them, on two legs, with horns. Then it picked up an old tire and threw it at them. " +
        "Nobody waited around to see what it did next.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Cloven hoofprints in the dirt on top of the bluff, but only two to a stride. Whatever made " +
                "them was walking upright.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone was recording in one of the cars. Doors slamming, someone yelling to go, and from " +
                "up the bluff a high, bleating scream.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the park gate caught a figure walking upright along the fence, broad through the " +
                "shoulders.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Two of them drew it, separately. Both drawings have the same thing on its head: a pair of " +
                "horns.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse hairs caught in the tread of the tire it threw.",
        ),
    ),
    suspects = TEXAS_SUSPECTS,
    answerId = LAKE_WORTH_MONSTER.id,
    solvedNote = "Hooves, horns and a good throwing arm. Fort Worth has been telling this one since 1969.",
)

val DOWN_IN_THE_RIVER_BOTTOM = CaseDefinition(
    id = "down-in-the-river-bottom",
    state = "TX",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Down in the River Bottom",
        "Deer Camp Pantry Raided",
        "Hunters Say the Woods Answered Back",
        "Whoever Took the Food Walked Upright",
        "Something Big in the Bottoms",
    ),
    teasers = listOf(
        "A deer camp's food went missing, and the tracks were bare feet.",
        "Hunters say a long call came back from down the river.",
        "The cooler was carried off, not dragged.",
        "Something in the bottoms is bigger than any hog.",
        "The camp dogs wouldn't go past the edge of the clearing.",
    ),
    lead = "Hunters at a deer camp down in the river bottom came back to find the cooler gone and the " +
        "food shed open. Not dragged off like a hog would. Carried. That night, something called from " +
        "down the river, long and low.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, five-toed prints in the mud by the shed, sunk in much farther than any of the hunters' " +
                "boots.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A trail camera with a microphone caught it: a long, rising call from down the river, and two " +
                "knocks, wood on wood, in answer.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A figure standing upright beside the shed, as tall as its door, the cooler held against its " +
                "chest.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One hunter saw it at the tree line: man-shaped, but massive, its arms hanging down past its " +
                "knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs snagged on the shed's door latch, well above a man's head.",
        ),
    ),
    suspects = TEXAS_SUSPECTS,
    answerId = TEXAS_BIGFOOT.id,
    solvedNote = "Bare feet, a long call and a stolen cooler. The Navidad's Wild Woman was taking food in 1837.",
)

val SOMETHING_RODE_THE_CAMEL = CaseDefinition(
    id = "something-rode-the-camel",
    state = "AZ",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Something Rode the Camel",
        "Rancher Swears It Had a Rider",
        "Red Beast Crosses the Wash",
        "Too Big for a Horse",
        "Who Was Strapped On Its Back?",
    ),
    teasers = listOf(
        "A rancher saw something huge and red cross the wash, with a rider.",
        "The rider never moved, she says. Not once.",
        "Its tracks are twice the size of a horse's.",
        "It went past the stock tank at dusk and didn't stop.",
        "Old-timers say they've heard of this before. A long time before.",
    ),
    lead = "A rancher out at the stock tank at dusk says something huge and red came up the wash, taller " +
        "than any horse, with a figure strapped upright on its back. The figure never moved. The beast " +
        "never made a sound.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Cloven tracks in the sand of the wash, each one twice the size of a horse's, in a long, even " +
                "stride.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her phone was recording at the tank. Cattle lowing, then quiet, and nothing else at all as " +
                "it went by.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera at the tank caught something tall and upright against the sky, taller than the " +
                "windmill's ladder.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: a long neck and a hump on its back, and on the hump, something sitting up.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, reddish hair caught on the mesquite along the wash, higher than a horse's back.",
        ),
    ),
    suspects = ARIZONA_SUSPECTS,
    answerId = RED_GHOST.id,
    solvedNote = "A red camel with a rider who never moves. " +
        "The Army let its camels go in the 1860s, and one of them didn't go quietly.",
)

/*
 * West Virginia's field guide.
 *   - Footprints: none (Mothman, Grafton Monster), shallow (Flatwoods Monster), clawed (Sheepsquatch, Ogua)
 *   - Field recording: a screech (Mothman, Sheepsquatch), a splash (Ogua), no voice (Flatwoods, Grafton)
 *   - Trail cam: a glow (Mothman, Flatwoods), upright (Grafton, Sheepsquatch), low (Ogua)
 *   - Witness sketch: winged, man-shaped, a smudge, horned, a lizard: one each
 *   - Sample: nothing (Mothman, Grafton), slime (Flatwoods), hair (Sheepsquatch), a scale (Ogua)
 * Any two differ on at least three types, so any three clues settle a case. The Grafton Monster
 * shares two traits with Mothman, so two clues can leave both standing.
 */
private val MOTHMAN = Suspect(
    "mothman",
    "Mothman",
    "Seen around Point Pleasant for thirteen months in 1966 and 1967, most often near the old TNT " +
        "plant and the Silver Bridge. Man-shaped, taller than a man, with great wings folded at its " +
        "back and no neck to speak of. Its red eyes glow in headlights. Leaves no tracks and nothing " +
        "behind, and its cry is a high squeal, like a fan belt slipping.",
    traits(PRINTS_NONE, CALL_SHRILL, CAM_GLOW, SKETCH_WINGED, SAMPLE_NOTHING),
)

private val FLATWOODS_MONSTER = Suspect(
    "flatwoods-monster",
    "Flatwoods Monster",
    "Met by Kathleen May, her sons and a few neighbor boys on a Braxton County hilltop in 1952, after a " +
        "light fell from the sky. Ten feet tall and man-shaped, under a pointed hood like the ace of " +
        "spades, its face glowing. It glided more than walked, leaving only shallow skid marks, and " +
        "made no call beyond a hiss. An oily residue was found on the grass afterward.",
    traits(PRINTS_SHALLOW, CALL_NONE, CAM_GLOW, SKETCH_MAN, SAMPLE_SLIME),
)

private val GRAFTON_MONSTER = Suspect(
    "grafton-monster",
    "Grafton Monster",
    "First seen by a Grafton Sentinel reporter driving River Road in 1964: seven to nine feet tall, " +
        "smooth and white like a seal's hide, and with no head at all. It stood upright at the " +
        "roadside, then was gone. Nobody found a track or a trace, and it never made a sound.",
    traits(PRINTS_NONE, CALL_NONE, CAM_UPRIGHT, SKETCH_SMUDGE, SAMPLE_NOTHING),
)

private val SHEEPSQUATCH = Suspect(
    "sheepsquatch",
    "Sheepsquatch",
    "Boone County's woolly beast, reported up the hollows since the 1990s. As big as a bear, covered " +
        "in dirty white wool, with a ram's curling horns. It stands upright to look at you and goes " +
        "on all fours to run, leaving clawed prints. It screams, and it leaves wool on the fences.",
    traits(PRINTS_CLAWED, CALL_SHRILL, CAM_UPRIGHT, SKETCH_HORNED, SAMPLE_HAIR),
)

private val OGUA = Suspect(
    "ogua",
    "Ogua",
    "A giant turtle said to live in the Potomac and drag deer under, with a tail longer than its " +
        "body and, some say, two heads. It hauls out on the banks, low to the ground, leaving clawed " +
        "prints and shed scales. Never heard to call, only the heavy splash as it goes back in.",
    traits(PRINTS_CLAWED, CALL_SPLASH, CAM_LOW, SKETCH_LIZARD, SAMPLE_SCALE),
)

private val WEST_VIRGINIA_SUSPECTS =
    fieldGuide("WV", listOf(MOTHMAN, FLATWOODS_MONSTER, GRAFTON_MONSTER, SHEEPSQUATCH, OGUA), REGIONAL_CREATURES)

val ON_THE_BRIDGE = CaseDefinition(
    id = "on-the-bridge",
    state = "WV",
    scene = Scene.RIVER_TOWN,
    headlines = listOf(
        "Something on the Bridge",
        "Late-Night Drivers Report Figure Over River",
        "Night Shift Won't Take the Bridge Road",
        "Couple Turns Car Around Mid-Span",
        "Who's Up on the Girders?",
    ),
    teasers = listOf(
        "Two drivers saw the same thing on the same bridge, an hour apart.",
        "A couple on their way home say something stood up in their headlights.",
        "It was taller than a man, they say, and it didn't move like one.",
        "A nurse coming off the night shift now drives the long way around.",
        "Nobody on the bridge, then somebody, then nobody again.",
    ),
    lead = "Two drivers called it in last night, an hour apart. Both say something was standing on " +
        "the old bridge over the river, taller than a man, and that when their headlights hit it, " +
        "it rose straight up into the dark.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Grit and dust along the walkway where it stood, and not one print in it. Whatever was " +
                "here didn't walk away.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Audio from the second car's dashcam. Tires on the bridge deck, then a high, squealing " +
                "screech from somewhere overhead, like a fan belt slipping.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the riverbank caught two red points of light high on the bridge, a hand's " +
                "width apart, shining back at the infrared flash.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The first driver's drawing: man-shaped, with no neck, its head sunk between its " +
                "shoulders, and great wings folded behind it.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The railings swabbed, the walkway swept. No hair, no feathers, no blood. Nothing that " +
                "came from an animal at all.",
        ),
    ),
    suspects = WEST_VIRGINIA_SUSPECTS,
    answerId = MOTHMAN.id,
    solvedNote = "Red eyes, great wings and a bridge over the river. Point Pleasant knows this story.",
)

/**
 * Each state's field guide, by its two-letter code: the states with their own creatures, and any
 * that only regional creatures reach.
 */
val FIELD_GUIDES: Map<String, List<Suspect>> = allFieldGuides(
    mapOf(
        "MT" to MONTANA_SUSPECTS,
        "WA" to WASHINGTON_SUSPECTS,
        "CA" to CALIFORNIA_SUSPECTS,
        "WY" to WYOMING_SUSPECTS,
        "TX" to TEXAS_SUSPECTS,
        "AZ" to ARIZONA_SUSPECTS,
        "WV" to WEST_VIRGINIA_SUSPECTS,
        "NJ" to NEW_JERSEY_SUSPECTS,
        "AK" to ALASKA_SUSPECTS,
        "HI" to HAWAII_SUSPECTS,
    ),
    REGIONAL_CREATURES,
)

/** Stories that run in every state their creature ranges over. Their words name no place. */
private val BLED_DRY = RegionalStory(
    id = "bled-dry",
    creature = CHUPACABRA,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Bled Dry",
        "Goats Found Dead in the Pen",
        "Two Small Wounds, No Blood",
        "Rancher Loses Half Her Herd Overnight",
        "What Got Into the Goat Pen?",
    ),
    teasers = listOf(
        "Six goats dead overnight, each with two small wounds in the neck.",
        "The dogs barked once and then hid under the porch.",
        "No tracks to the fence, and hardly a drop of blood.",
        "Her neighbor says the same thing happened to his chickens.",
        "Something came in over the fence and left the same way.",
    ),
    lead = "A rancher went out to the goat pen this morning and found six of her goats dead, each with " +
        "two small, neat wounds in the neck and hardly any blood. The dogs barked once in the night, she " +
        "says, then went quiet and hid under the porch.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Prints in the dust around the pen like a dog's, but with long claws dug in deep at the front " +
                "of every toe.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A baby monitor in the barn caught the goats crying out, then a high, rasping screech, and then " +
                "nothing.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the pen caught something low to the ground going under the bottom rail, grey and " +
                "bare.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her neighbor saw it in his headlights and drew it: hunched, high at the shoulders, its head " +
                "held low.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The pen swept and the fence swabbed. No hair, no blood trail, nothing. Even the goats have " +
                "hardly bled.",
        ),
    ),
    solvedNote = "Two neat punctures and an empty pen. " +
        "The goat-sucker came up from Puerto Rico in 1995 and never went home.",
)

private val WINGS_OVER_THE_WASH = RegionalStory(
    id = "wings-over-the-wash",
    creature = WINGED_MONSTER,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Wings Over the Wash",
        "Hiker Ducks as Shadow Passes",
        "Too Big for a Buzzard",
        "Something Glided Off the Cliffs",
        "No Feathers, Says Witness",
    ),
    teasers = listOf(
        "A hiker swears something the size of a small plane glided off the cliffs.",
        "It had no feathers, he says. The wings were skin.",
        "Its shadow went right over him, and he hit the dirt.",
        "It shrieked once and was gone over the ridge.",
        "He's seen condors. This wasn't a condor.",
    ),
    lead = "A hiker up on the ridge this morning says two things glided off the cliffs above the wash, " +
        "wider than a small plane, and one of them passed right over him. No feathers, he says. The wings " +
        "were skin, like a bat's.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The ledge it took off from is soft with sand and droppings, and there isn't a single track. " +
                "Nothing walked here.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His video has sound: wind, his own breathing, then a long, piercing shriek from overhead.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera by a stock tank caught nothing solid, only a huge shadow sweeping across the ground.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: great wings stretched out wide, and a long narrow head out in front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Not a feather. A single hard, grey scale on the ledge where they took off, the size of a " +
                "thumbnail.",
        ),
    ),
    solvedNote = "Leathery wings, a shriek and a shed scale. " +
        "Tombstone wrote one up in 1890, and the reports never stopped.",
)

private val UP_ON_THE_RIM = RegionalStory(
    id = "up-on-the-rim",
    creature = MOGOLLON_MONSTER,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Up on the Rim",
        "Scout Troop Cuts Campout Short",
        "Rocks Came Out of the Dark",
        "Leaders Smell Something Awful",
        "Whistles in the Pines",
    ),
    teasers = listOf(
        "A scout troop packed up at midnight after rocks came out of the trees.",
        "Something whistled back at the scouts. Then it screamed.",
        "The leaders say the smell came first.",
        "Thirty kids, four adults, and nobody slept.",
        "The rocks were thrown, not rolled, says one leader.",
    ),
    lead = "A scout troop camped up on the rim packed up at midnight. First came a smell like something " +
        "dead, the leaders say, then whistles from the pines, then rocks thrown into camp from the dark. " +
        "Then a scream.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, five-toed prints at the edge of the camp, far bigger than any adult's boot, and pressed " +
                "in hard.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A scout's phone caught the whistles, then a long, rising howl from up the slope that the " +
                "whole troop went silent for.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the trail caught a figure standing upright between the pines, a head taller than " +
                "the lowest branches.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A leader drew it the next morning: man-shaped and enormous, with long arms and no neck to " +
                "speak of.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark, coarse hairs on the bark of a ponderosa, higher than anyone in the troop can reach.",
        ),
    ),
    solvedNote = "Whistles, a smell and rocks out of the dark. " +
        "Every scout who's camped on the Rim has heard about this one.",
)

private val THE_RABBIT_HUTCHES = RegionalStory(
    id = "the-rabbit-hutches",
    creature = CHUPACABRA,
    scene = Scene.PASTURE,
    headlines = listOf(
        "4-H Rabbits Found Drained in Their Hutches",
        "Girl's Show Rabbits Killed Days Before the Fair",
        "Hutch Latch Untouched, Rabbits Dead",
        "Neighbor's Headlights Catch Something Bare and Grey",
        "\"It Went Over the Fence Like a Dog\"",
    ),
    teasers = listOf(
        "A girl's show rabbits were found dead, each with two neat holes in the neck.",
        "The fair is on Saturday. She has nothing left to show.",
        "The hutches were still latched. Whatever did it reached in through the wire.",
        "A neighbor's headlights caught something grey and hairless going over the fence.",
        "There's hardly a drop of blood in the hutches.",
    ),
    lead = "A girl raising rabbits for the county fair went out to feed them this morning and found eight of " +
        "them dead in their hutches, each with two neat punctures in the neck and almost no blood. The hutches " +
        "were still latched. A neighbor driving home late saw something grey and hairless go over the fence.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the dirt below the hutches: prints like a dog's, with long, sharp claws dug in at every toe.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The family's barn camera has sound: rabbits thumping, then a high, rasping screech, then quiet.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the hutches caught something low and grey, bare of hair, pressed against the wire.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The neighbor's drawing: hunched on four legs, high at the shoulders, with spines down its back.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The wire and the ground swept and swabbed. No hair, no blood trail. Nothing left behind at all.",
        ),
    ),
    solvedNote = "Clawed prints, a screech and two neat holes. " +
        "The goat-sucker isn't fussy about what it drains.",
)

private val THE_TOWER_CLIMBER = RegionalStory(
    id = "the-tower-climber",
    creature = WINGED_MONSTER,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Tower Climber Says Something Circled Him",
        "Five Hundred Feet Up, and Not Alone",
        "\"It Had Wings Like a Bat's, Only Bigger Than a Plane's\"",
        "Radio Tower Repair Postponed",
        "Shriek Heard Over the Mesa",
    ),
    teasers = listOf(
        "A radio tower climber says something with leathery wings circled him three times.",
        "No feathers, he says. The wings were skin stretched over long bones.",
        "Its shadow went over his crew on the ground like a cloud.",
        "He clipped in and stayed put until it was gone.",
        "It shrieked once, and the ground crew heard it half a mile off.",
    ),
    lead = "A climber repairing a radio tower on the mesa says something circled him three times at five " +
        "hundred feet: wider than a small plane, with no feathers, its wings smooth skin like a bat's. He " +
        "clipped himself to the tower and waited. His ground crew saw its shadow cross the whole site.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The ground under the tower and the ledge it seemed to come from, searched. Not a single track.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His helmet radio: wind, the climber breathing hard, then a long, piercing shriek close by.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A security camera at the tower gate caught nothing solid, only a vast shadow crossing the yard.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: huge wings spread wide, and a long, narrow head stretched out in front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A hard grey scale wedged in a tower bolt at the height where it passed closest.",
        ),
    ),
    solvedNote = "Bat's wings, a shriek and a scale in the bolt. " +
        "The winged monster has been circling the desert since 1890.",
)

private val ELK_CAMP = RegionalStory(
    id = "elk-camp",
    creature = MOGOLLON_MONSTER,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Elk Hunters Pelted With Rocks After Dark",
        "Whistling Circled the Elk Camp All Night",
        "Hunters Break Camp a Day Early",
        "\"The Smell Hit First, Then the Rocks\"",
        "Elk Tag Unfilled, and Nobody Cares",
    ),
    teasers = listOf(
        "Elk hunters say rocks landed in camp from the dark, one after another.",
        "Something whistled from the pines, and moved round them as it did.",
        "A stink like something dead came through the camp before the rocks did.",
        "They left a day early with their tags unfilled.",
        "One hunter put his spotlight on it. He wishes he hadn't.",
    ),
    lead = "Elk hunters camped in the pines say a smell like something dead rolled through their camp after " +
        "dark, then whistles started up in the trees, circling them. Then rocks came flying into camp. One of " +
        "them put a spotlight on the tree line and saw something standing upright, far taller than a man.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, five-toed prints along the tree line, much longer than any of their boots, pressed in hard.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A hunter's phone: whistles from three sides, then a long, rising howl that silenced the camp.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "Their camera on the game trail caught a figure walking upright, head above the lowest branches.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The hunter with the spotlight drew it: man-shaped and huge, long arms, no neck to speak of.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark, coarse hairs caught on the bark of a pine, higher than any of them could reach.",
        ),
    ),
    solvedNote = "Whistles, a stink and rocks out of the trees. " +
        "The Mogollon Monster doesn't share its elk.",
)

// ---- Second stories for the single-state cases ------------------------------------------------------
// Each state's paper rotates between these and the first ones: see runToday.

private val CHERRY_PICKERS = CaseDefinition(
    id = "cherry-pickers",
    state = "MT",
    scene = Scene.LAKE_SHORE,
    place = "The east shore orchards",
    headlines = listOf(
        "Cherry Pickers Stop Work to Watch the Lake",
        "Orchard Crew Counts Seven Humps",
        "Ladders Empty Along the East Shore",
        "\"Longer Than the Whole Row of Trees\"",
        "Harvest Pauses for Something in the Water",
    ),
    teasers = listOf(
        "A whole crew of cherry pickers came down their ladders to watch the lake.",
        "Seven humps, says the foreman, and he counted twice.",
        "It made no sound at all, they say. It just breathed.",
        "The orchard runs right to the water. So, it seems, does the monster.",
        "The cherries can wait, says the grower. This can't.",
    ),
    lead = "A crew picking cherries in an orchard above the east shore of Flathead Lake came down off their " +
        "ladders yesterday afternoon to watch something in the water below: a long, dark back in humps, longer " +
        "than a row of trees, with a neck raised at one end. It sank without a splash.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The orchard's beach and the rocks either side, walked end to end. Pickers' boot prints, deer, and " +
                "nothing that came out of the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A picker's video from the ladder: wind in the trees, someone counting humps out loud, and from the " +
                "water a long, slow breath. No call, no cry, not even a splash.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The orchard's frost camera on the shore caught a long, dark back riding low in the water, no higher " +
                "than the dock.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The foreman drew it on a picking ticket: seven humps along the waterline and a long neck at the " +
                "front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A smear of clear, fishy slime on the rocks below the orchard, where something heavy rubbed along the " +
                "shore.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = FLATHEAD_MONSTER.id,
    solvedNote = "A long low back, no voice and slime on the rocks. The Flathead monster has been watching the " +
        "cherry harvest since the steamboat days.",
)

private val LAUGHING_IN_THE_CALVING_BARN = CaseDefinition(
    id = "laughing-in-the-calving-barn",
    state = "MT",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Laughter Outside the Calving Barn",
        "Rancher Loses a Newborn Calf",
        "\"It Sounded Like Somebody Laughing at Me\"",
        "Something Got Under the Calving Pen",
        "Madison Valley Rancher Sits Up With a Rifle",
    ),
    teasers = listOf(
        "Calving season, three in the morning, and something outside was laughing.",
        "A newborn calf was dragged under the pen rails and gone by dawn.",
        "It ran low and fast, she says, its back sloping like a hyena's.",
        "The old mount in the museum has the same look, says her neighbor.",
        "She's sitting up in the barn every night now, rifle across her knees.",
    ),
    lead = "A rancher checking on her heifers in the calving barn at three in the morning heard yelping outside " +
        "that sounded like a man laughing. By the time she got the light on, a newborn calf had been dragged under " +
        "the pen rails. She saw something low and hunch-shouldered loping off into the dark with it.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, broad prints in the straw and mud under the pen rails, pressed in hard where it pulled the calf " +
                "through.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The barn's calving camera has sound: heifers bawling, then a high, broken yelping, rising and falling " +
                "like laughter.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The yard camera caught something long and low crossing under the yard light, its back sloping down " +
                "from its shoulders.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: hunched, high at the shoulders, its head held low and pushed out in front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, bristly hairs on the bottom pen rail, too stiff and dark for a coyote's.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = RINGDOCUS.id,
    solvedNote = "Deep prints, a laughing yelp and bristly hair on the rail. The Ringdocus was shot in 1886, but " +
        "something like it is still around.",
)

private val LOOKOUT_TOWER = CaseDefinition(
    id = "lookout-tower",
    state = "MT",
    scene = Scene.RIDGE,
    headlines = listOf(
        "Fire Lookout Watches Figure Cross the Burn",
        "\"It Walked Like a Man, But Bigger\"",
        "Lookout Logs a Sighting Instead of a Smoke",
        "Bitterroot Lookout Hears Howls All Night",
        "Something Walked the Ridge Below the Tower",
    ),
    teasers = listOf(
        "A fire lookout spotted something walking the burn below her tower. It wasn't a smoke.",
        "Through the binoculars, she says, it was upright, dark, and far too big.",
        "She logged it in the same book she logs the lightning strikes.",
        "Howls came up the ridge after dark, and knocks answered them.",
        "She's had the tower for six summers. She's never closed the shutters before.",
    ),
    lead = "A fire lookout in a tower above the Bitterroot was scanning the old burn below her at dusk when " +
        "something walked out of the snags: upright, dark all over, and taller than the scorched stumps it passed. " +
        "It crossed the burn without hurrying. After dark, she says, the howling started.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the burn below the tower: deep, wide, five-toed prints in the ash, nearly twice the length of her " +
                "boot.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The lookout's radio was open to dispatch all night. Between calls, a long howl rolling up the ridge, " +
                "and two knocks of wood on wood.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A Forest Service camera on the access trail caught a figure walking upright between the snags, head " +
                "above the stumps.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her sketch in the logbook margin: man-shaped and huge, broad shoulders, arms hanging long.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark, coarse hairs on a charred snag, higher than she could reach.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = MONTANA_BIGFOOT.id,
    solvedNote = "Deep prints, a howl and hair on a snag. The Bitterroot's big man is still walking the burns.",
)

private val THE_HUM_ON_THE_SECTION_ROAD = CaseDefinition(
    id = "the-hum-on-the-section-road",
    state = "MT",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Combine Driver Sees Giant at the Field Edge",
        "The Hum on the Section Road",
        "Radio Went to Static, Then It Was There",
        "\"Taller Than the Grain Truck\"",
        "Wheat Harvest Crew Won't Work Nights",
    ),
    teasers = listOf(
        "A combine driver working nights saw a figure taller than the grain truck at the edge of the field.",
        "His radio filled with static, and then he heard the hum.",
        "It stood still, arms at its sides, and then it simply wasn't there.",
        "The crew is cutting in daylight only, for now.",
        "It's the same thing the ranchers saw in '75, his father says.",
    ),
    lead = "A combine driver cutting wheat at night outside Great Falls says his radio dissolved into static and a " +
        "low hum came through the cab. In the headlights at the end of the row stood a figure shaped like a man, " +
        "taller than the grain truck, with its arms at its sides. He looked away to shut off the header, and it " +
        "was gone.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "At the field edge where it stood: a few shallow marks in the stubble, barely pressed in, impossibly " +
                "far apart.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The combine's dashcam audio: the engine, the header, a steady low hum, and no other sound. Nothing " +
                "with a voice.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The grain truck's backup camera caught an upright shape at the edge of the lights, arms at its sides, " +
                "very tall.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing on the yield sheet: shaped like a man, standing straight, its head far above the wheat.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The stubble and the fence line gone over with a flashlight at dawn. No hair, no scat, nothing from an " +
                "animal.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = PRAIRIE_GIANT.id,
    solvedNote = "Shallow prints, a hum and nothing left behind. The Prairie Giant is back out in the wheat.",
)

private val KIDS_FISHING_DERBY = CaseDefinition(
    id = "kids-fishing-derby",
    state = "MT",
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Kids' Fishing Derby Ends Early",
        "Something Took the Bobber Against the Current",
        "Derby Winner Won't Say What She Saw",
        "Shadow Under the Pond, Longer Than the Dock",
        "Every Line in the Pond Went Slack",
    ),
    teasers = listOf(
        "A ten-year-old's bobber went under and moved upstream, fast.",
        "Every line at the derby went slack at once.",
        "A shadow longer than the dock slid under the kids' feet.",
        "The organizers called it early. Nobody objected.",
        "No fish, no sound, and no sign of what took the line.",
    ),
    lead = "The kids' fishing derby at Blacktail Pond was called early on Saturday after a girl's bobber went " +
        "under and moved upstream against the current, faster than any trout swims. Parents on the dock say a " +
        "long, inky shadow slid under the boards, and every line in the pond went slack at once.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The muddy bank where the kids stood, and the reeds either side. Sneakers and dogs, and nothing from " +
                "the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A parent's video: kids shouting, a reel screaming, then silence. No splash, no call, only ripples " +
                "lapping the bank.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The derby's camera on the dock caught a long shadow under the water, blurred, with nothing you could " +
                "call a shape.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The girl tried to draw it and ended up with a dark smudge under the water.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The girl's line came back with the hook gone, and nothing on it at all. The nets came up clean.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = BLACKTAIL_MONSTER.id,
    solvedNote = "A shadow, no sound and nothing on the line. Blacktail's monster doesn't like to be caught.",
)

private val THE_PARAGLIDER = CaseDefinition(
    id = "the-paraglider",
    state = "MT",
    scene = Scene.RIDGE,
    headlines = listOf(
        "Paraglider Shares a Thermal With Something Bigger",
        "\"It Was Wider Than My Wing\"",
        "Pilot Lands Early Above the Valley",
        "Shadow Crossed the Launch Site",
        "Bird Watched Him From the Ridge",
    ),
    teasers = listOf(
        "A paraglider says a bird wider than his wing rode the same thermal beside him.",
        "It screeched once, and he decided to land.",
        "Friends at the launch saw its shadow go over before they saw the bird.",
        "It sat on the ridge afterwards, hunched, watching him pack up.",
        "He's flown with eagles before. This was not an eagle.",
    ),
    lead = "A paraglider circling in a thermal over the ridge says a bird came up beside him in the same column of " +
        "air, its wings wider than his own canopy. It screeched, and he spiraled down. From the landing field he " +
        "watched it perch on the rocks above, hunched, its head low between its shoulders.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the rocks where it perched: talon marks scratched lightly into the lichen, far apart, as if it " +
                "landed in hops.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His flight camera has sound: wind roaring, his variometer beeping, then a piercing, cackling screech " +
                "right beside him.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera at the launch caught nothing solid, only a huge shadow sweeping over the grass and the " +
                "parked cars.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch from the landing field: hunched on the rock, shoulders high, head sunk low.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A dark feather in the launch-site grass, longer than his forearm, stiff as a ruler.",
        ),
    ),
    suspects = MONTANA_SUSPECTS,
    answerId = THUNDERBIRD.id,
    solvedNote = "Shallow hops, a screech and a feather at the launch. Montana's Thunderbird shares the sky when " +
        "it feels like it.",
)

private val HOOFPRINTS_ON_THE_ROOFS = CaseDefinition(
    id = "hoofprints-on-the-roofs",
    state = "NJ",
    scene = Scene.PINE_BARRENS,
    headlines = listOf(
        "Hoofprints on the Rooftops",
        "Snow Shows Tracks That Go Up the Wall",
        "Pines Town Wakes to a Trail Across the Roofs",
        "\"Just Like 1909,\" Says Historian",
        "Something Walked the Rooftops in the Snow",
    ),
    teasers = listOf(
        "Fresh snow, and a line of hoofprints going right across the rooftops.",
        "The tracks start in a yard, go up a wall, and stop on a chimney.",
        "Two to a stride, cloven, and too far apart for any goat.",
        "A local historian says the same thing happened in January 1909.",
        "A scream woke the street at four in the morning.",
    ),
    lead = "A town at the edge of the Pines woke after a night of snow to find a line of cloven hoofprints across " +
        "three rooftops: up from a back yard, over the shingles, and stopping dead at a chimney. At four in the " +
        "morning, the whole street had been woken by a scream.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the snow on the roofs: small, split hoofprints, two to a stride, far too long a stride for " +
                "anything on two legs.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A doorbell camera across the street: a scream that sets every dog barking, then slow, heavy wingbeats " +
                "overhead.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A neighbor's camera caught a figure standing upright on the roof ridge, with something like wings " +
                "half open.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A boy who looked out drew it: upright, a long horse's head, and great leathery wings behind.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, dark hairs caught in the chimney flashing where the tracks stop.",
        ),
    ),
    suspects = NEW_JERSEY_SUSPECTS,
    answerId = JERSEY_DEVIL.id,
    solvedNote = "Hoofprints on the roofs, wings and a scream. Mother Leeds's thirteenth is still walking the " +
        "Pines in winter.",
)

private val CHANTERELLE_PICKERS = CaseDefinition(
    id = "chanterelle-pickers",
    state = "WA",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Mushroom Pickers Driven Out of Their Patch",
        "Something Big Was Already Foraging",
        "Whoops in the Fog, Then Rocks",
        "\"It Stood Up Out of the Salal\"",
        "Pickers Abandon a Full Day's Harvest",
    ),
    teasers = listOf(
        "Two chanterelle pickers found their secret patch was somebody else's.",
        "It stood up out of the salal, they say, taller than either of them.",
        "A whoop in the fog, then rocks landing round them.",
        "They left two full baskets behind and didn't go back.",
        "The Cascades have always had a foraging season. Now they know who else forages.",
    ),
    lead = "Two mushroom pickers working a chanterelle patch in the Cascade foothills say something stood up out " +
        "of the salal thirty yards off: upright, dark, and far taller than either of them. It whooped. Then rocks " +
        "began landing among the trees around them, and they left their baskets where they were.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the moss of the patch: deep, wide, five-toed prints, toes splayed, sunk in far deeper than their " +
                "boots.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "One picker's phone: rain, a long, rising whoop from up the slope, another answering, and knocks of " +
                "wood on wood.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A hunter's camera on the logging spur caught a figure walking upright through the fog, head above the " +
                "vine maple.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: man-shaped but massive, no neck, its arms hanging past its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs on a cedar's bark beside the patch, well above head height.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = WASHINGTON_BIGFOOT.id,
    solvedNote = "Deep prints, whoops and thrown rocks. The Cascades' big forager doesn't like sharing its patch.",
)

private val ORCA_TOUR = CaseDefinition(
    id = "orca-tour",
    state = "WA",
    scene = Scene.LAKE_SHORE,
    place = "The San Juans",
    headlines = listOf(
        "Orca Tour Sees Something That Isn't an Orca",
        "Kayakers Hear a Blow Behind Them",
        "Horse-Headed Thing Rises Off the Islands",
        "Guide: \"I Know Every Pod. That Wasn't One\"",
        "Humps in the Channel at Slack Tide",
    ),
    teasers = listOf(
        "A kayak tour out looking for orcas heard a blow behind them. It wasn't an orca.",
        "A head like a horse's rose up on a long neck and looked at the group.",
        "The guide knows every whale in the pod by name. She has no name for this.",
        "Three humps in a row, then a breath, then gone.",
        "Caddy, the old-timers call it. The kayakers call it a reason to paddle back.",
    ),
    lead = "A guided kayak tour looking for orcas among the San Juan Islands heard a heavy breath blown out behind " +
        "them. When they turned, a long neck with a head like a horse's was standing out of the water, with three " +
        "dark humps behind it. It looked at them, breathed again, and sank.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach where the tour pulled out, and the coves either side. Kayak hulls and paddlers, and no " +
                "tracks from the sea.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The guide's helmet camera: paddles dipping, then a long breath blown out at the surface, close. No " +
                "voice at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the tour company's dock caught a long, low back between the swells, darker than the " +
                "water.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The guide's sketch: humps in a row and a long neck with a horse's head at the front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Clear, fishy slime on the bow of the rearmost kayak, where it surfaced closest.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = CADBOROSAURUS.id,
    solvedNote = "Humps, a horse's head and a breath at the surface. Caddy has been surprising boaters in these " +
        "waters since the 1890s.",
)

private val NIGHT_GROOMER = CaseDefinition(
    id = "night-groomer",
    state = "WA",
    scene = Scene.TIMBER,
    place = "The ski area",
    headlines = listOf(
        "Night Groomer Sees Something on the Chairlift",
        "It Dropped Off the Lift Tower and Flew",
        "\"It Had Fur, and Wings Like a Bat's\"",
        "Ski Patrol Finds Tracks That Start From Nowhere",
        "Grooming Crew Works in Pairs Now",
    ),
    teasers = listOf(
        "A snowcat driver says something was sitting on top of a lift tower at two in the morning.",
        "It dropped off, spread wings wider than the cat, and went up into the dark.",
        "Bluish fur, he says, and arms as well as wings.",
        "Ski patrol found two deep prints in the snow, and no tracks leading to them.",
        "A screech over the mountain, and then nothing.",
    ),
    lead = "A snowcat driver grooming runs at a ski area under Mount Rainier says his lights picked out something " +
        "sitting on top of a lift tower: ape-like, furred, as big as a man. It dropped off, spread great leathery " +
        "wings and went straight up past his lights. Ski patrol found where it had landed the night before.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the corduroy under the tower: two deep prints sunk into the packed snow, with no tracks in or out.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The snowcat's radio was keyed. The engine, his shouting, then a high, ragged screech from overhead.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The lift's camera caught no animal, only a huge shadow sweeping over the snow under the lights.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch: man-shaped and heavy, with arms, and a pair of bat's wings behind.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, bluish hairs caught in the lift tower's ladder rungs, high above the snow.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = BATSQUATCH.id,
    solvedNote = "Deep prints, a screech and wings with arms. Batsquatch has been coming down off Rainier since " +
        "1994.",
)

private val ORCHARD_SPRINKLERS = CaseDefinition(
    id = "orchard-sprinklers",
    state = "WA",
    scene = Scene.LAKE_SHORE,
    place = "Lakeside orchard",
    headlines = listOf(
        "Something Hauled Out Into the Apple Orchard",
        "Irrigation Lines Torn Up by the Lake",
        "Orchardist Finds a Scale the Size of His Palm",
        "Drag Trail Runs From the Water to the Trees",
        "\"Built Like a Gator,\" Says Night Watchman",
    ),
    teasers = listOf(
        "Something came up out of Lake Chelan and into the apple trees.",
        "Sixty feet of irrigation line ripped up and dragged to the water.",
        "The night watchman saw it: built like an alligator, with a snake's head.",
        "A scale the size of his palm, pale as bone, in the grass.",
        "It never made a sound, he says, even when the sprinklers hit it.",
    ),
    lead = "An orchardist on the shore of Lake Chelan found a wide drag trail this morning running from the water " +
        "up between his apple trees and back, with sixty feet of irrigation line torn up along it. His night " +
        "watchman saw it in the sprinklers: long and low, built like an alligator, with a long, scaly tail.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Along both sides of the drag trail: clawed prints splayed out sideways, like a lizard's, dug deep " +
                "into the wet ground.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The pump-house camera has sound: sprinklers ticking, something heavy dragging, pipe snapping, and no " +
                "voice at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The orchard's frost camera caught a long, low shape along the bottom of the frame, no higher than the " +
                "drip lines.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The watchman's drawing: alligator-built on short legs, with a narrow snake's head and a long tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A hard, ridged scale as big as his palm in the grass, pale as bone.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = LAKE_CHELAN_MONSTER.id,
    solvedNote = "Clawed prints, a dragged pipe and a scale. Lake Chelan's dragon has been coming ashore since " +
        "1892.",
)

private val THE_WOODSHED = CaseDefinition(
    id = "the-woodshed",
    state = "WA",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Woodshed Flattened, Firewood Untouched",
        "Homesteader's Buckshot Didn't Slow It",
        "\"It Smelled Like the Woodstove\"",
        "Something Black and Bare Pushed Down the Shed",
        "Old Logging Story Turns Up in a Back Yard",
    ),
    teasers = listOf(
        "A woodshed was pushed flat overnight. The firewood is still stacked.",
        "He fired buckshot from the porch. It didn't even look round.",
        "Bigger than a bear, black and almost hairless, he says.",
        "The whole yard smelled of smoke. His stove was out.",
        "His grandfather logged these hills and told him about this. He never believed it.",
    ),
    lead = "A man living at the end of a logging road in the Cascades woke to a crash and saw something black and " +
        "nearly hairless, bigger than a bear, leaning its weight on his woodshed until it fell flat. He fired " +
        "buckshot from the porch. It didn't flinch, and walked off on all fours. The yard smelled of smoke.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Round, deep prints around the flattened shed, wide as a skillet and sunk into the hard-packed yard.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His phone was recording from the porch: timber cracking, two shotgun blasts, and not one sound from " +
                "the animal.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the driveway caught something long, low and glossy black crossing the gravel on all " +
                "fours.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: hunched and massive, its head low between big shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The shed boards and the yard searched with a lamp. No hair, no blood, only a smell of smoke on " +
                "everything.",
        ),
    ),
    suspects = WASHINGTON_SUSPECTS,
    answerId = GUMBEROO.id,
    solvedNote = "Deep prints, no hair and buckshot that bounced. The Gumberoo hasn't left the logging country.",
)

private val SALMON_RUN = CaseDefinition(
    id = "salmon-run",
    state = "CA",
    scene = Scene.TIMBER,
    place = "The Klamath",
    headlines = listOf(
        "Fly Fisherman Shares the River With Something Big",
        "\"It Was Taking Salmon With Its Hands\"",
        "Klamath Angler Leaves His Rod on the Bank",
        "Whoops Across the River at First Light",
        "Something Waded the Riffle on Two Legs",
    ),
    teasers = listOf(
        "A fly fisherman says something downriver was catching salmon with its hands.",
        "It stood up in the riffle, he says, taller than he could believe.",
        "He left his rod on the bank and walked out backwards.",
        "A whoop from across the river, and knocks answering it.",
        "Bluff Creek isn't far, the old guides like to say.",
    ),
    lead = "A fly fisherman wading the Klamath at first light says something was standing in the riffle downstream " +
        "from him, upright and covered in dark hair, scooping salmon out of the water with its hands. It looked up " +
        "at him. He left his rod on the bank and backed out of the river.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the gravel bar: sixteen-inch, five-toed prints pressed deep into the wet sand, going into the " +
                "water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His phone in his wader pocket recorded it: river noise, a whooping call from the far bank, then wood " +
                "knocking on wood.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera a fisheries crew left on the bank caught a figure standing upright in the riffle, mid-river.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch: man-shaped, enormous in the chest, arms swinging down past its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark hairs on a willow by the gravel bar, higher than his head.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = CALIFORNIA_BIGFOOT.id,
    solvedNote = "Deep prints, whoops and hair on the willow. Bigfoot got its name not far from here, and it still " +
        "fishes the river.",
)

private val THE_PADDLEWHEELER = CaseDefinition(
    id = "the-paddlewheeler",
    state = "CA",
    scene = Scene.LAKE_SHORE,
    place = "Lake Tahoe",
    headlines = listOf(
        "Paddlewheel Tour Passes Over a Long Shadow",
        "Sonar Shows Something Bigger Than the Boat",
        "Tour Captain Slows Down for a Second Look",
        "\"It Rolled, and Everyone on Deck Heard It\"",
        "Tourists Photograph a Dark Shape in Clear Water",
    ),
    teasers = listOf(
        "A paddlewheel tour boat's sonar showed something longer than the boat, just under them.",
        "Passengers at the rail saw a long, dark back under the clear water.",
        "There was no head, they all agree. Just the back, and then the slap.",
        "The captain slowed and circled. It didn't come up again.",
        "The water's clear for seventy feet. It still couldn't be seen properly.",
    ),
    lead = "Passengers on a paddlewheel tour across Lake Tahoe crowded the rail when the captain announced " +
        "something on the sonar longer than the boat. Under the clear water they saw a long, dark back sliding " +
        "past, with no head they could make out. Then it rolled, and the slap on the water carried across the " +
        "whole lake.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beaches nearest where it was seen, walked at sunset. Tourists' footprints and dogs, and nothing " +
                "from the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A passenger's video: the paddlewheel churning, someone gasping, then one huge slap on the water.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the tour dock caught a dark shadow under the surface, longer than the dock, moving into " +
                "deep water.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A passenger tried to draw it: a long dark smear under the water, no head, no fins, nothing certain.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The hull and the paddlewheel checked when the boat docked. Nothing on them at all.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = TAHOE_TESSIE.id,
    solvedNote = "A dark back, a slap and no head. Tessie's been rolling under Tahoe's boats since the 1860s.",
)

private val THE_PAINTER = CaseDefinition(
    id = "the-painter",
    state = "CA",
    scene = Scene.RIDGE,
    place = "Big Sur",
    headlines = listOf(
        "Painter Finds Figure in Her Canvas She Didn't See",
        "Tall Man in a Hat on the Ridge",
        "\"I Painted It Before I Saw It\"",
        "Plein-Air Painter Packs Up at Dusk",
        "Somebody Was Watching the Coast",
    ),
    teasers = listOf(
        "A painter working the coast at sunset noticed she'd painted a figure on the ridge.",
        "It was there when she looked up. Then it wasn't.",
        "Tall, dark, and wearing something like a hat, she says.",
        "Steinbeck wrote about them. She'd never read the story.",
        "She went up the ridge the next day. There was nothing there.",
    ),
    lead = "A painter working above the Big Sur coast at sunset stepped back from her canvas and saw she'd painted " +
        "a tall, dark figure on the ridge above the sea. She looked up, and it was there: still, man-shaped, " +
        "perhaps in a hat, facing her. She looked down at her brushes, and when she looked up again it was gone.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "She hiked up to the ridgetop the next morning. Loose dust and grit, and not one footprint where it " +
                "stood.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "She'd been recording the surf for the mood. Waves, then the birds stopping all at once. Nothing else " +
                "at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the fire road caught an upright figure on the skyline, frame after frame, then only the " +
                "ridge.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her painting itself: a tall, man-shaped outline against the sky, with no face.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The ridgetop combed on hands and knees. No thread, no hair, nothing. Nobody was ever standing there.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = DARK_WATCHERS.id,
    solvedNote = "No tracks, no sound and a figure on the skyline. The Dark Watchers still keep an eye on the " +
        "coast.",
)

private val VINEYARD_FROST_WATCH = CaseDefinition(
    id = "vineyard-frost-watch",
    state = "CA",
    scene = Scene.PASTURE,
    place = "Valley vineyard",
    headlines = listOf(
        "Frost Watch Sees Two White Shapes in the Vines",
        "Infrared Camera Catches Walkers in the Rows",
        "\"All Legs, and Glowing\"",
        "Vineyard Worker Won't Do Nights Alone",
        "Something Strolled Through the Grapes",
    ),
    teasers = listOf(
        "A worker on frost watch saw two small white shapes walking between the vines.",
        "On infrared they glowed brighter than anything else in the field.",
        "No arms, she says. Just a head and two long legs.",
        "They walked side by side, in no hurry, and out of the rows.",
        "Fresno's had these before. Now the vineyards have them too.",
    ),
    lead = "A vineyard worker sitting up on frost watch in the valley outside Fresno says two small white shapes " +
        "came walking down a row of vines at three in the morning, side by side, smooth and unhurried. They seemed " +
        "to be nothing but a head and two long legs. On the infrared camera they glowed.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the soft dirt between the vines: faint marks, barely pressed in, and much too far apart for " +
                "something so small.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The frost fan's camera has sound: the fan, frogs in the ditch, and from the row itself, nothing.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The vineyard's infrared camera: two shapes glowing white against the cold vines.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: a small head on top of two long, thin legs, and nothing else.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The row walked with a flashlight at dawn. No hair, no droppings, not a leaf knocked off a vine.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = FRESNO_NIGHTCRAWLER.id,
    solvedNote = "Faint prints, no sound and two white glowing walkers. The Nightcrawlers have taken up wine " +
        "country.",
)

private val PACKING_HOUSE = CaseDefinition(
    id = "packing-house",
    state = "CA",
    scene = Scene.PASTURE,
    place = "Santa Paula",
    headlines = listOf(
        "Orange Pickers Run From the Canyon Grove",
        "Something With Horns in the Citrus",
        "\"It Screamed Like a Goat, But Bigger\"",
        "Packing House Night Shift Hears Hoofbeats",
        "Grove Ladders Knocked Down in the Night",
    ),
    teasers = listOf(
        "Orange pickers say something stood up in the grove at the head of the canyon.",
        "It had twisted horns and walked like a man.",
        "The night shift at the packing house heard hooves on the loading dock.",
        "Ladders knocked over all down one row, and a scream from the trees.",
        "The old dairy's just up the road. Everyone knows the story.",
    ),
    lead = "Pickers in an orange grove at the mouth of Aliso Canyon say something stood up between the trees at " +
        "dusk: upright, with coarse grey-white hair and twisted horns. It screamed at them like a goat and came " +
        "down the row, knocking ladders over. That night the packing house crew heard hooves on the loading dock.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the dirt between the trees: cloven hoofprints, but only two to a stride, walking upright down the " +
                "row.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A picker's phone video: ladders falling, people shouting, and a long, bleating scream from the trees.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The packing house camera caught a figure walking upright past the loading dock, broad in the " +
                "shoulders.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A picker's drawing: a man's shape with a pair of twisted horns curling back from its head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse grey-white hairs caught in the bark of an orange tree, and more on the ladder rungs.",
        ),
    ),
    suspects = CALIFORNIA_SUSPECTS,
    answerId = BILLIWHACK_MONSTER.id,
    solvedNote = "Hooves on two legs, horns and a goat's scream. The Billiwhack still haunts the canyon.",
)

private val SHEEP_WAGON = CaseDefinition(
    id = "sheep-wagon",
    state = "WY",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Sheepherder's Harmonica Gets an Answer",
        "Something Out in the Sage Knows His Song",
        "\"It Played It Back Better Than I Did\"",
        "Herder Sees Antlers by the Wagon",
        "Converse County Herder Swears by It",
    ),
    teasers = listOf(
        "A sheepherder played his harmonica outside his wagon. Something sang the tune back.",
        "It knew the second verse, he says, and he never played it.",
        "In the morning: tiny prints round the wagon and antlers in the sage.",
        "He's herded alone for twenty summers. He's never had company before.",
        "The dogs wouldn't go after it. They just sat and listened.",
    ),
    lead = "A sheepherder camped alone in his wagon outside Douglas says he was playing his harmonica after dark " +
        "when a thin voice out in the sagebrush began singing the tune back to him, verse after verse. At first " +
        "light he saw something small sitting up by the wagon wheel, with a rack of antlers, before it shot off " +
        "too fast to follow.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Round the wagon: small, light prints in the dust, in pairs, with long jumps between them.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His recorder caught it: the harmonica, then out of the dark a thin, reedy voice singing the same " +
                "tune.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the stock tank caught something low to the ground, a blur across two frames.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: a jackrabbit sitting up, with antlers on its head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Soft grey-brown fur on the wagon's step, where it sat.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = JACKALOPE.id,
    solvedNote = "Bounding prints, antlers and a voice that sings back. Douglas has always known its jackalopes " +
        "are musical.",
)

private val TRUCKERS_PULL_OVER = CaseDefinition(
    id = "truckers-pull-over",
    state = "WY",
    scene = Scene.LAKE_SHORE,
    place = "Lake De Smet",
    headlines = listOf(
        "Truckers Pull Over to Watch the Lake",
        "Humps Beside the Interstate",
        "\"A Mane Down Its Back, Like a Horse's\"",
        "Waves Hit the Shore With No Wind",
        "Highway Patrol Gets Three Calls About the Lake",
    ),
    teasers = listOf(
        "Three truckers pulled onto the shoulder to watch something in the lake.",
        "A row of humps with a ridge down the back like a horse's mane.",
        "The lake was glass. Then waves came in on a windless day.",
        "Highway patrol took three calls in ten minutes.",
        "Buffalo's had this story since 1892. Now the interstate has it.",
    ),
    lead = "Three truckers on the interstate pulled onto the shoulder above Lake De Smet this morning to watch " +
        "something rolling in the water: a row of dark humps, thick as a tree trunk, with a bony ridge along the " +
        "top like a horse's mane. It went under with a slap, and waves rolled into a shore with no wind.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The muddy bank below the highway, walked end to end. Deer and people, and not a print from the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A trucker's dashcam: traffic noise, then a single enormous slap on the water and waves breaking " +
                "after.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the boat ramp caught a long, low back on the water, longer than the dock.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One trucker drew it on his log book: humps in a row, with a mane-like ridge down them.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The bank and the boat ramp checked. Nothing on them. The nets came back with carp.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = SMETTY.id,
    solvedNote = "Humps, a mane and waves with no wind. Smetty has been rolling in De Smet since the 1890s.",
)

private val THE_SNOWCAT = CaseDefinition(
    id = "the-snowcat",
    state = "WY",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Snowmobile Guide Finds Tracks Bigger Than His Boots",
        "Something Walked the Pass in the Night",
        "\"It Was Standing in the Trail When the Lights Came On\"",
        "Wind River Guide Turns His Group Around",
        "Pinecones Rained on the Warming Hut",
    ),
    teasers = listOf(
        "A snowmobile guide's headlight came on, and something was standing in the trail.",
        "Upright, dark, and taller than the trail marker.",
        "By morning there were tracks down the trail, deep and five-toed.",
        "Pinecones rained on the warming hut half the night.",
        "He turned his group around and nobody asked why.",
    ),
    lead = "A snowmobile guide warming up the sleds outside a hut in the Wind River Range before dawn switched on " +
        "his headlight, and something was standing in the trail: upright, dark, and taller than the trail marker " +
        "beside it. It walked into the trees. All the night before, pinecones had been landing on the hut's roof.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Down the trail: deep, five-toed prints in the snow, longer than his boots and sunk in to the knee.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A guest's phone from the hut: pinecones on the roof, then a long, rising call and two knocks from " +
                "across the draw.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the hut caught a figure walking upright between the lodgepoles, head at the lowest " +
                "branches.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: man-shaped and huge, arms hanging past its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse hairs stuck in the pitch of a lodgepole by the trail, well over his head.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = WYOMING_BIGFOOT.id,
    solvedNote = "Deep prints, pinecones and hair in the pitch. The Wind Rivers' big one hasn't moved on.",
)

private val THE_MINNOW_TRAP = CaseDefinition(
    id = "the-minnow-trap",
    state = "WY",
    scene = Scene.LAKE_SHORE,
    place = "Fishing dock",
    headlines = listOf(
        "Minnow Trap Comes Up With Horns in It",
        "Boy's Trap Holds Something Grey as Lead",
        "\"Three Horns on Each Side\"",
        "It Slipped Out Through the Mesh",
        "Uncle Says Don't Touch It, Too Late",
    ),
    teasers = listOf(
        "A boy's minnow trap came up holding something grey, with horns down its sides.",
        "It slid out through the mesh like it was greased.",
        "His uncle told him not to touch it. His hands are still slimy.",
        "Three horns on each side, the boy says, and a head like a bull's.",
        "A Sheridan saloon showed one off in 1915. Nobody's seen one since.",
    ),
    lead = "A boy checking his minnow trap off the dock at Lake De Smet pulled it up to find something wedged " +
        "inside that wasn't a minnow: grey as lead, as long as his arm, with a blunt bull's head and horns down " +
        "both sides. It slid out through the mesh, slick with slime, and was gone under the boards.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The dock boards and the mud below it. Nothing climbed out. It went straight back into the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His sister was filming: the boy shouting, the trap rattling, and from the thing inside, not a sound.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the dock post caught only a shadow in the shallows, sliding under the boards.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The boy's drawing: a blunt head with horns sticking out of it, and not much else he can swear to.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The trap's mesh coated in thick, clear slime that won't wash off.",
        ),
    ),
    suspects = WYOMING_SUSPECTS,
    answerId = WIMPUS.id,
    solvedNote = "No sound, horns and slime that won't wash off. The Wimpus wasn't the only one in the lake.",
)

private val THE_ISLAND_TRAIL = CaseDefinition(
    id = "the-island-trail",
    state = "TX",
    scene = Scene.LAKE_SHORE,
    place = "Greer Island",
    headlines = listOf(
        "Birders Flee the Island Trail",
        "Horned Figure on the Bluff Again",
        "\"It Threw a Log at Us\"",
        "Nature Center Closes Island Path",
        "The Goat-Man of Lake Worth Is Back",
    ),
    teasers = listOf(
        "A group of birders on the island trail say something on the bluff threw a log at them.",
        "Upright, hairy, and horned, they say, just like in 1969.",
        "It screamed, and the herons all went up at once.",
        "The nature center has closed the island path for a week.",
        "Every October they throw the monster a party. Maybe it came early.",
    ),
    lead = "A birdwatching group on the Greer Island trail at Lake Worth say something stood up on the bluff above " +
        "them: upright, shaggy, with horns. It screamed, picked up a driftwood log and threw it down at them. The " +
        "nature center has closed the island path while staff take a look.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On top of the bluff: cloven hoofprints in the dirt, but only two to a stride. Something walked " +
                "upright.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A birder's video: herons going up, someone yelling, and from the bluff a high, bleating scream.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the nature center gate caught a figure walking upright along the fence, broad through the " +
                "shoulders.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Two birders drew it separately. Both drawings have the same pair of horns.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse hairs stuck in the bark of the log it threw.",
        ),
    ),
    suspects = TEXAS_SUSPECTS,
    answerId = LAKE_WORTH_MONSTER.id,
    solvedNote = "Hooves on two legs, horns and a thrown log. The Lake Worth Monster is still throwing things " +
        "after fifty years.",
)

private val THE_HOG_TRAP = CaseDefinition(
    id = "the-hog-trap",
    state = "TX",
    scene = Scene.TIMBER,
    headlines = listOf(
        "Hog Trap Torn Open in the River Bottom",
        "Steel Panels Bent Back by Hand",
        "\"Something Carried Off Three Hogs\"",
        "Trapper Hears Calls Down the River All Night",
        "Feral Hogs Gone, and Nobody's Complaining",
    ),
    teasers = listOf(
        "A hog trap's steel panels were bent back like they were cardboard.",
        "Three hogs gone, and no drag marks. Carried.",
        "A long, low call came up the river bottom after dark.",
        "The trapper's camera caught something standing in the trap. Upright.",
        "The Wild Woman of the Navidad took food from cabins. This took hogs.",
    ),
    lead = "A hog trapper in the East Texas river bottom found his corral trap torn open this morning: steel " +
        "panels bent back by hand, and three feral hogs gone. Not dragged off. Carried. All night, he says, " +
        "something was calling down the river, long and low, and something knocked back.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Deep, five-toed prints in the mud inside the trap, sunk much farther than his boots.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The trap camera's microphone: hogs squealing, steel groaning, then a long, rising call and two " +
                "knocks.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The trap camera's picture: a figure standing upright inside the corral, taller than the panels.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch: man-shaped and massive, arms hanging past its knees.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs caught on a bent panel, well above his head.",
        ),
    ),
    suspects = TEXAS_SUSPECTS,
    answerId = TEXAS_BIGFOOT.id,
    solvedNote = "Deep prints, bent steel and hair on the panel. The river bottoms' big one likes pork.",
)

private val GHOST_RIDER_ON_THE_HIGHWAY = CaseDefinition(
    id = "ghost-rider-on-the-highway",
    state = "AZ",
    scene = Scene.PASTURE,
    headlines = listOf(
        "Trucker Swears He Saw a Camel With a Rider",
        "The Red Ghost on Route 60",
        "\"The Rider Never Moved\"",
        "Huge Red Beast Crosses Highway at Night",
        "Old Camel Corps Story Comes Back to Life",
    ),
    teasers = listOf(
        "A trucker says a red beast taller than his cab crossed the highway in his lights.",
        "Something was strapped upright on its back. It never moved.",
        "Not a sound from it, he says, not even hoofbeats.",
        "The Army's camels were turned loose in the 1860s. One of them may still be out there.",
        "He's driven the Salt River route for years. He'll take the interstate now.",
    ),
    lead = "A trucker driving the highway through the Salt River country at night says a huge red beast walked " +
        "into his headlights, taller than his cab, with a long neck and a hump. On the hump, strapped upright, was " +
        "something like a rider, slumped and still. It crossed without a sound and went down into the wash.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the sand of the wash: cloven tracks twice the size of a horse's, in a long, even stride.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His dashcam audio: the engine, his brakes, his swearing, and nothing at all from the animal.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A highway camera at the next turnout caught something tall and upright against the stars, taller than " +
                "the sign.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: a long neck, a hump, and on the hump something sitting up.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse red hair caught on the mesquite beside the road, higher than a horse's back.",
        ),
    ),
    suspects = ARIZONA_SUSPECTS,
    answerId = RED_GHOST.id,
    solvedNote = "Huge cloven tracks, no sound and red hair on the mesquite. The Red Ghost is still riding the " +
        "Salt River country.",
)

private val THE_IGLOOS = CaseDefinition(
    id = "the-igloos",
    state = "WV",
    scene = Scene.RIVER_TOWN,
    place = "The TNT area",
    headlines = listOf(
        "Red Eyes Over the Old TNT Bunkers",
        "Couple Chased by Something With Wings",
        "\"It Kept Up With the Car at Sixty\"",
        "Point Pleasant Hears the Squeal Again",
        "Something Rose Off the Igloos",
    ),
    teasers = listOf(
        "A couple parked by the old TNT bunkers saw two red eyes rise off the top of one.",
        "It followed their car down the road, they say, at sixty miles an hour.",
        "Man-shaped, wings folded, and no neck at all.",
        "The squeal sounded like a fan belt slipping, right over the roof.",
        "Point Pleasant has a statue of it. Some people think that's tempting fate.",
    ),
    lead = "A couple parked near the old TNT plant outside Point Pleasant last night say two glowing red eyes rose " +
        "off the top of one of the concrete bunkers: something man-shaped, taller than a man, with great wings. " +
        "When they drove off, it followed above the car at sixty miles an hour, squealing.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The dirt and grit on top of the bunker where it rose. Not a single print.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Their dashcam audio: the engine racing, and over it a high, squealing screech like a fan belt " +
                "slipping.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the gate caught two red points of light high above the road, shining back at the flash.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: man-shaped, its head sunk between its shoulders, with great wings behind.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The bunker roof and the car swabbed. No hair, no feathers, nothing from an animal at all.",
        ),
    ),
    suspects = WEST_VIRGINIA_SUSPECTS,
    answerId = MOTHMAN.id,
    solvedNote = "No tracks, a squeal and red eyes in the dark. The Mothman hasn't left Point Pleasant.",
)

private val REGIONAL_STORIES: List<RegionalStory> =
    listOf(BLED_DRY, WINGS_OVER_THE_WASH, UP_ON_THE_RIM, THE_RABBIT_HUTCHES, THE_TOWER_CLIMBER, ELK_CAMP) +
        WIDE_STORIES + MORE_WIDE_STORIES

val ALL_CASES = listOf(
    FLAT_WATER,
    CARRIES_OFF_DOGS,
    KNOCKS_IN_THE_TIMBER,
    LIGHTS_OVER_THE_PASTURE,
    THE_WATCHED_SHORE,
    SHADOW_OVER_THE_RIDGE,
    ROCKS_ON_THE_ROOF,
    SOMETHING_IN_THE_STRAIT,
    SOMETHING_LANDED_ON_THE_ROAD,
    DRAG_MARKS_AT_THE_DOCK,
    BULLETS_BOUNCED_OFF,
    TRACKS_ROUND_THE_DOZER,
    CLEAR_WATER,
    FIGURE_ON_THE_NEXT_RIDGE,
    ACROSS_THE_LAWN,
    SCREAMING_IN_THE_CANYON,
    SINGING_IN_THE_SAGEBRUSH,
    NO_WIND_ON_THE_LAKE,
    PINECONES_ON_THE_TENT,
    NOT_A_CARP,
    THE_TIRE_CAME_FLYING,
    DOWN_IN_THE_RIVER_BOTTOM,
    SOMETHING_RODE_THE_CAMEL,
    DUSK_CROSSING,
    ON_THE_BRIDGE,
    CHERRY_PICKERS,
    LAUGHING_IN_THE_CALVING_BARN,
    LOOKOUT_TOWER,
    THE_HUM_ON_THE_SECTION_ROAD,
    KIDS_FISHING_DERBY,
    THE_PARAGLIDER,
    HOOFPRINTS_ON_THE_ROOFS,
    CHANTERELLE_PICKERS,
    ORCA_TOUR,
    NIGHT_GROOMER,
    ORCHARD_SPRINKLERS,
    THE_WOODSHED,
    SALMON_RUN,
    THE_PADDLEWHEELER,
    THE_PAINTER,
    VINEYARD_FROST_WATCH,
    PACKING_HOUSE,
    SHEEP_WAGON,
    TRUCKERS_PULL_OVER,
    THE_SNOWCAT,
    THE_MINNOW_TRAP,
    THE_ISLAND_TRAIL,
    THE_HOG_TRAP,
    GHOST_RIDER_ON_THE_HIGHWAY,
    THE_IGLOOS,
) + ALASKA_HAWAII_CASES + REGIONAL_STORIES.flatMap { it.cases(FIELD_GUIDES) }

fun caseById(id: String): CaseDefinition? = ALL_CASES.firstOrNull { it.id == id }

/** Cases whose cryptid is reported in [state]. An area with no state gets none. */
fun casesIn(state: String?): List<CaseDefinition> = ALL_CASES.filter { state != null && it.state == state }
