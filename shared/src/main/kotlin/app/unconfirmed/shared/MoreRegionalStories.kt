package app.unconfirmed.shared

/*
 * Second and third stories for the creatures in RegionalCases.kt, so a state's paper doesn't run the
 * same story about a creature every day: see runToday. The most widely reported creatures get
 * three. Like the first stories, each runs in every state its creature reaches, so the words name
 * no place.
 *
 * Nothing here may refer to the vals in Cases.kt, which reads these lists while it's being set up.
 */

// ---- Bigfoot ---------------------------------------------------------------------------------------

private val STEPPED_OVER_THE_RAIL = RegionalStory(
    id = "stepped-over-the-rail",
    creature = WIDE_BIGFOOT,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Plow Driver Sees Figure Cross the Highway",
        "It Stepped Over the Guardrail",
        "Night Shift Sighting on the Mountain Road",
        "\"Too Tall to Be a Man\"",
        "Road Crew Finds Tracks in the Snowbank",
    ),
    teasers = listOf(
        "A plow driver says it crossed both lanes in four strides.",
        "It didn't climb the guardrail. It stepped over it.",
        "His blade lights caught it from the side. It never looked round.",
        "The road crew went back at daylight and found the tracks.",
        "He's driven that road twenty winters. He'd never stopped the truck before.",
    ),
    lead = "A snowplow driver working the mountain road before dawn says something walked out of the trees " +
        "into his lights, crossed both lanes in a few long strides, and stepped over the guardrail on the far " +
        "side without breaking stride. It was taller than the cab of his truck, he says, and dark all over.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the snowbank past the guardrail: five-toed prints, wide and nearly twice the length of a boot, " +
                "sunk deep enough to show the gravel underneath.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The truck's radio was open to dispatch. Under his voice, from up the slope, a long, low howl that " +
                "rises and falls away.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The road department's camera on the pass caught a dark shape walking upright along the shoulder, " +
                "taller than the reflector posts.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn on the back of his route sheet: man-shaped, wide in the shoulders, with long arms swinging " +
                "low.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark, coarse hairs caught on the bolt of the guardrail where it stepped over.",
        ),
    ),
    solvedNote = "Deep prints in the snowbank and hair on the rail. " +
        "Bigfoot has been crossing roads in front of night drivers for as long as there have been roads.",
)

private val BERRY_PICKERS = RegionalStory(
    id = "berry-pickers",
    creature = WIDE_BIGFOOT,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Berry Pickers Leave Their Buckets Behind",
        "Something Else Was Picking the Ridge",
        "Family Outing Ends in a Run for the Car",
        "Bushes Stripped Above Head Height",
        "\"It Was Eating With Its Hands\"",
    ),
    teasers = listOf(
        "A family picking berries on the ridge saw something else doing the same.",
        "It stood up out of the bushes, and it kept standing up.",
        "They left three full buckets and ran for the car.",
        "Whatever it was had stripped the bushes higher than any of them could reach.",
        "Two knocks from the trees, then a third, closer.",
    ),
    lead = "A family picking wild berries on the ridge heard branches snapping on the far side of the patch. " +
        "Then something stood up out of the bushes, dark and hairy and far taller than the father, pulling " +
        "berries off with its hands. It looked at them. They left their buckets and ran.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the bare dirt between the bushes: deep, broad, five-toed prints, each one pressed in hard, " +
                "spaced wider than a tall man's stride.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The daughter's phone was still filming in her pocket. Their running, and behind them, two hard " +
                "knocks of wood on wood and a long howl off the ridge.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A hunter's camera at the top of the patch: a figure walking upright through the bushes, the " +
                "bushes only reaching its waist.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The father drew it back at the car: man-shaped and huge, with heavy shoulders and hardly any neck.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Long, dark hairs twisted into the thorns of the tallest bush, well above anyone's head.",
        ),
    ),
    solvedNote = "Deep prints, knocks and hair in the thorns. " +
        "Bigfoot has a sweet tooth, and berry season is when people find out.",
)

// ---- Thunderbird -----------------------------------------------------------------------------------

