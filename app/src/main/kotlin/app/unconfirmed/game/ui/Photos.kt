package app.unconfirmed.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.unconfirmed.shared.Photo
import app.unconfirmed.shared.Region
import app.unconfirmed.shared.Scene
import app.unconfirmed.shared.SkyState
import java.text.DateFormat
import java.util.Date

/**
 * The square inside the viewfinder at the moment [photo] was taken. The whole scene is drawn
 * again at the size it was on screen, held still at that moment, and shrunk to fit, so the ink
 * lines keep their weight and the shot looks just as it did through the camera.
 */
@Composable
fun PhotoImage(photo: Photo, creatureId: String, scene: Scene, region: Region, modifier: Modifier = Modifier) {
    var side by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val frameWidth = photo.frameWidthDp.coerceAtLeast(1f)
    val frameHeight = photo.frameHeightDp.coerceAtLeast(1f)
    val sky = SkyState(photo.timeOfDay, photo.weather)
    Box(
        modifier
            .aspectRatio(1f)
            .clipToBounds()
            .onSizeChanged { side = it.width.toFloat() }
            .semantics { contentDescription = photoDescription(photo) },
        contentAlignment = Alignment.Center,
    ) {
        // The viewfinder sits dead in the middle of the screen, so the scene is centred and
        // scaled until the viewfinder's square fills this one.
        val scale = with(density) { side / (frameWidth.dp.toPx() * VIEWFINDER_WIDTH) }
        Box(
            Modifier
                .requiredSize(frameWidth.dp, frameHeight.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            FirstPersonScene(
                scene = scene,
                region = region,
                sky = sky,
                headingDegrees = photo.headingDegrees,
                walkedMeters = 0.0,
                hintBearing = null,
                modifier = Modifier.fillMaxSize(),
                creature = sceneCreature(
                    creatureId,
                    bearing = photo.creatureBearing,
                    distance = photo.distanceMeters,
                    visibility = photo.visibility,
                    movingRight = photo.movingRight,
                    stride = photo.stride,
                ),
                frozenAt = photo.sceneSeconds.toFloat(),
            )
            AtmosphereOverlay(
                biome = scene.biome,
                sky = sky,
                modifier = Modifier.fillMaxSize(),
                frozenAt = photo.sceneSeconds.toFloat(),
            )
        }
    }
}

/**
 * A print of [photo], with a white border and a pencilled caption, as it's taped into the
 * journal. Tapping it opens it up large, when [onOpen] is given.
 */
@Composable
fun PhotoPrint(
    photo: Photo,
    creatureId: String,
    scene: Scene,
    region: Region,
    width: Dp,
    modifier: Modifier = Modifier,
    caption: String? = shotCaption(photo.score),
    onOpen: (() -> Unit)? = null,
) {
    val open = if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier
    PaperSheet(
        fill = Card,
        seed = photo.takenAtMillis.toInt(),
        shadow = 3.dp,
        modifier = modifier.width(width).then(open),
    ) {
        Column(
            Modifier.padding(start = width * 0.06f, end = width * 0.06f, top = width * 0.06f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box {
                PhotoImage(photo, creatureId, scene, region, Modifier.fillMaxWidth())
                Canvas(Modifier.matchParentSize()) { drawRect(Ink, style = Stroke(1.dp.toPx())) }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1f / 0.14f), contentAlignment = Alignment.Center) {
                if (caption != null && width >= 110.dp) {
                    Text(
                        caption,
                        fontFamily = HandFont,
                        fontSize = (width.value * 0.075f).coerceIn(12f, 20f).sp,
                        color = BluePencil,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** [photo] blown up to fill most of the screen, with when it was taken. Tap anywhere to close. */
@Composable
fun PhotoViewer(photo: Photo, creatureId: String, scene: Scene, region: Region, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onClose),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoPrint(photo, creatureId, scene, region, width = 300.dp)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(photo.takenAtMillis)) +
                    " · ${photo.score} of 100",
                fontFamily = FieldFont,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/** What the player might pencil under a shot. */
fun shotCaption(score: Int): String = when {
    score >= 75 -> "Got it!"
    score >= 35 -> "There it is?"
    score > 0 -> "Something..."
    else -> "Nothing"
}

private fun photoDescription(photo: Photo): String = when {
    photo.score >= 75 -> "A clear photo of the creature"
    photo.score >= 35 -> "A blurry photo of the creature"
    photo.score > 0 -> "A photo with something hard to make out in it"
    else -> "A photo with nothing in it"
}
