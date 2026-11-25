package app.unconfirmed.shared

/*
 * Creatures that range over much of the country, so every mainland state has a field guide big
 * enough to play. The plan, the reasoning behind each range and the sources are in
 * build/regional.md, and build/regional_roster.py checks that every state's guide still works:
 * at least four suspects, and any two of them differ on at least three clue types.
 *
 * A state that tells its own stories about one of these creatures, with its own entry for it, is
 * left out of that creature's range. Its own entry already puts the creature in its guide, and
 * leaving it out keeps the regional story from running there as a second case about the same
 * creature.
 *
 * Nothing here may refer to the vals in Cases.kt, which reads these lists while it's being set up.
 */

private val MAINLAND = setOf(
    "AL", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "ID", "IL", "IN", "IA", "KS", "KY", "LA",
    "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND",
    "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
)

private val NEW_ENGLAND = setOf("ME", "NH", "VT", "MA", "RI", "CT")
private val GREAT_LAKES = setOf("MI", "WI", "MN", "IL", "IN", "OH")
private val PLAINS = setOf("ND", "SD", "NE", "KS")
private val DEEP_SOUTH = setOf("SC", "GA", "FL", "AL", "MS", "LA", "AR")
private val UPPER_SOUTH = setOf("VA", "NC", "TN", "KY", "WV")

// ---- Creatures that share art with one already in the game ---------------------------------------

/**
 * MT, WA, CA, WY and TX have their own Bigfoot. AZ and NM have the Mogollon Monster, the
 * Southwest's Bigfoot, with the same traits. Alaska has its reports too, from the Yukon–Kuskokwim
 * Delta to the North Slope.
 */
internal val WIDE_BIGFOOT = RegionalCreature(
    Suspect(
        "bigfoot",
        "Bigfoot",
        "North America's forest giant, reported in nearly every state: the Pacific Northwest's " +
            "Sasquatch, but also the swamps of the South, the hollows of Appalachia, the north woods " +
            "and the Alaskan bush. Upright and man-shaped, heavy and covered in long dark hair. Deep, wide, " +
            "five-toed prints, long howls in the night and knocks on the trees.",
        traits(PRINTS_DEEP, CALL_HOWL, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_HAIR),
    ),
    MAINLAND - setOf("MT", "WA", "CA", "WY", "TX", "AZ", "NM") + "AK",
)

internal val WIDE_THUNDERBIRD = RegionalCreature(
    Suspect(
        "thunderbird",
        "Thunderbird",
        "Giant birds of Plains and Great Lakes lore that are still reported: in 1977 a boy in " +
            "Lawndale, Illinois was said to have been lifted off the ground by one, and the woods of " +
            "northern Pennsylvania have sightings of their own. Wingspans of ten to twenty feet. It " +
            "lands in long hops, so its talon marks are shallow and far apart, and perched it hunches " +
            "with its head low. A piercing screech, and feathers longer than any eagle's.",
        traits(PRINTS_SHALLOW, CALL_SHRILL, CAM_SHADOW, SKETCH_HUNCHED, SAMPLE_FEATHER),
    ),
    // Montana has its own Thunderbird.
    PLAINS + GREAT_LAKES + setOf("OK", "IA", "MO", "PA", "CO"),
)

internal val WIDE_JACKALOPE = RegionalCreature(
    Suspect(
        "jackalope",
        "Jackalope",
        "A jackrabbit with antlers, sold on postcards and mounted over bars from the Plains to the " +
            "Rockies since a Wyoming taxidermy shop made the first in the 1930s. Said to be blinding " +
            "fast. It bounds, so its prints are small and far apart, and it sheds soft fur. It mimics " +
            "voices, and sings back to anyone singing by a fire at night.",
        traits(PRINTS_SHALLOW, CALL_VOICE, CAM_LOW, SKETCH_HORNED, SAMPLE_HAIR),
    ),
    // Wyoming, the Jackalope's home, has its own.
    PLAINS + setOf("MT", "CO", "UT", "ID", "NV"),
)

/** The same kind of creature as the Billiwhack, so it shares its art. Kept out of WV, where it's too like the Sheepsquatch. */
internal val GOATMAN = RegionalCreature(
    Suspect(
        "goatman",
        "Goatman",
        "Half man and half goat, the terror of back roads in Prince George's County, Maryland since " +
            "the early 1970s, and of the Pope Lick railroad trestle outside Louisville, Kentucky. " +
            "Upright and shaggy, with a goat's horns and legs. It walks on cloven hooves, screams in " +
            "the night, and leaves coarse hair on the fences.",
        traits(PRINTS_HOOVES, CALL_SHRILL, CAM_UPRIGHT, SKETCH_HORNED, SAMPLE_HAIR),
    ),
    setOf("MD", "KY"),
)