private val ON_THE_TURBINE = RegionalStory(
    id = "on-the-turbine",
    creature = WIDE_THUNDERBIRD,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Wind Tech Finds Something Perched on the Turbine",
        "Bird Bigger Than the Blade Tip",
        "Turbine Crew Calls It a Day",
        "\"It Took Off and the Whole Nacelle Shook\"",
        "Wind Farm Shuts Down a Row for Inspection",
    ),
    teasers = listOf(
        "A repair tech climbed three hundred feet and found something already up there.",
        "It sat on the nacelle hunched like a vulture, only far bigger.",
        "When it took off, he says, the whole tower swayed.",
        "The crew shut the row down. Nobody's said why.",
        "Its shadow ran across the fields for half a mile.",
    ),
    lead = "A wind turbine technician climbed to the top of a tower this morning and found a bird perched " +
        "on the housing, hunched, its head low between its shoulders. When it opened its wings they ran " +
        "longer than his truck. It dropped off the edge, and its shadow went across the fields below him.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the mud around the tower base: talon marks, light and far apart, as if it came down in long " +
                "hops.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His helmet radio picked it up as he called it in: a piercing, cackling screech, right above him.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The tower's own camera caught no bird, only a vast shadow crossing the blades and the ground.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch: perched and hunched, shoulders high, head pulled down between them.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A dark flight feather caught in the access hatch, longer than his arm from wrist to shoulder.",
        ),
    ),
    solvedNote = "Shallow hops, a screech and a feather in the hatch. " +
        "The Thunderbird has found a new perch, and it likes the view.",
)

private val GRAIN_ELEVATOR = RegionalStory(
    id = "grain-elevator",
    creature = WIDE_THUNDERBIRD,
    scene = Scene.RIVER_TOWN,
    place = "The grain elevator",
    headlines = listOf(
        "Something Roosting on the Grain Elevator",
        "Co-op Workers Watch It Leave at Dawn",
        "Pigeons Gone From the Elevator",
        "Dark Wings Over Main Street",
        "Town Asks: What's Up on the Elevator?",
    ),
    teasers = listOf(
        "The pigeons that lived on the elevator for years have gone.",
        "Two co-op workers saw it drop off the top and glide over town.",
        "It sat up there all night, they say, hunched against the wind.",
        "A screech woke half of Main Street before sunrise.",
        "A dog was carried halfway across the rail yard, its owner says.",
    ),
    lead = "Two workers opening up the grain co-op at dawn saw something huge sitting hunched on top of the " +
        "elevator. As they watched, it dropped off the edge, spread its wings wider than the loading dock, " +
        "and glided low over the rooftops toward the river. The pigeons that roosted there are all gone.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the spilled grain on the elevator's catwalk: talon marks, barely pressed in, and far apart.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A security camera on the co-op office recorded it: a shrill, cackling screech that echoes off the " +
                "bins.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the rail yard caught nothing solid, only a shadow sweeping over the tracks, wider " +
                "than a boxcar.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "One worker drew it as it sat on top: hunched, its head sunk low between its shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Two feathers in the grain dust on the catwalk, dark and stiff, each one longer than a man's arm.",
        ),
    ),
    solvedNote = "Light talon marks, a screech and feathers in the grain. " +
        "The Thunderbird has come to town, and the pigeons have left it.",
)

// ---- Jackalope -------------------------------------------------------------------------------------

private val THE_CALLER = RegionalStory(
    id = "the-caller",
    creature = WIDE_JACKALOPE,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Coyote Hunter's Calls Come Back to Him",
        "Something Out There Was Calling Too",
        "\"It Was Doing My Voice\"",
        "Hunter Packs Up Before Sunrise",
        "Little Horns on the Ridgeline",
    ),
    teasers = listOf(
        "He called for coyotes. Something answered in his own voice.",
        "It said his dog's name back to him, he says.",
        "He saw it once in the scope: small, sitting up, with horns.",
        "Not an echo. It came from the wrong direction.",
        "He's hunted that ridge for years and never been spooked till now.",
    ),
    lead = "A coyote hunter set up on the ridge before dawn and started blowing his calls. Something out in " +
        "the dark began answering them, he says, then started copying him: his calls, then his voice, then " +
        "his dog's name. When it got light he saw something small sitting up in the sagebrush, with antlers.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Small, light prints in pairs in the frost below the ridge, with long jumps between each pair.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "His recorder caught it: his own voice calling the dog, then the same words again from out in the " +
                "brush, thinner, a little wrong.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a salt lick: something small and low in the brush, with tall ears and two little " +
                "points above them.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "What he saw through the scope: a jackrabbit sitting up, with a small set of antlers on its head.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A tuft of soft grey-brown fur on the sagebrush where the prints went past.",
        ),
    ),
    solvedNote = "Bounding prints, little antlers and a voice that talks back. " +
        "The jackalope can mimic anyone, and now it's learned a coyote call.",
)

