package app.unconfirmed.shared

/*
 * Alaska's and Hawaii's field guides. Research notes and sources are in build/alaska_hawaii.md.
 * Neither state's guide takes in any of the Southwest's creatures, so these use WIDE_CREATURES
 * directly, which also keeps this file from reading anything in Cases.kt while it's set up.
 *
 * Alaska: Bigfoot (regional), the Thunderbird, Illie and Caddy.
 *   - Footprints: deep (Bigfoot), shallow (Thunderbird), none (Illie, Caddy)
 *   - Field recording: a howl, a screech, a splash, nothing: one each
 *   - Trail cam: upright (Bigfoot), a shadow (Thunderbird, Illie), low (Caddy)
 *   - Witness sketch: man-shaped, hunched, a smudge, humps: one each
 *   - Sample: hair, a feather, nothing, slime: one each
 *
 * Hawaii: the Menehune, the island big cat, the Nuuanu caiman, the kraken and the Pacific sea serpent.
 *   - Footprints: shallow (Menehune), padded (cat), clawed (caiman), none (kraken, serpent)
 *   - Field recording: stone knocking on stone (Menehune), a scream (cat), a splash (caiman,
 *     serpent), nothing (kraken)
 *   - Trail cam: upright (Menehune), low (cat, caiman, serpent), a shadow (kraken)
 *   - Witness sketch: man-shaped, a cat, a lizard, tentacles, humps: one each
 *   - Sample: nothing (Menehune), hair (cat), a scale (caiman), slime (kraken, serpent)
 * In both, any two differ on at least three types, so any three clues settle a case.
 */

private val ALASKA_THUNDERBIRD = Suspect(
    "thunderbird",
    "Thunderbird",
    "In October 2002 people in Togiak and Manokotak, in Southwest Alaska, reported a bird the size of " +
        "a small plane. A heavy-equipment operator first took it for an old Otter floatplane, and a " +
        "pilot saw it from his cockpit. Biologists believed the witnesses but not the wingspan. It " +
        "lands in long hops, leaving shallow talon marks far apart, and hunches with its head low. " +
        "A piercing screech, and feathers longer than any eagle's.",
    traits(PRINTS_SHALLOW, CALL_SHRILL, CAM_SHADOW, SKETCH_HUNCHED, SAMPLE_FEATHER),
)

private val ILIAMNA_LAKE_MONSTER = Suspect(
    "iliamna-lake-monster",
    "Iliamna Lake Monster",
    "Illie, of Iliamna, Alaska's biggest lake. In 1942 two pilots reported huge, dull-silver shapes " +
        "like fish below them, and from the shore it's described as dark. Twenty to thirty feet long. " +
        "It never comes ashore. From the air it's only a long shadow under the water, and from a boat " +
        "a heavy slap as it goes down. Sturgeon, belugas and the lake's own freshwater seals have all " +
        "been put forward.",
    traits(PRINTS_NONE, CALL_SPLASH, CAM_SHADOW, SKETCH_SMUDGE, SAMPLE_NOTHING),
)

