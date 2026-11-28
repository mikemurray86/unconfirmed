# Unconfirmed

An Android game about investigating cryptids on foot. While you investigate, the screen shows a first-person cartoon scene of the kind of place the case is set in. Clues turn up as you walk, in any direction.

## Build and test

Tasks run through [just](https://github.com/casey/just). Run `just` to list them all:

```sh
just check      # Linters, tests and the debug APK
just build      # Debug APK only
just test       # Game rules, cases and clue trail
just lint       # ktlint and Android Lint
just format     # Fix the Kotlin style problems ktlint can fix
just install    # Install on a connected phone or emulator
just cryptids   # List the creature ids just encounter takes
just encounter bigfoot  # Go straight to a creature's encounter
just soundscape # Render a minute of the music to listen to
```

Gradle can't run on the system's Java 27, so the `justfile` uses Android Studio's bundled JDK unless `JAVA_HOME` is already set. To run `./gradlew` directly, set `JAVA_HOME=/opt/android-studio/jbr` first.

`GAME_CONTACT` is the game's contact address, read when you build. Debug and release builds use it in two places:

- **Weather.** MET Norway refuses requests without a contact, so without it the game shows clear skies.
- **Request a cryptid.** Players write to this address from the case selection screen. Without it, the link is hidden.

Use the game's own address, not a personal one, since players will see it. Set it in your shell profile (or an `.envrc`, which is in `.gitignore`) so the address never goes into the project:

```sh
export GAME_CONTACT=you@example.com
```

Android Studio only sees the variable if it was started from a shell where it's set, or if you add it to the Gradle run configuration.

## Release builds

Google Play takes a signed app bundle. Play App Signing holds the real signing key, and you sign uploads with your own upload key.

1. Run `just keystore` once. It asks for a password and your details, and writes `upload-key.jks`. Back this file and its password up outside the project. Losing it means asking Google to reset the upload key.
2. Create `keystore.properties` at the project root:
   ```properties
   storeFile=upload-key.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
3. Set `GAME_CONTACT` to the game's contact address (see above). `just bundle` stops if it isn't set.
4. Run `just bundle`. The `.aab` lands in `app/build/outputs/bundle/release/`.

Both files are in `.gitignore`. Raise `versionCode` in `app/build.gradle.kts` before every upload. Release builds are shrunk with R8, so play a release build through a whole case before uploading it.

## Ads

The newspaper's front page has an AdMob banner slot (`ui/AdSlot.kt`). The AdMob app ID and the banner unit ID are in `app/build.gradle.kts`. Debug builds use Google's test banner unit, so you can tap their ads safely. Never tap the live ads in a release build, because AdMob can suspend the account for it.

Each launch, Google's User Messaging Platform checks whether the player must be asked about ads, and shows the consent form if so (`Ads.kt`). Ads start only once consent allows them. Until a banner loads, or when none fills, the slot shows one of the paper's made-up classifieds. Where the law requires it, the About screen (under Options) has a "Privacy choices for ads" button. The consent form itself is set up in the AdMob console under Privacy & messaging.

## Where it works

Anywhere. There's no map data to add. The game asks the phone's geocoder which US state you're in, and offers the cases reported there. The geocoder usually needs a data connection, so the answer is saved and reused when you're offline. Outside the US, or in a state with no cases yet, the list is empty.

Saves from the old map version can't be loaded. An open or paused case from before this change is dropped, but the journal is kept.

## Sound

The music isn't recorded. `shared/.../Soundscape.kt` makes it up as it plays: a low drone, slow minor chords that slide into one another, wind, and stray music-box bells. In an encounter a heartbeat and a high tremolo come in. A clue turning up plays a short cue of rising bells, and the music dips under it. Run `just soundscape` to render a minute of it to `shared/build/soundscape-preview.wav`.

It plays only while the game is on screen, and not at all while the phone is on silent or vibrate. If something else is already playing when the game opens, like a podcast, the music stays off and only the clue cue sounds. "Sound: on" at the foot of the case list turns it all off.

## Clue pictures and photos

Each clue comes with a picture drawn in code by `ClueArt.kt`: the sample in an evidence bag, prints in mud beside a ruler, the recording's trace, a trail-cam still or the witness's sketch. A picture shows only the trait the clue gives, so two suspects that share a trait get the same picture. All hair is the same dark tuft, for example. A picture never settles more than the clue's words do.

Photos from an encounter aren't saved as images. Each one keeps what the scene needs to draw that moment again: where the camera and the creature were, its stride, the time of day and weather, and the size of the screen. The journal redraws the viewfinder's square from that. The best photo goes on the creature's page, and every shot goes on the case's field report. Tap a shot to see it large. Entries filed before this change have no photos and keep the old stand-in picture.

## Options and traveler mode

Sound, Request a cryptid and About live on the Options page, reached from the field journal's Options button. Options also has **traveler mode**, in every build and off by default. With it on, the player picks a state to read the paper from and takes its cases as if they were there. Clues still turn up by walking wherever they really are. The choice is kept between launches, and turning traveler mode off brings them home.

## Debug builds

- The **+25m** button in the investigation counts as walking 25 m, so a case can be played sitting down. A clue always turns up within 400 m. Turn it on under Options, at the back of the field journal.
- The case list also prints every other state's papers, one pile per state. Scroll up or down to move between them.
- Tap the field readout to cycle the weather, and long-press it to cycle the time of day.
- To go straight to a creature's encounter, over and over, run `just encounter bigfoot`. It installs the debug build first. `just cryptids` lists the creature ids it takes. It's for checking the creature art and animation. After each round it shows the photos you took, printed as the journal prints them. To see an encounter in one case's own scenery, such as a regional story's copy in one state, pass a case id instead: `adb shell am start -n app.unconfirmed.game/.MainActivity --es debugEncounter tree-stand-al`.
- `just clues` shows the picture for every clue type and trait on one page.
- The animation frames for the creatures that walk, fly or stand come from `tools/cryptid_frames.py`. Change a rig there and run `python3 tools/cryptid_frames.py` to rewrite the drawables and `CreatureFrames.kt`. Add `--preview DIR` to also write SVGs of every frame.
- On an emulator, set GPS with `adb emu geo fix <lon> <lat>`, and turn the phone with Extended controls → Virtual sensors to pan the scene.
