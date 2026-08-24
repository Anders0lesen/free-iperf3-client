plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "net.olesens.freeiperf3client"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.olesens.freeiperf3client"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.synaptic-tools:iperf:1.0.0")
}