/*
 * Four lake and sea serpents, the same kind of creature as Montana's Flathead monster, so they
 * share its art. They splash where it's silent, because the hoop snake is in some of their states
 * and is otherwise too like them. No two of them share a state.
 */

internal val CHAMP = RegionalCreature(
    Suspect(
        "champ",
        "Champ",
        "Lake Champlain's monster, seen from the Vermont and New York shores. The Abenaki told of a " +
            "creature in the lake long before Sandra Mansi's photograph of July 1977, the best-known " +
            "picture of it: a long neck and small head rising from the water. A long body in humps. " +
            "It never comes ashore, so it leaves no tracks, only a fishy slime and a heavy splash.",
        traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
    ),
    setOf("VT", "NY"),
)

internal val SOUTH_BAY_BESSIE = RegionalCreature(
    Suspect(
        "south-bay-bessie",
        "South Bay Bessie",
        "Lake Erie's serpent, said to have chased a ship's captain in 1793 and named in the 1990s, " +
            "when sightings off the Ohio shore were so common that a marina offered a reward for her " +
            "capture. Thirty feet or more, dark, and in humps. Never on land, so no tracks: just a " +
            "splash and a slick of slime.",
        traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
    ),
    setOf("OH", "PA", "MI"),
)

internal val BEAR_LAKE_MONSTER = RegionalCreature(
    Suspect(
        "bear-lake-monster",
        "Bear Lake Monster",
        "The serpent of Bear Lake, which straddles the Utah–Idaho line, made famous by Joseph C. " +
            "Rich's letters to the Deseret News in 1868. Rich is said to have admitted later that he " +
            "made it up, but people still see it: a long body in humps and a small head. No tracks, " +
            "only a splash and slime on the rocks.",
        traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
    ),
    setOf("UT", "ID"),
)

internal val SEA_SERPENT = RegionalCreature(
    Suspect(
        "sea-serpent",
        "Atlantic Sea Serpent",
        "In August 1817 crowds watched a great serpent in the harbor at Gloucester, Massachusetts, " +
            "and Boston's Linnaean Society took sworn statements. Reports run from Maine down to " +
            "Delaware. A long dark body in humps, and a head like a snake's or a horse's. It leaves " +
            "no tracks, only slime on the rocks, and a splash.",
        traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
    ),
    NEW_ENGLAND - setOf("VT") + setOf("NJ", "DE"),
)

internal val WIDE_TAHOE_TESSIE = RegionalCreature(
    Suspect(
        "tahoe-tessie",
        "Tahoe Tessie",
        "Lake Tahoe's monster, seen from the Nevada and California shores since the 1860s. Witnesses " +
            "see only a long, dark back under the clear water, never a head. It never comes ashore. " +
            "The only sound is a heavy slap on the water as it rolls.",
        traits(PRINTS_NONE, CALL_SPLASH, CAM_SHADOW, SKETCH_SMUDGE, SAMPLE_NOTHING),
    ),
    // The lake is shared with California, which has its own entry.
    setOf("NV"),
)

internal val WIDE_GUMBEROO = RegionalCreature(
    Suspect(
        "gumberoo",
        "Gumberoo",
        "A terror of the Northwest's old logging camps: bigger than a bear, coal-black and almost " +
            "hairless, with a hide that bullets bounce off. It goes on all fours and leaves deep, " +
            "round prints. Silent, and it sheds no hair at all, though it smells of smoke. Only fire " +
            "is said to kill it.",
        traits(PRINTS_DEEP, CALL_NONE, CAM_LOW, SKETCH_HUNCHED, SAMPLE_NOTHING),
    ),
    // Washington has its own.
    setOf("OR"),
)

// ---- Creatures with art of their own ---------------------------------------------------------------

internal val DOGMAN = RegionalCreature(
    Suspect(
        "dogman",
        "Dogman",
        "Michigan's upright canine. A Traverse City DJ's April Fool's song in 1987 set off hundreds " +
            "of reports, and Wisconsin's Beast of Bray Road, near Elkhorn, is the same kind of thing. " +
            "Seven feet tall on its hind legs, with a wolf's head and amber eyes that shine in the " +
            "dark. Clawed prints, a long howl, and coarse grey-brown hair.",
        traits(PRINTS_CLAWED, CALL_HOWL, CAM_GLOW, SKETCH_DOG, SAMPLE_HAIR),
    ),
    GREAT_LAKES,
)

