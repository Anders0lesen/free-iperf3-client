plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.freeiperf3client.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeiperf3client.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
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

    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
