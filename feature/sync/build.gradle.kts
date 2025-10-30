plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.metrolist.feature.sync"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.material3)
    implementation(libs.hilt.navigation)
    implementation(libs.viewmodel)
    implementation(libs.timber)
    implementation(libs.datastore)
}