internal val BLACK_PANTHER = RegionalCreature(
    Suspect(
        "black-panther",
        "Black Panther",
        "Big black cats, reported from the Ozarks to Maine for more than a century. No black cougar " +
            "has ever been confirmed, and the eastern cougar was declared extinct in 2018, but the " +
            "reports keep coming. Long and low, with a heavy tail. Round prints with no claw marks, a " +
            "scream like a woman's, and short black hair caught on the fence wire.",
        traits(PRINTS_PADDED, CALL_SHRILL, CAM_LOW, SKETCH_CAT, SAMPLE_HAIR),
    ),
    NEW_ENGLAND + DEEP_SOUTH + UPPER_SOUTH + GREAT_LAKES +
        setOf("NY", "NJ", "PA", "DE", "MD", "MN", "IA", "MO", "OK", "TX"),
)

internal val HOOP_SNAKE = RegionalCreature(
    Suspect(
        "hoop-snake",
        "Hoop Snake",
        "Told of from Texas to the Great Lakes and all through Appalachia: a snake that takes its tail " +
            "in its mouth and rolls at you like a wheel, then lets go and strikes with a stinger in its " +
            "tail. Get behind a tree and the tree takes the sting, swells up and dies. It leaves no " +
            "tracks, only a thin groove, never makes a sound, and sheds its scales.",
        traits(PRINTS_NONE, CALL_NONE, CAM_LOW, SKETCH_HOOP, SAMPLE_SCALE),
    ),
    PLAINS + DEEP_SOUTH + UPPER_SOUTH + setOf("TX", "OK", "MO", "MD", "DE", "IL", "IN", "OH", "IA"),
)

/** Its traits are Montana's Blacktail's, and close to West Virginia's Grafton Monster's, so it stays out of both states. */
internal val HIDEBEHIND = RegionalCreature(
    Suspect(
        "hidebehind",
        "Hidebehind",
        "The lumberjacks' terror, from the Maine woods to the Northwest: something that's always " +
            "behind you. Turn round and it's behind a tree, thin enough to hide behind any trunk, and " +
            "men who went missing from the camps were put down to it. Nobody has ever seen it clearly. " +
            "No tracks, no sound, and nothing left behind. The smell of whiskey is said to keep it off.",
        traits(PRINTS_NONE, CALL_NONE, CAM_SHADOW, SKETCH_SMUDGE, SAMPLE_NOTHING),
    ),
    NEW_ENGLAND + setOf("NY", "NJ", "PA", "MI", "WI", "MN", "OR", "ID", "CO", "VA", "NC", "TN", "KY"),
)

/** Kept out of MT and CA, where it's too like the Prairie Giant and the Fresno Nightcrawler. */
internal val TOMMYKNOCKER = RegionalCreature(
    Suspect(
        "tommyknocker",
        "Tommyknocker",
        "The little people of the mines. Cornish miners called them knockers and brought them to the " +
            "West's mining camps, where they became tommyknockers. Their knocking in the rock warns of " +
            "a cave-in, and miners left them the crusts of their pasties. Waist-high, bearded and " +
            "hooded, seen as a lamp moving where no miner is. Small, light prints, and nothing left behind.",
        traits(PRINTS_SHALLOW, CALL_KNOCK, CAM_GLOW, SKETCH_MAN, SAMPLE_NOTHING),
    ),
    setOf("CO", "NV", "UT", "ID", "AZ", "NM", "OR"),
)

/** Every regional creature in this file. Cases.kt adds the Southwest's. */
internal val WIDE_CREATURES: List<RegionalCreature> = listOf(
    WIDE_BIGFOOT,
    WIDE_THUNDERBIRD,
    WIDE_JACKALOPE,
    GOATMAN,
    CHAMP,
    SOUTH_BAY_BESSIE,
    BEAR_LAKE_MONSTER,
    SEA_SERPENT,
    WIDE_TAHOE_TESSIE,
    WIDE_GUMBEROO,
    DOGMAN,
    BLACK_PANTHER,
    HOOP_SNAKE,
    HIDEBEHIND,
    TOMMYKNOCKER,
)

// ---- Stories ---------------------------------------------------------------------------------------
// Each runs in every state its creature reaches, so the words name no place.

private val TREE_STAND = RegionalStory(
    id = "tree-stand",
    creature = WIDE_BIGFOOT,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Hunter Comes Down From the Stand Early",
        "Something Walked Under the Tree Stand",
        "Bow Season Opener Cut Short",
        "\"It Looked Right Up at Me\"",
        "Deer Scattered, Then the Woods Went Quiet",
    ),
    teasers = listOf(
        "A bow hunter says something on two legs walked right under his stand.",
        "First the deer bolted. Then the birds stopped.",
        "He's hunted these woods thirty years. He climbed down and went home.",
        "It stopped under the stand, he says, and looked up.",
        "A smell like a wet dog, and footsteps far too heavy for a man.",
    ),
    lead = "A bow hunter came down from his tree stand at first light and drove straight home. Something " +
        "on two legs, taller than the lowest branches, walked under the stand, he says, stopped, and " +
        "looked up at him. Then it went off into the trees without hurrying.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Under the stand, in the soft ground by the creek: deep, wide, five-toed prints, nearly twice " +
                "the length of his boot, pressed in hard.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His phone was recording for the turkeys. Wind, then a long, rising howl from up the ridge, " +
                "and two hard knocks of wood on wood.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "His own camera on the feeder: a dark figure walking upright past the edge of the frame, its " +
                "head level with the lowest branch.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "He drew it at his kitchen table: man-shaped, huge in the shoulders, long arms, and no neck to " +
                "speak of.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, coarse, dark hairs snagged on the bark of the stand's own tree, well above head height.",
        ),
    ),
    solvedNote = "Deep prints, a howl and hair on the bark. " +
        "Every state with woods has its Bigfoot stories, and now this one has another.",
)

