plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.freeiperf3client.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeiperf3client.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            // Public sideload builds do not need Android debugger attachment.
            isDebuggable = false
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.03.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    testImplementation("junit:junit:4.13.2")
}
