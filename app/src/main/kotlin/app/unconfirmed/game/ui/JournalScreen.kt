package app.unconfirmed.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.shared.ClueType
import app.unconfirmed.shared.CreaturePage
import app.unconfirmed.shared.Evidence
import app.unconfirmed.shared.JournalEntry
import app.unconfirmed.shared.JournalRank
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.bestPhoto
import app.unconfirmed.shared.caseById
import app.unconfirmed.shared.casesIn
import app.unconfirmed.shared.journalPages
import app.unconfirmed.shared.metCreatures
import app.unconfirmed.shared.reachedStates
import app.unconfirmed.shared.regionOf
import app.unconfirmed.shared.stateName
import java.text.DateFormat
import java.util.Date

/** Where the handwriting starts, to the right of the red margin. */
private val MARGIN = 40.dp

/**
 * The player's investigation journal, as a handwritten notebook: one entry per creature, each
 * with a snapshot taped in that gets clearer as the record improves, and the case reports filed
 * behind it.
 */
@Composable
fun JournalScreen(entries: List<JournalEntry>, areaState: String?, onBack: () -> Unit) {
    var openCreatureId by rememberSaveable { mutableStateOf<String?>(null) }
    val pages = journalPages(entries, areaState)
    val open = pages.firstOrNull { it.creatureId == openCreatureId }

    BackHandler { if (open != null) openCreatureId = null else onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .desk()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (open == null) {
            InkButton("← Back", onBack, fill = Paper)
            JournalIndex(pages, entries, areaState) { openCreatureId = it }
        } else {
            InkButton("← Back to the journal", { openCreatureId = null }, fill = Paper)
            CreatureDetail(open)
        }
    }
}