private val LIFTED_THE_CALF = RegionalStory(
    id = "lifted-the-calf",
    creature = WIDE_THUNDERBIRD,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Calf Lifted Off the Ground",
        "Farmer Swears It Was a Bird",
        "Shadow Crossed the Whole Barnyard",
        "\"Wings Like a Small Plane\"",
        "Neighbors Keep the Kids Indoors",
    ),
    teasers = listOf(
        "A farmer says a bird picked up a calf and dropped it in the next field.",
        "The calf is bruised but fine. The farmer isn't sleeping.",
        "Its shadow covered the barnyard, says his wife.",
        "It screamed going up, and the calf bawled.",
        "Two neighbors saw it from the road and pulled over.",
    ),
    lead = "A farmer says a bird with wings wider than his pickup came down on his pasture yesterday " +
        "evening, took hold of a calf, and carried it the length of the field before it let go. The calf " +
        "is bruised but alive. Two neighbors saw it from the road.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Where it came down: talon marks, light in the dirt and spaced far apart, as if it landed in " +
                "long hops.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A neighbor's video: the calf bawling, then a piercing, cackling screech overhead.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the gate caught nothing but a shadow sweeping across the whole frame, far too big " +
                "for a hawk.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The farmer's wife saw it perched on the fence first and drew it: hunched, its head sunk low " +
                "between high shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A single dark feather in the pasture, longer than her forearm. No eagle grows one like it.",
        ),
    ),
    solvedNote = "Shallow hops, a screech and a feather no eagle grew. " +
        "The Thunderbird is still out over the farm country, and still hungry.",
)

private val SANG_BACK = RegionalStory(
    id = "sang-back",
    creature = WIDE_JACKALOPE,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Something Sang Back at the Branding",
        "Ranch Hands Hear Their Own Song Returned",
        "The Echo That Got the Words Wrong",
        "Antlers in the Headlights",
        "Hunting Licenses Back on Sale, Jokes Rancher",
    ),
    teasers = listOf(
        "The crew sang one verse. Something out in the dark sang the next.",
        "Not an echo, they say. It changed the tune.",
        "A driver says a jackrabbit with antlers crossed in front of her truck.",
        "The rancher thinks it's a joke. His crew doesn't.",
        "Whatever it was, it stopped singing when the flashlight came on.",
    ),
    lead = "A ranch crew sitting up late after branding say they sang a verse around the fire and " +
        "something out in the dark sang the next one back, in a voice almost like a man's. Driving home, " +
        "one of them saw something small with antlers cross the road.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Small, light prints at the edge of the firelight, in pairs, with long jumps between them.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone was recording the singing. After the verse, out of the dark, a thin voice carries the " +
                "tune on, a little off, word by word.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the stock tank: something small and low in the grass, ears up, and two thin " +
                "points above them.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The driver's drawing: a big jackrabbit sitting up on its haunches, with a small rack of " +
                "antlers on its head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A tuft of soft, grey-brown fur on the barbed wire where the prints went under.",
        ),
    ),
    solvedNote = "Small bounding prints, antlers and a voice that sings back. " +
        "Somebody get the crew a hunting license for jackalope.",
)

private val UNDER_THE_TRESTLE = RegionalStory(
    id = "under-the-trestle",
    creature = GOATMAN,
    scene = Scene.RIVER_TOWN,
    place = "The old trestle",
    headlines = listOf(
        "Teens Flee the Old Trestle",
        "Something Under the Railroad Bridge",
        "Dog Found Dead Near Back Road",
        "Dare Goes Wrong at Midnight",
        "Parents Warn: Stay Off the Tracks",
    ),
    teasers = listOf(
        "Four teenagers went to the trestle on a dare and ran back.",
        "It stood up from under the bridge, they say, and it had horns.",
        "A family's dog was found dead in the ditch the next morning.",
        "Police say stay off the tracks. The teenagers say they will.",
        "One of them heard it walking on the gravel. Hooves, she says.",
    ),
    lead = "Four teenagers went down to the old railroad trestle at midnight on a dare. Something stood up " +
        "from under the bridge, they say: taller than any of them, shaggy, with horns. It screamed, and " +
        "they ran. A family's dog was found dead in a ditch nearby in the morning.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Under the trestle, in the cinders and mud: a line of cloven hoof prints, but only two of them " +
                "to a stride, as if something walked on two legs.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "One of them kept filming as they ran. Over their own panting, a long, shrill scream from " +
                "under the bridge.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera the railroad put up after vandalism caught a figure standing upright among the " +
                "bridge piers.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn by the girl who saw it best: upright and man-shaped, with long, curling horns.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, greasy hair on the barbed wire by the tracks, and a smell like a billy goat's.",
        ),
    ),
    solvedNote = "Hooves on two legs, horns and a scream. " +
        "The Goatman has been warning kids off back roads for fifty years.",
)