// ---- Goatman ---------------------------------------------------------------------------------------

private val PARKED_ON_THE_BACK_ROAD = RegionalStory(
    id = "parked-on-the-back-road",
    creature = GOATMAN,
    scene = Scene.TIMBER,
    place = "The back road",
    headlines = listOf(
        "Couple Says Something Pounded on Their Car",
        "Dents in the Hood, and Hoof Marks",
        "Scream on the Dead-End Road",
        "\"It Had Horns, and It Was Standing Up\"",
        "Sheriff Asks Drivers to Avoid the Old Road",
    ),
    teasers = listOf(
        "A young couple parked at the end of the old road. Something came out of the trees.",
        "It beat on the hood with its fists, then screamed.",
        "The dents are still there. So are the hoof marks.",
        "It stood on two legs, they say, like a man with a goat's head.",
        "The sheriff says it was kids. The couple say it wasn't.",
    ),
    lead = "A couple parked at the dead end of an old back road last night say something came out of the " +
        "trees on two legs, shaggy and horned, and beat on the hood of their car. It screamed at them through " +
        "the windshield. They drove off in reverse the whole way back to the paved road.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the gravel where the car was parked: cloven hoof prints, but only two to a stride, like " +
                "something walking upright.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her phone was recording a voice note. Banging on metal, then a scream so shrill it clips the " +
                "microphone.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on a gate nearby caught a figure walking upright along the edge of the road, long horns " +
                "against the sky.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His drawing: man-shaped and standing, with a goat's long, curling horns.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse, greasy hair stuck in the wiper blade, and a rank smell like a billy goat's on the hood.",
        ),
    ),
    solvedNote = "Hooves on two legs, horns and a scream at the windshield. " +
        "The Goatman still haunts the back roads, and he doesn't like visitors.",
)

// ---- Lake and sea serpents -------------------------------------------------------------------------

private val THE_SWIM_RAFT = RegionalStory(
    id = "the-swim-raft",
    creature = CHAMP,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Summer Camp Calls Swimmers In",
        "Something Bumped the Swim Raft",
        "Counselors Count Heads Twice",
        "Neck Rose Beside the Diving Board",
        "Campers' Drawings All Look the Same",
    ),
    teasers = listOf(
        "Kids on the swim raft felt something big pass underneath.",
        "A counselor saw a neck come up beside the raft, then sink.",
        "Every camper who saw it drew the same humps.",
        "The camp director says it was a sturgeon. The campers don't.",
        "The raft rocked like a boat had gone by. There was no boat.",
    ),
    lead = "A summer camp blew the whistle and called everyone in yesterday after something big bumped the " +
        "swim raft from below and rocked it. A counselor on the dock says a long neck and a small head came up " +
        "beside the raft for a moment, with dark humps behind, then sank away.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The camp's beach and the reeds either side, checked twice. Bare feet and canoe keels, and nothing " +
                "that came out of the lake.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A camper's waterproof camera, under the surface: a dull thump on the raft, then one huge splash.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The camp's dock camera caught something long and low in the water, sliding out past the buoys.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A dozen campers' drawings, all the same: humps in a row and a long neck at the front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A smear of clear, fishy slime on the underside of the raft's ladder.",
        ),
    ),
    solvedNote = "Humps, a neck and slime on the ladder. " +
        "The lake's monster has a soft spot for summer camp.",
)

private val NEAR_THE_SHANTIES = RegionalStory(
    id = "near-the-shanties",
    creature = SOUTH_BAY_BESSIE,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Ice Fishermen Pull Their Lines",
        "Something Surfaced in the Open Lead",
        "Shanty Row Empties at Noon",
        "Humps Where the Ice Ends",
        "Perch Bite Stops Dead",
    ),
    teasers = listOf(
        "Ice fishermen saw it surface in the open water past the shanties.",
        "The perch quit biting all at once. Then they saw why.",
        "Dark humps in the lead, and the ice shook.",
        "Every shanty on the row was empty by noon.",
        "One man left his auger behind. He's not going back for it.",
    ),
    lead = "Late-season ice fishermen out past the shanties say something surfaced in the open lead where the " +
        "ice gives way to water: a row of dark humps, longer than the whole line of shanties, and a head held " +
        "low on a neck. The ice shook as it went under.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The ice along the lead, walked end to end: boot prints, sled tracks, and nothing that climbed out " +
                "of the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A fisherman's underwater camera has sound: perch scattering, then a deep, heavy splash.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the access road caught something long and low rolling in the open water.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Drawn on a bait-shop receipt: humps in a line and a head on a neck at the front.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Thick, fishy slime frozen on the edge of the ice, where something heavy slid along it.",
        ),
    ),
    solvedNote = "Humps in the lead and slime on the ice. " +
        "Bessie doesn't take the winter off.",
)