private val ALASKA_CADBOROSAURUS = Suspect(
    "cadborosaurus",
    "Cadborosaurus",
    "Caddy, the Pacific coast's serpent, reported from Southeast Alaska down to Oregon, with most " +
        "sightings around Vancouver Island. A long body in humps, and a horse-like head on a long " +
        "neck. It never comes ashore, so there are no tracks. Silent apart from a blow at the surface, " +
        "and it leaves a fishy slime.",
    traits(PRINTS_NONE, CALL_NONE, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
)

internal val ALASKA_SUSPECTS =
    fieldGuide("AK", listOf(ALASKA_THUNDERBIRD, ILIAMNA_LAKE_MONSTER, ALASKA_CADBOROSAURUS), WIDE_CREATURES)

private val ISLAND_BIG_CAT = Suspect(
    "island-big-cat",
    "Island Big Cat",
    "No big cat lives wild in Hawaii, and none has ever been caught. But in 2003 Maui's wildlife " +
        "officers set traps above Olinda for months after six people described a dark, long-tailed " +
        "cat the size of a leopard, and a deer was found killed in a gulch. The Big Island had its own " +
        "reports in 2022, and the hills above Manoa in 2026. Round prints with no claw marks, a scream " +
        "in the night, and short dark hair.",
    traits(PRINTS_PADDED, CALL_SHRILL, CAM_LOW, SKETCH_CAT, SAMPLE_HAIR),
)

private val NUUANU_CAIMAN = Suspect(
    "caiman",
    "Nuuanu Caiman",
    "In the early 1980s a caiman, a small cousin of the alligator, was seen again and again around " +
        "Nuuanu Reservoir on Oahu, most likely a pet someone let go. State crews baited a trap on a " +
        "floating platform for months and never caught it, and then it was gone. Clawed prints either " +
        "side of a tail drag, a splash as it slides in, and a shed scale or two.",
    traits(PRINTS_CLAWED, CALL_SPLASH, CAM_LOW, SKETCH_LIZARD, SAMPLE_SCALE),
)

private val KRAKEN = Suspect(
    "kraken",
    "Kraken",
    "The sailors' kraken was most likely the giant squid, and they live in the deep water off the " +
        "islands: in 2020 scientists described an oceanic whitetip shark off Kona scarred by the " +
        "suckers of some huge cephalopod. It comes up at night. No tracks, no sound, only a vast " +
        "shadow under the boat, arms over the rail, and slime where they touched.",
    traits(PRINTS_NONE, CALL_NONE, CAM_SHADOW, SKETCH_TENTACLES, SAMPLE_SLIME),
)

private val PACIFIC_SEA_SERPENT = Suspect(
    "pacific-sea-serpent",
    "Pacific Sea Serpent",
    "In 1855 American papers reported that a schooner's crew had seen a sea serpent a few degrees " +
        "east of the islands, and Pacific ships kept reporting them for the rest of the century. A " +
        "long dark body in humps and a raised head. It leaves no tracks, only a splash and slime.",
    traits(PRINTS_NONE, CALL_SPLASH, CAM_LOW, SKETCH_HUMPS, SAMPLE_SLIME),
)

/** From Hawaiian tradition rather than modern sightings: see build/alaska_hawaii.md. */
private val MENEHUNE = Suspect(
    "menehune",
    "Menehune",
    "The small people of Hawaiian tradition, master builders who work only at night and only " +
        "unwatched. On Kauai, the Alekoko fishpond's wall and the Kikiaola ditch at Waimea are said " +
        "to be their work, each finished in a single night, and the ditch is said to have been paid " +
        "for in shrimp. Small, light prints, the click of stone on stone in the dark, and by " +
        "morning nothing but the finished wall.",
    traits(PRINTS_SHALLOW, CALL_KNOCK, CAM_UPRIGHT, SKETCH_MAN, SAMPLE_NOTHING),
)

internal val HAWAII_SUSPECTS =
    fieldGuide("HI", listOf(MENEHUNE, ISLAND_BIG_CAT, NUUANU_CAIMAN, KRAKEN, PACIFIC_SEA_SERPENT), WIDE_CREATURES)

// ---- Alaska's cases --------------------------------------------------------------------------------

private val SIZE_OF_AN_OTTER = CaseDefinition(
    id = "size-of-an-otter",
    state = "AK",
    scene = Scene.PASTURE,
    place = "The village airstrip",
    headlines = listOf(
        "Bird the Size of a Floatplane",
        "Villagers Told to Keep Children Inside",
        "Pilot Banks Away From Giant Bird",
        "\"I Thought It Was an Otter\"",
        "Biologists Doubt the Wingspan, Not the Witnesses",
    ),
    teasers = listOf(
        "A heavy-equipment operator thought it was an old Otter floatplane. Then it flapped.",
        "Somebody got on the radio and told the village to bring the kids in.",
        "A pilot says it flew alongside him a thousand feet up.",
        "Biologists say it must be an eagle. Nobody who saw it agrees.",
        "It came down on the tundra past the airstrip and hopped.",
    ),
    lead = "A heavy-equipment operator working by the airstrip saw what he took for an old Otter " +
        "floatplane coming in low, until it flapped its wings. He radioed the village to bring the " +
        "children inside. Later that day a pilot reported the same bird from his cockpit.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the tundra past the airstrip: talon marks, light in the moss and far apart, as if it " +
                "landed in long hops.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The radio call itself: the operator's voice, his engine, and over it a piercing, cackling " +
                "screech.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the fuel shed caught a shadow sweeping over the whole airstrip, the size of a " +
                "plane's.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The operator's drawing, of when it landed: hunched, its head sunk low between high shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A single dark feather on the gravel at the end of the strip, longer than his arm.",
        ),
    ),
    suspects = ALASKA_SUSPECTS,
    answerId = ALASKA_THUNDERBIRD.id,
    solvedNote = "Shallow hops, a screech and a feather as long as an arm. " +
        "Southwest Alaska knows what it saw in 2002, whatever the biologists say.",
)

