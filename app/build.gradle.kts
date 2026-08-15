plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.leemmcc.hallpass"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.leemmcc.hallpass"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