private val OFF_THE_FERRY = RegionalStory(
    id = "off-the-ferry",
    creature = CHAMP,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Ferry Passengers Crowd the Rail",
        "Neck Rose Out of the Wake",
        "Crossing Delayed by Sighting",
        "\"It Looked Back at the Boat\"",
        "Deckhand: Not a Log",
    ),
    teasers = listOf(
        "Half the passengers on the morning ferry saw it.",
        "A long neck came up behind the boat, they say, and went back down.",
        "The deckhand has worked the crossing twenty years. He's never seen anything like it.",
        "Three humps in a row, then a head, then nothing.",
        "The captain slowed down so people could look.",
    ),
    lead = "Passengers on the morning ferry crowded the rail when something came up in the wake: a " +
        "long neck and a small head, then a dark back in humps behind it. It went under before anyone " +
        "got a good picture. The deckhand says it wasn't a log.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The shore nearest where it surfaced, walked end to end. Wet sand, pebbles, and no tracks " +
                "coming out of the water at all.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A passenger's video: the engine, people shouting, then one huge splash as it went under.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the dock caught something long and low riding the swell past the pilings.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The deckhand's drawing: three humps in a line, and a long neck at the front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A streak of clear, fishy slime on the ferry's hull, just above the waterline.",
        ),
    ),
    solvedNote = "Humps, a long neck and slime on the hull. " +
        "The lake's monster has been seen from both shores for centuries.",
)

private val PAST_THE_BREAKWALL = RegionalStory(
    id = "past-the-breakwall",
    creature = SOUTH_BAY_BESSIE,
    scene = Scene.LAKE_SHORE,
    place = "The marina",
    headlines = listOf(
        "Charter Captain Reports Thirty-Foot Shape",
        "Something Followed the Boat In",
        "Walleye Trip Turns Back Early",
        "Dark Humps Off the Breakwall",
        "Marina Owner Raises the Reward",
    ),
    teasers = listOf(
        "A fishing charter came in early with six shaken customers.",
        "It was as long as the boat, says the captain, and it kept up.",
        "Three dark humps, and then a fourth.",
        "The marina has offered a reward for years. Nobody's claimed it.",
        "The sonar showed something big under them. Then it was gone.",
    ),
    lead = "A charter captain brought his boat in early with six customers who'd seen something keep " +
        "pace with them off the breakwall: dark humps in a line, longer than the boat. The sonar showed " +
        "something big underneath. Then it sank away.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach by the breakwall, checked at first light. Driftwood, gull tracks, and nothing " +
                "that came up out of the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A customer's video: the motor, someone swearing, then a heavy splash right off the stern.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the breakwall caught something long and low in the water, rolling past the light.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The captain's sketch: a row of dark humps, and a head on a neck at the front, held low.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A smear of thick, fishy slime on the swim platform, where something brushed the boat.",
        ),
    ),
    solvedNote = "Humps off the breakwall and slime on the boat. " +
        "Bessie has been seen on this lake since the 1790s, and the reward's still unclaimed.",
)

private val SWIMMERS_OUT = RegionalStory(
    id = "swimmers-out",
    creature = BEAR_LAKE_MONSTER,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Lifeguard Clears the Water",
        "Swimmers Out After Sighting",
        "Something Rose Off the Sandbar",
        "Campers Watch Serpent at Sunset",
        "Paddleboarder Paddles Fast for Shore",
    ),
    teasers = listOf(
        "A lifeguard whistled everybody out of the water. Nobody argued.",
        "A paddleboarder says it surfaced right beside her.",
        "Campers watched it at sunset from the bluff.",
        "Long, dark, in humps, and moving against the wind.",
        "The old-timers have a name for it, and they're not laughing.",
    ),
    lead = "A lifeguard cleared the beach yesterday afternoon after a paddleboarder came in shouting that " +
        "something long and dark had surfaced beside her. Campers on the bluff watched it again at " +
        "sunset: a row of humps, moving against the wind.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach where it was seen closest, checked end to end. Footprints from swimmers, and " +
                "nothing else.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A camper's video from the bluff: quiet, then a heavy splash that carries all the way up.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the boat ramp caught something long and low gliding in the shallows after dark.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The paddleboarder's drawing: humps in the water and a small head on a long neck.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Slime on the paddleboard's edge, clear and stringy, that smells of fish.",
        ),
    ),
    solvedNote = "Humps, a splash and slime on the board. " +
        "The lake's serpent has been a story since 1868, and it's still not finished.",
)