private val SILVER_UNDER_THE_FLOATS = CaseDefinition(
    id = "silver-under-the-floats",
    state = "AK",
    scene = Scene.LAKE_SHORE,
    place = "The big lake",
    headlines = listOf(
        "Silver Under the Floats",
        "Floatplane Pilot Circles Back for a Second Look",
        "Fishing Guide Says Something Followed the Skiff",
        "Something Longer Than the Boat",
        "Old Reward for Illie Still Stands",
    ),
    teasers = listOf(
        "A floatplane pilot saw long, dull-silver shapes under the water and circled back.",
        "A fishing guide says something bigger than his skiff came up behind it.",
        "From the air it looked like a fish. A fish thirty feet long.",
        "The slap of its tail soaked the stern.",
        "People have been seeing it in this lake since before the war.",
    ),
    lead = "A floatplane pilot coming in over the lake yesterday saw a long, dull-silver shape under " +
        "the water, longer than his plane, and circled back to look. It was gone. An hour later a " +
        "fishing guide says something came up behind his skiff and went down with a slap that soaked " +
        "the stern.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The gravel beach nearest where it was seen, walked end to end. Bear tracks, gulls, and " +
                "nothing that came out of the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The guide's phone was recording a client's catch: the outboard, laughing, then one huge " +
                "slap on the water and a lot of shouting.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the floatplane dock caught a long shadow sliding under the water past the floats.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The pilot tried to draw it and gave up: a dark, smudged shape under the water, long and with " +
                "no edges he could be sure of.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The skiff's stern and the dock pilings swabbed. Nothing. Whatever it was never touched them.",
        ),
    ),
    suspects = ALASKA_SUSPECTS,
    answerId = ILIAMNA_LAKE_MONSTER.id,
    solvedNote = "A long shadow, a slap and nothing left behind. " +
        "Illie has been glimpsed from planes and boats on that lake since 1942.",
)

// ---- Hawaii's cases --------------------------------------------------------------------------------

private val DEER_IN_THE_GULCH = CaseDefinition(
    id = "deer-in-the-gulch",
    state = "HI",
    scene = Scene.PASTURE,
    place = "Upcountry",
    headlines = listOf(
        "Deer Found Killed in Upcountry Gulch",
        "Traps Set for Cat That Isn't Supposed to Exist",
        "\"Bigger Than Any Dog\"",
        "Ranch Hand Sees Long Tail in the Grass",
        "Wildlife Officers: Call Us, Don't Chase It",
    ),
    teasers = listOf(
        "A deer was found killed in a gulch, and it wasn't dogs, say the officers.",
        "There are no big cats in the islands. Six people say otherwise.",
        "A ranch hand saw a dark cat cross the pasture, tail as long as its body.",
        "The traps are set. Nothing's gone in yet.",
        "A scream woke the whole subdivision at three in the morning.",
    ),
    lead = "A deer was found killed in a gulch above the pastures this week, and wildlife officers say " +
        "it doesn't look like dogs. A ranch hand saw a dark cat the size of a leopard cross the pasture " +
        "the evening before, and a scream woke half the houses up the road that night.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the mud at the bottom of the gulch: round prints, four toes and a broad pad, wider than " +
                "a big dog's and with no claw marks.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A doorbell camera up the road: frogs, then a scream like a woman's, twice, close by.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the pasture fence caught something long and low moving through the grass, tail " +
                "held near the ground.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The ranch hand's drawing: a long, low cat with a small round head and a tail as long as its " +
                "body.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Short, dark hairs on the fence wire where something went under, too coarse for a house cat.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = ISLAND_BIG_CAT.id,
    solvedNote = "Round prints, a scream and dark hair on the wire. " +
        "Nobody has ever caught a big cat in the islands, but nobody's stopped seeing one.",
)