@Composable
private fun NotebookPage(seed: Int, content: @Composable () -> Unit) {
    PaperSheet(seed = seed, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .ruledLines(margin = MARGIN - 8.dp)
                .padding(start = MARGIN, end = 16.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
}

@Composable
private fun JournalIndex(
    pages: List<CreaturePage>,
    entries: List<JournalEntry>,
    areaState: String?,
    onOpen: (String) -> Unit,
) {
    NotebookPage(seed = 21) {
        Text("Field Journal", fontFamily = HandFont, fontSize = 36.sp, lineHeight = 40.sp)
        // No tallies here: what's left to find is for the player to wonder about.
        Text(
            if (entries.isEmpty()) {
                "No reports filed yet. Close a case and I'll write it up here."
            } else {
                "Everything I've seen, and everything I've only heard about."
            },
            fontFamily = HandFont,
            fontSize = 18.sp,
            color = BluePencil,
        )
        areaState?.let { Frontier(it, entries) }
        Spacer(Modifier.height(4.dp))
        pages.forEach { page -> CreatureRow(page, onOpen) }
    }
}

/**
 * A note on how far word of the player's work has spread: a nudge that their own state still has
 * more to find before the neighboring states' reports start coming in, without saying how much,
 * or which states' reports are reaching them now.
 */
@Composable
private fun Frontier(home: String, entries: List<JournalEntry>) {
    val met = metCreatures(entries)
    if (casesIn(home).isEmpty()) return
    val reached = reachedStates(home, met)
    val note = if (reached.isEmpty()) {
        "There's more out there in ${stateName(home)}. Find it all and its neighbors will start sending their reports."
    } else {
        // A few of the nearest by name, and the rest left to the imagination.
        val names = reached.take(3).map(::stateName)
        val list = when {
            reached.size > names.size -> names.joinToString(", ") + " and farther afield"
            names.size == 1 -> names.single()
            else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
        }
        "Word has spread beyond ${stateName(home)}. Reports are coming in from $list."
    }
    Text(note, fontFamily = HandFont, fontSize = 17.sp, lineHeight = 22.sp, color = RedPencil)
}

/** One creature on the contents page: its snapshot, its name and the best stamp it's earned. */
@Composable
private fun CreatureRow(page: CreaturePage, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${page.name}, ${(page.best ?: JournalRank.RUMORED).label}" }
            .clickable(role = Role.Button) { onOpen(page.creatureId) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Snapshot(
            page.creatureId,
            page.best,
            bestPhoto(page.entries),
            width = 76.dp,
            tilt = tiltFor(page.creatureId, 4f),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(page.name, fontFamily = HandFont, fontSize = 22.sp, lineHeight = 24.sp)
            Text(
                stateName(page.state) + when (page.entries.size) {
                    0 -> ""
                    1 -> " · 1 report"
                    else -> " · ${page.entries.size} reports"
                },
                fontFamily = HandFont,
                fontSize = 15.sp,
                color = Ink.copy(alpha = 0.6f),
            )
            RankStamp(page.best, Modifier.padding(top = 4.dp), angle = tiltFor(page.name, 7f))
        }
    }
}

@Composable
private fun CreatureDetail(page: CreaturePage) {
    NotebookPage(seed = page.creatureId.hashCode()) {
        Text(page.name, fontFamily = HandFont, fontSize = 34.sp, lineHeight = 38.sp)
        Text("Reported in ${stateName(page.state)}", fontFamily = HandFont, fontSize = 17.sp, color = BluePencil)
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Snapshot(
                page.creatureId,
                page.best,
                bestPhoto(page.entries),
                width = 200.dp,
                tilt = tiltFor(page.creatureId, 3f),
                modifier = Modifier.align(Alignment.Center),
            )
            RankStamp(
                page.best,
                Modifier.align(Alignment.BottomEnd).offset(y = 6.dp),
                angle = -12f,
                size = 16,
            )
        }
        Text(
            "Field notes",
            fontFamily = HandFont,
            fontSize = 22.sp,
            textDecoration = TextDecoration.Underline,
        )
        Text(page.fieldNotes, fontFamily = HandFont, fontSize = 18.sp, lineHeight = 26.sp)
        EvidenceSection(page)
        Text(
            "Reports",
            fontFamily = HandFont,
            fontSize = 22.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (page.entries.isEmpty()) {
            Text(
                "Nothing filed yet. Only rumors so far.",
                fontFamily = HandFont,
                fontSize = 18.sp,
                color = Ink.copy(alpha = 0.6f),
            )
        }
    }
    page.entries.forEach { ReportSlip(it) }
}

/** A typed report, clipped in behind the creature's page, with the photos from that encounter. */
@Composable
private fun ReportSlip(entry: JournalEntry) {
    var viewing by rememberSaveable { mutableStateOf<Int?>(null) }
    val scene = caseById(entry.caseId)?.scene
    val region = entry.region ?: regionOf(entry.state)
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.finishedAtMillis))
    val minutes = entry.startedAtMillis?.let { ((entry.finishedAtMillis - it) / 60_000).coerceAtLeast(1) }
    val tilt = tiltFor(entry.id, 1.2f)
    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        PaperSheet(
            fill = Card,
            seed = entry.id.hashCode(),
            tilt = tilt,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FIELD REPORT", fontFamily = FieldFont, fontSize = 11.sp, letterSpacing = 2.sp, color = RedPencil)
                Row(verticalAlignment = Alignment.Top) {
                    Text(entry.caseTitle, fontFamily = FieldFont, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    RankStamp(entry.rank, angle = 6f)
                }
                Text(
                    "$date · ${stateName(entry.state)}",
                    fontFamily = FieldFont,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Ink.copy(alpha = 0.65f),
                )
                Text(
                    "Clues ${entry.cluesFound} of ${entry.cluesTotal} · photo ${entry.photoScore} of 100 · " +
                        formatDistance(entry.walkedMeters) + " walked" +
                        (minutes?.let { " in $it min" } ?: ""),
                    fontFamily = FieldFont,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                if (entry.clueIds.isNotEmpty()) {
                    val definition = caseById(entry.caseId)
                    val found = entry.clueIds.mapNotNull { id ->
                        definition?.clues?.firstOrNull { it.id == id }?.type?.title
                    }
                    Text(
                        "Found: " + found.joinToString(", "),
                        fontFamily = FieldFont,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
                Text(
                    if (entry.correct) {
                        "Named it correctly."
                    } else {
                        "Said it was the ${entry.namedName ?: "nothing"}. It was the ${entry.creatureName}."
                    },
                    fontFamily = HandFont,
                    fontSize = 18.sp,
                    color = if (entry.correct) BluePencil else RedPencil,
                )
                if (scene != null && entry.photos.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        entry.photos.forEachIndexed { index, photo ->
                            PhotoPrint(
                                photo,
                                entry.creatureId,
                                scene,
                                region,
                                width = 88.dp,
                                caption = null,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = tiltFor("${entry.id}-$index", 5f)
                                },
                                onOpen = { viewing = index },
                            )
                        }
                    }
                }
            }
        }
        TapeStrip(Modifier.align(Alignment.TopCenter), angle = tilt * 4)
    }
    val open = viewing?.let { entry.photos.getOrNull(it) }
    if (open != null && scene != null) {
        PhotoViewer(open, entry.creatureId, scene, region) { viewing = null }
    }
}

/**
 * The clues found on this creature so far, one card for each kind, taped in two to a row. A kind of
 * clue its cases can turn up but the player hasn't found yet is an empty pencilled box, so it's
 * plain what's still out there. Tapping a card shows it full size with its note.
 */