private val LOST_THE_BUOY = RegionalStory(
    id = "lost-the-buoy",
    creature = BEAR_LAKE_MONSTER,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Regatta Mark Dragged Off Course",
        "Sailors See Humps Round the Buoy",
        "Race Called Off Mid-Lake",
        "\"The Buoy Just Started Moving\"",
        "Harbor Master Logs Strange Report",
    ),
    teasers = listOf(
        "A race buoy anchored in forty feet of water moved a hundred yards.",
        "The sailors rounding it saw humps come up beside it.",
        "The race committee called it off and nobody complained.",
        "Something was pulling the anchor line, says the committee boat.",
        "A small head on a long neck, says the skipper who saw it closest.",
    ),
    lead = "A sailing race was called off yesterday when the turning mark began to move. The buoy is anchored " +
        "in deep water, but it was dragged a hundred yards while boats were rounding it, and two crews saw a " +
        "row of dark humps and a small head come up beside it.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach nearest the course, checked at dusk. Tracks from people pulling their boats up, and " +
                "nothing else.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A boat's camera on the transom: the sails luffing, someone shouting, then a deep splash right by " +
                "the buoy.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the committee boat caught something long and low in the water, swimming away from the " +
                "mark.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The skipper's sketch: humps in a row, and a small head on a long neck.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Stringy, clear slime all along the buoy's anchor line when they hauled it up.",
        ),
    ),
    solvedNote = "Humps, a dragged buoy and slime on the line. " +
        "The lake's monster has been told of since 1868, and now it's entered the regatta.",
)

private val NOT_A_WHALE = RegionalStory(
    id = "not-a-whale",
    creature = SEA_SERPENT,
    scene = Scene.LAKE_SHORE,
    place = "The harbor",
    headlines = listOf(
        "Whale Watch Gets More Than It Paid For",
        "Naturalist: \"That Was Not a Whale\"",
        "Passengers Film a Long Neck Off the Bow",
        "Tour Boat Logs an Unknown Species",
        "Humps in a Line, Not a Spout in Sight",
    ),
    teasers = listOf(
        "Fifty passengers came for whales. They got something else.",
        "The naturalist on board has seen thousands of whales. She says it wasn't one.",
        "A long neck, a horse-like head, and humps trailing out behind.",
        "No blow, no fluke. Just humps, and a head held up out of the sea.",
        "The tour company is offering everyone a free trip back.",
    ),
    lead = "A whale-watching boat came back to the harbor with fifty passengers who all saw the same thing: a " +
        "dark body in a long line of humps, and a head held up on a neck like a horse's. The naturalist on " +
        "board says there was no blow and no fluke. Whatever it was, it wasn't a whale.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The ledges and coves nearest where it was seen, searched at low tide. Gulls and crabs, and no " +
                "tracks out of the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The boat's hydrophone was down for whale song. No song, just one great splash as it went under.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A passenger's camera on a long lens: something long and low riding the swell, far bigger than a " +
                "seal.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The naturalist's sketch for the log book: humps in a long line, and a head up on a raised neck.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A smear of thick, clear slime on the boat's bow pulpit, where it came close enough to touch.",
        ),
    ),
    solvedNote = "Humps, a horse's head and slime on the bow. " +
        "The sea serpent has been fooling whale watchers since long before there were whale watchers.",
)