private val TRAP_ON_THE_RESERVOIR = CaseDefinition(
    id = "trap-on-the-reservoir",
    state = "HI",
    scene = Scene.LAKE_SHORE,
    place = "The reservoir",
    headlines = listOf(
        "Something With Teeth in the Reservoir",
        "Floating Trap Comes Up Empty Again",
        "Hikers Swear They Saw a Gator",
        "Eyes on the Water at Dusk",
        "Agriculture Crews Bait the Platform",
    ),
    teasers = listOf(
        "Two hikers say a small alligator slid off the bank in front of them.",
        "There's a trap on a raft in the middle of the water. Nothing's gone in.",
        "At dusk, two eyes on the water, and then just ripples.",
        "Somebody's pet got loose, say the officers. Years ago, maybe.",
        "The kids at the trailhead have a name for it already.",
    ),
    lead = "Two hikers at the reservoir say something like a small alligator slid off the bank in front " +
        "of them and went under. State agriculture crews have baited a trap on a floating platform, " +
        "but it's come up empty every morning.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the mud bank: clawed, splayed prints either side of a long, smooth drag mark, going into " +
                "the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The hikers' video: birds, one of them laughing, then a heavy splash and both of them " +
                "yelling.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the trap platform caught something long and low lying at the waterline, then " +
                "gone.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One hiker's drawing: like an alligator, long and flat, with a long snout and a heavy tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A single hard, ridged scale in the mud at the top of the drag mark.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = NUUANU_CAIMAN.id,
    solvedNote = "A tail drag, a splash and a scale. " +
        "The reservoir had a caiman once, in the 1980s, and the trap never caught it.",
)

private val ARMS_OVER_THE_RAIL = CaseDefinition(
    id = "arms-over-the-rail",
    state = "HI",
    scene = Scene.LAKE_SHORE,
    place = "The harbor",
    headlines = listOf(
        "Arms Over the Rail",
        "Night Boat Comes In With Slime on the Deck",
        "Deckhand: \"It Was Under Us the Whole Time\"",
        "Something Huge Under the Lights",
        "Fishermen Cut Lines and Run for Harbor",
    ),
    teasers = listOf(
        "A night boat came in early, its deck slick with slime.",
        "Something came up under the lights, says the captain, and kept coming.",
        "An arm as thick as a man's leg came over the rail, says the deckhand.",
        "They cut every line and ran.",
        "The sonar showed something bigger than the boat, rising.",
    ),
    lead = "A night fishing boat came into the harbor early with its crew shaken and its deck slick. " +
        "They'd been fishing under lights offshore when the sonar showed something bigger than the boat " +
        "rising under them. Then an arm as thick as a man's leg came over the rail.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Nothing on the deck but water and slime. Whatever came aboard didn't walk on it.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The wheelhouse camera has sound: the generator, men shouting, the hull creaking, and from " +
                "whatever was there, not a sound.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The masthead camera, pointed down, caught a vast dark shadow spreading under the boat's lights.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The deckhand's drawing: long arms curling up out of the water, one of them over the rail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Thick, clear slime scraped off the rail, and the round marks of suckers in it.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = KRAKEN.id,
    solvedNote = "No sound, a shadow under the lights and sucker marks on the rail. " +
        "The giant squid lives in the deep water off the islands. Now and then it comes up.",
)