@Composable
private fun EvidenceSection(page: CreaturePage) {
    if (page.clueTypes.isEmpty()) return
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }
    Text(
        "Evidence",
        fontFamily = HandFont,
        fontSize = 22.sp,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.padding(top = 8.dp),
    )
    val found = page.evidence.associateBy { it.clue.type }
    page.clueTypes.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { type ->
                val evidence = found[type]
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (evidence != null) {
                        Box(
                            Modifier
                                .graphicsLayer { rotationZ = tiltFor(evidence.clue.id, 2.5f) }
                                .clickable(role = Role.Button) { viewing = type.name },
                        ) {
                            ClueArt(type, evidence.trait, evidence.seed, Modifier.padding(end = 3.dp, bottom = 3.dp))
                        }
                    } else {
                        MissingClue(type)
                    }
                    Text(
                        type.title,
                        fontFamily = HandFont,
                        fontSize = 15.sp,
                        lineHeight = 17.sp,
                        color = if (evidence != null) Ink else Ink.copy(alpha = 0.5f),
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    val open = viewing?.let { name -> page.evidence.firstOrNull { it.clue.type.name == name } }
    if (open != null) EvidenceViewer(open) { viewing = null }
}

/** The same wobble the card had in the case file, so it looks like the one the player found. */
private val Evidence.seed: Int get() = (caseId + clue.id).hashCode()

/** An empty, dashed-pencil box where a kind of clue not yet found will go. */
@Composable
private fun MissingClue(type: ClueType) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .padding(end = 3.dp, bottom = 3.dp)
            .semantics { contentDescription = "${type.title}: not found yet" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Ink.copy(alpha = 0.4f),
                style = Stroke(
                    1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
        Text("not found yet", fontFamily = HandFont, fontSize = 15.sp, color = Ink.copy(alpha = 0.45f))
    }
}

/** A found clue, full size, with the note that came with it. */
@Composable
private fun EvidenceViewer(evidence: Evidence, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(evidence.clue.type.title, fontFamily = FontFamily.Serif) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ClueArt(evidence.clue.type, evidence.trait, evidence.seed, Modifier.padding(end = 4.dp, bottom = 4.dp))
                Spacer(Modifier.height(14.dp))
                Text(evidence.clue.text)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}

/**
 * A snapshot taped into the journal. A creature that's only rumoured has no photo yet, just a
 * pencilled question mark. Once seen, it's the best photo the player took of it. An entry filed
 * before photos were kept, or with nothing on film, gets a stand-in that sharpens with each
 * better rank.
 */
@Composable
private fun Snapshot(
    creatureId: String,
    rank: JournalRank?,
    photo: Pair<JournalEntry, Photo>?,
    width: Dp,
    tilt: Float,
    modifier: Modifier = Modifier,
) {
    val scene = photo?.let { (entry, _) -> caseById(entry.caseId)?.scene }
    Box(modifier.width(width).graphicsLayer { rotationZ = tilt }) {
        PaperSheet(fill = Card, seed = creatureId.hashCode(), shadow = 3.dp, modifier = Modifier.padding(top = 6.dp)) {
            Box(
                Modifier
                    .padding(start = width * 0.06f, end = width * 0.06f, top = width * 0.06f, bottom = width * 0.2f)
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                if (photo != null && scene != null) {
                    PhotoImage(
                        photo.second,
                        creatureId,
                        scene,
                        photo.first.let {
                            it.region ?: regionOf(it.state)
                        },
                        Modifier.fillMaxSize(),
                    )
                    Canvas(Modifier.matchParentSize()) { drawRect(Ink, style = Stroke(1.dp.toPx())) }
                    return@Box
                }
                NightPhoto(Modifier.fillMaxSize())
                if (rank == null || rank == JournalRank.RUMORED) {
                    Text(
                        "?",
                        fontFamily = HandFont,
                        fontSize = (width.value * 0.45f).sp,
                        color = Paper.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val blur = when (rank) {
                        JournalRank.SIGHTED -> width * 0.04f
                        JournalRank.DOCUMENTED -> width * 0.012f
                        else -> 0.dp
                    }
                    Image(
                        painterResource(creatureArt(creatureId)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(width * 0.06f).blur(blur),
                    )
                }
            }
        }
        TapeStrip(Modifier.align(Alignment.TopCenter), angle = -tilt * 2, width = width * 0.45f)
    }
}

/** The dark of a night-time photo, with a bit of moon. */
@Composable
private fun NightPhoto(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF26344A), Color(0xFF3B4A55), Color(0xFF1C2420))))
        drawCircle(
            Color(0xFFE9E2C6).copy(alpha = 0.7f),
            size.minDimension * 0.07f,
            Offset(
                size.width * 0.78f,
                size.height * 0.2f,
            ),
        )
        drawRect(Color(0xFF151C17), topLeft = Offset(0f, size.height * 0.8f))
        drawRect(Ink, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun RankStamp(rank: JournalRank?, modifier: Modifier = Modifier, angle: Float = -6f, size: Int = 12) {
    val shown = rank ?: JournalRank.RUMORED
    RubberStamp(shown.label, rankInk(shown), modifier, angle, size)
}

/** Stamp inks that read on paper. */
private fun rankInk(rank: JournalRank): Color = when (rank) {
    JournalRank.RUMORED -> Color(0xFF857A68)
    JournalRank.SIGHTED -> BluePencil
    JournalRank.DOCUMENTED -> Color(0xFFB0701A)
    JournalRank.CONFIRMED -> Color(0xFF3E7A35)
}
