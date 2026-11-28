import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

base {
    archivesName = "unconfirmed"
}

// The upload key's details live in keystore.properties at the project root, which is never committed.
// Without it, release builds come out unsigned and debug builds are unaffected. See README.md.
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

// Debug and release builds take the game's contact address from the GAME_CONTACT environment
// variable, so no address is ever written into the project. MET Norway gets it with each weather
// request, and players write to it from Request a cryptid. See README.md.
val gameContact = providers.environmentVariable("GAME_CONTACT").orNull.orEmpty().replace("\"", "")

android {
    namespace = "app.unconfirmed.game"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.unconfirmed.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.1"

        // MET Norway blocks weather requests whose User-Agent has no contact. With an empty contact,
        // the game skips the weather and shows clear skies.
        buildConfigField("String", "GAME_CONTACT", "\"$gameContact\"")

        // The AdMob app ID is public: it goes in the manifest of every build.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-2671069014341671~2362246826"
        // The real banner unit. Debug builds use Google's test unit instead, because tapping your own
        // live ads can get the AdMob account suspended.
        buildConfigField("String", "AD_UNIT_ID", "\"ca-app-pub-2671069014341671/2863588518\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Compose Preview needs compiled resources, and AGP won't create its tasks without this.
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    // AGP turns off unit tests for release by default, but Compose Preview needs them in every variant.
    beforeVariants { (it as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.androidx.fragment)
}