private val WALL_BY_MORNING = CaseDefinition(
    id = "wall-by-morning",
    state = "HI",
    scene = Scene.LAKE_SHORE,
    place = "The old fishpond",
    headlines = listOf(
        "The Wall Was Fixed by Morning",
        "Nobody Rebuilt the Fishpond Wall. Somebody Did.",
        "Volunteers Arrive to Find the Job Done",
        "Stone on Stone in the Dark",
        "Kupuna Says: Don't Go Down There at Night",
    ),
    teasers = listOf(
        "The storm knocked a gap in the fishpond wall. By sunrise it was closed.",
        "The volunteer crew showed up with gloves and found nothing to do.",
        "Neighbors heard stones clicking together all night, and never saw a light.",
        "Every stone fitted, says the crew leader, better than they'd have done it.",
        "An elder says to leave some shrimp out, and not to go and look.",
    ),
    lead = "Last week's storm knocked a gap in the old fishpond wall, and a volunteer crew was due to " +
        "rebuild it this morning. They found it already closed, every stone fitted tight. Neighbors " +
        "say they heard stone clicking on stone all night, but nobody saw a light or a truck.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the wet sand along the wall: small, light footprints, dozens of them, back and forth, " +
                "no bigger than a child's.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A neighbor's phone left recording on the lanai: hours of surf, then stone tapping on stone, " +
                "steady and close together, until just before dawn.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the sluice gate caught small upright figures in a line along the wall, passing " +
                "something from hand to hand.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A boy who looked out his window drew it: small, stocky people, man-shaped, each with a stone " +
                "on the shoulder.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The new stones and the sand swabbed. No hair, no blood, no thread, nothing. Only the wall, " +
                "finished.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = MENEHUNE.id,
    solvedNote = "Small footprints, stone on stone and a wall finished before dawn. " +
        "The Menehune still keep their old rule: the work gets done as long as nobody watches.",
)

// Second stories, which the paper rotates with the first: see runToday.

private val FISH_CAMP = CaseDefinition(
    id = "fish-camp",
    state = "AK",
    scene = Scene.LAKE_SHORE,
    place = "The fish camp",
    headlines = listOf(
        "Something Landed on the Fish Rack",
        "Fish Camp Loses a Day's Salmon",
        "\"Its Wings Covered the Whole Smokehouse\"",
        "Elders Tell the Kids to Stay Close",
        "Bird Took a King Salmon Whole",
    ),
    teasers = listOf(
        "Something came down on the drying racks and took a king salmon whole.",
        "Its wings covered the whole smokehouse, says the family.",
        "It perched on the rack, hunched, and looked at them.",
        "The elders brought the kids inside, the way they did in 2002.",
        "A screech carried across the river, and then it was gone.",
    ),
    lead = "A family at their summer fish camp on the river say something came down on the salmon drying racks " +
        "yesterday: a bird with wings wider than the smokehouse. It perched, hunched with its head low, then " +
        "lifted off with a king salmon in its talons. Its shadow went right across the camp.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the gravel by the racks: talon marks, light and far apart, as if it came down in long hops.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A teenager's video: dogs barking, a piercing, cackling screech, then the elders shouting for the " +
                "kids.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the smokehouse caught only a shadow, sweeping across the whole camp.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The grandmother's drawing: hunched on the rack, its head sunk low between high shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A dark feather by the racks, longer than her arm.",
        ),
    ),
    suspects = ALASKA_SUSPECTS,
    answerId = ALASKA_THUNDERBIRD.id,
    solvedNote = "Light talon marks, a screech and a feather by the racks. The great bird of the Southwest still " +
        "comes for salmon.",
)