private val DRONE_OVER_THE_SHALLOWS = RegionalStory(
    id = "drone-over-the-shallows",
    creature = WIDE_TAHOE_TESSIE,
    scene = Scene.LAKE_SHORE,
    headlines = listOf(
        "Drone Footage Shows Shape Under the Water",
        "Photographer Can't Explain the Shadow",
        "\"Longer Than the Pier, and Moving\"",
        "Wedding Shoot Catches More Than the Couple",
        "Clear Water, Dark Shape",
    ),
    teasers = listOf(
        "A wedding photographer's drone caught something long and dark under the water.",
        "It was longer than the pier, she says, and it was moving.",
        "The water's clear to the bottom. Except where it wasn't.",
        "The couple heard a slap on the water mid-vows.",
        "She's looked at the footage frame by frame. There's no head.",
    ),
    lead = "A wedding photographer flying a drone over the shallows for an aerial shot found something on the " +
        "footage afterwards: a long, dark shape under the clear water, longer than the pier, moving slowly " +
        "along the shore. Halfway through the vows the guests heard a heavy slap on the water.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The beach under the pier and along the shore. Guests' shoes and dogs, and nothing from the water.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The videographer's audio, during the vows: the officiant, then a single heavy slap on the water.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The drone footage itself: a dark shadow under the water, no shape you can name, gliding past.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A guest who saw it from the pier tried to draw it: a dark smudge in the water, nothing more.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The pier's pilings and the shallows swabbed. Nothing at all. It never came near enough to touch.",
        ),
    ),
    solvedNote = "A long dark shadow, a slap and no head. " +
        "Tessie came to the wedding, and stayed in the background.",
)

// ---- Gumberoo --------------------------------------------------------------------------------------

private val THE_MEAT_POLE = RegionalStory(
    id = "the-meat-pole",
    creature = WIDE_GUMBEROO,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Hunting Camp Loses Its Whole Season's Meat",
        "Rifle Shots Didn't Slow It Down",
        "Cabin Wall Pushed In Overnight",
        "\"I Hit It Twice and It Didn't Flinch\"",
        "Camp Smelled Like a Wildfire, With No Fire",
    ),
    teasers = listOf(
        "Hunters woke to the meat pole torn down and two elk quarters gone.",
        "One of them fired twice. He says it didn't even turn round.",
        "The back wall of the cabin is bowed in.",
        "The whole camp smelled of smoke. The stove was cold.",
        "Black, almost bare, and bigger than any bear they'd seen.",
    ),
    lead = "Hunters at a backcountry camp woke at three in the morning to the meat pole crashing down. One of " +
        "them fired twice at something black and almost hairless, bigger than a bear, dragging an elk " +
        "quarter away. It didn't flinch. The cabin's back wall is bowed in and the camp smells of smoke.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Round, deep prints all round the meat pole, wider than a skillet and sunk into the frozen ground.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A hunter's phone recorded the whole thing: wood splitting, two rifle shots, men shouting, and not " +
                "one sound from the animal.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "Their own camera on the meat pole: something long, low and glossy black, moving on all fours.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The shooter's drawing: hunched and massive, its head carried low between huge shoulders.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The pole, the cabin wall and the drag trail all searched. No hair, no blood, only a smell of " +
                "smoke on everything.",
        ),
    ),
    solvedNote = "Deep prints, no hair, and bullets that bounced. " +
        "Only fire kills a Gumberoo, the old loggers said, and the hunters didn't bring any.",
)

// ---- Dogman ----------------------------------------------------------------------------------------

private val SNOWMOBILE_TRAIL = RegionalStory(
    id = "snowmobile-trail",
    creature = DOGMAN,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Snowmobilers Say Something Ran Alongside Them",
        "Eyes Kept Pace in the Trees",
        "\"It Was Running on Two Legs\"",
        "Night Ride Ends at the Trailhead",
        "Club Closes Trail After Sighting",
    ),
    teasers = listOf(
        "Four sleds at forty miles an hour, and something kept up with them.",
        "Amber eyes in the trees, level with a man's.",
        "It ran beside the trail on its hind legs, they say.",
        "The snowmobile club has closed the loop until further notice.",
        "A howl at the trailhead, and every sled started at once.",
    ),
    lead = "Four snowmobilers riding a woods trail at night say something kept pace with them through the trees " +
        "for nearly a mile: two amber eyes, shining in their headlights at the height of a man's. When the " +
        "trail opened up they saw it running on its hind legs, wolf-headed, before it veered off.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the snow beside the trail: huge dog-like prints, with long claw marks at every toe, and only " +
                "two to a stride.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A rider's helmet camera at the trailhead: engines idling, then a long, deep howl from the woods.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the trail sign: nothing but two pale eyes glowing in the dark, too high for any dog.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The lead rider's drawing: a man's body running, with a dog's long muzzle and tall pointed ears.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Coarse grey-brown hair caught on a branch beside the trail, at shoulder height.",
        ),
    ),
    solvedNote = "Clawed prints, a howl and eyes at a man's height. " +
        "The Dogman doesn't need a sled to keep up.",
)

// ---- Black panther ---------------------------------------------------------------------------------

