package app.unconfirmed.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.unconfirmed.game.LocalSound
import app.unconfirmed.game.WorldStatus
import app.unconfirmed.shared.ActiveCase
import app.unconfirmed.shared.CaseDefinition
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.caseById
import app.unconfirmed.shared.stateName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The case list, as a stack of the local paper on the desk: each open report has a front page of
 * its own, and the player swipes the top paper aside to see the next. Paused cases are clippings
 * the player has kept above the stack.
 *
 * Debug builds also print every other state's papers, one stack per state, and scrolling up or
 * down moves between the stacks.
 */
@Composable
fun CaseSelectionScreen(
    status: WorldStatus.ChoosingCase,
    paused: List<ActiveCase>,
    onChoose: (String) -> Unit,
    onResume: (String) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenRequest: (() -> Unit)?,
    onDebugNextState: (() -> Unit)? = null,
    /** Debug builds only: whether the +25 m button is on, and how to flip it. Null hides the switch. */
    debugWalkButton: Pair<Boolean, (Boolean) -> Unit>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .desk()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Unconfirmed",
                fontFamily = HandFont,
                fontSize = 22.sp,
                color = Paper.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
            )
            InkButton("Field journal", onOpenJournal)
        }

        if (paused.isNotEmpty()) PausedClippings(paused, status.state, onResume)

        val editions = remember(status) { editionsOf(status) }
        val hasPaused = paused.isNotEmpty()
        if (editions.size == 1) {
            PaperStack(editions.single(), status, hasPaused, onChoose, Modifier.weight(1f))
        } else {
            val home = editions.indexOfFirst { it.state == status.state }.coerceAtLeast(0)
            val pager = rememberPagerState(initialPage = home) { editions.size }
            // Open on the player's own state, and go back to it whenever the app comes back to the
            // front or they pretend to be somewhere else, rather than wherever they last scrolled.
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(status.state, lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { pager.scrollToPage(home) }
            }
            VerticalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                pageSpacing = 16.dp,
                // A state's page is nearly the whole screen, so half of one is a long way to drag.
                flingBehavior = PagerDefaults.flingBehavior(pager, snapPositionalThreshold = 0.2f),
                key = { editions[it].state ?: "" },
            ) { page ->
                val edition = editions[page]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Debug: ${stateName(edition.state)} · ${page + 1} of ${editions.size}" +
                            (if (edition.state == status.state) " · you are here" else "") +
                            " · scroll for other states",
                        fontFamily = HandFont,
                        fontSize = 15.sp,
                        color = Amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PaperStack(edition, status, hasPaused, onChoose, Modifier.weight(1f))
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            onDebugNextState?.let { DeskLink("Debug: elsewhere", onClick = it) }
            debugWalkButton?.let { (shown, setShown) ->
                DeskLink(if (shown) "+25m on" else "+25m off") { setShown(!shown) }
            }
            LocalSound.current?.let { sound ->
                DeskLink(if (sound.enabled) "Sound: on" else "Sound: off") { sound.turnOn(!sound.enabled) }
            }
            onOpenRequest?.let { DeskLink("Request a cryptid", onClick = it) }
            DeskLink("About", onClick = onOpenAbout)
        }
    }
}

/** One state's papers: the cases reported in [state], each printed on a front page of its own. */
private data class Edition(val state: String?, val cases: List<CaseDefinition>)

/**
 * The player's own state's papers, and in debug builds every other state's too, in alphabetical
 * order by the state's name. The player's state is always there, even on a slow news day. Stories
 * off the wire from neighboring states go in the player's own pile, after the local ones.
 */
private fun editionsOf(status: WorldStatus.ChoosingCase): List<Edition> {
    val home = Edition(status.state, status.local + status.wire)
    if (status.elsewhere.isEmpty()) return listOf(home)
    val others = status.elsewhere
        .filter { it.state != status.state }
        .groupBy { it.state }
        .map { (state, cases) -> Edition(state, cases) }
    return (others + home).sortedBy { stateName(it.state) }
}

/** How far the top paper has to be pulled, as a share of its width, to go on to the next. */
private const val TURN_THRESHOLD = 0.25f

/** A flick faster than this, in widths a second, turns the paper however far it was pulled. */
private const val FLING_SPEED = 1.2f

/** How many papers show under the top one. Any further down are hidden by the pile. */
private const val PILE_DEPTH = 3