private val SET_NETS = CaseDefinition(
    id = "set-nets",
    state = "AK",
    scene = Scene.LAKE_SHORE,
    place = "The big lake",
    headlines = listOf(
        "Set-Net Dragged Half a Mile",
        "Fishermen Watch Their Net Swim Away",
        "\"Something Bigger Than a Seal Hit It\"",
        "Long Silver Shape Seen From the Skiff",
        "Illie Takes the Sockeye Run",
    ),
    teasers = listOf(
        "A family's set-net was dragged half a mile up the lake, buoys and all.",
        "A long, dull-silver shape under the water, longer than their skiff.",
        "It went down with a slap that soaked them.",
        "No seal tears a net like that, says the grandfather.",
        "The pilots have seen it. Now the net fishermen have too.",
    ),
    lead = "A family set-netting for sockeye on Iliamna Lake watched their net start moving: the buoys towed half " +
        "a mile up the lake against the wind. When they caught up in the skiff, a long, dull-silver shape was " +
        "under the water beside the net, longer than the boat. It went down with a slap that soaked all of them.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The gravel beach where the net was staked, walked end to end. Bear tracks and boots, nothing from the " +
                "lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A phone video from the skiff: the outboard, shouting, and one huge slap on the water.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the floatplane dock caught a long shadow sliding under the water, longer than the floats.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The grandfather tried to draw it: a long, smudged shape under the water, no edges he could swear to.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The net and the buoys hauled up and checked. Nothing on them at all, not even a scale.",
        ),
    ),
    suspects = ALASKA_SUSPECTS,
    answerId = ILIAMNA_LAKE_MONSTER.id,
    solvedNote = "A shadow, a slap and an empty net. Illie has been spotted from planes since 1942, and now from " +
        "the nets.",
)

private val PROTEA_FARM = CaseDefinition(
    id = "protea-farm",
    state = "HI",
    scene = Scene.PASTURE,
    place = "Upcountry",
    headlines = listOf(
        "Flower Farmer Sees Black Cat in the Proteas",
        "Upcountry Farm Loses a Goat",
        "\"A Leopard, Walking Down My Rows\"",
        "Wildlife Officers Set Traps Again",
        "Scream Above the Flower Fields",
    ),
    teasers = listOf(
        "A flower farmer says a cat the size of a leopard walked between her protea rows.",
        "One of her goats is missing, and the fence wire has black hairs on it.",
        "A scream from the gulch woke the farm at midnight.",
        "Wildlife officers set traps in 2003. They're setting them again.",
        "Its tail was as long as its body, she says.",
    ),
    lead = "A farmer growing protea upcountry saw a dark cat the size of a leopard walk slowly down between her " +
        "rows at dusk, its long tail held low. That night a scream came up from the gulch, and in the morning one " +
        "of her goats was gone from its pen.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the red dirt between the rows: round prints, four toes and a broad pad, with no claw marks.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The farm's camera has sound: coqui frogs, then a scream like a woman's from the gulch.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the goat pen caught something long and low slinking through the grass, tail near the " +
                "ground.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: a long, low cat with a small round head and a very long tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Short, dark hairs caught on the goat pen's wire, too coarse for a house cat.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = ISLAND_BIG_CAT.id,
    solvedNote = "Round prints, a scream and dark hair on the wire. No big cat lives wild in Hawaii, they say. " +
        "Something does.",
)

private val TARO_PATCH = CaseDefinition(
    id = "taro-patch",
    state = "HI",
    scene = Scene.LAKE_SHORE,
    place = "The taro patches",
    headlines = listOf(
        "Taro Farmer Sees Small Gator in the Patch",
        "Something Slid Into the Flooded Patch",
        "\"Long Snout, Heavy Tail\"",
        "Agriculture Crews Called to the Valley",
        "Not a Mongoose, Says Farmer",
    ),
    teasers = listOf(
        "A taro farmer says something like a small alligator slid off the bank into his patch.",
        "Agriculture crews have been called out. The trap's empty so far.",
        "It went under with a splash and never came up.",
        "A long snout, a heavy tail, and scales.",
        "Somebody let one go in the eighties, too. It was never caught.",
    ),
    lead = "A taro farmer working his loi in a windward valley says something long and low, like a small " +
        "alligator, slid off the bank into the flooded patch in front of him and vanished under the leaves with a " +
        "heavy splash. State agriculture crews have set a trap. It's been empty every morning.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "On the bank of the patch: clawed, splayed prints either side of a smooth drag mark, going into the " +
                "water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His nephew's video: the farmer yelling, then a heavy splash among the taro leaves.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The trap's camera caught something long and low lying at the edge of the water, then gone.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The farmer's drawing: long and flat like an alligator, with a long snout and a heavy tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A hard, ridged scale in the mud at the top of the drag mark.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = NUUANU_CAIMAN.id,
    solvedNote = "Clawed prints, a splash and a scale. Someone's old pet caiman may have had a family.",
)

