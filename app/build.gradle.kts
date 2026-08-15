plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Version comes from the release tag when there is one, so the tablet reports
 * the build that is actually on the wall. Ordinary CI runs have no tag, no env
 * vars, and fall back to the values below.
 *
 * Overridable by env var (what the release workflow uses) or Gradle property.
 */
val defaultVersionName = "0.1.0"
val defaultVersionCode = 1

fun leadingInt(part: String?): Int = part?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

/**
 * major.minor.patch -> a single monotonically increasing integer, on the
 * assumption that minor and patch stay under 100. Google Play would reject a
 * regression here; sideloading just silently refuses the update, which is
 * worse, so the derivation has to be ordering-safe by construction.
 */
fun versionCodeFrom(name: String): Int {
    val parts = name.split('.')
    return leadingInt(parts.getOrNull(0)) * 10_000 +
        leadingInt(parts.getOrNull(1)) * 100 +
        leadingInt(parts.getOrNull(2))
}

val overriddenVersionName: String? =
    System.getenv("HALLPASS_VERSION_NAME")?.takeIf { it.isNotBlank() }
        ?: (project.findProperty("hallpassVersionName") as String?)?.takeIf { it.isNotBlank() }

val resolvedVersionName: String = overriddenVersionName ?: defaultVersionName

val resolvedVersionCode: Int =
    System.getenv("HALLPASS_VERSION_CODE")?.toIntOrNull()
        ?: (project.findProperty("hallpassVersionCode") as String?)?.toIntOrNull()
        ?: overriddenVersionName?.let { versionCodeFrom(it) }
        ?: defaultVersionCode

android {
    namespace = "io.github.leemmcc.hallpass"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.leemmcc.hallpass"
        // 21, not 26. The classroom tablet turned out to run Android 7 or
        // older, and an APK whose minSdkVersion exceeds the device's API level
        // fails to install with "There was a problem parsing the package" --
        // an error that says nothing about the actual cause.
        //
        // 21 is the floor: Theme.Material, which every activity uses, arrived
        // there. Nothing else in the app needs anything newer, given the
        // API 23 guard in MainActivity.requestLockTask and the non-adaptive
        // launcher icon in res/mipmap/.
        minSdk = 21
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("HALLPASS_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("HALLPASS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HALLPASS_KEY_ALIAS")
                keyPassword = System.getenv("HALLPASS_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Falls back to unsigned when the env vars are absent, so CI test
            // runs and local inspection do not require the keystore.
            if (System.getenv("HALLPASS_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