/** How far past the last paper it can be pulled, as a share of its width, before it springs back. */
private const val LAST_PAPER_GIVE = 0.12f

/**
 * A pile of papers, one per case in [edition]. The top one can be read and tapped; swiping it left
 * slides it off to show the one beneath, and swiping right brings the last one back on top.
 *
 * Where the pile is is a single number, [position], counted in papers: 1.4 means the first paper
 * is gone, and the second is 40% of the way off. Every paper's place, tilt and depth is worked out
 * from it, so dragging, flinging and the arrow buttons all move the papers the same way.
 */
@Composable
private fun PaperStack(
    edition: Edition,
    status: WorldStatus.ChoosingCase,
    hasPaused: Boolean,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cases = edition.cases
    val count = cases.size.coerceAtLeast(1)
    val last = (count - 1).toFloat()
    val position = remember(edition.state) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(count) { if (position.value > last) position.snapTo(last) }

    val base by remember { derivedStateOf { floor(position.value).toInt().coerceIn(0, count - 1) } }
    val showing by remember { derivedStateOf { position.value.roundToInt().coerceIn(0, count - 1) } }
    // The paper on top when the current drag began; letting go turns at most one paper from it.
    var from by remember { mutableIntStateOf(0) }

    fun turnTo(paper: Int) {
        scope.launch {
            position.animateTo(
                paper.coerceIn(0, count - 1).toFloat(),
                spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics {
                    // No count of how many papers there are, to keep what's out there a mystery.
                    stateDescription = if (showing < count - 1) "More papers underneath" else "Last paper"
                    customActions = buildList {
                        if (showing < count - 1) {
                            add(
                                CustomAccessibilityAction("Next paper") {
                                    turnTo(showing + 1)
                                    true
                                },
                            )
                        }
                        if (showing > 0) {
                            add(
                                CustomAccessibilityAction("Previous paper") {
                                    turnTo(showing - 1)
                                    true
                                },
                            )
                        }
                    }
                },
        ) {
            val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val drag = rememberDraggableState { delta ->
                val next = position.value - delta / width
                // Past the last paper it gives a little, with resistance, then springs back.
                val resisted = if (next > last) last + (next - last) * 0.35f else next
                scope.launch { position.snapTo(resisted.coerceIn(0f, last + LAST_PAPER_GIVE)) }
            }
            val pile = Modifier.draggable(
                state = drag,
                orientation = Orientation.Horizontal,
                enabled = count > 1,
                onDragStarted = { from = showing },
                onDragStopped = { velocity ->
                    val speed = velocity / width
                    val pulled = position.value - from
                    turnTo(
                        when {
                            speed < -FLING_SPEED || (speed <= 0f && pulled >= TURN_THRESHOLD) -> from + 1
                            speed > FLING_SPEED || (speed >= 0f && pulled <= -TURN_THRESHOLD) -> from - 1
                            else -> from
                        },
                    )
                },
            )
            Box(pile.fillMaxSize().padding(bottom = 18.dp)) {
                // Drawn from the bottom of the pile up, so the top paper is drawn last, over the rest.
                val lowest = (base + PILE_DEPTH).coerceAtMost(count - 1)
                for (index in lowest downTo base) {
                    val case = cases.getOrNull(index)
                    key(case?.id ?: "slow-news") {
                        val seed = case?.id?.hashCode() ?: edition.state.hashCode()
                        // Only the top two can be seen; the rest are just edges showing under them.
                        val printed = index <= base + 1
                        LoosePaper(
                            index = index,
                            seed = seed,
                            position = { position.value },
                            width = width,
                        ) {
                            FrontPage(
                                edition = edition,
                                case = case,
                                status = status,
                                number = index + 1,
                                hasPaused = hasPaused,
                                printed = printed,
                                onTop = index == showing,
                                onChoose = onChoose,
                            )
                        }
                    }
                }
            }
        }
        if (count > 1) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DeskLink("← Back", enabled = showing > 0) { turnTo(showing - 1) }
                Text(
                    if (showing < count - 1) "swipe for more" else "that's all for today",
                    fontFamily = HandFont,
                    fontSize = 15.sp,
                    color = Paper.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                DeskLink("Next →", enabled = showing < count - 1) { turnTo(showing + 1) }
            }
        }
    }
}

/**
 * One paper in the pile, placed from the pile's [position]. A paper still on the pile sits a little
 * lower and more askew the deeper it is; one being taken off slides away to the left, turning as
 * it goes. All of it is worked out while drawing, so a drag doesn't recompose the page.
 */