private val NIGHT_DIVE = CaseDefinition(
    id = "night-dive",
    state = "HI",
    scene = Scene.LAKE_SHORE,
    place = "Off Kona",
    headlines = listOf(
        "Night Divers See Arms Beyond the Lights",
        "Manta Dive Cut Short Off Kona",
        "\"Something Bigger Than the Boat Was Under Us\"",
        "Dive Boat Comes In With Slime on the Ladder",
        "The Deep Came Up to Look",
    ),
    teasers = listOf(
        "A manta ray night dive was called off when something huge rose below the lights.",
        "Arms as thick as a man's waist, says the divemaster, at the edge of the light.",
        "There wasn't a sound, the divers say. Just a shadow spreading.",
        "The boat's ladder came up coated in slime, with sucker marks in it.",
        "Giant squid live in the deep water off the islands. This one came up.",
    ),
    lead = "A dive boat running a manta ray night dive off Kona brought its divers up early. Something huge rose " +
        "out of the deep below the lights, they say: a vast shadow spreading under them, and at the edge of the " +
        "light, arms as thick as a man's waist, curling. One reached the boat's ladder.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Nothing on the deck or the swim step but seawater and slime. Whatever came up never walked aboard.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The divemaster's camera: bubbles, divers' signals clanking, and from whatever was there, not a sound.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The boat's down-light camera caught a vast dark shadow spreading out under the lights.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A diver's drawing: long arms curling up out of the dark water, one wrapped round the ladder.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Thick, clear slime on the ladder rungs, and the round marks of suckers in it.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = KRAKEN.id,
    solvedNote = "A shadow, no sound and sucker marks on the ladder. The deep water off the islands keeps its big " +
        "ones.",
)

private val TRAIL_STEPS = CaseDefinition(
    id = "trail-steps",
    state = "HI",
    scene = Scene.RIDGE,
    place = "The valley trail",
    headlines = listOf(
        "Washed-Out Trail Rebuilt Overnight",
        "Stone Steps Appear on the Valley Trail",
        "\"Every Stone Fitted Like a Puzzle\"",
        "Trail Crew Arrives to Find the Work Done",
        "Neighbors Heard Tapping Till Dawn",
    ),
    teasers = listOf(
        "A storm washed out the valley trail. By morning it had stone steps.",
        "The trail crew came with shovels and found nothing left to do.",
        "Neighbors heard stone tapping on stone all night.",
        "Every step fitted tight, with no mortar and no tool marks.",
        "Some jobs get done only when nobody watches.",
    ),
    lead = "A storm washed out a stretch of the valley trail last week, and a volunteer crew came up this morning " +
        "to rebuild it. They found it already done: a flight of stone steps, every stone fitted tight, with no " +
        "mortar. Families living below the trail say they heard stone tapping on stone all night, but saw no " +
        "lights.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the wet earth beside the new steps: dozens of small, light footprints, back and forth, no bigger " +
                "than a child's.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A neighbor's phone left recording on the lanai: rain, then steady tapping of stone on stone until " +
                "just before dawn.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A hunter's camera on the trail caught small upright figures in a line, passing stones from hand to " +
                "hand.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A girl who looked out her window drew them: small, stocky people, man-shaped, carrying stones.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The steps and the earth swabbed. No hair, no thread, nothing at all. Only the finished steps.",
        ),
    ),
    suspects = HAWAII_SUSPECTS,
    answerId = MENEHUNE.id,
    solvedNote = "Small footprints, stone on stone and steps by morning. The Menehune still finish their work in a " +
        "single night.",
)

internal val ALASKA_HAWAII_CASES = listOf(
    SIZE_OF_AN_OTTER,
    SILVER_UNDER_THE_FLOATS,
    DEER_IN_THE_GULCH,
    TRAP_ON_THE_RESERVOIR,
    ARMS_OVER_THE_RAIL,
    WALL_BY_MORNING,
    FISH_CAMP,
    SET_NETS,
    PROTEA_FARM,
    TARO_PATCH,
    NIGHT_DIVE,
    TRAIL_STEPS,
)
