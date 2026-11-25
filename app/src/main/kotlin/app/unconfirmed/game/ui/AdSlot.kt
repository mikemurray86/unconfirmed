package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The ad space on the newspaper's front page: the width of the page's column, up to 320 dp, and
 * 100 dp tall, the size of an AdMob large banner. On a narrow phone the column is less than
 * 320 dp across, so the box narrows with it.
 *
 * There are no real ads yet, so it shows [PlaceholderAd]: a made-up classified in the paper's own
 * style. To serve AdMob later, put an `AndroidView` wrapping an `AdView` where the placeholder is,
 * sized with `AdSize.getInlineAdaptiveBannerAdSize(width, AD_HEIGHT)` for the box's width, and
 * keep the placeholder as what shows while an ad loads or when none fills. The "Advertisement" label and the rules either side stay, so
 * the ad is never mistaken for a case and sits well clear of the stories' tap targets.
 */
@Composable
fun AdSlot(modifier: Modifier = Modifier, index: Int = 0) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "ADVERTISEMENT",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 2.sp,
            color = Ink.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(Modifier.widthIn(max = AD_MAX_WIDTH.dp).fillMaxWidth().height(AD_HEIGHT.dp)) {
            PlaceholderAd(index)
        }
    }
}

/** The size of an AdMob large banner, in dp. */
private const val AD_MAX_WIDTH = 320
private const val AD_HEIGHT = 100

/** A made-up ad for the paper's readers, with a little ink drawing of what's on offer. */
private class Classified(val headline: String, val body: String, val footer: String, val picture: DrawScope.() -> Unit)

private val CLASSIFIEDS = listOf(
    Classified(
        "BIG-FOOT BOOTS",
        "Size 24 soles strap on over your own. Confound your neighbors! Fool the experts!",
        "Write to Box 19, Willow Creek",
        DrawScope::drawBoot,
    ),
    Classified(
        "SEE IN THE DARK!",
        "Genuine Army-surplus night glasses. Owls hate them. Satisfaction or your dime back.",
        "Two pair for \$5 · Box 7, Point Pleasant",
        DrawScope::drawBinoculars,
    ),
    Classified(
        "PLASTER OF PARIS",
        "Cast any track in minutes. Bulk sacks for the serious investigator. Ask about our ruler.",
        "Hollis & Sons Hardware · Since 1931",
        DrawScope::drawSack,
    ),
    Classified(
        "LOST: ONE GOAT",
        "Answers to Gerald. Last seen walking upright toward the dairy. Do not approach.",
        "Reward · Ask at the feed store",
        DrawScope::drawQuestion,
    ),
)

/**
 * One of the made-up classifieds. Each paper in the pile gets the next one along from [index], and
 * the run starts somewhere new each day, so the same paper keeps the same ad between visits.
 */
@Composable
private fun PlaceholderAd(index: Int) {
    val ad = remember(index) {
        val day = System.currentTimeMillis() / 86_400_000L
        CLASSIFIEDS[Math.floorMod(day + index, CLASSIFIEDS.size.toLong()).toInt()]
    }
    Row(
        Modifier
            .fillMaxSize()
            .border(2.dp, Ink)
            .padding(2.dp)
            .border(0.5.dp, Ink)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .semantics { contentDescription = "Advertisement: ${ad.headline}. ${ad.body}" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.width(64.dp).fillMaxHeight()) { ad.picture(this) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                ad.headline,
                fontFamily = HeadlineFont,
                fontSize = 16.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                ad.body,
                fontFamily = FontFamily.Serif,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                maxLines = 3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                ad.footer,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---- Pictures for the classifieds, in plain ink like a woodcut --------------------------------

private fun DrawScope.ink(path: Path, fill: Boolean = false) {
    if (fill) drawPath(path, Ink)
    drawPath(path, Ink, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawBoot() {
    val w = size.width
    val h = size.height
    val boot = Path().apply {
        moveTo(w * 0.30f, h * 0.10f)
        lineTo(w * 0.62f, h * 0.10f)
        lineTo(w * 0.62f, h * 0.55f)
        quadraticTo(w * 0.98f, h * 0.60f, w * 0.96f, h * 0.80f)
        lineTo(w * 0.96f, h * 0.90f)
        lineTo(w * 0.10f, h * 0.90f)
        lineTo(w * 0.10f, h * 0.70f)
        quadraticTo(w * 0.30f, h * 0.60f, w * 0.30f, h * 0.10f)
        close()
    }
    ink(boot)
    // Toes stuck on the front, and the laces.
    for (k in 0 until 4) drawCircle(Ink, w * 0.04f, Offset(w * (0.60f + 0.09f * k), h * 0.93f))
    for (k in 0 until 4) {
        val y = h * (0.20f + 0.09f * k)
        drawLine(Ink, Offset(w * 0.36f, y), Offset(w * 0.56f, y + h * 0.04f), 1.dp.toPx())
    }
    drawRect(Ink, Offset(w * 0.10f, h * 0.86f), Size(w * 0.86f, h * 0.04f))
}

private fun DrawScope.drawBinoculars() {
    val w = size.width
    val h = size.height
    for (x in listOf(0.28f, 0.72f)) {
        drawRect(Ink, Offset(w * (x - 0.13f), h * 0.22f), Size(w * 0.26f, h * 0.30f), style = Stroke(1.5.dp.toPx()))
        drawCircle(Ink, w * 0.20f, Offset(w * x, h * 0.66f), style = Stroke(1.5.dp.toPx()))
        drawCircle(Ink, w * 0.12f, Offset(w * x, h * 0.66f))
        drawCircle(Paper, w * 0.04f, Offset(w * (x - 0.05f), h * 0.61f))
    }
    drawRect(Ink, Offset(w * 0.42f, h * 0.30f), Size(w * 0.16f, h * 0.10f))
}

private fun DrawScope.drawSack() {
    val w = size.width
    val h = size.height
    val sack = Path().apply {
        moveTo(w * 0.30f, h * 0.18f)
        lineTo(w * 0.70f, h * 0.18f)
        quadraticTo(w * 0.92f, h * 0.55f, w * 0.82f, h * 0.92f)
        lineTo(w * 0.18f, h * 0.92f)
        quadraticTo(w * 0.08f, h * 0.55f, w * 0.30f, h * 0.18f)
        close()
    }
    ink(sack)
    drawLine(Ink, Offset(w * 0.26f, h * 0.24f), Offset(w * 0.74f, h * 0.24f), 2.dp.toPx())
    // A footprint on the label.
    drawOval(Ink, Offset(w * 0.40f, h * 0.48f), Size(w * 0.20f, h * 0.32f))
    for (k in 0 until 4) drawCircle(Ink, w * 0.03f, Offset(w * (0.40f + 0.065f * k), h * 0.44f))
}

private fun DrawScope.drawQuestion() {
    val w = size.width
    val h = size.height
    val mark = Path().apply {
        moveTo(w * 0.30f, h * 0.32f)
        cubicTo(w * 0.30f, h * 0.05f, w * 0.75f, h * 0.05f, w * 0.72f, h * 0.32f)
        cubicTo(w * 0.70f, h * 0.48f, w * 0.50f, h * 0.48f, w * 0.50f, h * 0.68f)
    }
    drawPath(mark, Ink, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    drawCircle(Ink, 4.dp.toPx(), Offset(w * 0.50f, h * 0.86f))
}
