package app.unconfirmed.game

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.unconfirmed.shared.ALL_CASES
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CLUES_PER_CASE
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.CasePhase
import app.unconfirmed.shared.Cell
import app.unconfirmed.shared.JournalEntry
import app.unconfirmed.shared.LocationFix
import app.unconfirmed.shared.MovementTracker
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.STATE_NAMES
import app.unconfirmed.shared.canPause
import app.unconfirmed.shared.caseById
import app.unconfirmed.shared.caseSeed
import app.unconfirmed.shared.casesIn
import app.unconfirmed.shared.dayWindow
import app.unconfirmed.shared.editionSeed
import app.unconfirmed.shared.journalEntryFor
import app.unconfirmed.shared.pickClueTypes
import app.unconfirmed.shared.pickHeadline
import app.unconfirmed.shared.pickTeaser
import app.unconfirmed.shared.regionAt
import app.unconfirmed.shared.runToday
import app.unconfirmed.shared.searchFindsClue
import app.unconfirmed.shared.suggestedBearings
import app.unconfirmed.shared.weekWindow
import app.unconfirmed.shared.wireCases
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

sealed interface WorldStatus {
    data object WaitingForLocation : WorldStatus
    data object LocatingState : WorldStatus
    data object Ready : WorldStatus

    /**
     * The player has no case open. [local] are the cases reported in [state], which is null outside
     * the United States. [elsewhere] lists the other states' cases, and is only filled in debug builds.
     * [headlines] and [teasers] say which headline and teaser today's paper runs each case with, by
     * case id. [region] is the scenery where the player is, which the local cases' photos show.
     * [wire] are stories from neighboring states, which reach the paper once the player has met
     * every creature in their own (see [wireCases]); they're printed after the local ones.
     */
    data class ChoosingCase(
        val state: String?,
        val local: List<CaseDefinition>,
        val elsewhere: List<CaseDefinition>,
        val headlines: Map<String, Int> = emptyMap(),
        val teasers: Map<String, Int> = emptyMap(),
        val region: Region? = null,
        val wire: List<CaseDefinition> = emptyList(),
    ) : WorldStatus {
        fun regionOf(case: CaseDefinition): Region = region?.takeIf { case in local } ?: case.region

        fun headlineOf(case: CaseDefinition): String = case.headline(headlines[case.id] ?: 0)
        fun teaserOf(case: CaseDefinition): String = case.teaser(teasers[case.id] ?: 0)
    }

    /** The phone couldn't say which state the player is in, and there's no earlier answer to use. */
    data object NeedsConnection : WorldStatus
}

