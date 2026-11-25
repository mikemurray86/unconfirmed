package app.unconfirmed.game.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.game.BuildConfig

private const val NAME_LIMIT = 80
private const val PLACE_LIMIT = 120
private const val DESCRIPTION_LIMIT = 1_000

/**
 * A note to the developer asking for a creature the game doesn't have yet. The game sends nothing
 * itself: it hands a prefilled email to the player's own email app, addressed to the game's
 * contact, and the player chooses whether to send it. Only shown when there's a contact.
 */
@Composable
fun RequestCryptidScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var handedOff by rememberSaveable { mutableStateOf(false) }
    var noEmailApp by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .desk()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InkButton("← Back", onBack, fill = Paper)
        PaperSheet(seed = 33, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .ruledLines(margin = 32.dp)
                    .padding(start = 40.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Request a cryptid", fontFamily = HandFont, fontSize = 32.sp, lineHeight = 36.sp)
                if (handedOff) {
                    Note(
                        "Your email app has the request ready. Send it from there, and I'll look " +
                            "into the sightings.",
                    )
                    InkButton("Write another", {
                        name = ""
                        place = ""
                        description = ""
                        handedOff = false
                    })
                    return@Column
                }
                Note(
                    "Heard of a creature that isn't in the game yet? Tell me about it. This opens " +
                        "your email app with the details filled in, and you choose whether to send it.",
                )
                Field("What's it called?", name, NAME_LIMIT, singleLine = true) { name = it }
                Field("Where is it seen? (state, town, lake...)", place, PLACE_LIMIT, singleLine = true) {
                    place = it
                }
                Field("What do people say about it?", description, DESCRIPTION_LIMIT, singleLine = false) {
                    description = it
                }
                if (noEmailApp) {
                    Text(
                        "There's no email app on this phone to send it with.",
                        fontFamily = HandFont,
                        fontSize = 17.sp,
                        color = RedPencil,
                    )
                }
                val ready = name.isNotBlank() && place.isNotBlank()
                InkButton(
                    if (ready) "Write the email" else "Name it and say where first",
                    onClick = {
                        if (!ready) return@InkButton
                        if (context.emailRequest(name.trim(), place.trim(), description.trim())) {
                            handedOff = true
                            noEmailApp = false
                        } else {
                            noEmailApp = true
                        }
                    },
                    fill = if (ready) Amber else Newsprint,
                )
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, fontFamily = HandFont, fontSize = 18.sp, lineHeight = 23.sp, color = BluePencil)
}

/** A label in ink, and the player's answer in blue pencil on an underline. */
@Composable
private fun Field(label: String, value: String, limit: Int, singleLine: Boolean, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontFamily = HandFont, fontSize = 18.sp)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(limit)) },
            singleLine = singleLine,
            textStyle = TextStyle(fontFamily = HandFont, fontSize = 20.sp, lineHeight = 26.sp, color = BluePencil),
            cursorBrush = SolidColor(BluePencil),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 32.dp else 96.dp)
                .drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(Ink.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                },
        )
    }
}

/**
 * Opens the player's email app with the request filled in. The details go in both the mailto link
 * and the extras, since email apps differ in which they read. False when there's no email app.
 */
private fun Context.emailRequest(name: String, place: String, description: String): Boolean {
    val to = BuildConfig.GAME_CONTACT
    val subject = "Cryptid request: $name"
    val body = buildString {
        appendLine("Name: $name")
        appendLine("Where: $place")
        appendLine()
        appendLine(description.ifEmpty { "(No description given.)" })
        appendLine()
        append("Sent from Unconfirmed ${BuildConfig.VERSION_NAME}")
    }
    val uri = Uri.parse("mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