private val FAIRWAY_AT_DAWN = RegionalStory(
    id = "fairway-at-dawn",
    creature = BLACK_PANTHER,
    scene = Scene.PASTURE,
    place = "The golf course",
    headlines = listOf(
        "Groundskeeper Sees Big Cat on the Fairway",
        "Early Tee Times Pushed Back",
        "\"Black, Long, and Not in Any Hurry\"",
        "Something Screamed by the Water Hazard",
        "Golfers Asked to Stay in Their Carts",
    ),
    teasers = listOf(
        "A groundskeeper mowing at dawn watched a big black cat walk the ninth fairway.",
        "It stopped at the water hazard and drank, he says.",
        "Its tail was longer than his rake.",
        "The clubhouse says it was a dog. The groundskeeper says it wasn't.",
        "A scream from the woods behind the green, just before sunrise.",
    ),
    lead = "A golf course groundskeeper mowing before sunrise says a big black cat walked out of the trees and " +
        "down the middle of the fairway, long and low, with a tail almost as long again as its body. It stopped " +
        "to drink at the pond and then went back into the woods. Earlier he'd heard a scream.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the sand trap by the pond: round prints with four toes and a broad pad, and no claw marks at all.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The maintenance shed's camera has sound: before dawn, a scream from the trees, high, like a woman " +
                "in pain.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the cart path caught something long and low slinking along the edge of the rough.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "His sketch on a scorecard: a long, low cat with a small round head and a very long tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Short, glossy black hairs on the wire fence behind the ninth green.",
        ),
    ),
    solvedNote = "Round prints, a scream and black hair on the fence. " +
        "The black panther played through.",
)

private val DOORBELL_CAMERA = RegionalStory(
    id = "doorbell-camera",
    creature = BLACK_PANTHER,
    scene = Scene.RIVER_TOWN,
    place = "The new subdivision",
    headlines = listOf(
        "Doorbell Camera Catches Big Cat on Porch",
        "Neighborhood Group Chat Lights Up",
        "\"That's Not a House Cat\"",
        "Residents Bring Pets In at Night",
        "Animal Control Takes a Second Look",
    ),
    teasers = listOf(
        "A doorbell camera caught a black cat on the porch. It was the size of a deer.",
        "The neighborhood chat had two hundred messages by breakfast.",
        "Its tail swept the welcome mat from one end to the other.",
        "Animal control said bobcat. Then they saw the video.",
        "The houses back onto the woods. Something came out of them.",
    ),
    lead = "A family in a new subdivision at the edge of the woods checked their doorbell camera this morning " +
        "and found a big black cat had walked across the porch at two a.m.: long and low, with a tail that " +
        "swept the full width of the doormat. Neighbors say they heard screaming the same night.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the mulch by the porch: round prints, wider than a big dog's, with no sign of claws.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A neighbor's camera recorded the scream: long and high, like a woman's, from the tree line.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "The doorbell video: something long and low, black from nose to tail, slinking past the door.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A neighbor who saw it in the street drew it: a long, low cat with a small head and a heavy tail.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Short, glossy black hairs on the corner of the porch railing where it rubbed past.",
        ),
    ),
    solvedNote = "Round prints, a scream and black hair on the railing. " +
        "The woods were here first, and so was the panther.",
)

// ---- Hoop snake ------------------------------------------------------------------------------------

private val THE_FENCE_POST = RegionalStory(
    id = "the-fence-post",
    creature = HOOP_SNAKE,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Rider Says Snake Chased Her Horse Downhill",
        "Fence Post Swelled Up and Split",
        "Horse Outran Something Rolling",
        "\"It Hit the Post Instead of Us\"",
        "Ranch Hand Brings In a Split Cedar Post",
    ),
    teasers = listOf(
        "A rider says a snake rolled after her horse like a hoop.",
        "It struck a cedar post. The post is twice as thick as it was.",
        "Her horse wouldn't stop running until the barn.",
        "The post split down the middle overnight.",
        "Her uncle used to tell this story. She thought he was joking.",
    ),
    lead = "A woman riding fence on a ranch says a snake came rolling down the slope after her horse, its tail " +
        "in its mouth like a hoop. She swung behind a cedar fence post and it let go and struck the post with " +
        "its tail. By evening the post had swelled to twice its size and split down the middle.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "No tracks on the slope. Only one thin, straight groove pressed into the dirt, ending at the post.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her phone was in her saddlebag recording: hooves, the horse snorting, her shouting, and from the " +
                "snake, not a sound.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the stock tank caught something low and fast in the grass, going in a dead straight " +
                "line.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "Her drawing: a snake curled into a ring, tail in mouth, with speed lines behind it.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "A shed scale caught in the splinters of the post, and a puncture where the tail went in.",
        ),
    ),
    solvedNote = "A groove, a scale and a fence post twice its size. " +
        "The hoop snake missed, and the fence paid for it.",
)

