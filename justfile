# Android Studio's bundled JDK, because Gradle can't run on the system's Java 27.
export JAVA_HOME := env_var_or_default("JAVA_HOME", "/opt/android-studio/jbr")

# List the available recipes
default:
    @just --list

# Build the debug APK
build:
    ./gradlew :app:assembleDebug

# Build the signed release bundle (.aab) for Google Play. Needs keystore.properties, see README.md
bundle:
    @test -f keystore.properties || (echo "keystore.properties is missing, see README.md" && exit 1)
    @test -n "${GAME_CONTACT:-}" || (echo "GAME_CONTACT is not set, so the release would have no weather or cryptid requests. See README.md" && exit 1)
    ./gradlew :app:bundleRelease
    @echo "Bundle: app/build/outputs/bundle/release/"

# Make the upload key for Google Play. Run once and back the file up somewhere safe
keystore:
    @test ! -f upload-key.jks || (echo "upload-key.jks already exists" && exit 1)
    $JAVA_HOME/bin/keytool -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000

# Run the unit tests
test:
    ./gradlew :shared:test

# Run ktlint and Android Lint
lint:
    ./gradlew ktlintCheck :app:lintDebug

# Fix the Kotlin style problems ktlint can fix by itself
format:
    ./gradlew ktlintFormat

# Run the linters and tests, then build the APK
check: lint test build

# Install the debug build on a connected phone or emulator
install:
    ./gradlew :app:installDebug

# List the cryptids that have encounter art, by the id `just encounter` takes
cryptids:
    @sed -n '/^fun creatureFrames/,/^}/s/^ *"\([a-z-]*\)" ->.*/\1/p' app/src/main/kotlin/app/unconfirmed/game/ui/EncounterScreen.kt

# Install the debug build and go straight to a cryptid's encounter, over and over. See `just cryptids`
encounter id: install
    @just cryptids | grep -qx '{{id}}' || (echo "No cryptid called '{{id}}'. Known cryptids:" && just cryptids && exit 1)
    adb shell am force-stop app.unconfirmed.game
    adb shell am start -n app.unconfirmed.game/.MainActivity --es debugEncounter {{id}}

# Install the debug build and show every clue's picture on one page, for checking the art
clues: install
    adb shell am force-stop app.unconfirmed.game
    adb shell am start -n app.unconfirmed.game/.MainActivity --ez debugClues true

# Render a minute of the game's music to listen to: calm, a clue cue, an encounter, another cue
soundscape:
    ./gradlew :shared:test --tests '*SoundscapeTest*' --rerun -q
    @echo "Preview: shared/build/soundscape-preview.wav"

# Start the Android emulator, for example: just emulator, or just emulator Pixel_9
emulator avd="Medium_Phone_API_36.1":
    ~/Android/Sdk/emulator/emulator -avd {{avd}} &

# Delete build output
clean:
    ./gradlew clean