private val OFF_THE_POINT = RegionalStory(
    id = "off-the-point",
    creature = SEA_SERPENT,
    scene = Scene.LAKE_SHORE,
    place = "The harbor",
    headlines = listOf(
        "Lobstermen Report Serpent Off the Point",
        "Harbor Watches Something Swim Past",
        "Coast Guard Gets Strange Call",
        "Head Like a Horse, Says Fisherman",
        "Old Sea-Serpent Stories Come Back",
    ),
    teasers = listOf(
        "Two lobster boats saw the same thing an hour apart.",
        "A long body in humps, and a head held up out of the water.",
        "The harbor master took the call. He wrote it all down.",
        "One of them says it turned and looked at him.",
        "Their grandfathers told stories like this. Now they're telling them.",
    ),
    lead = "Two lobster boats called the harbor master an hour apart this morning. Both say something " +
        "swam past the point: a dark body in humps, longer than either boat, and a head held up out of " +
        "the water like a horse's.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The rocks and the beach below the point, searched at low tide. Seaweed and crab shells, and " +
                "no tracks.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The second boat's radio call: the skipper talking fast, then a huge splash, close enough to " +
                "hear over the engine.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the breakwater caught something long and low, riding in the swell.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The first skipper's drawing: humps in a long line behind a head on a raised neck.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Slime on the rocks at the point, thick and clear, well above where the waves reach.",
        ),
    ),
    solvedNote = "Humps, a raised head and slime on the rocks. " +
        "The great sea serpent has been seen off this coast since 1817.",
)

private val KAYAK_OVER_IT = RegionalStory(
    id = "kayak-over-it",
    creature = WIDE_TAHOE_TESSIE,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Kayaker Paddles Over Something Big",
        "Dark Shape Under the Clear Water",
        "\"It Went Under My Boat\"",
        "Glass-Bottom Tour Sees Long Shadow",
        "Slap on the Water, Then Nothing",
    ),
    teasers = listOf(
        "The water's clear enough to see the bottom. Something got between.",
        "A long dark back, she says, and no head she could see.",
        "A glass-bottom boat tour saw it too.",
        "It rolled, and the slap knocked her paddle.",
        "Nobody saw it come up. Everybody heard it.",
    ),
    lead = "A kayaker was crossing the shallows where the water's clear to the bottom when a long, dark " +
        "back slid under her boat. She never saw a head. Then something slapped the water so hard it " +
        "knocked her paddle out of her hands.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach where she came ashore, and the coves either side. Paddlers' footprints, and nothing " +
                "that came out of the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her helmet camera: the paddle dipping, a gasp, then one heavy slap on the water.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a dock caught a dark shadow moving under the water, the size of a canoe.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "She tried to draw it and gave up: a dark smudge under the water, with no shape she could be " +
                "sure of.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The kayak's hull, the paddle and her hands all swabbed. Nothing. Whatever it was never touched " +
                "the boat.",
        ),
    ),
    solvedNote = "A dark back, a slap and no head. " +
        "Tessie has been rolling under boats in that clear water since the 1860s.",
)

private val SMOKE_AND_NO_FIRE = RegionalStory(
    id = "smoke-and-no-fire",
    creature = WIDE_GUMBEROO,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Smoke and No Fire",
        "Logging Crew Smells Burning, Finds Nothing",
        "Something Black in the Burn Scar",
        "Skidder Shoved Off Its Tracks",
        "Fire Lookout Calls In a Bear That Isn't",
    ),
    teasers = listOf(
        "A logging crew smelled smoke all morning. There was no fire.",
        "Something pushed a skidder half off the landing overnight.",
        "The lookout says it was black and shiny, and too big for a bear.",
        "Not a hair on it, says the foreman.",
        "The old hands won't work the burn scar now.",
    ),
    lead = "A logging crew came up to the landing in an old burn this morning and found the skidder " +
        "shoved half off it, and a smell of smoke all through the trees, though there's no fire for " +
        "miles. A fire lookout saw something black and shiny go into the burn at dawn.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Round, deep prints in the ash around the skidder, as wide as a dinner plate, sunk in far " +
                "deeper than a bear's.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A recorder left out for owls ran all night. Wind, the skidder's steel groaning, and not one " +
                "sound from whatever pushed it.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera at the landing caught something long and low, black and glossy, moving along the " +
                "edge of the burn.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The lookout's drawing: hunched and heavy, its head carried low between big shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The skidder's blade swabbed and the ash sifted. No hair at all, only a smell of smoke on " +
                "everything.",
        ),
    ),
    solvedNote = "Deep prints, no hair and a smell of smoke. " +
        "The Gumberoo has been haunting the burns since the old logging days.",
)