@Composable
private fun LoosePaper(index: Int, seed: Int, position: () -> Float, width: Float, content: @Composable () -> Unit) {
    val askew = paperNoise(seed, 7) * 2.4f
    val sideways = paperNoise(seed, 8)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // How far down the pile this paper is: 0 on top, 1 just under it, below 0 going off.
                val depth = index - position()
                if (depth <= 0f) {
                    translationX = depth * width * 1.15f
                    rotationZ = depth * 9f
                } else {
                    val settled = depth.coerceAtMost(1f)
                    val shown = depth.coerceAtMost(PILE_DEPTH.toFloat())
                    translationY = shown * 6.dp.toPx()
                    translationX = sideways * settled * 5.dp.toPx()
                    rotationZ = askew * settled
                    scaleX = 1f - 0.015f * shown
                    scaleY = scaleX
                }
                alpha = if (depth <= -1f) 0f else 1f
            },
    ) {
        content()
    }
}

/**
 * The front page for [case]: masthead, the story with its photo, and the ad below it. Papers deep
 * in the pile, which can't be read, are left blank ([printed] false) so they cost nothing to draw.
 * A null [case] is the slow-news-day paper.
 */
@Composable
private fun FrontPage(
    edition: Edition,
    case: CaseDefinition?,
    status: WorldStatus.ChoosingCase,
    number: Int,
    hasPaused: Boolean,
    printed: Boolean,
    onTop: Boolean,
    onChoose: (String) -> Unit,
) {
    val seed = case?.id?.hashCode() ?: edition.state.hashCode()
    PaperSheet(fill = Newsprint, seed = seed, shadow = 3.dp, modifier = Modifier.fillMaxSize()) {
        if (!printed) return@PaperSheet
        val name = stateName(edition.state)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Masthead(name, number)
            Spacer(Modifier.height(10.dp))
            if (case == null) {
                SlowNewsDay(name, hasPaused, Modifier.weight(1f))
            } else {
                LeadStory(
                    case = case,
                    region = status.regionOf(case),
                    headline = status.headlineOf(case),
                    teaser = status.teaserOf(case),
                    wireFrom = case.state.takeIf { it != edition.state && case in status.wire },
                    enabled = onTop,
                    onChoose = onChoose,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            ThickRule()
            // Ruled off from the story above and set below it, so the ad can't be mistaken for
            // the case or tapped by accident while picking it.
            AdSlot(Modifier.padding(vertical = 8.dp), index = number - 1)
            ThickRule()
        }
    }
}

@Composable
private fun Masthead(stateName: String, number: Int) {
    val today = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("The Night Ledger", fontFamily = MastheadFont, fontSize = 38.sp, lineHeight = 42.sp, maxLines = 1)
        ThickRule()
        Text(
            "${stateName.uppercase()} EDITION  ·  ${today.uppercase()}  ·  NO. $number",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 3.dp),
        )
        ThickRule()
    }
}

/**
 * The paper's one story: a big headline across the page, then a photo and the start of the report.
 * The photo takes whatever height the page has left, so the story always fits on a single sheet.
 */
