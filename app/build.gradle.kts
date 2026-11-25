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
        versionCode = 1
        versionName = "0.1.0"

        // MET Norway blocks weather requests whose User-Agent has no contact. With an empty contact,
        // the game skips the weather and shows clear skies.
        buildConfigField("String", "GAME_CONTACT", "\"$gameContact\"")
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
}