private val UP_ON_TWO_LEGS = RegionalStory(
    id = "up-on-two-legs",
    creature = DOGMAN,
    scene = Scene.PASTURE,
    headlines = listOf(
        "It Stood Up on Two Legs",
        "Driver Says Dog Rose Up in Headlights",
        "Eyes in the Cornfield",
        "Farm Dogs Won't Leave the Porch",
        "\"Like a Wolf Walking Like a Man\"",
    ),
    teasers = listOf(
        "A driver thought it was a big dog on the shoulder. Then it stood up.",
        "Its eyes shone amber in the headlights, she says, at the height of a man's.",
        "The farm dogs howled all night and wouldn't leave the porch.",
        "It went into the corn on two legs.",
        "She's driven this road every night for years. She takes another one now.",
    ),
    lead = "A woman driving home along the edge of a cornfield late last night saw what she took for a " +
        "big dog on the shoulder. Then it stood up on its hind legs, taller than her car, and its eyes " +
        "shone amber in the headlights. It walked into the corn upright.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the field edge: prints like a dog's, but huge, with long claw marks dug in at every toe, " +
                "and only two to a stride.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The farmer's porch camera has sound: his dogs barking, then a long, deep howl from the corn " +
                "that shuts them up.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the field gate caught nothing but two pale eyes, glowing in the dark, higher off " +
                "the ground than a dog's could be.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: a man's body, but with a dog's long muzzle and tall, pointed ears.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse grey-brown hair on the fence wire where it went into the corn, at the height of her " +
                "shoulder.",
        ),
    ),
    solvedNote = "Claws, a howl and eyes at a man's height. " +
        "The Dogman has been standing up in headlights since 1987, and some say long before.",
)

private val CAT_ON_THE_ROAD = RegionalStory(
    id = "cat-on-the-road",
    creature = BLACK_PANTHER,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Big Black Cat Crosses County Road",
        "\"Its Tail Was as Long as My Leg\"",
        "Wildlife Officers Say It Can't Be",
        "Scream in the Night Wakes Farmhouse",
        "Calf Lost, Neighbors Blame a Panther",
    ),
    teasers = listOf(
        "A school bus driver saw it cross in front of her. So did the kids.",
        "Wildlife officers say there are no black panthers here. Nobody told the panther.",
        "A scream like a woman's woke a whole farmhouse.",
        "Long and low and black, and a tail that wouldn't end.",
        "It looked at the bus, says the driver, then walked on.",
    ),
    lead = "A school bus driver had to brake hard this morning when a big black cat walked across the " +
        "road in front of her, long and low, with a tail nearly as long as its body. Half the kids on " +
        "the bus saw it. Last night, the farmhouse nearby was woken by a scream.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Round prints in the ditch where it crossed: four toes and a broad pad, wider than a big dog's, " +
                "and not a single claw mark.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The farm's doorbell camera from last night: crickets, then a scream like a woman in pain, " +
                "twice.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A deer camera along the fence line caught something long and low slinking through the grass, " +
                "its tail held near the ground.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A kid on the bus drew it: a long, low cat with a little round head and a tail that goes off " +
                "the edge of the page.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Short, glossy black hairs caught on the barbed wire where it went under the fence.",
        ),
    ),
    solvedNote = "Round prints with no claws, a scream and black hair on the wire. " +
        "Science says there's no such thing as a black panther. The reports say otherwise.",
)

private val THE_TREE_DIED = RegionalStory(
    id = "the-tree-died",
    creature = HOOP_SNAKE,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Farmer Says Snake Rolled at Him",
        "The Tree Died Overnight",
        "Like a Wagon Wheel, Coming Downhill",
        "Old Story Turns Up in a Hayfield",
        "Grandpa Was Right, Says Grandson",
    ),
    teasers = listOf(
        "He jumped behind a hickory. By morning the hickory was dead.",
        "A snake with its tail in its mouth, rolling straight at him.",
        "His grandfather used to tell this one. He never believed it.",
        "The bark's split and black. The tree was fine on Monday.",
        "It let go of its tail and struck the trunk, he says.",
    ),
    lead = "A farmer cutting hay says a snake came rolling down the hill at him like a wagon wheel, its " +
        "tail in its mouth. He jumped behind a hickory, and it let go and struck the trunk with its " +
        "tail. This morning the hickory's bark is split and black, and the leaves have all dropped.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "No tracks on the hill at all. Only a single thin line pressed into the dust, straight as a " +
                "wheel rut, ending at the tree.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The tractor's dashcam has sound: the engine, then the farmer yelling, and under it, from " +
                "whatever was coming, nothing at all. Not even a hiss.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the fence row caught something low in the grass, moving fast and in a straight " +
                "line.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: a snake bent into a ring, its tail in its mouth, with lines behind it to show it " +
                "rolling.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Two shed scales by the roots of the dead tree, and a hole in the bark where something stuck it.",
        ),
    ),
    solvedNote = "A groove, not tracks, a scale and a dead tree. " +
        "The hoop snake has been rolling through farm stories for two hundred years.",
)

