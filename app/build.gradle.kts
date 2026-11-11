import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Release signing.
 *
 * A release APK signed with the debug key is worse than unsigned: the Android debug keystore is
 * generated per machine, so a CI-built "release" gets a brand new key on every run and users can
 * never install an update over the previous version. Signing material is therefore read from
 * `keystore.properties` (local) or the matching environment variables (CI), and the build fails
 * loudly at runtime use-time rather than silently shipping a dead-end APK.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

fun signingValue(key: String): String? =
    (keystoreProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = signingValue("storeFile")
val hasReleaseSigning = releaseStoreFilePath != null &&
    rootProject.file(releaseStoreFilePath).exists() &&
    signingValue("storePassword") != null &&
    signingValue("keyAlias") != null &&
    signingValue("keyPassword") != null

/**
 * Monotonic version code derived from the commit count.
 *
 * A constant `versionCode` means Android refuses every update after the first one is installed.
 */
fun gitCommitCount(): Int = runCatching {
    val process = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }
    process.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

val commitCount = System.getenv("VERSION_CODE")?.toIntOrNull() ?: gitCommitCount()

android {
    namespace = "com.shieldguard.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shieldguard.app"
        minSdk = 26
        targetSdk = 34
        versionCode = commitCount
        versionName = "1.0.$commitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = signingValue("storePassword")
                keyAlias = signingValue("keyAlias")
                keyPassword = signingValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "ShieldGuard: no release keystore configured. " +
                        "Falling back to the debug key - this build cannot be published or updated in place."
                )
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room Database ($0 local persistence)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager ($0 Background rule sync)
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Pure-JVM tests for the filtering engine. No emulator, no device.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
}
