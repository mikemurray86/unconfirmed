package app.unconfirmed.game.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True when the window has room to set things side by side rather than one above the other: a
 * phone on its side, a tablet or an unfolded foldable in landscape, or a wide tablet either way up.
 */
@Composable
fun sideBySide(): Boolean {
    val config = LocalConfiguration.current
    val width = config.screenWidthDp
    return width >= 600 && (width > config.screenHeightDp || width >= 840)
}

/** A comfortable reading width for a column of text, so lines don't run across a whole tablet. */
val READING_WIDTH = 600.dp

/** Holds what follows to at most [max] across, centred in the space it's given. */
fun Modifier.readable(max: Dp = READING_WIDTH): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

/**
 * Keeps clear of the status bar and of a camera cutout, which on a phone on its side is at the
 * left or right rather than the top.
 */
@Composable
fun Modifier.safeTopPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))

/** Keeps clear of the navigation bar and a camera cutout, wherever they are. */
@Composable
fun Modifier.safeBottomPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