private val ALWAYS_BEHIND = RegionalStory(
    id = "always-behind",
    creature = HIDEBEHIND,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Hiker Says Something Followed Her All Day",
        "Every Time She Turned Around",
        "Trail Crew Member Wanders Off, Found Confused",
        "\"It Was Always Behind a Tree\"",
        "Rangers Puzzled by Lost Hour",
    ),
    teasers = listOf(
        "Every time she turned round, something slipped behind a tree.",
        "A trail crew member walked off the job and can't say why.",
        "She never saw it. She just knew it was there.",
        "The crew found him an hour later, sitting by a stump.",
        "The old loggers had a name for what follows you.",
    ),
    lead = "A hiker came off the trail shaking yesterday. All afternoon, she says, something followed her, " +
        "and every time she turned round it was gone behind a tree. The same day a trail crew member " +
        "walked away from his crew and was found an hour later, unable to say where he'd been.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Behind the trees she pointed out, the duff is soft and smooth. Not a print, not a scuff. " +
                "Nothing stood there.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "She'd been recording birdsong. Birds, her footsteps, and then the birds stop, and there's no " +
                "other sound at all.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the trail caught her walking past, and a moment later a dark shadow falling " +
                "across the path from behind a trunk, with nothing there to cast it.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "She tried to draw it and couldn't: a dark smudge at the edge of a tree. She never saw it " +
                "straight on.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The bark of every tree she pointed to, swabbed. No hair, no scratch, nothing at all.",
        ),
    ),
    solvedNote = "No tracks, no sound, and only ever a shadow. " +
        "The Hidebehind has followed people through the woods since the logging camps.",
)

private val KNOCKING_IN_THE_ADIT = RegionalStory(
    id = "knocking-in-the-adit",
    creature = TOMMYKNOCKER,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Knocking in the Old Mine",
        "Light Moves in Shaft Closed Since the War",
        "Hikers Hear Hammers Underground",
        "\"Somebody's Still Working Down There\"",
        "Old Miners Say: Listen to the Knocking",
    ),
    teasers = listOf(
        "The mine's been sealed for eighty years. Somebody's knocking.",
        "A lamp moved in the tunnel mouth, waist-high, and went back in.",
        "The old-timers say the knocking means get out.",
        "Two hikers heard tapping in the rock, steady as a clock.",
        "The grate's still locked. The light was inside.",
    ),
    lead = "Two hikers stopped at the entrance to an old mine up on the ridge, sealed with a grate for " +
        "eighty years, and heard tapping in the rock: steady, like a hammer. Then a light moved inside the " +
        "tunnel, carried low, and went back into the dark.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Just inside the grate, in the dust: little, light prints, child-sized, close together, going " +
                "in and none coming out.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A recorder left at the grate overnight: hours of quiet, then sharp taps on rock, evenly " +
                "spaced, for most of a minute.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the grate caught a small, glowing light deep in the tunnel, too low for anyone " +
                "to be carrying it.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One hiker's drawing: man-shaped, but small and stooped, in a hood, with a long beard. It came " +
                "up to her waist.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The dust inside the grate swept and the rock swabbed. No hair, no blood, nothing from any " +
                "animal. Only an old pasty crust on a ledge, left by some miner long ago.",
        ),
    ),
    solvedNote = "Small prints, a lamp and a steady knocking. " +
        "The Tommyknockers came over with the Cornish miners, and some of them never left.",
)

/** Every story in this file. Cases.kt adds the Southwest's. */
internal val WIDE_STORIES: List<RegionalStory> = listOf(
    TREE_STAND,
    LIFTED_THE_CALF,
    SANG_BACK,
    UNDER_THE_TRESTLE,
    OFF_THE_FERRY,
    PAST_THE_BREAKWALL,
    SWIMMERS_OUT,
    OFF_THE_POINT,
    KAYAK_OVER_IT,
    SMOKE_AND_NO_FIRE,
    UP_ON_TWO_LEGS,
    CAT_ON_THE_ROAD,
    THE_TREE_DIED,
    ALWAYS_BEHIND,
    KNOCKING_IN_THE_ADIT,
)