private val THE_HOE_HANDLE = RegionalStory(
    id = "the-hoe-handle",
    creature = HOOP_SNAKE,
    scene = Scene.PASTURE,
    headlines = listOf(
        "Gardener's Hoe Handle Swells Up After Snake Strike",
        "Snake Came Rolling Across the Garden",
        "\"I Held Up the Hoe and It Hit That\"",
        "Hoe Handle Too Big to Hold by Supper",
        "Neighbors Come to See the Swollen Hoe",
    ),
    teasers = listOf(
        "She held the hoe out in front of her, and the snake struck the handle.",
        "By supper the handle was too thick to get a hand round.",
        "It came across the garden rolling, tail in its mouth.",
        "Half the road has come to look at the hoe on her porch.",
        "Her grandmother said this happened to her grandmother. Now it's happened to her.",
    ),
    lead = "A woman hoeing her garden says a snake came rolling between the rows at her with its tail in its " +
        "mouth. She held the hoe out in front of her and it let go and struck the handle with its tail. By " +
        "supper the handle had swollen so thick she couldn't close her hand round it.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Across the garden: no tracks at all, just a single thin line in the soil, straight as a ruler.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her radio was on in the kitchen window. Music, her yelling, and nothing else. No hiss, no rattle.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the bird feeder caught something low in the rows, moving fast and dead straight.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "She drew it on a seed packet: a snake bent into a ring, its tail in its mouth.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Two shed scales in the bean row, and a small hole in the hoe handle where it struck.",
        ),
    ),
    solvedNote = "A groove, scales and a hoe handle swollen past holding. " +
        "That's the oldest hoop snake story there is, and it's still happening.",
)

// ---- Hidebehind ------------------------------------------------------------------------------------

private val THE_WHISKEY_FLASK = RegionalStory(
    id = "the-whiskey-flask",
    creature = HIDEBEHIND,
    scene = Scene.TIMBER,
    headlines = listOf(
        "Thinning Crew Says Something Stalked Them",
        "Only the Man With the Flask Felt Safe",
        "Crew Member Missing for an Afternoon",
        "\"Every Tree Had Something Behind It\"",
        "Foreman Won't Send Anyone in Alone",
    ),
    teasers = listOf(
        "A forestry crew says something watched them all day from behind the trees.",
        "Only one of them never felt it. He had whiskey in his pocket.",
        "A sawyer was gone four hours and doesn't know where.",
        "Turn round, and it's behind the next trunk.",
        "The foreman's grandfather worked the camps. He knows the story.",
    ),
    lead = "A forestry crew thinning timber say something stalked them all day. Whenever anyone turned round, " +
        "it was already behind a tree. One sawyer walked off and was found four hours later with no memory of " +
        "it. The only man who never felt watched was the one with a flask of whiskey in his jacket.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "Behind every tree the crew pointed to, the needles lie flat and untouched. Not one print.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "The foreman's radio log: saws running, then all of them stopping, and a long silence with no " +
                "birds and no wind.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the skid road caught a dark shadow slipping across the ground behind a tree, with " +
                "nothing there to cast it.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "The sawyer tried to draw what he'd seen and got only a dark smudge at the edge of a trunk.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Bark and needles swabbed behind a dozen trees. Nothing at all.",
        ),
    ),
    solvedNote = "No tracks, no sound, and a man with whiskey who never felt it. " +
        "The old loggers knew what kept the Hidebehind off.",
)