data class GameState(
    val safetyAccepted: Boolean,
    val status: WorldStatus = WorldStatus.WaitingForLocation,
    val fix: LocationFix? = null,
    val tooFast: Boolean = false,
    val activeCase: ActiveCase? = null,
    /** The number (from 1) of a clue that just turned up, so the screen can announce it. */
    val foundClueNumber: Int? = null,
    /** Every case the player has closed, oldest first. */
    val journal: List<JournalEntry> = emptyList(),
    /** The US state the player is standing in, once known. */
    val areaState: String? = null,
    /** Investigations set aside to pick up later, oldest first. */
    val pausedCases: List<ActiveCase> = emptyList(),
    /** Debug builds only: the state the player is pretending to be in. */
    val debugState: String? = null,
    /** Debug builds only: whether the investigation shows a button that counts as walking 25 m. */
    val debugWalkButton: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_GAME, Context.MODE_PRIVATE)

    // Where the player was last seen, kept apart from the game's progress so a backup never takes
    // it off the phone. See res/xml/backup_rules.xml.
    private val place = placePrefs(application)
    private val json = Json { ignoreUnknownKeys = true }
    private val tracker = MovementTracker()
    private val locator = StateLocator(application)

    // The last state lookup. It's kept across restarts, so a player with no signal can still play
    // where they were last time.
    private var stateCell: Cell? = loadStateCell()
    private var knownState: String? = place.getString(KEY_STATE, null)?.ifEmpty { null }
    private var lookingUp = false
    private var lastLookupMillis = 0L

    private val _state = MutableStateFlow(
        GameState(
            safetyAccepted = prefs.getBoolean(KEY_SAFETY, false),
            debugWalkButton = BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_WALK_BUTTON, false),
            activeCase = loadCase(),
            journal = loadJournal(),
            pausedCases = loadPaused(),
            areaState = knownState,
        ).let { if (it.activeCase != null) it.copy(status = WorldStatus.Ready) else it },
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val hasKnownState: Boolean get() = stateCell != null || _state.value.debugState != null
    private val currentState: String? get() = _state.value.debugState ?: knownState

    fun acceptSafety() {
        prefs.edit { putBoolean(KEY_SAFETY, true) }
        _state.update { it.copy(safetyAccepted = true) }
    }

    fun onLocation(fix: LocationFix) {
        val walked = tracker.update(fix)
        _state.update { it.copy(fix = fix, tooFast = tracker.isTooFast) }
        updateState(fix)
        val case = _state.value.activeCase
        when {
            case == null -> offerCases()
            case.phase == CasePhase.INVESTIGATING && walked > 0 ->
                saveCase(search(case.copy(walkedMeters = case.walkedMeters + walked), walked))
        }
    }

    /** Looks up the player's state on moving into a new cell, and retries now and then if it fails. */
    private fun updateState(fix: LocationFix) {
        val cell = Cell.containing(fix.point)
        val now = System.currentTimeMillis()
        if (cell == stateCell || lookingUp || now - lastLookupMillis < LOOKUP_RETRY_MILLIS) return
        lookingUp = true
        lastLookupMillis = now
        viewModelScope.launch {
            // The cell's centre, not the fix, so the geocoder never sees the player closer than ~1 km.
            val result = locator.lookUp(cell.center)
            lookingUp = false
            if (result is StateLookup.Found) {
                stateCell = cell
                knownState = result.code
                place.edit {
                    putString(KEY_STATE, result.code.orEmpty())
                    putString(KEY_STATE_CELL, json.encodeToString(Cell.serializer(), cell))
                }
            }
            _state.update { it.copy(areaState = currentState) }
            if (_state.value.activeCase == null) offerCases()
        }
    }

    /**
     * Walking can turn up the next clue at any moment, in any direction. The suggested heading is
     * only a hint, so nobody is ever sent towards a particular place. Never at driving speed.
     */
    private fun search(case: ActiveCase, walked: Double): ActiveCase {
        val definition = caseById(case.caseId) ?: return case
        if (tracker.isTooFast || case.allCluesFound(definition)) return case
        if (!searchFindsClue(case.searchedMeters, walked, Random.nextDouble())) {
            return case.copy(searchedMeters = case.searchedMeters + walked)
        }
        val found = case.cluesFound + 1
        _state.update { it.copy(foundClueNumber = found) }
        return case.copy(cluesFound = found, searchedMeters = 0.0)
    }

    fun findShown() {
        _state.update { it.copy(foundClueNumber = null) }
    }

    /** Shows the cases reported in the player's state, until they pick one. */
    private fun offerCases() {
        val current = _state.value
        if (current.activeCase != null) return
        val status = when {
            current.fix == null && current.debugState == null -> WorldStatus.WaitingForLocation
            !hasKnownState && lookingUp -> WorldStatus.LocatingState
            !hasKnownState && lastLookupMillis == 0L -> WorldStatus.LocatingState
            !hasKnownState -> WorldStatus.NeedsConnection
            else -> {
                // A paused case is picked up from its own card, so it isn't offered again as new, and
                // nor is another story about the same creature.
                val paused = current.pausedCases.map { it.caseId }.toSet()
                val pausedCreatures = paused.mapNotNull { caseById(it)?.answerId }.toSet()
                val edition = editionSeed(dayWindow(System.currentTimeMillis()), prefs.getInt(KEY_SLOT, 0))
                val local = runToday(casesIn(currentState), edition).filter { it.answerId !in pausedCreatures }
                val wire = wireCases(currentState, current.journal, edition).filter { it.answerId !in pausedCreatures }
                val elsewhere = if (BuildConfig.DEBUG) {
                    ALL_CASES.filter { it !in local && it !in wire && it.id !in paused }
                } else {
                    emptyList()
                }
                val printed = local + wire + elsewhere
                val headlines = printed.associate { it.id to pickHeadline(it, edition) }
                val teasers = printed.associate { it.id to pickTeaser(it, edition) }
                WorldStatus.ChoosingCase(currentState, local, elsewhere, headlines, teasers, currentRegion(), wire)
            }
        }
        if (status != current.status) _state.update { it.copy(status = status) }
    }

    /** Opens [caseId] where the player is standing. Its trail comes from the cell and the week. */
    fun chooseCase(caseId: String) {
        val current = _state.value
        if (current.activeCase != null || current.pausedCases.any { it.caseId == caseId }) return
        val definition = caseById(caseId) ?: return
        val state = currentState ?: definition.state
        val slot = prefs.getInt(KEY_SLOT, 0)
        val cell = current.fix?.let { Cell.containing(it.point) } ?: stateCell ?: Cell(0, 0)
        val seed = caseSeed(cell, weekWindow(System.currentTimeMillis()), slot)
        // Kept as the paper printed it, even if the day has turned over since the page was drawn.
        val headline = (current.status as? WorldStatus.ChoosingCase)?.headlines?.get(caseId)
            ?: pickHeadline(definition, editionSeed(dayWindow(System.currentTimeMillis()), slot))
        saveCase(
            ActiveCase(
                caseId = definition.id,
                state = state,
                region = currentRegion(),
                slot = slot,
                bearings = suggestedBearings(seed, CLUES_PER_CASE),
                clueTypes = pickClueTypes(definition, seed),
                headline = headline,
            ),
        )
        _state.update { it.copy(status = WorldStatus.Ready) }
    }

    /**
     * The scenery where the player is standing, from their state and position. While pretending to
     * be somewhere else in a debug build, the real position doesn't apply, so it's the state's usual.
     */
    private fun currentRegion(): Region {
        val current = _state.value
        val point = if (current.debugState == null) current.fix?.point ?: stateCell?.center else null
        return regionAt(currentState, point)
    }

    /** Asks the geocoder again straight away, after the player has found a signal. */
    fun retryState() {
        lastLookupMillis = 0L
        _state.update { it.copy(status = WorldStatus.LocatingState) }
        _state.value.fix?.let(::updateState) ?: offerCases()
    }

    /**
     * Until the player names the creature, they can drop the case and pick a different one. The
     * next case gets the same trail, so dropping isn't a way to reroll it.
     */
    fun returnCase() {
        val phase = _state.value.activeCase?.phase
        if (phase != CasePhase.LEAD && phase != CasePhase.INVESTIGATING) return
        saveCase(null)
        offerCases()
    }

    /** Sets the investigation aside, clues and all, and goes back to the list of cases. */
    fun pauseCase() {
        val current = _state.value
        val case = current.activeCase
        if (!canPause(case, current.pausedCases) || case == null) return
        val paused = current.pausedCases + case
        prefs.edit {
            putInt(KEY_SLOT, prefs.getInt(KEY_SLOT, 0) + 1)
            putString(KEY_PAUSED, json.encodeToString(ListSerializer(ActiveCase.serializer()), paused))
            remove(KEY_CASE)
        }
        _state.update { it.copy(activeCase = null, pausedCases = paused) }
        offerCases()
    }

    /** Picks a paused investigation back up where the player left it. */
    fun resumeCase(caseId: String) {
        val current = _state.value
        if (current.activeCase != null) return
        val case = current.pausedCases.firstOrNull { it.caseId == caseId } ?: return
        val paused = current.pausedCases - case
        prefs.edit {
            putString(KEY_PAUSED, json.encodeToString(ListSerializer(ActiveCase.serializer()), paused))
            putString(KEY_CASE, json.encodeToString(ActiveCase.serializer(), case))
        }
        _state.update { it.copy(activeCase = case, pausedCases = paused, status = WorldStatus.Ready) }
    }

    fun startInvestigating() = changeCase {
        it.copy(phase = CasePhase.INVESTIGATING, startedAtMillis = System.currentTimeMillis())
    }

    fun deduce(suspectId: String) = changeCase {
        it.copy(deductionId = suspectId, phase = CasePhase.ENCOUNTER)
    }

    /** Files the encounter's [photos]. The best of them is what the journal rank goes by. */
    fun finishEncounter(photos: List<Photo>) {
        val case = _state.value.activeCase ?: return
        if (case.phase != CasePhase.ENCOUNTER) return
        val done = case.copy(
            photoScore = photos.maxOfOrNull { it.score } ?: 0,
            photos = photos,
            phase = CasePhase.DONE,
            finishedAtMillis = System.currentTimeMillis(),
        )
        val definition = caseById(done.caseId) ?: return
        journalEntryFor(done, definition, UUID.randomUUID().toString())?.let(::addToJournal)
        saveCase(done)
    }

    private fun addToJournal(entry: JournalEntry) {
        val journal = _state.value.journal + entry
        prefs.edit { putString(KEY_JOURNAL, json.encodeToString(ListSerializer(JournalEntry.serializer()), journal)) }
        _state.update { it.copy(journal = journal) }
    }

    fun takeAnotherCase() {
        prefs.edit { putInt(KEY_SLOT, prefs.getInt(KEY_SLOT, 0) + 1) }
        saveCase(null)
        offerCases()
    }

    /** Debug builds only: counts as walking [meters], so a case can be played sitting down. */
    fun debugWalk(meters: Double = DEBUG_WALK_METERS) {
        if (!BuildConfig.DEBUG) return
        val case = _state.value.activeCase ?: return
        if (case.phase != CasePhase.INVESTIGATING) return
        saveCase(search(case.copy(walkedMeters = case.walkedMeters + meters), meters))
    }

    /** Debug builds only: shows or hides the button that counts as walking, and remembers the choice. */
    fun setDebugWalkButton(shown: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit { putBoolean(KEY_DEBUG_WALK_BUTTON, shown) }
        _state.update { it.copy(debugWalkButton = shown) }
    }

    /** Debug builds only: steps through the states that have cases, then back to the real one. */
    fun debugNextState() {
        if (!BuildConfig.DEBUG) return
        val options = STATE_NAMES.keys.filter { casesIn(it).isNotEmpty() }
        val next = when (val now = _state.value.debugState) {
            null -> options.first()
            options.last() -> null
            else -> options[options.indexOf(now) + 1]
        }
        _state.update { it.copy(debugState = next, areaState = next ?: knownState) }
        offerCases()
    }

    private fun changeCase(change: (ActiveCase) -> ActiveCase) {
        _state.value.activeCase?.let { saveCase(change(it)) }
    }

    private fun saveCase(case: ActiveCase?) {
        prefs.edit {
            if (case == null) {
                remove(KEY_CASE)
            } else {
                putString(KEY_CASE, json.encodeToString(ActiveCase.serializer(), case))
            }
        }
        _state.update { it.copy(activeCase = case) }
    }

    private fun loadStateCell(): Cell? = place.getString(KEY_STATE_CELL, null)?.let { saved ->
        runCatching { json.decodeFromString(Cell.serializer(), saved) }.getOrNull()
    }

    private fun loadJournal(): List<JournalEntry> = prefs.getString(KEY_JOURNAL, null)?.let { saved ->
        runCatching { json.decodeFromString(ListSerializer(JournalEntry.serializer()), saved) }.getOrNull()
    }.orEmpty()

    /** A saved case whose definition no longer exists, or from the map version, is dropped. */
    private fun loadCase(): ActiveCase? = prefs.getString(KEY_CASE, null)?.let { saved ->
        runCatching { json.decodeFromString(ActiveCase.serializer(), saved) }.getOrNull()
    }?.takeIf { caseById(it.caseId) != null }

    private fun loadPaused(): List<ActiveCase> = prefs.getString(KEY_PAUSED, null)?.let { saved ->
        runCatching { json.decodeFromString(ListSerializer(ActiveCase.serializer()), saved) }.getOrNull()
    }.orEmpty().filter { caseById(it.caseId) != null }

    private companion object {
        /** The player's progress and settings. Auto Backup copies this file. */
        const val PREFS_GAME = "game"

        /** The last state lookup and the cell it was made in. Never backed up. */
        const val PREFS_PLACE = "place"

        /**
         * The place prefs, moving the last lookup out of the game prefs if an older version left
         * it there.
         */
        private fun placePrefs(context: Context): SharedPreferences {
            val game = context.getSharedPreferences(PREFS_GAME, Context.MODE_PRIVATE)
            val place = context.getSharedPreferences(PREFS_PLACE, Context.MODE_PRIVATE)
            if (game.contains(KEY_STATE) || game.contains(KEY_STATE_CELL)) {
                place.edit {
                    game.getString(KEY_STATE, null)?.let { putString(KEY_STATE, it) }
                    game.getString(KEY_STATE_CELL, null)?.let { putString(KEY_STATE_CELL, it) }
                }
                game.edit {
                    remove(KEY_STATE)
                    remove(KEY_STATE_CELL)
                }
            }
            return place
        }

        const val KEY_SAFETY = "safety_accepted"
        const val KEY_SLOT = "case_slot"
        const val KEY_CASE = "active_case"
        const val KEY_PAUSED = "paused_cases"
        const val KEY_JOURNAL = "journal"
        const val KEY_STATE = "state_code"
        const val KEY_STATE_CELL = "state_cell"
        const val KEY_DEBUG_WALK_BUTTON = "debug_walk_button"
        const val LOOKUP_RETRY_MILLIS = 30_000L
        const val DEBUG_WALK_METERS = 25.0
    }
}
