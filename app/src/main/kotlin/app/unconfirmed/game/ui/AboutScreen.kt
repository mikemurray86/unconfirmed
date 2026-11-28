package app.unconfirmed.game.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unconfirmed.game.Ads
import app.unconfirmed.game.BuildConfig

private class FontCredit(val credit: String, val licenseAsset: String, val licenseName: String)

private val FONTS = listOf(
    FontCredit(
        "Special Elite by Astigmatic, licensed under the Apache License, Version 2.0.",
        "licenses/special-elite-apache-2.0.txt",
        "Apache License 2.0",
    ),
    FontCredit(
        "Abril Fatface by TypeTogether, licensed under the SIL Open Font License, Version 1.1.",
        "licenses/abril-fatface-ofl-1.1.txt",
        "Open Font License for Abril Fatface",
    ),
    FontCredit(
        "UnifrakturMaguntia by j. 'mach' wust and Peter Wiegel, licensed under the SIL Open Font License, Version 1.1.",
        "licenses/unifraktur-maguntia-ofl-1.1.txt",
        "Open Font License for UnifrakturMaguntia",
    ),
    FontCredit(
        "Patrick Hand by Patrick Wagesreiter, licensed under the SIL Open Font License, Version 1.1.",
        "licenses/patrick-hand-ofl-1.1.txt",
        "Open Font License for Patrick Hand",
    ),
)

/** Version, data credits and open-source licences. MET Norway's licence requires the weather credit. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var showingLicense by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler { if (showingLicense != null) showingLicense = null else onBack() }
    val links = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .readable()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        showingLicense?.let { asset ->
            TextButton(onClick = { showingLicense = null }) { Text("Back") }
            LicenseText(asset)
            return@Column
        }
        TextButton(onClick = onBack) { Text("Back") }
        Text(
            "About Unconfirmed",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)

        Heading("Weather")
        Text(
            "Weather data from MET Norway (the Norwegian Meteorological Institute), licensed under CC BY 4.0.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { links.openUri("https://api.met.no/") }) { Text("api.met.no") }

        // Where the law gives players the right to change their ad choices later, they do it here.
        val activity = LocalActivity.current
        if (Ads.privacyOptionsRequired && activity != null) {
            Heading("Ads")
            TextButton(onClick = { Ads.showPrivacyOptions(activity) }) { Text("Privacy choices for ads") }
        }

        Heading("Fonts")
        FONTS.forEach { font ->
            Text(font.credit, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showingLicense = font.licenseAsset }) { Text("Read the ${font.licenseName}") }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun LicenseText(asset: String) {
    val context = LocalContext.current
    val text = remember(asset) {
        runCatching { context.assets.open(asset).bufferedReader().use { it.readText() } }
            .getOrDefault("The license text couldn't be loaded.")
    }
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
}