private val OUT_IN_THE_BOG = RegionalStory(
    id = "out-in-the-bog",
    creature = HIDEBEHIND,
    scene = Scene.PINE_BARRENS,
    place = "The bog",
    headlines = listOf(
        "Bog Walker Felt Followed the Whole Way Out",
        "Boardwalk Hikers Report Being Watched",
        "Birdwatcher Loses Two Hours",
        "\"Something Was Behind Every Pine\"",
        "Dog Refuses to Go Past the Bog",
    ),
    teasers = listOf(
        "A birdwatcher on the bog boardwalk lost two hours and can't account for them.",
        "Something was behind every pine, she says. She never saw it.",
        "Her dog sat down at the boardwalk's edge and wouldn't move.",
        "Three hikers in a week say the same thing: they were followed.",
        "The bog goes quiet, and then you know it's there.",
    ),
    lead = "A birdwatcher walking the boardwalk across the bog says something followed her from pine to pine " +
        "all afternoon, always gone behind a trunk when she turned. She checked her watch at the far end, and " +
        "two hours were missing. Her dog wouldn't go past the first bend.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "The moss behind the pines beside the boardwalk is soaked and soft, and there isn't a print in it.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "Her bird-song recorder: warblers, frogs, then everything stops at once, and nothing at all for " +
                "two hours.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera on the boardwalk caught a shadow falling across the planks from behind a pine, with " +
                "nothing there to cast it.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "She couldn't draw it: just a dark smudge half behind a tree, never seen straight on.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "Every pine she pointed to swabbed, and the moss sifted. Nothing.",
        ),
    ),
    solvedNote = "No tracks, no sound and two lost hours. " +
        "The Hidebehind isn't only in the deep woods. It goes wherever there's something to hide behind.",
)

// ---- Tommyknocker ----------------------------------------------------------------------------------

private val GHOST_TOWN_TOUR = RegionalStory(
    id = "ghost-town-tour",
    creature = TOMMYKNOCKER,
    scene = Scene.RIDGE,
    headlines = listOf(
        "Ghost Town Tour Hears Knocking From the Shaft",
        "Tour Guide: \"That's Not in the Script\"",
        "Lantern Seen Below the Boarded Shaft",
        "Tourists Leave Pasty Crusts at the Mine",
        "Old Camp Isn't as Empty as It Looks",
    ),
    teasers = listOf(
        "A tour group heard hammering deep in a mine boarded up for a century.",
        "The guide tells the tommyknocker story every day. Today it knocked back.",
        "A small lamp moved below the boards, too low for a man.",
        "Someone left a pasty crust at the shaft. By morning it was gone.",
        "The knocking was steady, like a clock, then stopped.",
    ),
    lead = "A guide leading a tour through an old mining ghost town stopped at a boarded-up shaft to tell the " +
        "group about tommyknockers, and something below started knocking: steady taps on rock, like a hammer. " +
        "Through the gaps in the boards, a small lamp moved, carried waist-high, and went out of sight.",
    clues = listOf(
        Clue(
            "prints",
            ClueType.FOOTPRINTS,
            "In the dust on the ledge just below the boards: small, light prints, close together, like a " +
                "child's.",
        ),
        Clue(
            "audio",
            ClueType.AUDIO,
            "A tourist's video: the guide talking, then sharp, even taps on rock from below, for nearly a minute.",
        ),
        Clue(
            "trailcam",
            ClueType.TRAIL_CAM,
            "A camera the guide put up that night caught a small glowing light deep in the shaft, moving slowly.",
        ),
        Clue(
            "sketch",
            ClueType.WITNESS_SKETCH,
            "A child on the tour drew it: a tiny bearded man in a hood, stooped, with a lamp.",
        ),
        Clue(
            "sample",
            ClueType.SAMPLE,
            "The ledge and the boards swabbed. Nothing from any animal, and the pasty crust left there is gone.",
        ),
    ),
    solvedNote = "Small prints, a lamp and a steady knocking. " +
        "The miners left the town, but the tommyknockers stayed with the mine.",
)

/** The second and third stories, which Cases.kt runs alongside WIDE_STORIES. */
internal val MORE_WIDE_STORIES: List<RegionalStory> = listOf(
    STEPPED_OVER_THE_RAIL,
    BERRY_PICKERS,
    ON_THE_TURBINE,
    GRAIN_ELEVATOR,
    THE_CALLER,
    PARKED_ON_THE_BACK_ROAD,
    THE_SWIM_RAFT,
    NEAR_THE_SHANTIES,
    LOST_THE_BUOY,
    NOT_A_WHALE,
    DRONE_OVER_THE_SHALLOWS,
    THE_MEAT_POLE,
    SNOWMOBILE_TRAIL,
    FAIRWAY_AT_DAWN,
    DOORBELL_CAMERA,
    THE_FENCE_POST,
    THE_HOE_HANDLE,
    THE_WHISKEY_FLASK,
    OUT_IN_THE_BOG,
    GHOST_TOWN_TOUR,
)