@Composable
private fun LeadStory(
    case: CaseDefinition,
    region: Region,
    headline: String,
    teaser: String,
    /** The neighboring state a story came in from over the wire, or null for a local one. */
    wireFrom: String?,
    enabled: Boolean,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kicker = wireFrom?.let { "Over the wire from ${stateName(it)}" }
    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = listOfNotNull(kicker, headline, teaser).joinToString(". ") }
            .clickable(enabled = enabled, role = Role.Button) { onChoose(case.id) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (kicker != null) {
            Text(
                "★ ${kicker.uppercase()} ★",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = RedPencil,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        BasicText(
            headline.uppercase(),
            style = TextStyle(
                fontFamily = HeadlineFont,
                lineHeight = 1.05.em,
                textAlign = TextAlign.Center,
                color = Ink,
            ),
            maxLines = 3,
            autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 30.sp, stepSize = 2.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            teaser,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        // Set in two columns, as a paper would: the photo on the left, the report running down
        // beside it and cut off wherever the page runs out.
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(0.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NewsPhoto(
                    case.scene,
                    region,
                    case.id.hashCode(),
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).weight(1f, fill = false).aspectRatio(0.8f, true),
                )
                Text(
                    "A reader’s photo, taken near the scene. Blurry, as always.",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Ink.copy(alpha = 0.7f),
                )
            }
            Column(Modifier.weight(0.55f).fillMaxHeight()) {
                Dateline(case, region, case.lead, Modifier.weight(1f, fill = false))
                Investigate(Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}

/** The opening of a report: where it's from in small capitals, then [body], cut off to fit. */
@Composable
private fun Dateline(case: CaseDefinition, region: Region, body: String, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
            append(
                "${(case.place ?: case.scene.place(region)).uppercase()}, ${stateName(case.state).uppercase()} — ",
            )
            pop()
            append(body)
        },
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A note in red pencil in the margin, so the story reads as something to tap. */
@Composable
private fun Investigate(modifier: Modifier = Modifier) {
    Text("Investigate →", fontFamily = HandFont, fontSize = 18.sp, color = RedPencil, modifier = modifier)
}

@Composable
private fun SlowNewsDay(stateName: String, hasPaused: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text(
            "NOTHING STRANGE IN ${stateName.uppercase()}",
            fontFamily = HeadlineFont,
            fontSize = 30.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (hasPaused) {
                "No other sightings have been reported in $stateName yet. Your open cases are still waiting."
            } else {
                "A slow news day. No sightings have been reported in $stateName yet."
            },
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The stories the player clipped out and kept, side by side above the pile. */
@Composable
private fun PausedClippings(paused: List<ActiveCase>, here: String?, onResume: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Cases you're still working",
            fontFamily = HandFont,
            fontSize = 15.sp,
            color = Paper.copy(alpha = 0.8f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            paused.forEachIndexed { index, case ->
                caseById(case.caseId)?.let { definition ->
                    PausedClipping(case, definition, here, index, onResume, Modifier.weight(1f))
                }
            }
            // A lone clipping takes half the desk, not all of it.
            if (paused.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** A story the player clipped out and kept, with how far they'd got. */
@Composable
private fun PausedClipping(
    case: ActiveCase,
    definition: CaseDefinition,
    here: String?,
    index: Int,
    onResume: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tilt = if (index % 2 == 0) -1.6f else 1.3f
    Box(modifier) {
        PaperSheet(
            fill = Newsprint,
            seed = definition.id.hashCode(),
            tilt = tilt,
            shadow = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .semantics { contentDescription = "Paused: ${case.title(definition)}" }
                .clickable(role = Role.Button) { onResume(case.caseId) },
        ) {
            Column(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    case.title(definition).uppercase(),
                    fontFamily = HeadlineFont,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${case.cluesFound}/${case.clues(definition).size} clues · " +
                        if (case.state == here) "Resume →" else "In ${stateName(case.state)} →",
                    fontFamily = HandFont,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    color = RedPencil,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TapeStrip(Modifier.align(Alignment.TopCenter), angle = tilt * 3, width = 44.dp)
    }
}

@Composable
private fun DeskLink(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = HandFont,
        fontSize = 16.sp,
        color = Paper.copy(alpha = if (enabled) 0.6f else 0.2f),
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun ThickRule() {
    HorizontalDivider(thickness = 2.dp, color = Ink)
    Spacer(Modifier.height(2.dp))
    HorizontalDivider(thickness = 0.5.dp, color = Ink)
}

/** Where a story is filed from, for its dateline, in the words used in that part of the country. */
private fun Scene.place(region: Region): String = when (this) {
    Scene.LAKE_SHORE -> if (region == Region.HAWAII) "The shore" else "Lakeshore"
    Scene.TIMBER -> when (region) {
        Region.NORTHWEST, Region.ALASKA -> "Timber country"
        Region.DESERT -> "Pine country"
        Region.SOUTH -> "Piney woods"
        Region.HAWAII -> "Upland forest"
        Region.PLAINS -> "Creek bottoms"
        Region.MIDWEST, Region.EAST -> "The deep woods"
    }
    Scene.PASTURE -> when (region) {
        Region.NORTHWEST, Region.DESERT, Region.PLAINS -> "Ranch country"
        Region.MIDWEST, Region.EAST, Region.SOUTH -> "Farm country"
        Region.ALASKA -> "Out on the tundra"
        Region.HAWAII -> "Upcountry"
    }
    Scene.RIDGE -> when (region) {
        Region.DESERT -> "Mesa country"
        Region.EAST -> "Up on the ridge"
        else -> "High ridge"
    }
    Scene.RIVER_TOWN -> "Riverside"
    Scene.PINE_BARRENS -> "Pine Barrens"
}

/**
 * A grainy newspaper photo of the kind of place the story is set in, printed in halftone dots,
 * with a dark smudge somewhere that might be anything. The smudge is the same for every creature,
 * so the photo never gives the answer away.
 */
@Composable
private fun NewsPhoto(scene: Scene, region: Region, seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF8E8A80), Color(0xFFB9B4A6)), endY = size.height * 0.6f),
        )
        val horizon = size.height * 0.58f
        drawRect(Color(0xFF5E5A52), topLeft = Offset(0f, horizon), size = Size(size.width, size.height - horizon))
        drawScenery(scene, region, horizon, seed)
        val smudge = Offset(size.width * (0.3f + 0.4f * (paperNoise(seed, 5) + 1) / 2), horizon - size.height * 0.08f)
        drawCircle(
            Brush.radialGradient(
                listOf(Ink.copy(alpha = 0.85f), Ink.copy(alpha = 0.4f), Color.Transparent),
                center = smudge,
                radius = size.minDimension * 0.16f,
            ),
            radius = size.minDimension * 0.16f,
            center = smudge,
        )
        drawCircle(
            RedPencil.copy(alpha = 0.8f),
            radius = size.minDimension * 0.22f,
            center = smudge,
            style = Stroke(2.dp.toPx()),
        )
        halftone()
        drawRect(Ink, style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.drawScenery(scene: Scene, region: Region, horizon: Float, seed: Int) {
    val dark = Color(0xFF3A372F)
    val mid = Color(0xFF6E6A60)

    // The far horizon behind open country, in the region's own shape.
    fun distance(height: Float) = when (region) {
        Region.DESERT -> drawMesas(horizon, height * 1.6f, mid, seed)
        Region.PLAINS, Region.MIDWEST -> drawHills(horizon, height * 0.25f, mid, seed)
        Region.HAWAII -> drawVolcano(horizon, height * 2.4f, mid)
        else -> drawHills(horizon, height, mid, seed)
    }
    when (scene) {
        Scene.TIMBER, Scene.PINE_BARRENS -> {
            val short = scene == Scene.PINE_BARRENS
            val count = if (short) 9 else 7
            for (i in 0..count) {
                val x = size.width * i / count + paperNoise(seed, i) * 8.dp.toPx()
                val h = size.height * (if (short) 0.22f else 0.42f) * (0.7f + 0.3f * (paperNoise(seed, i, 2) + 1))
                val w = h * (if (short) 0.6f else 0.42f)
                val color = if (i % 2 == 0) dark else mid
                val shape = when {
                    short -> "pine"
                    region == Region.HAWAII -> "palm"
                    region == Region.MIDWEST || region == Region.EAST || region == Region.PLAINS -> "round"
                    region == Region.SOUTH && i % 2 == 1 -> "round"
                    else -> "pine"
                }
                when (shape) {
                    "round" -> {
                        drawLine(color, Offset(x, horizon + 2), Offset(x, horizon - h * 0.5f), w * 0.12f)
                        drawOval(color, Offset(x - w * 0.65f, horizon - h), Size(w * 1.3f, h * 0.7f))
                    }
                    "palm" -> {
                        drawLine(color, Offset(x, horizon + 2), Offset(x + w * 0.3f, horizon - h), w * 0.08f)
                        for (k in -2..2) {
                            drawLine(
                                color,
                                Offset(x + w * 0.3f, horizon - h),
                                Offset(x + w * 0.3f + k * w * 0.35f, horizon - h + abs(k) * h * 0.12f + h * 0.05f),
                                w * 0.07f,
                            )
                        }
                    }
                    else -> drawPath(
                        Path().apply {
                            moveTo(x, horizon - h)
                            lineTo(x + w / 2, horizon + 2)
                            lineTo(x - w / 2, horizon + 2)
                            close()
                        },
                        color,
                    )
                }
            }
            if (short) {
                for (i in 0..24) {
                    val x = size.width * i / 24f
                    drawLine(dark, Offset(x, size.height), Offset(x + 3.dp.toPx(), size.height * 0.8f), 1.dp.toPx())
                }
            }
        }
        Scene.LAKE_SHORE -> {
            distance(size.height * 0.1f)
            drawRect(Color(0xFF9C988C), topLeft = Offset(0f, horizon), size = Size(size.width, size.height * 0.3f))
            for (i in 1..5) {
                val y = horizon + size.height * 0.05f * i
                drawLine(mid, Offset(size.width * 0.1f * i, y), Offset(size.width * (0.4f + 0.1f * i), y), 1.dp.toPx())
            }
        }
        Scene.PASTURE -> {
            distance(size.height * 0.05f)
            val posts = 6
            for (i in 0..posts) {
                val x = size.width * i / posts
                drawLine(dark, Offset(x, horizon + size.height * 0.08f), Offset(x, size.height * 0.92f), 3.dp.toPx())
            }
            drawLine(dark, Offset(0f, size.height * 0.75f), Offset(size.width, size.height * 0.72f), 2.dp.toPx())
        }
        Scene.RIDGE -> if (region == Region.DESERT) {
            drawMesas(horizon, size.height * 0.28f, mid, seed)
        } else {
            // Two bands of broad, uneven peaks: a pale far range and a dark near ridge.
            listOf(mid to 0.3f, dark to 0.16f).forEachIndexed { band, (color, height) ->
                val peaks = Path().apply {
                    moveTo(0f, horizon)
                    val count = 3 + band
                    for (i in 0..count) {
                        val x = size.width * (i + 0.2f * paperNoise(seed, i, band)) / count
                        lineTo(x, horizon - size.height * height * (1f + 0.4f * paperNoise(seed, i, band + 4)))
                        lineTo(x + size.width / count / 2, horizon - size.height * height * 0.35f)
                    }
                    lineTo(size.width, horizon)
                    close()
                }
                drawPath(peaks, color)
            }
        }
        Scene.RIVER_TOWN -> {
            var x = 0f
            var i = 0
            while (x < size.width) {
                val w = size.width * (0.12f + 0.06f * (paperNoise(seed, i) + 1))
                val h = size.height * (0.18f + 0.12f * (paperNoise(seed, i, 3) + 1))
                drawRect(if (i % 2 == 0) dark else mid, Offset(x, horizon - h), Size(w - 2.dp.toPx(), h + 2))
                x += w
                i++
            }
        }
    }
}

/** Flat-topped mesas with sheer sides along the horizon. */
private fun DrawScope.drawMesas(horizon: Float, height: Float, color: Color, seed: Int) {
    var x = -size.width * 0.05f
    var i = 0
    while (x < size.width) {
        val w = size.width * (0.18f + 0.1f * (paperNoise(seed, i, 11) + 1))
        if (i % 2 == 0) {
            val top = horizon - height * (0.6f + 0.2f * (paperNoise(seed, i, 12) + 1))
            drawPath(
                Path().apply {
                    moveTo(x, horizon)
                    lineTo(x + w * 0.12f, top)
                    lineTo(x + w * 0.88f, top)
                    lineTo(x + w, horizon)
                    close()
                },
                color,
            )
        }
        x += w
        i++
    }
}

/** A broad volcano rising from the sea. */
private fun DrawScope.drawVolcano(horizon: Float, height: Float, color: Color) {
    drawPath(
        Path().apply {
            moveTo(size.width * 0.05f, horizon)
            quadraticTo(size.width * 0.3f, horizon - height * 1.05f, size.width * 0.45f, horizon - height)
            quadraticTo(size.width * 0.62f, horizon - height * 1.05f, size.width * 0.95f, horizon)
            close()
        },
        color,
    )
}

private fun DrawScope.drawHills(horizon: Float, height: Float, color: Color, seed: Int) {
    drawPath(
        Path().apply {
            moveTo(0f, horizon)
            for (i in 0..4) {
                val x0 = size.width * i / 4
                quadraticTo(
                    x0 + size.width / 8,
                    horizon - height * (1 + paperNoise(seed, i, 9)),
                    x0 + size.width / 4,
                    horizon,
                )
            }
            close()
        },
        color,
    )
}

/** Overprints the photo with a grid of dots, like cheap newspaper printing. */
private fun DrawScope.halftone() {
    val step = 3.5.dp.toPx()
    val (width, height) = size
    val dots = buildList {
        var y = step / 2
        var row = 0
        while (y < height) {
            var x = if (row % 2 == 0) step / 2 else step
            while (x < width) {
                add(Offset(x, y))
                x += step
            }
            y += step
            row++
        }
    }
    drawPoints(dots, PointMode.Points, Ink.copy(alpha = 0.22f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
}
